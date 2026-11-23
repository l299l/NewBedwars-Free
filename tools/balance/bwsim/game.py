import math
from dataclasses import dataclass, field

import numpy as np

from .agents import MILESTONES, POLICIES
from .income import IslandGenerator, TeamGenerator, island_rate, team_rates
from .load import RESOURCES_ORDER

INF = math.inf


@dataclass
class Layout:
    teams: int
    team_size: int
    diamond_gens: int
    emerald_gens: int
    diamond_share: float
    emerald_share: float
    collect_interval: float
    island_interval: float
    death_interval: float
    carry_loss: float
    policy: str = "balanced"
    share_sigma: float = 0.35


@dataclass
class GameResult:
    milestones: dict
    bed_time: float
    end_time: float
    banked_at_beds: dict
    spent: dict = field(default_factory=dict)


def _phase_schedule(phases):
    t, events, bed_time = 0, [], INF
    for p in phases:
        events.append((t, p.actions))
        if any(a.upper().startswith("BED_DESTRUCTION") for a in p.actions):
            bed_time = min(bed_time, t)
        if p.duration <= 0:
            break
        t += p.duration
    end = t
    for at, actions in events:
        if any(a.upper().startswith(("GAME_END", "END_GAME")) for a in actions):
            end = min(end, at)
    return events, bed_time, end


class Team:
    def __init__(self, gens, prices, layout, rng):
        self.gens = gens
        self.prices = prices
        self.layout = layout
        self.rng = rng
        self.wallet = dict.fromkeys(RESOURCES_ORDER, 0.0)
        self.pile = dict.fromkeys(RESOURCES_ORDER, 0.0)
        self.island_pile = dict.fromkeys(RESOURCES_ORDER, 0.0)
        self.owned = {}
        self.levels = {}
        self.next_due = {}
        self.milestones = dict.fromkeys(MILESTONES, INF)
        self.spent = dict.fromkeys(RESOURCES_ORDER, 0.0)
        self.generator = TeamGenerator(gens.team, rng)
        self.goals = POLICIES[layout.policy]

    def mark(self, name, t):
        if name and self.milestones.get(name, INF) == INF:
            self.milestones[name] = t

    def pay(self, price, currency):
        if currency in ("None", "Other") or price <= 0:
            return True
        if self.wallet[currency] + 1e-9 < price:
            return False
        self.wallet[currency] -= price
        self.spent[currency] += price
        return True

    def forge_changed(self, t):
        self.generator.level = self.levels.get("FORGE", 0) + 1
        _, amounts, _ = self.gens.team.level(self.generator.level)
        rates, _, _ = self.gens.team.level(self.generator.level)
        if rates["Emerald"] > 0 and amounts["Emerald"] > 0:
            self.mark("emerald_forge", t)

    def shop(self, t):
        blocked = set()
        n = self.layout.team_size
        for g in self.goals:
            if g.kind == "upgrade":
                tiers = self.prices.upgrades.get(g.name, [])
                cur = self.levels.get(g.name, 0)
                if cur >= g.level or g.level > len(tiers):
                    continue
                price, currency = tiers[cur]
                if currency in blocked:
                    continue
                if self.pay(price, currency):
                    self.levels[g.name] = cur + 1
                    if g.level == cur + 1:
                        self.mark(g.milestone, t)
                    if g.name == "PROTECTION" and cur + 1 == len(tiers):
                        self.mark("protection_max", t)
                    if g.name == "FORGE":
                        self.forge_changed(t)
                else:
                    blocked.add(currency)
                continue
            if g.name not in self.prices.items:
                continue
            price, currency, _ = self.prices.items[g.name]
            if currency in blocked:
                continue
            need = n if g.per_player else 1
            if g.repeat:
                if self.next_due.get(g.name, 0) > t:
                    continue
                bought = 0
                while bought < need and self.pay(price, currency):
                    bought += 1
                if bought == need:
                    self.next_due[g.name] = t + g.repeat
                    self.mark(g.milestone, t)
                continue
            have = self.owned.get(g.name, 0)
            if have >= need:
                continue
            while have < need and self.pay(price, currency):
                have += 1
            self.owned[g.name] = have
            if have >= need:
                self.mark(g.milestone, t)
            else:
                blocked.add(currency)


def simulate(gens, phases, prices, layout, rng, fast=False):
    events, bed_time, end_time = _phase_schedule(phases)
    team = Team(gens, prices, layout, rng)
    diamonds = [IslandGenerator(gens.diamond, "Diamond") for _ in range(layout.diamond_gens)]
    emeralds = [IslandGenerator(gens.emerald, "Emerald") for _ in range(layout.emerald_gens)]
    skill = 1.0 if fast else float(np.exp(rng.normal(0, layout.share_sigma)))
    d_share = min(1.0, layout.diamond_share * skill)
    e_share = min(1.0, layout.emerald_share * skill)
    death_p = 1.0 / layout.death_interval if layout.death_interval > 0 else 0.0
    banked = None
    ev = 0
    next_collect = rng.uniform(0, layout.collect_interval)
    next_island = rng.uniform(0, layout.island_interval)
    horizon = int(min(end_time, 3600))
    for t in range(1, horizon + 1):
        while ev < len(events) and events[ev][0] <= t - 1:
            for action in events[ev][1]:
                name, _, arg = action.partition(":")
                name = name.upper()
                if name in ("DIAMOND_GENERATOR", "EMERALD_GENERATOR") and arg:
                    lvl = int(arg)
                    pool = diamonds if name.startswith("DIAMOND") else emeralds
                    for gen in pool:
                        gen.level = max(gen.level, min(lvl, gen.settings.max_level))
                elif name == "DISABLE_TEAMS_FORGE":
                    team.generator.level = 1
            ev += 1
        if fast:
            for r, v in team_rates(gens.team, team.generator.level).items():
                team.pile[r] += v
            for gen in diamonds:
                team.island_pile["Diamond"] += island_rate(gen.settings, gen.level) * d_share
            for gen in emeralds:
                team.island_pile["Emerald"] += island_rate(gen.settings, gen.level) * e_share
        else:
            for r, v in team.generator.step(1.0).items():
                team.pile[r] += v
            for gen in diamonds:
                drop = gen.step()
                if drop and rng.random() < d_share:
                    team.island_pile["Diamond"] += drop
            for gen in emeralds:
                drop = gen.step()
                if drop and rng.random() < e_share:
                    team.island_pile["Emerald"] += drop
        if t >= next_collect:
            for r in RESOURCES_ORDER:
                team.wallet[r] += team.pile[r]
                team.pile[r] = 0.0
            next_collect = t + layout.collect_interval
            team.shop(t)
        if t >= next_island:
            for r in RESOURCES_ORDER:
                team.wallet[r] += team.island_pile[r]
                team.island_pile[r] = 0.0
            next_island = t + layout.island_interval
            team.shop(t)
        if death_p:
            if fast:
                frac = layout.carry_loss * death_p
            else:
                frac = layout.carry_loss * rng.binomial(layout.team_size, death_p) / layout.team_size
            if frac:
                for r in RESOURCES_ORDER:
                    team.wallet[r] *= (1 - frac)
        if banked is None and t >= bed_time:
            banked = dict(team.wallet)
    return GameResult(team.milestones, bed_time, end_time, banked or dict(team.wallet), team.spent)
