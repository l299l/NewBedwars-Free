# Game modes and phases

A game mode bundles what an arena needs for one way of playing: the generator speeds, the game phases (what happens and when during a game), the upgrade menu and the team size. Five modes come with the plugin (Solo, Doubles, Squads, Rush and Duels) and you can make your own. In advanced setup mode, `/bw setMode <Mode>` copies a mode into an arena.

Modes and game phases are in `plugins/NewBedwars/modes.yml`. Edit it, then restart the server or run `/bw reload` (this stops running games).

## The modes.yml file

- **Updating from an older version:** the first time the new version starts, your `GamePhases` and `Modes` are moved from `config.yml` into `modes.yml` automatically. The old `config.yml` is backed up in `data/backups/config/<version>/`.
- **Built-in modes come back:** built-in modes and phase lists that are missing from `modes.yml` are added to the file. Your own entries and comments are kept, and your own modes are not removed.
- **Invalid file:** if `modes.yml` can not be read, the built-in modes are used and an error is written to the console. Fix the file and reload.
- `config.yml` only keeps `DefaultGeneratorConfig` and `DefaultGamePhase` (the [Arena defaults](configuration.md#arena-defaults) for new arenas).

Related pages: [Balance simulator](balance-simulator.md) (tune phases and prices before trying them with players), [Shop and upgrades](shop-and-upgrades.md) (generator files, `UpgradePrices`, `ItemPrices`) and [Arena setup](arena-setup.md#choose-a-game-mode).

## Built-in modes

Five modes come with the plugin. A mode is three parts that share its name; the files are copied to your server when they are missing (never overwritten, so your edits stay):

- a generator file `generators/<Mode>.yml` (how fast generators drop resources, see [Shop and upgrades](shop-and-upgrades.md#generators-generatorsyml)),
- a phase list with the same ID in `GamePhases` in `modes.yml`,
- an upgrade menu `data/guis/upgrade<Mode>.yml` with its own `UpgradePrices` (see [Shop and upgrades](shop-and-upgrades.md)).

| Mode | Tuned for | Longest game |
|---|---|---|
| `Solo` | 8 teams of 1 | 35 minutes |
| `Doubles` | 8 teams of 2 | 35 minutes |
| `Squads` | 4 teams of 4 | 35 minutes |
| `Rush` | 4 teams of 2 | 15 minutes |
| `Duels` | 2 teams of 1 | 12 minutes |

The generator speeds and prices were tuned with the [balance simulator](balance-simulator.md) in `tools/balance/`, so every mode has its own pacing.

To use a mode in an arena, create the arena as usual (team count and team size included) and, in advanced setup mode, run `/bw setMode <Mode>`. It copies the mode's generator preset, game phases, upgrade menu (and shop menu, if the mode has one) and team size into the arena and saves it. This happens once: arenas keep what was copied, so after you edit a mode, run `/bw setMode <Mode>` again in every arena that should get the change. If the mode refers to something that does not exist (a missing generator file, phase list or menu), nothing is changed and the missing names are listed. The single commands `/bw setGeneratorLeveling`, `/bw setGamePhases` and `/bw setGui` still work for fine-tuning one part afterwards. See [Arena setup](arena-setup.md#choose-a-game-mode). The [web panel](panel.md#game-modes) can apply a mode to a waiting arena too.

## Modes in modes.yml

The `Modes` section of `modes.yml` lists the modes that `/bw setMode` knows (and offers in tab completion). A mode is a name with these optional keys; a missing key means that part of the arena is left as it is.

| Key | Meaning |
|---|---|
| `Generators` | Generator preset (`ConfigurationId` of a file in `generators/`) |
| `GamePhases` | ID of a phase list from [`GamePhases`](#game-phases) below |
| `ShopGui` | Shop menu (`GuiId` of a file in `data/guis/`) |
| `UpgradeGui` | Upgrade menu (`GuiId` of a file in `data/guis/`) |
| `MaxInTeam` | Players per team (a whole number above 0) |
| `Tablist` | Tab list layout (a layout name from `tablists.yml`, see [configuration](configuration.md#custom-tab-list)); copied to the arena like the other keys |
| `Teams` | Number of teams the mode is made for. It is not applied: `/bw setMode` only warns when the arena has a different number of teams |

```yaml
Modes:
  Solo:
    Generators: Solo
    GamePhases: Solo
    UpgradeGui: upgradeSolo
    MaxInTeam: 1
    Teams: 8
  Trios:
    Generators: Rush
    GamePhases: Rush
    ShopGui: exampleShop
    UpgradeGui: upgradeRush
    MaxInTeam: 3
    Teams: 4
```

You can add your own modes by adding entries to `Modes`; they are kept when you update. An entry with the same name as a built-in mode replaces it completely (keys you leave out are not taken from the built-in one). Invalid numbers count as not set, and a mode that is not a section is skipped with a console warning.

### Phase times of the built-in modes

When each phase starts (minutes:seconds after the game begins):

| Event | Solo, Doubles, Squads | Rush | Duels |
|---|---|---|---|
| Diamonds II | 5:00 | 3:00 (with Emeralds II) | 2:40 (with Emeralds II) |
| Emeralds II | 10:00 | 3:00 | 2:40 |
| Diamonds III | 15:00 | 6:00 (with Emeralds III) | 5:20 (with Emeralds III) |
| Emeralds III | 20:00 | 6:00 | 5:20 |
| Beds are destroyed | 25:00 | 9:00 | 8:00 |
| Ender Dragons | 30:00 | 12:00 | none |
| Game ends | 35:00 | 15:00 | 12:00 |

Solo, Doubles and Squads end as a draw; Rush and Duels end with the most kills winning.

## Game phases

A phase list (`GamePhases` in `modes.yml`) is what happens during a game. When a phase **starts**, it runs its `Actions`; `Duration` is how many seconds it lasts until the next phase starts. Arenas use the list named in `DefaultGamePhase` in `config.yml`, or the one set with `/bw setGamePhases <id>` in advanced setup. The built-in lists are the ones in the table under [phase times](#phase-times-of-the-built-in-modes); `Default` is the old list (about 35 minutes, deprecated).

```yaml
GamePhases:
  - ID: Default
    Phases:
      - Name: "Starting phase"
        Duration: 300
        Actions: []
      - Name: "Diamonds II"
        Duration: 300
        Actions:
          - "DIAMOND_GENERATOR:2"
      - Name: "Bed destruction"
        Duration: 300
        Actions:
          - "BED_DESTRUCTION"
          - "DISABLE_REGENERATION"
      - Name: "Game end"
        Duration: 0
        Actions:
          - "GAME_END:DRAW"
```

| Action | Effect |
|---|---|
| `DIAMOND_GENERATOR:<level>` | Upgrade all diamond generators |
| `EMERALD_GENERATOR:<level>` | Upgrade all emerald generators |
| `BED_DESTRUCTION` | Destroy all remaining beds |
| `DISABLE_REGENERATION` | Turn off natural health regeneration |
| `DISABLE_TEAMS_FORGE` | Stop the team generators for the rest of the game (buying the Forge upgrade does not restart them) |
| `ENDER_DRAGON_START` | Spawn Ender Dragons (one per team with the Dragon Buff upgrade) |
| `ENDER_DRAGON_END` | Remove the Ender Dragons |
| `GAME_END:DRAW` | End the game as a draw |
| `GAME_END:MOST_KILLS` | End the game; the team with the most kills wins |

Addons can add their own actions. Admins can skip to the next phase with `/bw game nextPhase`.

## Ender Dragons

Dragons are part of sudden death. Spawned by `ENDER_DRAGON_START` for every team with the Dragon Buff upgrade. Dragons patrol over the middle of the map, hunt enemy players (first those near a bed), dive at them and shoot fireballs. They never hurt their own team and disappear when their team is out.

The dragon settings stay in `config.yml` (not in `modes.yml`):

| Setting | Default | Meaning |
|---|---|---|
| `DragonSpawnHeight` | `10` | Blocks above the team spawn where dragons appear |
| `DragonIndestructibleBlocks` | `OBSIDIAN`, `BEDROCK`, `BARRIER`, `END_PORTAL_FRAME`, `IRON_BARS` | Blocks dragons can't destroy (Bukkit material names). Beds are never destroyed. The map is restored after the game anyway |
| `Dragon.Speed` | `0.6` | Flight speed in blocks per tick |
| `Dragon.TurnRate` | `6.0` | How fast a dragon turns, in degrees per tick |
| `Dragon.PatrolHeight` | `15` | Patrol height above the highest team spawn, bed or generator |
| `Dragon.PatrolRadius` | `0` | Radius of the patrol circle around the map centre (`0` = automatic) |
| `Dragon.Margin` | `10` | How far outside the bases and generators a dragon may fly |
| `Dragon.TargetRange` | `128` | Maximum distance at which a dragon picks a target |
| `Dragon.AttackRange` | `20` | Horizontal distance at which a dragon starts a dive |
| `Dragon.FireballIntervalSeconds` | `[20, 10]` | Seconds between fireballs for Dragon Buff level 1, 2, ... |
| `Dragon.FireballSpeed` | `1.0` | Starting speed of the fireballs in blocks per tick |
