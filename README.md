![NewBedwars Banner](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/banner_main.svg)

<div align="center">

[![SpigotMC](https://img.shields.io/badge/SpigotMC-Download-ED8106?style=for-the-badge&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==)](https://www.spigotmc.org/resources/new-bedwars.136050/)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-1BD96A?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/new-bedwars)
[![Hangar](https://img.shields.io/badge/Hangar-Download-0EA5E9?style=for-the-badge)](https://hangar.papermc.io/l299l/New-Bedwars)

</div>

---
# NewBedwars

A modern, fully configurable Bedwars plugin built from the ground up for **Paper 1.17+**. You get the classic Bedwars experience (teams, beds, generators, shops) plus a proper in-game setup system, YAML-driven GUIs, multi-arena support, and deep gamerule control. No extra plugins needed.

Whether you're running a small survival server and just want to add a Bedwars minigame, or building a full-blown Bedwars network, NewBedwars gives you the tools to build polished arenas without a headache.

> **This is a beta release.** Core gameplay is stable and feature-complete. Please report any issues via the GitHub issue tracker.

---

## Why this exists

A lot of free Bedwars plugins make you pick one: simple but bare-bones, or powerful but bloated with dependencies and a config file the size of a novel. NewBedwars tries to skip that trade-off. You set up an arena entirely in-game through a guided setup flow, so there's no hand-editing YAML just to place a bed or a generator. Everything from the shop GUI to the scoreboard is defined in YAML you can reshape without touching a single line of Java. PlaceholderAPI and WorldEdit are supported if you have them, but you don't need either one. Install the jar, start the server, and you've got a fully working Bedwars setup.

---

## Highlights

**Gameplay**
- Multi-arena support, so you can run as many arenas as you want at once, each in its own world
- Configurable team count, colors, and player limits per arena
- Quick Void: instant death below a configurable Y level
- Per-team build protection zones so the opposing side can't place blocks where they shouldn't
- Rejoin system, so players can reconnect to a game if they get disconnected
- Party system: group up with `/party`, invite friends with clickable chat buttons, and let the party admin lead everyone into an arena together

**Chat & visibility**
- Arena chat isolation, so each arena has its own chat completely separate from the lobby and other arenas
- Team chat via `!message`, toggleable per arena
- Optional arena-wide global chat channel
- Tab list isolation, so lobby and arena players never see each other

**Display & HUD**
- YAML-templated bossbars for waiting, playing, and ending states
- Per-player scoreboards with full placeholder support
- Spectator mode with flight and a compass that teleports you to live players
- Six languages out of the box: English, Polish, German, Spanish, French, and Russian. Missing keys just fall back to English automatically
- Per-arena resource packs, sent automatically on join and cleared on leave

**Integrations**
- PlaceholderAPI: 14 placeholders under `%newbedwars_*%` for scoreboards, chat formatters, and other plugins. Totally optional, the plugin loads fine without it
- WorldEdit / FastAsyncWorldEdit: build an arena straight from a `.schem` file instead of placing blocks by hand. Also optional, manual setup works fine without it

---

## Requirements

- **Server:** Paper 1.17 – 26.2 *(Spigot will probably work too, but it's untested, so Paper is recommended)*
- **Java:** 16 or higher

Optional: **PlaceholderAPI**, if you want `%newbedwars_*%` placeholders elsewhere on your server, and **WorldEdit** or **FastAsyncWorldEdit**, if you'd rather build arenas from a schematic than by hand. Neither is required, NewBedwars runs fine standalone.

---

## Getting started

1. Grab the jar from the **[latest Modrinth release](https://modrinth.com/plugin/new-bedwars/versions)** and drop it into your server's `/plugins/` folder.
2. Start the server. Config files generate automatically.
3. Set your lobby spawn: `/bw setLobby`
4. Create your first arena: `/bw arena create myArena`
5. Follow the setup checklist shown by `/bw arena`.
6. Save the arena: `/bw save`
7. Enable it when ready: `/bw arena enable myArena`

### Building an arena from a schematic

If you'd rather paste in a finished map than build one from scratch in-game:

1. Save your map as a `.schem` file in WorldEdit's schematics folder (`plugins/WorldEdit/schematics/` or `plugins/FastAsyncWorldEdit/schematics/`).
2. Run `/bw arena create myArena -sche myMap`.
3. The map gets pasted automatically at world origin. Continue with the normal setup checklist from there.

---

## Commands

**Player**

| Command | Description |
|---|---|
| `/bw join [arena]` | Open the arena selector or join a specific arena |
| `/bw rejoin` | Rejoin your previous arena |
| `/bw lobby` | Leave the arena and return to the main lobby |
| `/bw spectate [arena]` | Join a running arena as a spectator |
| `/bw lang <en\|pl\|de\|es\|fr\|ru>` | Change your display language |
| `/bw help` | Show all available commands |
| `/bw stats [player]` | Show a player's stats |
| `/bw profile [player]` | Show a player's stats via GUI |
| `/bw leaderboard [wins\|kills\|beds\|fk]` | Show the top 10 players by a stat |
| `/lobby` | Shortcut for `/bw lobby` |
| `/lang <en\|pl\|de\|es\|fr\|ru>` | Shortcut for `/bw lang` |

**Party**

| Command | Description |
|---|---|
| `/party invite <player>` | Invite a player to your party |
| `/party accept` | Accept a pending invite (clickable in chat) |
| `/party deny` | Deny a pending invite (clickable in chat) |
| `/party leave` | Leave your current party |
| `/party list` | List all party members |
| `/party kick <player>` | Kick a member from your party (admin only) |
| `/party admin <player>` | Transfer the admin role (admin only) |
| `/p` | Alias for `/party` |

**Admin**

| Command | Description |
|---|---|
| `/bw arena create <name> [-sche <schematic>] [-n]` | Create a new arena, optionally from a WorldEdit schematic |
| `/bw arena list` | List all arenas and their current status |
| `/bw arena delete <name>` | Permanently delete an arena |
| `/bw arena enable <name>` | Enable a fully configured arena |
| `/bw arena disable <name>` | Disable a running arena |
| `/bw arena setup <name>` | Enter setup mode for an arena |
| `/bw setLobby` | Set the global lobby spawn point |
| `/bw manageGame` | In-game admin management panel (phase skip, force-start, stop) |
| `/bw reload` | Save arenas, kick all players, and reload the configuration |

---

## Permissions

**Admin**

| Permission | Default | Description |
|---|---|---|
| `newbedwars.bw.admin` | op | Full admin access, grants all sub-permissions below |
| `newbedwars.bw.arena` | op | Manage arenas (grants the 5 sub-permissions below) |
| `newbedwars.bw.arena.create` | op | Create arenas |
| `newbedwars.bw.arena.delete` | op | Delete arenas |
| `newbedwars.bw.arena.enable` | op | Enable arenas |
| `newbedwars.bw.arena.disable` | op | Disable arenas |
| `newbedwars.bw.arena.setup` | op | Enter arena setup mode |
| `newbedwars.bw.setlobby` | op | Set the lobby location |
| `newbedwars.bw.managegame` | op | Manage running games |
| `newbedwars.bw.bypass` | op | Bypass setup-mode restrictions |

**Player**

| Permission | Default | Description |
|---|---|---|
| `newbedwars.bw.player` | true | Bundle, grants all player permissions below |
| `newbedwars.bw.join` | true | Join arenas |
| `newbedwars.bw.lobby` | true | Teleport to the lobby |
| `newbedwars.bw.rejoin` | true | Rejoin after disconnect |
| `newbedwars.bw.spectate` | true | Spectate arenas |
| `newbedwars.bw.lang` | true | Change language |
| `newbedwars.bw.help` | true | View help menu |
| `newbedwars.party` | true | Use all `/party` commands |

Set `RequireJoinPermission: true` or `RequireLobbyPermission: true` in `config.yml` if you want `/bw join` and `/bw lobby` restricted to players with the explicit permission. Both default to `false` (open to everyone).

---

## PlaceholderAPI

If PlaceholderAPI is installed, all of the placeholders below become available under the prefix `%newbedwars_<placeholder>%`.

**In-game (per-player)**

| Placeholder | Description |
|---|---|
| `%newbedwars_arena%` | Arena name the player is in, or `""` |
| `%newbedwars_status%` | Game status: `waiting` / `starting` / `playing` / `ending` / `restarting`, or `lobby` |
| `%newbedwars_team%` | Team name, or `""` |
| `%newbedwars_team_color%` | Team ChatColor code, or `""` |
| `%newbedwars_kills%` | Kills in the current game |
| `%newbedwars_deaths%` | Deaths in the current game |
| `%newbedwars_final_kills%` | Final kills in the current game |
| `%newbedwars_beds_broken%` | Beds broken in the current game |
| `%newbedwars_players%` | Current player count in the arena |
| `%newbedwars_max_players%` | Max players in the arena |
| `%newbedwars_is_spectator%` | `true` / `false` |
| `%newbedwars_phase%` | Current phase name, or `""` |
| `%newbedwars_game_time%` | Seconds elapsed since game start, or `0` |

**Lifetime stats (per-player)**

| Placeholder | Description |
|---|---|
| `%newbedwars_stat_wins%` | Total wins |
| `%newbedwars_stat_losses%` | Total losses |
| `%newbedwars_stat_kills%` | Total kills |
| `%newbedwars_stat_deaths%` | Total deaths |
| `%newbedwars_stat_final_kills%` | Total final kills |
| `%newbedwars_stat_beds%` | Total beds broken |
| `%newbedwars_stat_games%` | Total games played |
| `%newbedwars_stat_kd%` | Kill/death ratio (1 decimal, e.g. `2.3`) |
| `%newbedwars_stat_wl%` | Win/loss ratio (1 decimal) |

**Per-arena**

| Placeholder | Description |
|---|---|
| `%newbedwars_arena_<name>_status%` | Game status of the named arena |
| `%newbedwars_arena_<name>_players%` | Current player count |
| `%newbedwars_arena_<name>_max_players%` | Max players |
| `%newbedwars_arena_<name>_phase%` | Current phase name |

---

## Roadmap

NewBedwars will leave beta once stable operation is verified across all supported server versions (Paper 1.17 – 26.1.2).

- [x] Party support
- [x] PlaceholderAPI support
- [x] WorldEdit / FAWE schematic-based arena creation
- [x] Per-arena resource pack forcing
- [x] More languages (DE, ES, FR, RU) (partial support)
- [x] More custom items (Iron Golem, Silverfish, etc.)
- [x] Arena statistics and leaderboards
- [x] Player profile GUI with stats history
- [ ] In-game map voting system
- [ ] Network / BungeeCord / Velocity mode

---

## Screenshots

### Joining & Waiting
<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_110222.png" width="100%"/><br><sub>Arena selector GUI</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_110543.png" width="100%"/><br><sub>Waiting lobby with bossbar & scoreboard</sub></td>
  </tr>
</table>

### In-Game HUD & Events
<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_114731.png" width="100%"/><br><sub>In-game HUD — scoreboard, phase info, NPC labels</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121249.png" width="100%"/><br><sub>Respawn countdown</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_120913.png" width="100%"/><br><sub>Bed destroyed announcement</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_1205111.png" width="100%"/><br><sub>Trap alarm</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121425.png" width="100%"/><br><sub>End game — spectator view with bossbar</sub></td>
    <td></td>
  </tr>
</table>

### Shop & Upgrades
<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_114843.png" width="100%"/><br><sub>Shop GUI</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_114904.png" width="100%"/><br><sub>Bridge Egg — shop tooltip (cost: 4 Gold)</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_114919.png" width="100%"/><br><sub>Team Upgrades shop</sub></td>
    <td></td>
  </tr>
</table>

### Resource Generators
<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_115559.png" width="100%"/><br><sub>Emerald Generator — hologram</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_120327.png" width="100%"/><br><sub>Diamond Generator — hologram</sub></td>
  </tr>
</table>

### Arena Setup
<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121511.png" width="100%"/><br><sub>Help command output</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121720.png" width="100%"/><br><sub>Arena setup checklist (NORMAL_SETUP)</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121727.png" width="100%"/><br><sub>Team configuration checklist — missing fields highlighted</sub></td>
    <td></td>
  </tr>
</table>

### Map & Environment
<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_122307.png" width="100%"/><br><sub>Arena map — aerial overview</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_123438.png" width="100%"/><br><sub>Live gameplay — combat near a bed 1</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_122037.png" width="100%"/><br><sub></sub>Live gameplay — bed protection</td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_122528.png" width="100%"/><br><sub>Live gameplay — building bridge</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_123625.png" width="100%"/><br><sub>Live gameplay — combat near a bed 2</sub></td>
    <td></td>
  </tr>
</table>

---

## Support & Bug Reports

Found a bug or have a suggestion? Please open an issue on **[GitHub](https://github.com/l299l/NewBedwars-Free/issues)** or use the platform's discussion section. Most problems can be resolved quickly with a good bug report.

---

## License

See [LICENSE.txt](LICENSE.txt).

---

<div align="center">

[![Latest version](https://img.shields.io/modrinth/v/new-bedwars?label=latest%20version&style=flat-square)](https://modrinth.com/plugin/new-bedwars/versions)

**NewBedwars** - Developed by **l299l** - Paper 1.17–26.2

</div>
