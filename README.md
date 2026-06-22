<!-- Replace the URL below with your actual uploaded banner image URL -->
![NewBedwars Banner](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/banner_main.svg)

<div align="center">

[![SpigotMC](https://img.shields.io/badge/SpigotMC-Download-ED8106?style=for-the-badge&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==)](https://www.spigotmc.org/resources/new-bedwars.136050/)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-1BD96A?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/new-bedwars)
[![Hangar](https://img.shields.io/badge/Hangar-Download-0EA5E9?style=for-the-badge)](https://hangar.papermc.io/l299l/New-Bedwars)

</div>

---

## 📖 Overview

**NewBedwars** is a modern, fully configurable Bedwars plugin built from the ground up for **Paper 1.17+**. It delivers the classic Bedwars experience with a powerful in-game setup system, YAML-driven GUIs, multi-arena support, and deep gamerule control — all without requiring any additional plugin dependencies.

Whether you're running a small survival server looking to add a Bedwars minigame, or building a dedicated Bedwars network, NewBedwars gives you the tools to create polished, production-ready arenas with minimal configuration effort.

> ⚠️ **This is a beta release.** Core gameplay is stable and feature-complete. Please report any issues via the GitHub issue tracker.

---

## ✨ Key Features

### ⚔️ Gameplay
| Feature | Description |
|---------|-------------|
| **Multi-Arena** | Run unlimited simultaneous arenas, each with its own world |
| **Team System** | Configurable team count, colors, player limits |
| **Quick Void** | Instant death below a configurable Y level |
| **Build Protection** | Per-team zones where players cannot place blocks |
| **Rejoin System** | Players can reconnect within a configurable time window |
| **Traps** | Trigger team traps when enemies enter your base area |
| **Party System** | Group up with friends via `/party`; invite with clickable chat buttons; party admin leads the whole group into an arena together |

### 💬 Chat & Visibility
| Feature | Description |
|---------|-------------|
| **Arena Chat Isolation** | Each arena has its own chat — lobby players and other arenas are completely isolated |
| **Team Chat** | Type `!message` to send a team-only message, controlled per arena via gamerule |
| **Global Arena Chat** | Optional arena-wide chat channel, toggle per arena via gamerule |
| **Tab List Isolation** | Each arena gets its own tab list — lobby players are hidden from arena players and vice versa |

### 🛒 Shop & Economy
| Feature | Description |
|---------|-------------|
| **YAML Shop GUIs** | Fully configurable item shops — no code changes needed |
| **Team Upgrades** | Forge, Traps, Heal Pool, Dragon Buff, and more |
| **Bridge Egg** | Creates a wool bridge along its flight path |
| **Fireball** | Custom fireball item with arena-safe explosion |
| **Potions** | Speed, Jump, and Invisibility custom potions |
| **Resource Generators** | Iron, Gold, Diamond, Emerald with phase-based leveling |

### 🐉 Game Phases
| Feature | Description |
|---------|-------------|
| **Configurable Phases** | Define phase names, durations, and action triggers in `config.yml` |
| **Generator Upgrades** | Automatically upgrade Diamond/Emerald generators at phase transitions |
| **Bed Destruction** | Trigger global bed destruction at a configured phase |
| **Ender Dragon** | Spawn per-team dragons that fire fireballs at enemies; level based on Dragon Buff upgrade |
| **Regen Disable** | Remove natural health regeneration in late game |
| **Forge Disable** | Remove team iron/gold generators in final phase |

### 🎭 Display & HUD
| Feature | Description |
|---------|-------------|
| **Bossbar** | YAML templates for waiting, playing, and ending states |
| **Scoreboard** | Per-player scoreboards with full placeholder support |
| **Spectator Mode** | Fly around the arena; compass teleports to live players |
| **Multi-Language** | English, Polish, German, Spanish, French, and Russian — missing keys fall back to English automatically |
| **TNT Particles** | Red dust particles above players holding TNT |
| **Per-Arena Resource Pack** | Force a custom resource pack for each arena; automatically sent on join and cleared on leave |

### 🔌 Integrations
| Feature | Description |
|---------|-------------|
| **PlaceholderAPI** | 14 placeholders under `%newbedwars_*%` for scoreboards, chat formatters, and external plugins *(soft dependency — plugin loads without it)* |
| **WorldEdit / FAWE** | Create arenas directly from `.schem` / `.schematic` files saved in the WorldEdit schematics folder *(soft dependency — manual setup still works without it)* |

### ⚙️ Administration
| Feature                 | Description                                                                         |
|-------------------------|-------------------------------------------------------------------------------------|
| **In-Game Setup**       | Configure everything without editing files                                          |
| **Arena Info Command**  | Visual checklist of configured and missing fields                                   |
| **JSON Persistence**    | Arenas saved automatically and survive restarts                                     |
| **Per-Arena Gamerules** | Random teams, spectators, team/global chat, team damage, permanent swords, and more |
| **Gamerule Command**    | Set any gamerule live from setup mode via `/bw gamerule <name> <true\|false>`       |
| **Manage Game Command** | Admins can easily force-start, advance to next phase, or stop a running game        |

---

## 💻 Commands

### Player Commands
```
/bw join [arena]      — Open arena selector or join a specific arena
/bw rejoin            — Rejoin your previous arena
/bw lobby             — Leave the arena and return to the main lobby
/bw spectate [arena]  — Join a running arena as spectator
/bw lang <en|pl|de|es|fr|ru>  — Change your display language
/bw help              — Show all available commands
/lobby                — Shortcut: teleport to lobby
/lang <en|pl|de|es|fr|ru>     — Shortcut: change language
```

### Party Commands
```
/party invite <player>  — Invite a player to your party
/party accept           — Accept a pending invite (clickable in chat)
/party deny             — Deny a pending invite (clickable in chat)
/party leave            — Leave your current party
/party list             — List all party members
/party kick <player>    — Kick a member from your party (admin only)
/party admin <player>   — Transfer the admin role (admin only)
/p                      — Alias for /party
```

### Admin Commands
```
/bw arena create <name> [-sche <schematic>] [-n]  — Create a new arena (optionally from a WorldEdit schematic)
/bw arena list                  — List all arenas and their current status
/bw arena delete <name>         — Permanently delete an arena
/bw arena enable <name>         — Enable a fully configured arena
/bw arena disable <name>        — Disable a running arena
/bw arena setup <name>          — Enter setup mode for an arena
/bw setLobby                    — Set the global lobby spawn point
/bw setupGuis                   — Open the GUI configuration tool
/bw manageGame                  — In-game admin management panel (phase skip, force-start, stop)
/bw reload                      — Save arenas, kick all players, and reload the configuration
```

### Setup Mode Commands
```
/bw gamerule <name> <true|false>             — Set an arena gamerule (tab-completes names and values)
/bw setResourcePack <url|clear> [sha1hash]   — Set or clear a per-arena resource pack URL
```

---

## 🔑 Permissions

### Admin Permissions
| Permission | Default | Description |
|------------|---------|-------------|
| `newbedwars.bw.admin` | op | Full admin access — grants all sub-permissions below |
| `newbedwars.bw.arena` | op | Manage arenas (grants the 5 sub-permissions below) |
| `newbedwars.bw.arena.create` | op | Create arenas |
| `newbedwars.bw.arena.delete` | op | Delete arenas |
| `newbedwars.bw.arena.enable` | op | Enable arenas |
| `newbedwars.bw.arena.disable` | op | Disable arenas |
| `newbedwars.bw.arena.setup` | op | Enter arena setup mode |
| `newbedwars.bw.setlobby` | op | Set the lobby location |
| `newbedwars.bw.setupguis` | op | Open the GUI editor |
| `newbedwars.bw.managegame` | op | Manage running games |
| `newbedwars.bw.bypass` | op | Bypass setup-mode restrictions |

### Player Permissions
| Permission | Default | Description |
|------------|---------|-------------|
| `newbedwars.bw.player` | true | Bundle — grants all player permissions below |
| `newbedwars.bw.join` | true | Join arenas |
| `newbedwars.bw.lobby` | true | Teleport to the lobby |
| `newbedwars.bw.rejoin` | true | Rejoin after disconnect |
| `newbedwars.bw.spectate` | true | Spectate arenas |
| `newbedwars.bw.lang` | true | Change language |
| `newbedwars.bw.help` | true | View help menu |
| `newbedwars.party` | true | Use all `/party` commands |

> **Config:** Set `RequireJoinPermission: true` or `RequireLobbyPermission: true` in `config.yml` to restrict `/bw join` and `/bw lobby` to players with the explicit permission. Both default to `false` (open to everyone).

---

## 📦 Requirements

- **Server:** Paper 1.17 – 26.1.2 *(Spigot likely compatible but untested — Paper recommended)*
- **Java:** 16 or higher

### Soft Dependencies *(optional)*
| Plugin | Purpose |
|--------|---------|
| **PlaceholderAPI** | Enables `%newbedwars_*%` placeholders for scoreboards, chat formatters, etc. |
| **WorldEdit** or **FastAsyncWorldEdit** | Required only for schematic-based arena creation (`-sche` flag) |

---

## 🚀 Installation

1. Download **NewBedwars-2.0-beta.jar**
2. Drop it into your server's `/plugins/` folder
3. Start the server — config files generate automatically
4. Set your lobby spawn: `/bw setLobby`
5. Create your first arena: `/bw arena create myArena`
6. Follow the checklist shown by `/bw arena`
7. Save arena by `/bw save`
8. Enable when ready: `/bw arena enable myArena`

### Creating an arena from a schematic
1. Save your map as a `.schem` file in WorldEdit's schematics folder (`plugins/WorldEdit/schematics/` or `plugins/FastAsyncWorldEdit/schematics/`)
2. Run: `/bw arena create myArena -sche myMap`
3. The map is pasted automatically at world origin; continue with normal setup

### PlaceholderAPI placeholders
| Placeholder | Returns |
|-------------|---------|
| `%newbedwars_arena%` | Arena name the player is in |
| `%newbedwars_status%` | Arena status (waiting / starting / playing / ending) |
| `%newbedwars_team%` | Player's team name |
| `%newbedwars_kills%` | Player's kill count this game |
| `%newbedwars_final_kills%` | Player's final kill count this game |
| `%newbedwars_beds_broken%` | Beds the player has broken this game |
| `%newbedwars_players%` | Current player count in the arena |
| `%newbedwars_max_players%` | Maximum players for the arena |
| `%newbedwars_is_spectator%` | `true` / `false` |
| `%newbedwars_phase%` | Current game phase name |
| `%newbedwars_arena_<name>_status%` | Status of a specific arena by name |
| `%newbedwars_arena_<name>_players%` | Player count of a specific arena |
| `%newbedwars_arena_<name>_max_players%` | Max players of a specific arena |
| `%newbedwars_arena_<name>_phase%` | Phase of a specific arena |

---

## 🗺️ Future Plans

> **Beta exit:** NewBedwars will leave beta once stable operation is verified across all supported server versions (Paper 1.17 – 26.1.2).

- [x] Party support
- [x] PlaceholderAPI support
- [x] WorldEdit / FAWE schematic-based arena creation
- [x] Per-arena resource pack forcing
- [x] More languages (DE, ES, FR, RU)
- [ ] More custom items (Iron Golem, Silverfish, etc.)
- [ ] Arena statistics and leaderboards
- [ ] In-game map voting system
- [ ] Player profile GUI with stats history
- [ ] Network / BungeeCord / Velocity mode

---

## 🐛 Support & Bug Reports

Found a bug or have a suggestion? Please open an issue on **[GitHub](https://github.com/l299l/NewBedwars-Free/issues)** or use the platform's discussion section.

> Most problems can be resolved quickly with a bug report.

---

## 📸 Screenshots

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

<div align="center">

**NewBedwars v2.0-beta** — Made with ❤️ by **l299l** — Paper 1.17 – 26.1.2

</div>
