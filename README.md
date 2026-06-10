<!-- Replace the URL below with your actual uploaded banner image URL -->
![NewBedwars Banner](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/banner_main.svg)

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
| **Multi-Language** | English and Polish included, fully translatable YAML |
| **TNT Particles** | Red dust particles above players holding TNT |

### ⚙️ Administration
| Feature                 | Description                                                                         |
|-------------------------|-------------------------------------------------------------------------------------|
| **In-Game Setup**       | Configure everything without editing files                                          |
| **Arena Info Command**  | Visual checklist of configured and missing fields                                   |
| **JSON Persistence**    | Arenas saved automatically and survive restarts                                     |
| **Per-Arena Gamerules** | Random teams, spectators, team/global chat, team damage, permanent swords, and more |
| **Manage game command** | Admins can easily forcestart, advance to next phase or stop game                    |

---

## 💻 Commands

### Player Commands
```
/bw join [arena]      — Open arena selector or join a specific arena
/bw rejoin            — Rejoin your previous arena
/bw lobby             — Leave the arena and return to the main lobby
/bw spectate [arena]  — Join a running arena as spectator
/bw lang <en|pl>      — Change your display language
/bw help              — Show all available commands
/lobby                — Shortcut: teleport to lobby
/lang <en|pl>         — Shortcut: change language
```

### Admin Commands
```
/bw arena create <name>         — Create a new arena
/bw arena delete <name>         — Permanently delete an arena
/bw arena enable <name>         — Enable a fully configured arena
/bw arena disable <name>        — Disable a running arena
/bw arena setup <name>          — Enter setup mode for an arena
/bw setLobby                    — Set the global lobby spawn point
/bw setupGuis                   — Open the GUI configuration tool
/bw manageGame                  — In-game admin management panel (phase skip, etc.)
```

[//]: # (#### Inside Setup Mode)

[//]: # (```)

[//]: # (/bw arena                       — Show full arena status and missing fields)

[//]: # (/bw createTeam <name> <color>   — Create a team)

[//]: # (/bw setSpawn <team>             — Set team spawn point)

[//]: # (/bw setBed <team>               — Set team bed location)

[//]: # (/bw setShop <team>              — Set team shop NPC location)

[//]: # (/bw setUpgrades <team>          — Set team upgrades NPC location)

[//]: # (/bw setGenerator [team]         — Set iron/gold &#40;team&#41; or diamond/emerald &#40;global&#41; generator)

[//]: # (/bw setBuildProtPos1/2 <team>   — Define build-protection zone)

[//]: # (/bw setBasePos1/2 <team>        — Define team base zone &#40;for traps & heal pool&#41;)

[//]: # (/bw setQuickVoidY <y>           — Set the quick-void Y threshold)

[//]: # (/bw setWaitingPos1/2            — Define waiting lobby zone)

[//]: # (/bw setWaitingSpawn             — Set waiting area spawn)

[//]: # (/bw setWaitingTime <seconds>    — Set pre-game countdown)

[//]: # (/bw save                        — Save all arena settings to disk)

[//]: # (/bw leave                       — Exit setup mode)

[//]: # (```)

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

> **Config:** Set `RequireJoinPermission: true` or `RequireLobbyPermission: true` in `config.yml` to restrict `/bw join` and `/bw lobby` to players with the explicit permission. Both default to `false` (open to everyone).

---

## 📦 Requirements

- **Server:** Paper 1.17 – 26.1.2 *(Spigot likely compatible but untested — Paper recommended)*
- **Java:** 16 or higher
- **No external plugins required**

---

## 🚀 Installation

1. Download **NewBedwars-1.0.0-beta.jar**
2. Drop it into your server's `/plugins/` folder
3. Start the server — config files generate automatically
4. Set your lobby spawn: `/bw setLobby`
5. Create your first arena: `/bw arena create myArena`
6. Follow the checklist shown by `/bw arena`
7. Save arena by `/bw save`
8. Enable when ready: `/bw arena enable myArena`

---

## 🗺️ Future Plans

- [ ] Party support
- [ ] PlaceholderAPI support
- [ ] WordEdit support
- [ ] Arena statistics and leaderboards
- [ ] More languages (DE, ES, FR, RU)
- [ ] Schematic-based arena import/export
- [ ] Per-arena resource pack forcing
- [ ] Network / BungeeCord / Velocity mode
- [ ] Custom death animations and kill effects
- [ ] In-game map voting system
- [ ] Player profiles with their own GUI configurations and stats

---

## 🐛 Support & Bug Reports

Found a bug or have a suggestion? Please open an issue on **[GitHub](https://github.com/l299l/NewBedwars-Free/issues)** or use the platform's discussion section.

> Most problems can be resolved quickly with a bug report.

---

## 📸 Screenshots

*Screenshots coming soon*

---

<div align="center">

**NewBedwars v1.0.0-beta** — Made with ❤️ by **l299l** — Paper 1.17 – 26.1.2

</div>
