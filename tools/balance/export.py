import argparse
import sys
from pathlib import Path

import yaml

sys.path.insert(0, str(Path(__file__).resolve().parent))

from bwsim.load import RESOURCES

HERE = Path(__file__).resolve().parent
MODES = {
    "solo": ("Solo", "8 teams of 1"),
    "doubles": ("Doubles", "8 teams of 2"),
    "squads": ("Squads", "4 teams of 4"),
    "rush": ("Rush", "fast games, about 15 minutes"),
    "duels": ("Duels", "1v1, about 12 minutes"),
}

GEN_HEADER = """# =====================================================
#  NewBedwars — {title} generators ({about})
#
#  Built-in mode preset, tuned with tools/balance (see docs/dev/BALANCE-SIM.md).
#  Copied to plugins/NewBedwars/generators/ when missing; edit the copy there.
#  Use it in an arena with /bw setGeneratorLeveling {title}
#  Format: see defaultGenerators.yml.
# =====================================================

"""

GUI_HEADER = """# =====================================================
#  NewBedwars — {title} upgrade GUI ({about})
#
#  Built-in mode preset: the example upgrade layout with the {title} upgrade prices.
#  Copied to plugins/NewBedwars/data/guis/ when missing; edit the copy there.
#  Use it in an arena with /bw setGui upgrade upgrade{title}
#  UpgradePrices: one list per upgrade, the first entry is level 1 (same format as upgrades.yml).
# =====================================================

"""


def flow(values):
    return "[" + ", ".join(f"{v:g}" if isinstance(v, float) else str(v) for v in values) + "]"


def generator_yaml(title, about, gens):
    out = [GEN_HEADER.format(title=title, about=about), f"ConfigurationId: {title}\n"]
    labels = {"TeamGenerator": "Team (Basic) Generator", "DiamondGenerator": "Diamond Generator",
              "EmeraldGenerator": "Emerald Generator"}
    for key in ("TeamGenerator", "DiamondGenerator", "EmeraldGenerator"):
        sec = gens[key]
        out.append(f"\n# ----- {labels[key]} ".ljust(52, "-") + "\n")
        out.append(f"{key}:\n  MaxLevel: {sec['MaxLevel']}\n  ShowNextSpawnTime: {str(sec['ShowNextSpawnTime']).lower()}\n")
        for res in ("Iron", "Gold", "Emerald", "Diamond"):
            if res in sec:
                out.append(f"  {res}:\n    SpawnRate: {flow(sec[res]['SpawnRate'])}\n    MaxAmount: {flow(sec[res]['MaxAmount'])}\n")
        out.append(f"  SpawnDelay: {flow(sec['SpawnDelay'])}\n")
    return "".join(out)


def gui_yaml(title, about, upgrades):
    base = (RESOURCES / "exampleUpgrade.yml").read_text(encoding="utf-8")
    body = base[base.index("GuiId:"):]
    body = body.replace("GuiId: exampleUpgrade", f"GuiId: upgrade{title}", 1)
    body = body.replace('GuiName: "Example Upgrade"', f'GuiName: "Upgrades"', 1)
    lines = ["\nUpgradePrices:\n"]
    for name, tiers in upgrades.items():
        lines.append(f"  {name}:\n")
        for t in tiers:
            lines.append(f"    - {{price: {t['price']}, currency: {t['currency']}}}\n")
    return GUI_HEADER.format(title=title, about=about) + body.rstrip("\n") + "\n" + "".join(lines)


def phases_yaml(title, phases):
    out = [f"  - ID: {title}\n    Phases:\n"]
    for p in phases:
        out.append(f'      - Name: "{p["Name"]}"\n        Duration: {p["Duration"]}\n')
        if p["Actions"]:
            out.append("        Actions:\n" + "".join(f'          - "{a}"\n' for a in p["Actions"]))
        else:
            out.append("        Actions: []\n")
    return "".join(out)


def main():
    ap = argparse.ArgumentParser(description="Write the plugin resource files from presets/*.yml")
    ap.add_argument("--resources", default=str(RESOURCES))
    args = ap.parse_args()
    res = Path(args.resources)
    (res / "generators").mkdir(exist_ok=True)
    (res / "guis").mkdir(exist_ok=True)
    phases = []
    for mode, (title, about) in MODES.items():
        preset = yaml.safe_load((HERE / "presets" / f"{mode}.yml").read_text(encoding="utf-8"))
        gens = dict(preset["Generators"])
        gens["ConfigurationId"] = title
        (res / "generators" / f"{title}.yml").write_text(generator_yaml(title, about, gens), encoding="utf-8")
        (res / "guis" / f"upgrade{title}.yml").write_text(gui_yaml(title, about, preset["UpgradePrices"]), encoding="utf-8")
        phases.append(phases_yaml(title, preset["Phases"]))
        if preset.get("ItemPrices"):
            print(f"{mode}: ItemPrices present, add them to a shop GUI by hand: {preset['ItemPrices']}")
    print("GamePhases entries for modes.yml:\n")
    print("".join(phases))


if __name__ == "__main__":
    main()
