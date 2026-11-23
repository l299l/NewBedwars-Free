import math

import numpy as np

from .load import RESOURCES_ORDER


def team_period(delay):
    return max(1, round(delay * 20)) / 20.0


def island_period(delay):
    return max(1, math.ceil(delay))


def team_rates(settings, level):
    rates, amounts, delay = settings.level(level)
    total = sum(rates.values())
    period = team_period(delay)
    out = {}
    for r in RESOURCES_ORDER:
        if total <= 0 or amounts[r] <= 0:
            out[r] = 0.0
        else:
            out[r] = rates[r] / total * (amounts[r] + 1) / 2 / period
    return out


def island_rate(settings, level):
    _, amounts, delay = settings.level(level)
    res = "Diamond" if settings.rates["Diamond"][0] > 0 else "Emerald"
    return max(1, amounts[res]) / island_period(delay)


class TeamGenerator:
    def __init__(self, settings, rng):
        self.settings = settings
        self.rng = rng
        self.level = 1
        self.acc = 0.0

    def step(self, dt):
        rates, amounts, delay = self.settings.level(self.level)
        period = team_period(delay)
        self.acc += dt
        out = dict.fromkeys(RESOURCES_ORDER, 0)
        names = list(RESOURCES_ORDER)
        weights = np.array([rates[r] for r in names], dtype=float)
        total = weights.sum()
        while self.acc >= period:
            self.acc -= period
            if total <= 0:
                continue
            r = names[self.rng.choice(len(names), p=weights / total)]
            if amounts[r] > 0:
                out[r] += 1 + int(self.rng.integers(amounts[r]))
        return out


class IslandGenerator:
    def __init__(self, settings, resource):
        self.settings = settings
        self.resource = resource
        self.level = 1
        self.remaining = None

    def step(self):
        _, amounts, delay = self.settings.level(self.level)
        if self.remaining is None:
            self.remaining = island_period(delay)
        self.remaining -= 1
        if self.remaining <= 0:
            self.remaining = island_period(delay)
            return max(1, amounts[self.resource])
        return 0
