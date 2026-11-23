import argparse
import copy
import math
import sys
from pathlib import Path

import numpy as np
import yaml

sys.path.insert(0, str(Path(__file__).resolve().parent))

from bwsim.game import simulate
from bwsim.load import RESOURCES, generators_from_dict, load_prices, parse_phases
from sim import MODES, TARGETS, layout_for, run, score

HERE = Path(__file__).resolve().parent
PRESETS = HERE / "presets"
BASE_GENERATORS = yaml.safe_load((RESOURCES / "defaultGenerators.yml").read_text(encoding="utf-8"))
BASE_UPGRADES = yaml.safe_load((HERE / "base" / "upgrades.yml").read_text(encoding="utf-8"))["Upgrades"]
BASE_ITEMS = load_prices().items

PARAMS = {
    "team": (0.4, 4.0),
    "diamond": (0.6, 2.5),
    "emerald": (0.6, 2.5),
    "upgrades": (0.4, 4.0),
    "forge": (0.4, 4.0),
    "armor": (0.5, 2.0),
    "step": (0.5, 1.5),
}
START = {k: 1.0 for k in PARAMS}
FIXED_ISLAND = {"rush": {"DiamondGenerator": [24.0, 16.0, 10.0]}}
TIER_CAP = {"duels": {"PROTECTION": 3, "FORGE": 5}}
WEIGHT = {"diamond": 0.15, "emerald": 0.15, "step": 0.1}

SHAPE = {
    "solo": ("normal", 300),
    "doubles": ("normal", 300),
    "squads": ("normal", 300),
    "rush": ("rush", 180),
    "duels": ("duels", 160),
}


def phases_for(shape, step):
    s = max(30, int(round(step / 10.0) * 10))
    if shape == "normal":
        return [
            {"Name": "Starting phase", "Duration": s, "Actions": []},
            {"Name": "Diamonds II", "Duration": s, "Actions": ["DIAMOND_GENERATOR:2"]},
            {"Name": "Emeralds II", "Duration": s, "Actions": ["EMERALD_GENERATOR:2"]},
            {"Name": "Diamonds III", "Duration": s, "Actions": ["DIAMOND_GENERATOR:3"]},
            {"Name": "Emeralds III", "Duration": s, "Actions": ["EMERALD_GENERATOR:3"]},
            {"Name": "Bed destruction", "Duration": s, "Actions": ["BED_DESTRUCTION", "DISABLE_REGENERATION"]},
            {"Name": "Sudden death", "Duration": s, "Actions": ["ENDER_DRAGON_START"]},
            {"Name": "Game end", "Duration": 0, "Actions": ["ENDER_DRAGON_END", "GAME_END:DRAW"]},
        ]
    if shape == "rush":
        return [
            {"Name": "Starting phase", "Duration": s, "Actions": []},
            {"Name": "Upgrade I", "Duration": s, "Actions": ["DIAMOND_GENERATOR:2", "EMERALD_GENERATOR:2"]},
            {"Name": "Upgrade II", "Duration": s, "Actions": ["DIAMOND_GENERATOR:3", "EMERALD_GENERATOR:3"]},
            {"Name": "Bed destruction", "Duration": s, "Actions": ["BED_DESTRUCTION", "DISABLE_REGENERATION"]},
            {"Name": "Sudden death", "Duration": s, "Actions": ["ENDER_DRAGON_START"]},
            {"Name": "Game end", "Duration": 0, "Actions": ["ENDER_DRAGON_END", "GAME_END:MOST_KILLS"]},
        ]
    return [
        {"Name": "Starting phase", "Duration": s, "Actions": []},
        {"Name": "Upgrade I", "Duration": s, "Actions": ["DIAMOND_GENERATOR:2", "EMERALD_GENERATOR:2"]},
        {"Name": "Upgrade II", "Duration": s, "Actions": ["DIAMOND_GENERATOR:3", "EMERALD_GENERATOR:3"]},
        {"Name": "Bed destruction", "Duration": int(1.5 * s), "Actions": ["BED_DESTRUCTION", "DISABLE_REGENERATION"]},
        {"Name": "Game end", "Duration": 0, "Actions": ["GAME_END:MOST_KILLS"]},
    ]


def _round_delay(v, island):
    return float(max(1, round(v))) if island else round(max(0.05, v) * 20) / 20


def generators_for(mode, p):
    g = copy.deepcopy(BASE_GENERATORS)
    g["ConfigurationId"] = mode.capitalize()
    g["TeamGenerator"]["SpawnDelay"] = [_round_delay(d / p["team"], False) for d in g["TeamGenerator"]["SpawnDelay"]]
    g["DiamondGenerator"]["SpawnDelay"] = [_round_delay(d / p["diamond"], True) for d in g["DiamondGenerator"]["SpawnDelay"]]
    g["EmeraldGenerator"]["SpawnDelay"] = [_round_delay(d / p["emerald"], True) for d in g["EmeraldGenerator"]["SpawnDelay"]]
    for key, delays in FIXED_ISLAND.get(mode, {}).items():
        g[key]["SpawnDelay"] = list(delays)
    return g


def _scale(price, k):
    return max(1, int(round(price * k)))


def prices_for(mode, p):
    upgrades = {}
    caps = TIER_CAP.get(mode, {})
    for name, tiers in BASE_UPGRADES.items():
        tiers = tiers[:caps.get(name, len(tiers))]
        k = p["forge"] if name == "FORGE" else p["upgrades"]
        if name in ("ALARMTRAP", "BLINDTRAP", "MININGFATIGUETRAP", "DRAGONBUFF"):
            k = min(k, 2.0)
        upgrades[name] = [{"price": _scale(t["price"], k), "currency": t.get("currency", "DIAMOND")} for t in tiers]
    items = {}
    for name in ("ChainmailArmor", "IronArmor", "DiamondArmor"):
        price, currency, _ = BASE_ITEMS[name]
        new = _scale(price, p["armor"])
        if new != price:
            items[name] = {"price": new, "currency": currency.upper()}
    return items, upgrades


def build(mode, p):
    shape, base_step = SHAPE[mode]
    gens = generators_for(mode, p)
    phases = phases_for(shape, base_step * p["step"])
    items, upgrades = prices_for(mode, p)
    return {"Generators": gens, "Phases": phases, "ItemPrices": items, "UpgradePrices": upgrades}


def evaluate(mode, preset, games=0, seed=3):
    gens = generators_from_dict(preset["Generators"])
    phases = parse_phases(preset["Phases"])
    prices = load_prices(preset["ItemPrices"], preset["UpgradePrices"])
    layout = layout_for(mode)
    rng = np.random.default_rng(seed)
    if games:
        results = [simulate(gens, phases, prices, layout, rng) for _ in range(games)]
    else:
        results = [simulate(gens, phases, prices, layout, rng, fast=True)]
    stats = {}
    for m in TARGETS[mode]:
        if m == "iron_per_player_min":
            from bwsim.income import team_rates
            v = team_rates(gens.team, 1)["Iron"] * 60 / layout.team_size
        elif m == "banked_at_beds":
            v = float(np.median([r.banked_at_beds["Diamond"] for r in results]))
        else:
            vals = np.array([r.milestones[m] for r in results], dtype=float)
            v = float(np.median(vals)) if not np.isinf(vals).sum() * 2 > len(vals) else math.inf
        stats[m] = (v, v, v)
    return score(mode, stats)[0]


def objective(mode, p, games):
    reg = sum(WEIGHT.get(k, 0.03) * abs(math.log(v)) for k, v in p.items())
    return evaluate(mode, build(mode, p), games) + reg


def tune(mode, games=0, rounds=6):
    p = dict(START)
    frozen = {"diamond"} if "DiamondGenerator" in FIXED_ISLAND.get(mode, {}) else set()
    best = objective(mode, p, games)
    factor = 1.5
    for _ in range(rounds):
        improved = True
        while improved:
            improved = False
            for key, (lo, hi) in PARAMS.items():
                if key in frozen:
                    continue
                for f in (factor, 1 / factor):
                    cand = dict(p)
                    cand[key] = min(hi, max(lo, p[key] * f))
                    if cand[key] == p[key]:
                        continue
                    val = objective(mode, cand, games)
                    if val < best - 1e-6:
                        p, best, improved = cand, val, True
        factor = 1 + (factor - 1) / 2
    return p, best


def main():
    ap = argparse.ArgumentParser(description="Search preset values that meet targets.yaml")
    ap.add_argument("--mode", default="all")
    ap.add_argument("--games", type=int, default=0, help="0 = fast expected-value search, N = Monte Carlo games per step")
    ap.add_argument("--verify", type=int, default=200, help="Monte Carlo games for the final check")
    args = ap.parse_args()
    modes = list(MODES) if args.mode == "all" else [args.mode]
    PRESETS.mkdir(exist_ok=True)
    for mode in modes:
        p, val = tune(mode, args.games)
        preset = build(mode, p)
        header = "# Generated by tools/balance/tune.py; factors vs the current defaults:\n# " + \
                 ", ".join(f"{k}={v:.2f}" for k, v in p.items()) + "\n"
        (PRESETS / f"{mode}.yml").write_text(header + yaml.safe_dump(preset, sort_keys=False, allow_unicode=True), encoding="utf-8")
        res = run(mode, None, args.verify, 1)
        print(f"{mode}: search objective {val:.2f}, verified score {score(mode, res['stats'])[0]:.2f}, factors "
              + ", ".join(f"{k}={v:.2f}" for k, v in p.items()))


if __name__ == "__main__":
    main()
