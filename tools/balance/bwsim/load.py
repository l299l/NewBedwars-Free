import re
from dataclasses import dataclass, field
from pathlib import Path

import yaml

REPO = Path(__file__).resolve().parents[3]
RESOURCES = REPO / "newbedwars-plugin/src/main/resources"
ITEM_SOURCE = REPO / "newbedwars-plugin/src/main/java/com/l299l/newbedwars/arena/shops/customitems/CustomItemManager.java"

RESOURCES_ORDER = ("Iron", "Gold", "Emerald", "Diamond")


@dataclass
class GenSettings:
    max_level: int
    rates: dict
    amounts: dict
    delay: list

    def level(self, lvl):
        i = max(0, min(lvl, self.max_level) - 1)
        return ({r: self.rates[r][i] for r in self.rates},
                {r: self.amounts[r][i] for r in self.amounts},
                self.delay[i])


@dataclass
class GeneratorConfig:
    id: str
    team: GenSettings
    diamond: GenSettings
    emerald: GenSettings


@dataclass
class Phase:
    name: str
    duration: int
    actions: list


@dataclass
class Prices:
    items: dict = field(default_factory=dict)
    upgrades: dict = field(default_factory=dict)

    def item(self, name):
        return self.items[name]

    def upgrade(self, name, level):
        tiers = self.upgrades.get(name, [])
        return tiers[level] if level < len(tiers) else None


def _expand(values, n, default):
    if values is None:
        return [default] * n
    values = list(values)
    if len(values) == 1:
        return values * n
    if len(values) != n:
        raise ValueError(f"list size {len(values)} != MaxLevel {n}")
    return values


def _settings(section):
    n = int(section["MaxLevel"])
    rates, amounts = {}, {}
    for r in RESOURCES_ORDER:
        sub = section.get(r)
        rates[r] = [float(v) for v in _expand(sub.get("SpawnRate") if sub else None, n, 0.0)]
        amounts[r] = [int(v) for v in _expand(sub.get("MaxAmount") if sub else None, n, 0)]
    delay = [float(v) for v in _expand(section.get("SpawnDelay"), n, 1.0)]
    return GenSettings(n, rates, amounts, delay)


def load_generators(path):
    return generators_from_dict(yaml.safe_load(Path(path).read_text(encoding="utf-8")))


def generators_from_dict(data):
    return GeneratorConfig(data["ConfigurationId"], _settings(data["TeamGenerator"]),
                           _settings(data["DiamondGenerator"]), _settings(data["EmeraldGenerator"]))


def parse_phases(entries):
    return [Phase(p["Name"], int(p["Duration"]), list(p.get("Actions") or [])) for p in entries]


def load_phases(phase_id, path=RESOURCES / "modes.yml"):
    data = yaml.safe_load(Path(path).read_text(encoding="utf-8"))
    for entry in data["GamePhases"]:
        if entry["ID"] == phase_id:
            return parse_phases(entry["Phases"])
    raise KeyError(f"GamePhases id {phase_id} not found in {path}")


_ITEM_RE = re.compile(r'createCustomItem\("(\w+)",\s*Material\.(\w+),\s*(\d+),\s*PriceType\.(\w+),\s*(\d+)')


def load_default_items(path=ITEM_SOURCE):
    items = {}
    for m in _ITEM_RE.finditer(Path(path).read_text(encoding="utf-8")):
        name, _, price, currency, amount = m.groups()
        items[name] = (int(price), currency.capitalize(), int(amount))
    return items


def load_items_yml(path):
    data = yaml.safe_load(Path(path).read_text(encoding="utf-8")) or {}
    items = {}
    for name, entry in data.items():
        if isinstance(entry, dict) and "price" in entry:
            items[name] = (int(entry["price"]), str(entry.get("priceType", "IRON")).capitalize(), int(entry.get("amount", 1)))
    return items


def _tiers(raw, default_currency):
    return [(int(t["price"]), str(t.get("currency", default_currency)).capitalize()) for t in raw]


def load_upgrades(path=RESOURCES / "upgrades.yml"):
    data = yaml.safe_load(Path(path).read_text(encoding="utf-8"))
    cur = data.get("DefaultCurrency", "DIAMOND")
    return {k: _tiers(v or [], cur) for k, v in data["Upgrades"].items()}


def load_prices(item_overrides=None, upgrade_overrides=None, items_yml=None, base_upgrades=None):
    items = load_default_items()
    if items_yml:
        items.update(load_items_yml(items_yml))
    for name, o in (item_overrides or {}).items():
        base = items[name]
        items[name] = (int(o.get("price", base[0])), str(o.get("currency", base[1])).capitalize(), int(o.get("amount", base[2])))
    upgrades = dict(base_upgrades) if base_upgrades is not None else load_upgrades()
    for name, raw in (upgrade_overrides or {}).items():
        upgrades[name] = _tiers(raw, "DIAMOND")
    return Prices(items, upgrades)
