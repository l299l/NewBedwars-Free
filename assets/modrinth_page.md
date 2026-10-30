![NewBedwars](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/banner_main.svg)

<div align="center">

[![Modrinth](https://img.shields.io/badge/Modrinth-Download-1BD96A?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/new-bedwars) [![GitHub](https://img.shields.io/badge/GitHub-Source-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/l299l/NewBedwars-Free)

</div>

# NewBedwars

A modern, fully configurable Bedwars plugin built from the ground up for **Paper 1.17 – 26.x**. You get the classic Bedwars experience (teams, beds, generators, shops) plus a proper in-game setup system, YAML-driven GUIs, multi-arena support and deep gamerule control. No extra plugins needed.

Whether you're running a small survival server and just want a Bedwars minigame, or building a full network, NewBedwars gives you the tools to build polished arenas without a headache.

> **This is a beta release.** Core gameplay is stable and feature-complete. Please report issues on [GitHub](https://github.com/l299l/NewBedwars-Free/issues).

## Why this exists

A lot of free Bedwars plugins make you pick one: simple but bare-bones, or powerful but bloated with dependencies and a config file the size of a novel. NewBedwars tries to skip that trade-off. You set up an arena entirely in-game through a guided setup flow, so there is no hand-editing YAML just to place a bed or a generator. Everything from the shop GUI to the scoreboard is defined in YAML you can reshape without touching Java. PlaceholderAPI and WorldEdit are supported if you have them, but you don't need either.

## Features

**Gameplay**
- Multi-arena support: run as many arenas as you want at once, each in its own world
- Automatic map reset after every game, with backups, export and import of arenas
- Configurable team count, colors and player limits per arena
- Quick Void: instant death below a configurable Y level
- Per-team build protection zones
- Rejoin system for players who get disconnected
- Parties: `/party`, clickable invites, and the party admin leads everyone into an arena together
- Configurable game phases: generator upgrades, bed destruction, Ender Dragons, and more
- Resource generators (iron, gold, diamond, emerald) with levels, plus team Forge upgrades

**Shop and upgrades**
- YAML shop and upgrade GUIs, with fast-buy customization
- Team upgrades (Forge, Heal Pool, Dragon Buff, traps and more)
- Custom items: Bridge Egg, Fireball, custom potions, Iron Golem, Silverfish, Lucky Block, Portable Tower and more
- Team-colored blocks: wool, terracotta, glass, concrete and more are given in the buyer's team color

**Chat, display and HUD**
- Arena chat isolation, team chat with `!message`, optional arena-wide chat, tab list isolation
- YAML-templated boss bars and per-player scoreboards
- Spectator mode with flight and a compass teleporter
- Six languages: English, Polish, German, Spanish, French and Russian (missing keys fall back to English)
- Per-arena resource packs
- Player stats, leaderboards and a profile GUI

**Integrations**
- PlaceholderAPI: 26 placeholders under `%newbedwars_*%` (optional)
- WorldEdit / FastAsyncWorldEdit: build an arena from a `.schem` file (optional)
- Addon API with events and registration of your own shop items, upgrades, armor, commands and phase actions

## Requirements

- **Server:** Paper 1.17 – 26.x
- **Java:** 16 or higher
- No other plugins are required. PlaceholderAPI and WorldEdit / FastAsyncWorldEdit are optional.

## Getting started

1. Download the jar from this page and drop it into your server's `plugins/` folder.
2. Start the server. Config files generate automatically.
3. Set your lobby spawn: `/bw setLobby`
4. Create your first arena: `/bw arena create myArena`
5. Follow the setup checklist shown by `/bw arena`.
6. Save the arena: `/bw save`
7. Enable it when ready: `/bw arena enable myArena`

**Building an arena from a schematic.** Save your map as a `.schem` file in WorldEdit's schematics folder, then run `/bw arena create myArena -sche myMap`. The map is pasted at the world origin; continue with the normal setup checklist from there. This needs WorldEdit or FastAsyncWorldEdit.

## Commands

`/bw help` shows the commands you can use right now, and `/bw setupHelp [page] [command]` shows the setup commands of your current setup mode.

### Player commands

| Command | Description |
|---|---|
| `/bw join [arena]` | Open the arena selector or join a specific arena |
| `/bw rejoin` | Rejoin the game you disconnected from |
| `/bw spectate <arena>` | Join a running arena as a spectator |
| `/bw lobby` (or `/lobby`) | Leave the arena and return to the lobby |
| `/bw lang <en\|pl\|de\|es\|fr\|ru>` (or `/lang`) | Change your language |
| `/bw help [page]` | Show the commands you can use |
| `/bw stats [player]` | Show a player's stats |
| `/bw profile [player]` | Open a player's profile GUI |
| `/bw leaderboard [wins\|kills\|beds\|fk]` | Top 10 players by a stat |

### Party commands

`/party` (alias `/p`)

| Command | Description |
|---|---|
| `/party invite <player>` | Invite a player to your party |
| `/party accept` / `/party deny` | Accept or deny an invite (clickable in chat) |
| `/party leave` | Leave your party |
| `/party list` | List the party members |
| `/party kick <player>` | Kick a member (party admin only) |
| `/party admin <player>` | Give the party admin role to another member (party admin only) |

### Admin commands

| Command | Description |
|---|---|
| `/bw arena create <name> [-sche <schematic>] [-n]` | Create an arena, optionally from a schematic (`-n`: do not teleport to it) |
| `/bw arena list` | List arenas and their status |
| `/bw arena setup <name>` | Enter setup mode for an arena |
| `/bw arena enable <name>` / `disable <name>` | Enable or disable an arena |
| `/bw arena delete <name>` | Permanently delete an arena, its world and saved data |
| `/bw arena backup <name>` / `backups <name>` / `restore <name> [id]` | Back up an arena, list its backups, restore one |
| `/bw arena export <name>` / `import <file>` | Export an arena to a zip in `data/exports`, or import one (loaded disabled) |
| `/bw arena resetmap <name>` | Put the arena map back to its saved state |
| `/bw setlobby` | Set the lobby spawn |
| `/bw game <start\|forcestart\|stop\|forcestop\|nextPhase>` | Manage the game you are in or spectating |
| `/bw reload` | Save arenas, send everyone to the lobby and reload the configuration |

### Setup commands

Used inside the arena world after `/bw arena setup <name>`. Pick a mode with `/bw setupMode`; each mode includes the commands of the ones before it. Position commands use your location when you give no coordinates.

| Mode | Commands |
|---|---|
| All | `/bw arena`, `/bw setupMode`, `/bw setupHelp`, `/bw save`, `/bw remove`, `/bw leave` |
| Normal | `/bw createTeam <color> <name>`, `setTeamSpawn`, `setTeamBed`, `setTeamShop`, `setTeamUpgrades`, `setGenerator <type> [team]`, `setMinPlayers`, `setMaxInTeam`, `setWaitingSpawn`, `setWaitingPos1/2`, `setWaitingTime`, `setQuickVoidY`, `setTeamBasePos1/2`, `setTeamBuildProtAreaPos1/2` |
| Advanced | `/bw gamerule <name> <true\|false>`, `setGui`, `setGeneratorLeveling`, `setResourcePack`, `setTeamAccessories` |

The complete list with every argument is in the [command reference](https://github.com/l299l/NewBedwars-Free/blob/master/docs/COMMANDS.md).

## Permissions

Parent permissions grant their children. Everything not marked `true` defaults to `op`.

| Permission | Default | Description |
|---|---|---|
| `newbedwars.bw.admin` | op | All admin commands |
| `newbedwars.bw.arena` | op | All `/bw arena ...` commands |
| `newbedwars.bw.arena.<action>` | op | A single arena command: `create`, `delete`, `enable`, `disable`, `setup`, `list`, `backup`, `backups`, `restore`, `export`, `import`, `resetmap` |
| `newbedwars.bw.setlobby` | op | `/bw setlobby` |
| `newbedwars.bw.managegame` | op | `/bw game ...` |
| `newbedwars.bw.bypass` | op | Bypass setup-mode restrictions |
| `newbedwars.bw.player` | true | Bundle of all player permissions below |
| `newbedwars.bw.join` / `lobby` / `rejoin` / `spectate` | true | Join arenas, go to the lobby, rejoin, spectate |
| `newbedwars.bw.lang` / `help` / `stats` / `profile` / `leaderboard` | true | Language, help, stats, profile and leaderboard commands |
| `newbedwars.party` | true | All `/party` commands |

`newbedwars.bw.join` and `newbedwars.bw.lobby` are only checked when `RequireJoinPermission` / `RequireLobbyPermission` is `true` in `config.yml`. Both default to `false`, which means everyone can use them.

## Configuration

Files are created in `plugins/NewBedwars/`:

| File | What it controls |
|---|---|
| `config.yml` | Defaults, gamerules, game phases, upgrade prices, rejoin |
| `language/*.yml` | All messages (add or edit translations) |
| `bossBars.yml` / `scoreboards.yml` | Boss bar and scoreboard templates |
| `generators/defaultGenerators.yml` | Resource generator speeds and levels |
| `data/guis/*.yml` | Shop and upgrade GUIs |
| `data/items.yml` | Shop items |
| `data/arenas/*.json` | Saved arenas (edited by the setup commands) |

## PlaceholderAPI

If PlaceholderAPI is installed, NewBedwars registers 26 placeholders under `%newbedwars_<placeholder>%`, for example `%newbedwars_arena%`, `%newbedwars_team%`, `%newbedwars_kills%`, `%newbedwars_stat_wins%` and `%newbedwars_arena_<name>_players%`. See the [full list](https://github.com/l299l/NewBedwars-Free/blob/master/docs/PLACEHOLDERS.md).

## Addon developers

NewBedwars has an API with events, so other plugins can hook into it. You can register your own shop items, upgrades, armor tiers, commands and phase actions, store extra data on arenas and edit arenas. Documentation: [Addon API](https://github.com/l299l/NewBedwars-Free/blob/master/docs/API.md), with a [complete example addon](https://github.com/l299l/NewBedwars-Free/tree/master/docs/examples/event-logger).

## Roadmap

- [x] Parties, PlaceholderAPI, WorldEdit / FAWE schematics, per-arena resource packs
- [x] Statistics, leaderboards and profile GUI
- [x] Addon API and events
- [ ] Citizens2 support
- [ ] In-game map voting
- [ ] Full translations for all languages
- [ ] Network / BungeeCord / Velocity mode

## Screenshots

<details>
<summary>Joining & Waiting</summary>

![Arena selector GUI](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_110222.png)
*Arena selector GUI*

![Waiting lobby with bossbar & scoreboard](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_110543.png)
*Waiting lobby with bossbar & scoreboard*

</details>

<details>
<summary>In-Game HUD & Events</summary>

![In-game HUD — scoreboard, phase info, NPC labels](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_114731.png)
*In-game HUD — scoreboard, phase info, NPC labels*

![Respawn countdown](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121249.png)
*Respawn countdown*

![Bed destroyed announcement](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_120913.png)
*Bed destroyed announcement*

![Trap alarm](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_1205111.png)
*Trap alarm*

![End game — spectator view with bossbar](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121425.png)
*End game — spectator view with bossbar*

</details>

<details>
<summary>Shop & Upgrades</summary>

![Shop GUI](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_114843.png)
*Shop GUI*

![Bridge Egg — shop tooltip (cost: 4 Gold)](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_114904.png)
*Bridge Egg — shop tooltip (cost: 4 Gold)*

![Team Upgrades shop](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_114919.png)
*Team Upgrades shop*

</details>

<details>
<summary>Resource Generators</summary>

![Emerald Generator — hologram](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_115559.png)
*Emerald Generator — hologram*

![Diamond Generator — hologram](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_120327.png)
*Diamond Generator — hologram*

</details>

<details>
<summary>Arena Setup</summary>

![Help command output](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121511.png)
*Help command output*

![Arena setup checklist (NORMAL_SETUP)](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121720.png)
*Arena setup checklist (NORMAL_SETUP)*

![Team configuration checklist — missing fields highlighted](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121727.png)
*Team configuration checklist — missing fields highlighted*

</details>

<details>
<summary>Map & Environment</summary>

![Arena map — aerial overview](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_122307.png)
*Arena map — aerial overview*

![Live gameplay — combat near a bed 1](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_123438.png)
*Live gameplay — combat near a bed 1*

![](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_122037.png)
**

![Live gameplay — building bridge](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_122528.png)
*Live gameplay — building bridge*

![Live gameplay — combat near a bed 2](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_123625.png)
*Live gameplay — combat near a bed 2*

</details>

## Support and bug reports

Found a bug or have a suggestion? Open an issue on **[GitHub](https://github.com/l299l/NewBedwars-Free/issues)** or use the discussion section here. Most problems can be fixed quickly with a good bug report: include your server version, the NewBedwars version and the relevant part of the log.

## License

See [LICENSE.txt](https://github.com/l299l/NewBedwars-Free/blob/master/LICENSE.txt).

---

**NewBedwars**, developed by **l299l**. Paper 1.17 – 26.x
