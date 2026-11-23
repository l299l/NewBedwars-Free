from dataclasses import dataclass


@dataclass(frozen=True)
class Goal:
    kind: str
    name: str
    level: int = 1
    per_player: bool = False
    milestone: str = None
    repeat: int = 0


def item(name, per_player=True, milestone=None, repeat=0):
    return Goal("item", name, 1, per_player, milestone, repeat)


def upgrade(name, level, milestone=None):
    return Goal("upgrade", name, level, False, milestone)


def upgrades_to(name, top, milestone_last=None, start=1):
    return [upgrade(name, lvl, milestone_last if lvl == top else None) for lvl in range(start, top + 1)]


BALANCED = [
    item("Wool", repeat=120),
    item("StoneSword", milestone="stone_sword"),
    upgrade("FORGE", 1, "forge_1"),
    item("IronArmor", milestone="iron_armor"),
    upgrade("SHARPNESS", 1, "sharpness"),
    upgrade("PROTECTION", 1, "protection_1"),
    item("IronPickaxe"),
    upgrade("FORGE", 2),
    item("IronSword"),
    upgrade("PROTECTION", 2),
    upgrade("HEALPOOL", 1),
    upgrade("FORGE", 3),
    item("DiamondArmor", milestone="diamond_armor"),
    item("GoldenApple", repeat=180),
    *upgrades_to("PROTECTION", 3),
    *upgrades_to("FORGE", 5, start=4),
    item("DiamondSword"),
    *upgrades_to("PROTECTION", 6),
    *upgrades_to("FORGE", 10, start=6),
    upgrade("HASTE", 1),
    item("EnderPearl", repeat=240),
    item("Fireball", repeat=150),
]

RUSHER = [
    item("Wool", repeat=60),
    item("StoneSword", milestone="stone_sword"),
    item("Fireball", repeat=90),
    item("IronArmor", milestone="iron_armor"),
    upgrade("SHARPNESS", 1, "sharpness"),
    upgrade("FORGE", 1, "forge_1"),
    item("IronSword"),
    upgrade("PROTECTION", 1, "protection_1"),
    item("TNT", repeat=120),
    upgrade("FORGE", 3),
    item("DiamondArmor", milestone="diamond_armor"),
    *upgrades_to("PROTECTION", 6, start=2),
    item("DiamondSword"),
    *upgrades_to("FORGE", 10, start=4),
    item("EnderPearl", repeat=180),
]

DEFENDER = [
    item("Wool", repeat=150),
    item("StoneSword", milestone="stone_sword"),
    item("EndStone", per_player=False, repeat=300),
    upgrade("FORGE", 1, "forge_1"),
    item("IronArmor", milestone="iron_armor"),
    upgrade("PROTECTION", 1, "protection_1"),
    upgrade("SHARPNESS", 1, "sharpness"),
    upgrade("HEALPOOL", 1),
    item("Obsidian", per_player=False, repeat=600),
    *upgrades_to("FORGE", 3, start=2),
    item("DiamondArmor", milestone="diamond_armor"),
    *upgrades_to("PROTECTION", 6, start=2),
    *upgrades_to("FORGE", 10, start=4),
    item("IronSword"),
    item("DiamondSword"),
]

POLICIES = {"balanced": BALANCED, "rusher": RUSHER, "defender": DEFENDER}
MILESTONES = ["stone_sword", "iron_armor", "forge_1", "sharpness", "protection_1",
              "emerald_forge", "diamond_armor", "protection_max"]
