from pathlib import Path

import yaml

from .load import RESOURCES, generators_from_dict, load_generators, load_phases, load_prices, load_upgrades, parse_phases

PRESETS = Path(__file__).resolve().parents[1] / "presets"
BASELINE_PHASES = {"rush": "Rush"}


def load_mode(mode, preset=None):
    path = PRESETS / f"{preset or mode}.yml"
    if preset == "baseline" or not path.exists():
        gens = load_generators(RESOURCES / "defaultGenerators.yml")
        phases = load_phases(BASELINE_PHASES.get(mode, "Default"))
        base = load_upgrades(Path(__file__).resolve().parents[1] / "base" / "upgrades.yml")
        return gens, phases, load_prices(upgrade_overrides=None, base_upgrades=base), "baseline"
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    gens = generators_from_dict(data["Generators"])
    phases = parse_phases(data["Phases"])
    prices = load_prices(data.get("ItemPrices"), data.get("UpgradePrices"))
    return gens, phases, prices, path.name
