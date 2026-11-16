# Arena setup

Every arena lives in its own world. You build or paste the map, mark where the teams, beds, shops and generators are, save, and enable the arena. The full list of setup commands is in [Commands](commands.md#setup-commands).

## 1. Create the arena

```
/bw arena create myArena
```

This creates an empty world for the arena and puts you into setup mode in it. You start flying, so you don't fall into the void.

- **From a schematic:** put your `.schem` file into the WorldEdit (or FastAsyncWorldEdit) `schematics` folder and run `/bw arena create myArena -sche myMap`. The map is pasted at the world origin. This needs WorldEdit or FAWE.
- **Later:** `/bw arena setup myArena` brings you back into setup mode. `/bw leave` leaves it.

## 2. Choose a setup mode

A menu asks which setup mode you want; `/bw setupMode` opens it again. Each mode includes the commands of the modes before it.

| Mode | For |
|---|---|
| **Normal** | Placing everything by hand with commands: teams, beds, spawns, shops, generators, areas |
| **Advanced** | Normal, plus per-arena gamerules, shop and upgrade menus, generator levels and a resource pack |
| **Automatic** | The plugin scans your built map and sets up what it finds ([see below](#automatic-setup)) |

`/bw setupHelp` lists the commands of your current mode, and `/bw info` (or `/bw info gui`) shows what is still missing.

## 3. Set up the arena (normal mode)

Stand at the right place and run the command; most position commands use your location when you give no coordinates.

**Waiting lobby**

- `/bw setWaitingSpawn`: where players wait before the game.
- `/bw setWaitingPos1` and `/bw setWaitingPos2`: two corners of the waiting area (it is removed when the game starts).

**Teams** (repeat for every team)

- `/bw createTeam <color> <name>`, for example `/bw createTeam RED Red`. Colors: `RED`, `BLUE`, `GREEN`, `YELLOW`, `AQUA`, `WHITE`, `PINK`, `GRAY`, plus the other Minecraft color names (`ORANGE`, `LIME`, `LIGHT_BLUE`, `CYAN`, `PURPLE`, `LIGHT_GRAY`, ...).
- `/bw setTeamSpawn <team>`: where the team spawns and respawns.
- `/bw setTeamBed <team>`: stand on the team's bed (or give its coordinates).
- `/bw setTeamShop <team> VILLAGER` and `/bw setTeamUpgrades <team> VILLAGER`: the item shop and the upgrades NPC (any mob type works).
- `/bw setGenerator BASIC <team>`: the team's iron and gold generator.
- `/bw setTeamBasePos1 <team>` and `...Pos2 <team>`: corners of the team base (used for traps and the heal pool).
- `/bw setTeamBuildProtAreaPos1 <team>` and `...Pos2 <team>`: corners of the zone around the spawn where players can't place blocks.

**Arena**

- `/bw setGenerator DIAMOND` and `/bw setGenerator EMERALD`: the island generators, one command per generator.
- `/bw setQuickVoidY`: the height below which players die instantly. Set it a few blocks under the lowest part of the map.
- `/bw setMinPlayers <n>`, `/bw setMaxInTeam <n>`, `/bw setWaitingTime <seconds>`: change the defaults from [`config.yml`](configuration.md#arena-defaults) for this arena.

Made a mistake? `/bw remove team <name>` removes a team, and `/bw remove team <name> spawn` (or `bed`, `shop`, `upgrades`, ...) removes one point of it. `/bw remove generator <index>` removes a generator.

## Automatic setup

Pick **Automatic** in the setup menu, build the arena, stand in the middle of it and run:

```
/bw startAutomaticSetup [radius]
```

The plugin scans the blocks around you (radius `AutomaticSetup.DefaultRadius`, at most `AutomaticSetup.MaxRadius`) and finds:

- **Beds:** every bed color becomes a team (`Red`, `Blue`, `LightBlue`, ...) with its bed set.
- **Generator blocks:** diamond and emerald blocks become island generators (touching blocks make one generator). The nearest iron or gold block near a bed becomes that team's generator.
- **Marker signs** for everything else. Line 1 is the type, line 2 the team color (empty means the team of the nearest bed), and line 3 an optional mob type for shops (default `VILLAGER`):

| Line 1 | Sets |
|---|---|
| `[spawn]` | Team spawn (faces the middle of the map) |
| `[shop]` | Item shop |
| `[upgrades]` | Upgrades shop |
| `[gen]` | Team generator. With `DIAMOND` or `EMERALD` on line 2 it adds an island generator instead |

The used signs are removed. The quick void height and each team's base and build protection areas are guessed from the map.

Anything still missing is shown in chat with clickable **[Set here]** and **[Skip]** buttons. `/bw startAutomaticSetup next` shows the current step again, `skip` skips it and `cancel` stops; what was already set stays. A new scan never overwrites what is already set.

## 4. Save and enable

```
/bw save
/bw arena enable myArena
```

`/bw save` saves the arena and stores the map as a clean template; after every game the arena is reset from it. Run `/bw save` again whenever you change the map.

An arena can only be enabled when it has:

- the waiting spawn and both waiting area corners;
- at least two teams, each with a spawn, bed, shop, upgrades NPC, team generator, base area and build protection area;
- at least one diamond or emerald generator;
- the quick void height.

`/bw arena list` shows every arena and, for disabled ones, what is missing.

## More arenas

- **Copy an arena:** `/bw arena copy myArena myArena2` copies the settings and the map, so one map can run several games at once.
- **Arena groups and map voting:** `/bw group create solo`, then `/bw group add solo myArena`. Players can join a group (fullest free arena first), and at the end of a game they vote for the next map of the group. All arenas of a group need the same number of teams and team size.
- **Join NPCs:** `/bw npc create VILLAGER GROUP solo` spawns an NPC in the lobby that joins the group when clicked. With Citizens, select an NPC and use `/bw npc set GROUP solo`.

## Backups and moving arenas

- Saving keeps backups of the previous versions (how many: `Backups.Keep` in `config.yml`). `/bw arena backup <name>` makes one now, `/bw arena backups <name>` lists them and `/bw arena restore <name> [backupId]` restores one.
- `/bw arena resetmap <name>` puts the map back to its saved state.
- `/bw arena export <name>` packs the arena and its map into a zip in `data/exports`. Copy the zip to another server's `data/exports` and run `/bw arena import <file>` there; the arena is loaded disabled.
