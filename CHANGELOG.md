# Changelog

All notable changes to NewBedwars are documented here.

---

## [2.1-beta] — 2026-06-24

### Added
- **Arena statistics & leaderboards** — wins, losses, kills, deaths, final kills, beds broken, and games played are now persisted to `data/playerStats.json` (or the `Players` MySQL table) and updated at the end of every game; `/bw leaderboard [wins|kills|beds|fk]` shows the top 10 in chat
- **Player profile GUI** — `/bw stats [player]`/`/bw profile [player]` opens a profile GUI showing all lifetime stats; K/D and W/L ratios are displayed alongside raw counts
- **Fast-buy slot customization** — players can rearrange the home-page fast-buy slots in the shop via the "Customize Fast-Buy" button in their profile GUI; preferences are saved per-player and persist across sessions
- **Iron Golem custom item** — spawns a iron golem that  attacks enemies
- **Silverfish custom item** — releases a swarm of 5 silverfish from the buyer's feet that automatically despawn after 30 seconds
- **Portable Tower custom item** —  instantly constructs a team-colored wool tower with a ladder
- **Expanded PlaceholderAPI support** — new placeholders: `%newbedwars_deaths%`, `%newbedwars_team_color%`, `%newbedwars_game_time%`; full lifetime stats via `%newbedwars_stat_wins%`, `stat_losses`, `stat_kills`, `stat_deaths`, `stat_final_kills`, `stat_beds`, `stat_games`, `stat_kd`, `stat_wl`

### Fixed
- **`cancelStart()` NPE** — `countdownTimer` is now null-checked before cancellation
- **`GamePhases` out-of-bounds** — `getCurrentPhase()` and `start()` now guard against `currentPhase >= phases.size()`
- **Respawn upgrade not applied** — `respawnPlayer()` now null-checks `getTeamUpgrades()` before calling `applyPlayerUpgrades()`
- **Win check silently discarded** — the empty `if (checkWin()) {}` block in `killPlayer()` was replaced with a direct `checkWin()` call so game endings trigger correctly
- **`getGameTime()` always returned 0** — game-start timestamp is now recorded in `start()` and elapsed seconds are computed correctly
- **Respawn task leaked on arena shutdown** — the `BukkitRunnable` for player respawn is now stored in `GamePlayer` and cancelled in both `stop()` and `forceShutdown()`
- **`arena_<name>_max_players` placeholder broken** — the per-arena placeholder parser now matches known multi-word suffixes (longest first) instead of using `lastIndexOf('_')`, which was splitting `max_players` incorrectly

### Changed
- **Shop prices rebalanced** 
- **Team-generator rebalanced**

### TODO
- **Fix portable tower direction**
- **Add fast-buy slot reset**
- **Add language support for newest features**
- **Add missing permissions**
---

## [2.0-beta] — 2026-06-22

### Added
- **WorldEdit / FAWE schematic support** — arenas can now be created from a `.schem` / `.schematic` file saved in the WorldEdit or FAWE schematics folder; use `/bw arena create <name> -sche <schematic>` (WorldEdit or FastAsyncWorldEdit soft dependency)
- **PlaceholderAPI support** — `NewBedwarsExpansion` registers 14 placeholders under `%newbedwars_*%`; PlaceholderAPI is a soft dependency so the plugin loads normally without it
  - Per-player: `%newbedwars_arena%`, `%newbedwars_status%`, `%newbedwars_team%`, `%newbedwars_kills%`, `%newbedwars_final_kills%`, `%newbedwars_beds_broken%`, `%newbedwars_players%`, `%newbedwars_max_players%`, `%newbedwars_is_spectator%`, `%newbedwars_phase%`
  - Per-arena: `%newbedwars_arena_<name>_status%`, `%newbedwars_arena_<name>_players%`, `%newbedwars_arena_<name>_max_players%`, `%newbedwars_arena_<name>_phase%`
- **Per-arena resource pack forcing** — `/bw setResourcePack <url|clear> [sha1hash]` sets a resource pack that is sent to every player who joins the arena; SHA-1 hash optional; use `clear` to remove
- **`/bw gamerule <name> <true|false>`** — set any per-arena gamerule from within setup mode without editing files; tab-completes both gamerule names and boolean values
- **More languages** — German (`de`), Spanish (`es`), French (`fr`), and Russian (`ru`) language files added; missing keys automatically fall back to English so partial translations still work fully

### Fixed
- **Sharpness not applied on sword purchase** — buying a sword from the shop now immediately applies the team's current Sharpness upgrade level; previously the enchant was only present after death/respawn
- **Potion effects broken on Paper 1.20.1** — right-clicking a custom potion now reliably applies the effect on all supported server versions; a two-layer approach (cancel in `PlayerInteractEvent` + fallback `PlayerItemConsumeEvent`) with next-tick scheduling ensures exactly one effect application regardless of how a specific Paper build handles the drinking animation
- **Blast-proof glass not protecting blocks below** — explosion handler now also removes from the explosion list any placed block that has blast-proof glass directly above it (one Y-level up)
- **Player can drop sword with Q key** — `PlayerDropItemEvent` now cancels sword drops during an active game while still allowing players to move items into chests
- **Schematic error shows literal `/name/`** — the `SchematicNotFound` message now substitutes the actual schematic name into the `/name/` placeholder before sending
- **`Objective.isRegistered()` compile error in `NScoreboard`** — `isRegistered()` does not exist in the Paper 1.17 API; replaced with plain `obj != null` checks
- **Arenas not cleaned up on server disable or reload** — `onDisable()` now calls `forceShutdown()` on every arena (cancels all tasks, teleports players to lobby, clears scoreboards and boss bars, removes ender dragons — skips world rollback which is unsafe during JVM shutdown); `reloadAll()` calls `stop()` (full rollback) on arenas with active players before clearing the arena maps, so worlds are cleanly reset and reloaded correctly
- **Schematic lookup only searched plugin folder** — schematic resolution now checks the WorldEdit / FAWE schematics directory as a fallback when the file is not found in the plugin's own folder
- **26.2 support**
---

## [1.0.3-beta] — 2026-06-15

### Fixed
- **Potion items not working on 1.21+** — `PlayerItemConsumeEvent` now applies the configured potion effect and removes the item manually; previously the event was cancelled with no effect applied, leaving the potion permanently in inventory on newer server versions
- **Armor can be bought multiple times** — purchasing a `GIVE_ARMOR` item is now blocked when the player's current armor tier is equal to or higher than the item being bought (prevents wasting currency on redundant purchases)
- **Sharpness and Haste lost on death** — upgrade effects are now re-applied on respawn after potion effects are cleared; previously only armor protection was restored
- **QuickVoid not killing armored players** — void kill damage changed from `health + 1` to `maxHealth × 1000`; armor can absorb up to 80 % of damage, so `health + 1` left heavily-armored players alive
- **Broken placed wool drops vanilla item** — breaking a placed wool block now suppresses the default drop and gives back one unit of the custom team-colored `Wool` shop item instead of a plain named block (e.g. `Red Wool`)
- **Wool block break returned 16 items** — the wool refund was accidentally using the shop stack size (16); now correctly gives back 1 wool per broken block
- **GUI category icons showed item cost and description** — category buttons in the shop GUI now show only the category name and an optional configurable description; the underlying item's price/amount lore is no longer inherited

### Added
- **`Description` field for shop GUI categories** — each category in a GUI YAML file can now define an optional `Description:` key that is shown as the lore on the category button (example values added to `exampleGui.yml` and `exampleGui2.yml`)

---

## [1.0.2-beta] — 2026-06-13

### Added
- **Party system** — group up with friends via `/party` (`/p` alias)
  - `/party invite <player>` sends a clickable `[Accept]` / `[Deny]` chat component (Adventure API, compatible with all supported server versions)
  - Invites expire automatically after 60 seconds
  - Party admin is the only member who can initiate a `/bw join`; all online members enter together
  - `/party leave`, `/party kick`, `/party admin` (transfer), `/party list`
- **`AllowParties` gamerule** — per-arena toggle to permit or block party joins (configurable via `/bw setupGuis`)
- **Party-aware arena selector GUI** — joining through the GUI now enforces party admin/member roles and checks party size against `maxInTeam`

### Fixed
- `ConfigUpdater` NPE on first startup when `CustomItemsNames` section was absent from language files on disk
- `DataManager.save()` NPE in `onDisable()` when startup had failed mid-way (dataManager was null)
- Party members could bypass the party-admin join restriction by clicking an arena in the selector GUI

### Internal
- Lobby location persistence extracted from `NewBedwars.java` into `LobbyData` utility class (`config/data/LobbyData.java`)
- `PartyManager` refactored to use constructor injection (`Plugin`, `Messages`) — no longer depends on `NewBedwars.plugin` static reference
- Added JUnit 5 + Mockito test suite: 54 tests across `PartyTest`, `PartyManagerTest`, `LangMessagesTest`, `LobbyDataTest`, `JoinCommandTest`, `PartyCommandTest`

---

## [1.0.1-beta] — 2026-06-11

### Fixed
- Various crash and stability fixes from the initial beta release

---

## [1.0.0-beta] — 2026-06-10

### Added
- Initial beta release
- Multi-arena support with per-world isolation
- Team system (configurable count, colors, player limits)
- YAML-driven shop and upgrade GUIs
- Configurable game phases with timed actions (generator upgrades, bed destruction, ender dragon, forge disable)
- Resource generators — Iron, Gold, Diamond, Emerald with phase-based leveling
- Boss bar and per-player scoreboard HUD with placeholder support
- Spectator mode with compass teleport
- Arena chat isolation and team chat (`!message`)
- Tab list isolation between arenas and lobby
- Rejoin system with configurable time window
- Traps and heal pool team upgrades
- Bridge Egg, Fireball, and custom Potion shop items
- TNT particle indicator
- Full in-game setup system (no file editing required)
- JSON arena persistence (survives restarts)
- Per-arena gamerules (random teams, team damage, permanent swords, spectators, AllowParties, and more)
- Multi-language support — English and Polish, fully translatable YAML
- MySQL and JSON player data storage backends
