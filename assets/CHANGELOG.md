# Changelog

## v1.0.0-beta — Initial Release

### Added
- **Multi-arena support** — unlimited simultaneous arenas, each in its own world
- **Team system** — configurable team count, colors, and player limits
- **YAML-driven shop & upgrade GUIs** — fully configurable without code changes
- **Resource generators** — Iron, Gold, Diamond, Emerald with phase-based level upgrades
- **Game phase system** — define custom phases with durations and triggered actions (generator upgrades, bed destruction, dragon spawn, game end)
- **Ender Dragon event** — per-team dragons spawn and attack enemies; strength scales with Dragon Buff upgrade level
- **Team upgrades** — Forge, Protection, Sharpness, Haste, Heal Pool, Alarm/Blind/Mining Fatigue Traps, Dragon Buff
- **Custom shop items** — Bridge Egg, Fireball, Speed/Jump/Invisibility potions, Blast Protection Glass
- **Build protection zones** — per-team areas where players cannot place blocks
- **Base zones** — define team base areas for traps and Heal Pool
- **Quick void** — instant kill below a configurable Y level
- **Rejoin system** — players can reconnect within a configurable time window
- **Traps** — triggered when an enemy enters a team's base
- **Spectator mode** — fly, compass player-teleport GUI, effects GUI; available to both eliminated players and external viewers via `/bw spectate`
- **Arena chat isolation** — each arena has its own chat channel; lobby players and other arenas are completely isolated. Use `!message` for team-only chat. `AllowTeamChat` and `AllowGlobalChat` gamerules control per-arena chat behaviour
- **Tab list isolation** — each arena has its own tab list; lobby players are hidden from arena players and vice versa
- **Bossbar** — YAML-configurable templates for waiting, in-game, and ending states
- **Scoreboard** — per-player scoreboards with full template support
- **In-game setup system** — configure arenas entirely in-game with Normal, Advanced, and Automatic setup modes
- **Per-arena gamerules** — random teams, spectators, team/global chat, team damage, permanent swords, and more
- **JSON arena persistence** — arenas save automatically and survive restarts
- **Party system** — players can group up and join arenas together
- **Multi-language** — English and Polish included; fully translatable YAML language files with auto-update on plugin reload
