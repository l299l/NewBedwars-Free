# Changelog

All notable changes to NewBedwars are documented here.

## [2.6-beta] - Coming soon

### Added
- **Group placeholders** - `%newbedwars_group%` (group of the player's arena) and `%newbedwars_group_<name>_players|playing|waiting|max_players|arenas|arenas_free|best_arena%` for scoreboards, holograms and menus
- **Map voting** - Put arenas with the same team size into groups (`/bw group`). Players can join a group with `/bw join <group>` or a join NPC. When a game in a grouped arena ends, everyone still there gets a vote item and votes for the next map among the group's other free arenas; when the after-game time is up, everyone joins the map with the most votes (ties are random, parties stay together). Turn it off for single arenas with the `MapVote` gamerule. New permission `newbedwars.bw.group`. The default `AfterGameWatchTime` is now 30 seconds
- **Copy arenas** - `/bw arena copy <arena> <newName>` copies an arena with its settings and map, so one map can run several games at once. The copy joins the same group
- **Lobby join NPCs** - Click an NPC in the lobby to join the fullest free arena of a group, join a specific arena or open the arena selector, with a live hologram above it showing players and free arenas. Make the NPC with Citizens and attach it with `/bw npc set`, or without Citizens spawn a simple one with `/bw npc create`. Parties follow the same rules as `/bw join`. New permission `newbedwars.bw.npc`, hologram lines under `JoinNpc` in `config.yml`
- **More translated texts** - The setup info (`/bw arena` in setup mode and its menu), the arena selector, spectate selector, profile, fast-buy editor and upgrade menus, the party help and invite buttons, the `/bw game` messages and the reload kick message now use the language files (English and Polish included; other languages fall back to English)

### Fixed
- **Leaving while respawning** - Leaving the game (lobby command, leave item, disconnect) while waiting to respawn no longer keeps the respawn countdown running and throws an error
- **One game to rejoin** - Joining another arena while you could still rejoin a game now gives up that game (you get a message) instead of keeping both, so nobody ends up with several games to rejoin. The arena selector now uses the same checks as `/bw join`
- **Broken arena maps after saving** - Saving or enabling an arena right after changes could copy half-written world data into its map copy ("Unexpected end of ZLIB input stream" after the next reset). Incomplete files are now copied again until they are complete
- **Spectators falling into the void** - Spectators (during the end of a game) and players in the waiting lobby are sent back to the waiting spawn when they fall below the Quick Void level or the bottom of the world, instead of dying and breaking their state. Spectators can no longer take damage
- **Team area setup commands** - `/bw setTeamBasePos1/2` and `/bw setTeamBuildProtAreaPos1/2` now accept `<x> <y> <z>` like the other position commands (tab completion already suggested them). The help of `/bw setWaitingPos1/2` and `/bw setWaitingSpawn` shows the right command name
- **Arena info** - `/bw arena` in setup mode (chat view) now also lists the `MapVote` gamerule, like the GUI. Its "ready" hint now names the right command (`/bw arena enable <arena>`)
- **Special characters in default files** - Bundled language, boss bar, scoreboard and generator files are read as UTF-8, so accents and symbols are no longer garbled on servers with another default charset

---

## [2.5-beta] - 30.10.2026

### Added
- **Addon extensions** - Addons can now register their own shop items, upgrades/traps, armor tiers, phase actions and `/bw` subcommands, store their own data in arenas and teams, and edit arenas (teams, spawns, beds, generators) while they are waiting
- **Team-colored blocks in the shop** - Any shop item made of a white block (wool, terracotta, stained glass, glass pane, concrete, concrete powder, carpet) now takes the buyer's team color, not just wool. Broken team blocks return to the player as the shop item
- **Addon API and events** - Other plugins can now hook into NewBedwars: read arenas, teams and players through `NewBedwarsAPI`, and listen to events for game start/end, joining and leaving, bed breaks, kills, shop purchases, team upgrades, armor, map resets and sending players to the lobby
- **Automatic team assignment** - Teams are now assigned when the game starts instead of when joining, balanced evenly, and party members are kept together when possible
- **`SplitPartiesToStart` gamerule** - Controls what happens when everyone in the arena would end up in one team (e.g. a single big party): `true` (default) splits the party over other teams so the game can start, `false` keeps waiting until players fill at least two teams
- **New map reset system** - Every arena now keeps a pristine copy of its map. After each game the map is replaced by that copy, so crashes, restarts or accidental saves can no longer damage a map. If the server stops during a game, the map is restored on the next start. Use `/bw save` (or enable the arena) after building to update the copy. The old system can be brought back with `Rollback.UseLegacySystem: true` in `config.yml`
- **Arena backups** - New commands `/bw arena backup|backups|restore|export|import|resetmap`. Arena files are also backed up automatically on every save (`Backups.Keep` in `config.yml`)
- **Safer arena saving** - Arena files are written safely (a crash while saving can no longer corrupt them), names with special characters are saved correctly, and arenas with missing spawns or beds now load disabled with the problems shown in `/bw arena list` instead of failing half-loaded
- **Improved `/bw setupHelp`** - Now paginated and clickable like `/bw help`, shows only the commands of your current setup mode, and `/bw setupHelp <command>` shows its description, syntax and example

### Fixed
- **Team colors in arena messages** - Player names in join/leave, kill, bed destruction and arena chat messages, and the winning team name, are now shown in their team color
- **Spectators at the end of a game** - Spectators now see the winner message and title, and the arena restart countdown
- **Team bed command** - `/bw setTeamBed` with a wrong number of arguments or a bad coordinate now shows the usage instead of an error
- **Boss bars** - Boss bar animations no longer pile up and keep running after a game, titles with `/players/` or `/phase/` now stay up to date, the waiting-lobby boss bar comes back after a game, and a short or incomplete bar config no longer breaks loading
- **Player count in the lobby** - The boss bar, scoreboard, arena list and placeholders now show the real number of players while the arena is still waiting
- **Scoreboards** - Scoreboards are no longer updated off the main thread and no longer fail on 1.17 because of long names
- **Messages with special characters** - Arena, team or player names containing `$` or `\` no longer break messages
- **Generators config** - Saving settings can no longer overwrite `defaultGenerators.yml` with a custom generator file
- **Parties** - Disbanding a party now cancels its open invites, and kicking or promoting someone who is not in the party does nothing
- **Fast-buy customizer** - More than 8 categories no longer overwrite the Reset and Save buttons
- **Game phases with several arenas** - Running the same game phases in more than one arena no longer resets or speeds up the other arenas' countdowns, and a failing phase action no longer skips the rest of the phase
- **Shop tools and armor** - Buying a tool replaces your old one of that kind where you put it instead of overwriting other items in your hotbar or duplicating your sword; buying a pickaxe no longer removes your sword's Sharpness; items that do not fit your inventory drop at your feet; armor that no tier knows is refused before you pay
- **Arena names** - `/bw arena create` now only accepts letters, numbers, `_` and `-`, and refuses a name that is already a world on the server; deleting an arena no longer deletes a world that could not be unloaded
- **Player stats** - Stats are now saved safely in the background, so a crash or two games ending together can no longer corrupt `playerStats.json`
- **Forge upgrade** - The first Forge level now upgrades the team generator (it did nothing), and each level speeds up the generator by one tier
- **Team generator speed** - The spawn delay of team generators now follows the generator level instead of staying at the level 1 delay
- **Diamond and emerald generators** - Items now drop exactly after the configured delay (they came one second late) and the hologram counts down correctly
- **Generator amounts** - `MaxAmount` now means a random amount from 1 to the configured maximum, a `MaxAmount` of 0 drops nothing, and a too short `SpawnDelay` list or a missing resource section in a generator config no longer breaks the game.
- **Rebalanced generators** - The default generator config was rebuilt around real resource income: team generators give about 1 iron/s and 0.14 gold/s at the start and grow with every Forge level up to about 3 iron/s and 0.43 gold/s (emeralds from Forge 3, diamonds from Forge 5), the diamond generator now spawns every 30 / 24 / 12 seconds and the emerald generator every 56 / 40 / 28 seconds. The team generator now has 9 levels so all 8 Forge purchases count. Existing servers keep their current `generators/defaultGenerators.yml`; delete it and restart to get the new values
- **Shop and upgrade NPCs** - A zombie NPC no longer sometimes spawns as a baby zombie
- **Items in shop/upgrade GUI** - Players can no longer move items into the shop, upgrade and other menus by shift-clicking them from their inventory
- **Game ending instantly with a party** - A party joining alone no longer starts a game that immediately ends with "your team wins"
- **Iron golem / silverfish kills** - When a player is killed by a golem or silverfish, their resources now drop on the ground instead of going to the owner (the owner still gets the kill)
- **Rejoin timer** - The rejoin countdown is now cancelled when the player rejoins, the game ends, or their team is eliminated, so players can't come back to a lost team or be removed from a later game
- **Party admin quitting** - When the party admin disconnects, the admin role now passes to another online member; a party with nobody online is disbanded, and pending invites from quitting players are removed
- **Game ending when everyone leaves** - If all remaining players leave (or every team is eliminated at once), the game now ends as a draw and the arena resets instead of staying stuck
- **Arena capacity** - An arena can no longer be joined by one player more than its maximum
- **Party join countdown** - Joining as a party now starts the countdown correctly when the minimum player count is reached
- **Leaving during countdown** - Leaving while the game is starting no longer cancels the countdown if enough players remain

### Removed
- **Lava bucket** - Removed from the default shop (Misc category). Servers that already have a shop file keep it until the item is removed from `data/guis/exampleShop.yml`

---

## [2.4-beta] - 22.10.2026

### Added
- **Configurable iron golem lifetime** - Iron golems now despawn after a configurable time period (default: 120 seconds, configurable via `IronGolemLifetime` in `config.yml`)
- **Iron golem spawn limit per team** - Teams can now only have one active iron golem at a time by default (configurable via `AllowMultipleGolems` gamerule, default: false)
- **Spectate selection GUI for `/bw spectate`** - Typing `/bw spectate` without an arena name now opens a GUI showing all enabled arenas, including those that cannot be spectated (grayed out)
- **Arena info GUI** - `/bw arena gui` shows a visual multi-page GUI with arena settings, team details, and generator counts while in setup mode
- **Advanced setup mode** - New setup mode that provides access to gamerules, resource packs, custom GUIs, and generator leveling configuration
- **`/bw setupMode` command** - Reopens the setup mode selection GUI at any time during setup to switch between normal and advanced modes
- **Team accessories location** - `/bw teamAccessories <team> [<x> <y> <z>]` sets a team-specific accessories location (available in advanced setup) (For now does nothing)
- **Improved `/bw help`** - The command list now shows only what you can use in your current situation (lobby, in a game, spectating, admin), is paginated (`/bw help <page>`), translated, and clickable with hover details
- **Better `/bw arena`** - Typing `/bw arena` (or a wrong subcommand) now shows a list of all arena commands with descriptions, and `/bw arena list` also shows each arena's state (setup mode or game status) and player count
- **Game ender chest** - Ender chests in an arena now open a private per-game chest for each player; players' real ender chest contents are no longer wiped when a game starts
- **Armor lock** - Players can no longer take off, swap or shift-click armor during a game, so purchased armor can't be lost or exploited

### Fixed
- **Wrong world generator on 26.1+** - Fixed void world generator on the newest mc versions
- **Leave message during gameplay** - Leaving an arena during active gameplay now shows a different message than leaving from the waiting lobby
- **Silverfish friendly-fire** - Silverfish no longer attack or take damage from their owner or teammates
- **Silverfish block breaking** - Silverfish can no longer break blocks in arenas
- **Silverfish price rebalanced** - Changed from 3 gold to 1 emerald (was too overpowered at original price)
- **Iron golem friendly-fire** - Team members can no longer damage their own team's iron golem
- **Iron golems persisting after game end** - Iron golems now properly despawn when the game ends
- **Portable tower item consumption bug** - Portable tower item is no longer consumed when placement is blocked in a protected area
- **Respawn after game ends** - Players with pending respawn timers no longer respawn after the game has ended
- **Resource pack reload on death** - Resource pack no longer reloads every time a player dies and becomes a spectator (massive UX improvement)
- **Resource pack reload on respawn** - Resource pack is not resent when players respawn from spectator mode
- **Resource pack tracking cleared on leave** - Resource pack tracking is properly cleaned up when players leave an arena
- **Resource pack tracking cleared on restart** - Resource pack tracking is properly reset when arenas restart
- **`/bw arena create` command error** while trying to create with -sche with no installed WordEdit
- **Setup gui** - now it's no longer possible to close it without choosing setup mode
- **Arena disablement error** - Fixed issue with disabling arena while game in waiting and reloading status
- **Plugin error while stopping server** - Fixed issue when stopping server while some arenas was still active
- **Min players** check on join is now correctly processed
- **Rejoin support in arena selection GUI** - Players can rejoin an arena they left mid-game by clicking it in the `/bw join` GUI
- **Player count display** - Bossbar, scoreboard, and GUI player counts no longer include players who left but still have rejoin time remaining

### Removed
- **Special gamerules** - was never actually wired up, so it did nothing

---

## [2.3-beta] - 2026-09-24

### Added
- **`/bw game start` command** - Allows to start countdown timer to start game (No need to use `/bw game forcestart`)
- **26.3 support**

### Fixed
- **Some team colors didn't work when creating team**
- **Ender chest** - The ender chest is now cleared at the start of every game
- **Disabled crafting in arenas**
- **Diamond/Emerald generator** - Interacting with the generator hologram no longer puts the item in the player's inventory
- **Join command tab completion** - The join command tab completion no longer suggests disabled arenas to join

---

## [2.2-beta] — 2026-07-09

### Added
- **Void deaths drop resources** — dying to the void without a player attacker now drops your iron/gold/diamond/emerald at your last standing position instead of losing them
- **Hunger disabled in arenas**
- **Locator Bar disabled in arenas** — the new XP-bar player tracker (26.x+) is turned off on arena worlds
- **`/bw setGeneratorLeveling <configId>`** — pick which `generators/*.yml` config an arena uses, from setup mode

### Fixed
- **Portable Tower direction** — the ladder/entrance now face back towards the player instead of a hardcoded direction
- **Fast-buy reset** — added a "Reset All" button to the Fast-Buy Customizer GUI, and fixed it (and plain Save) falling back to the YAML defaults correctly
- **Fast-buy default grid misaligned** — `exampleShop.yml`'s default fast-buy items are now under the correct category columns
- **Missing translations** — portable tower, fast-buy, and leaderboard/stats/profile messages are now localized
- **Missing permissions** — added `newbedwars.bw.stats`, `.profile`, and `.leaderboard`
- **Lobby location reverting on restart** — was being read before worlds finished loading at `STARTUP`; now loaded lazily

### Changed
- **Renamed `exampleGui.yml`/`exampleGui2.yml`** to `exampleShop.yml`/`exampleUpgrade.yml`
- **Redesigned the default upgrade GUI** — bordered layout, quick-access Home/Traps buttons in the header, and a Home button in the footer so there's a way back out of the Traps page

### Removed
- **`/bw setupGuis` command** — never did anything useful
- **`OtherGenerators` config section** — was never actually wired up, so it did nothing

---

## [2.1-beta] — 2026-06-25

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
