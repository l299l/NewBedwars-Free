# Changelog

All notable changes to NewBedwars are documented here.

## [2.8-beta] - Unreleased

### Added
- **Five built-in game modes** - Solo (8 teams of 1), Doubles (8x2), Squads (4x4), Rush (fast, about 15 minutes) and Duels (1v1, about 12 minutes). Each mode has its own generator speeds, game phases and upgrade prices, tuned with a simulator so the pacing fits the team size. New installs use Squads; existing servers and arenas keep what they have. Set a mode up in one step with `/bw setMode <Mode>`, which copies its generators, game phases, upgrade menu and team size into the arena (and warns when the team count differs). Modes are listed in the new `Modes` section of `config.yml`, where you can add your own (generator preset, game phases, shop and upgrade menu, players per team). See [docs/configuration.md](docs/configuration.md#game-modes)
- **`/bw setGamePhases <id>`** - Chooses the game phase list of an arena in advanced setup mode (tab completion lists the phase lists)
- **Shop item prices per menu** - A shop menu file can have an `ItemPrices:` section that changes the price, currency and amount of items in that menu, for example cheaper items in solo arenas. Editable in the web panel's shop menu editor. See [docs/shop-and-upgrades.md](docs/shop-and-upgrades.md)
- **Shop items that run commands, open menus or give permissions** - Three new `onBuyEvent` actions in `items.yml` and the web panel's shop editor: `COMMANDS` runs the item's `commands` (as the console, or as the buyer with `player:`; `{player}`, `{team}` and `{arena}` are replaced), `OPEN_GUI` opens another shop menu (`gui`), and `GIVE_PERM` gives the buyer a permission (`givePermission`) until they leave the game, are eliminated or the game ends (it is kept across `/bw rejoin`). Before, items with these actions could not be bought. See [docs/shop-and-upgrades.md](docs/shop-and-upgrades.md#commands-menus-and-permissions)
- **Web panel: `shop.commands` permission** - Changing a shop item's commands or given permission, or switching an item to or from `COMMANDS` or `GIVE_PERM`, needs this new permission, because those commands run as the server console. The built-in `admin` role has it; other roles only see the commands
- **Web panel: modes** - A new **Modes** tab in the shop editor lists every game mode with its settings and phase timeline (modes are still edited in `modes.yml`). **Shop > Arenas** shows each arena's mode (or "Custom" when its settings match no mode) with its generator config, phase list and team format, and users with the new `modes.apply` permission can apply a mode there while the arena is waiting for players; it warns when the arena's team count differs from the mode's. The tab and column need the new `modes.view` permission (admin role; not staff). See [docs/panel.md](docs/panel.md#game-modes)
- **Web panel: dashboard** - The team columns line up for arenas with different team counts, and a **Group arenas** selector groups the list by team format (teams x players per team, e.g. "4 x 4 - 16 players"); the choice is remembered in the browser
- **Pay with any item** - `priceType: OTHER` with `priceMaterial: <item>` makes a shop item cost any item, for example nether stars
- **Custom generators** - A `CustomGenerators` section in the generator files defines generators that drop any item (item, amount, delay, display block, hologram name). Place them in advanced setup with `/bw setGenerator OTHER <id>`. Before, `OTHER` was offered but did nothing
- **PlaceholderAPI in the shop** - The names and descriptions of shop items, team upgrades, traps and menu categories can use PlaceholderAPI placeholders (for example `%newbedwars_stat_wins%` or placeholders of other plugins); each player sees their own values in the menu. Needs PlaceholderAPI. The item a player receives keeps its normal text on purpose, so bought blocks still stack ([#5](https://github.com/l299l/NewBedwars-Free/issues/5)). See [docs/shop-and-upgrades.md](docs/shop-and-upgrades.md#shop-items-itemsyml)
- **Leaderboard placeholders** - `%newbedwars_top_<stat>_<position>_name%` and `_value%` (wins, kills, final_kills or beds, places 1 to 10) for leaderboard holograms and menus. They are refreshed every `Leaderboards.RefreshSeconds` (60 by default) and show the whole network with MySQL. See [docs/placeholders.md](docs/placeholders.md#leaderboards)
- **Lobby boss bar** - A boss bar for players in the lobby (not in an arena), set up as `MainLobby` in `bossBars.yml`. It is off by default (`Enabled: false`); `/players/` and `/max_players/` count all enabled arenas. It replaces the old `Lobby` entry, which was never shown
- **Team selector** - With the new gamerule `TeamSelection` (off by default), players in the waiting lobby get a selector item (slot and material in `TeamSelector.Item` in `config.yml`) and `/bw team [team|random]` to pick their team; players who don't pick are placed automatically at the start. `TeamSelectionBalanced` (default `true`) only allows picking one of the least picked teams, and `TeamSelectionPartyLeader` (default `true`) lets the party leader pick for the whole party. The game keeps waiting while fewer than two teams have players. Admins can take the permission `newbedwars.teamselect` away from a group. New placeholder `%newbedwars_picked_team%` and an addon event for team picks. See [docs/arena-setup.md](docs/arena-setup.md#team-selection)
- **Manual backups are kept** - Backups made with `/bw arena backup` end in `-manual` and are never deleted by the automatic backups; `/bw arena restore <name>` without an id restores the newest one with its matching map. See [docs/arena-setup.md](docs/arena-setup.md#backups-and-moving-arenas)
- **Web panel: roles editor** - Permissions are grouped (overview, running games, arenas, game modes, shop, panel administration) with a toggle and a counter per group, select all / clear all, a "sensitive" mark on risky permissions and a note on permissions you can't hand out yourself. The role list shows each role's permissions by group

### Fixed
- **Tab completion** - `/bw team` and its team names are only suggested when you can pick a team right now, and `/bw arena enable` / `disable` only suggest arenas that can be enabled or disabled
- **Game phase comment** - The `config.yml` comment said phase actions run at the end of a phase. They run when the phase starts, and `Duration` is how long it lasts
- **Settings that had no effect** - `ShowNextSpawnTime: false` in `generators/*.yml` now hides the "next spawn in" countdown, and `AllowPrivateChat: false` now blocks private messages (`/msg`, `/tell`, `/w`, `/r` and the EssentialsX variants) to and from players and spectators of a running game (admins with `newbedwars.bw.admin` can still send them)
- **Permissions** - `newbedwars.bw.rejoin`, `.spectate`, `.lang` and `.help` are now checked and `.join` and `.lobby` always are (all still default to `true`). The arena and group sub-permissions (`newbedwars.bw.arena.create`, `.delete`, `.enable`, `.setup`, `.backup`, `.restore` and the others, `newbedwars.bw.group.<sub>`) now work on their own, also on permission plugins that don't expand child permissions, and `/bw help` and tab completion show only what a player may use. `/bw arena setup <name> -l` only adds players who may set up arenas themselves
- **Shop messages and purchases** - The shop says why a purchase failed (not enough resources, no permission, already owned, upgrade maxed or not buyable here; new messages `ShopNoPermission`, `ShopAlreadyOwned`, `ShopUpgradeMaxed`, `ShopUnavailable`) instead of "You don't have enough money" every time. Spectators, players not in the game and purchases outside a running game are refused. A typo in a price (`price: 1O`) no longer makes the item free, the shop editor no longer accepts a price of -1, and a wrong entry in a menu's `UpgradePrices` section is reported in the console
- **Kill credit** - Kills and final kills by TNT go to the player who placed it, `AllowTeamDamage: true` no longer turns off kill credit or lets spectators hit players, a hit cancelled by another plugin no longer counts, and a player hit by their own projectile is no longer their own killer. Loot that doesn't fit in the killer's inventory is dropped instead of lost
- **Rejoin and wins** - `/bw rejoin` during the respawn countdown counted a player twice, so the team could never be eliminated; it now only works for players who really left. A player who rejoined and won got a loss, and final-killed players of the winning team now get the win too (players who quit still get a loss)
- **Players stuck in a game** - A player who left the arena world by another plugin or command (for example `/spawn`) stayed in the game, so their team could never be eliminated; a spectator who joined another arena was pulled out of it when the first arena reset. Both now leave properly
- **`GAME_END:MOST_KILLS`** - Kills of eliminated or disconnected team members count, eliminated teams can't win and a tie is a draw. An unknown `GAME_END` value ends the game as a draw (with a console warning) instead of skipping the end screen and stats
- **Smaller game fixes** - Bedrock next to a bed is no longer treated as a bed; an eliminated team's generator stops; lobby potion effects are cleared on join; the end of a game no longer sends one "X left the arena" message per player; a party leader gets an error when no member could join
- **`/bw reload`** - Stats stored in `playerStats.json` no longer double on every reload. Every reload no longer adds another void check (lag, double rescues or deaths). A reload is refused while a map is being reset or an arena is being enabled, restored, copied, imported or deleted, so no arena ends up on a half-copied map
- **Broken or mistyped files are never silently replaced** - A language, scoreboard, boss bar, generator, config or `groups.json` file that isn't valid is moved aside as `<name>.corrupt-<time>` with a console error; a damaged `playerStats.json` is moved aside too, and a damaged `items.yml` or `upgrades.yml` is not saved over (the web panel's shop editor refuses changes until it is fixed). A wrong value in `config.yml` (for example `RejoinTime: five`) uses the default and names the setting instead of stopping the plugin, a broken `GamePhases` entry is skipped, and a boss bar without `Texts` no longer throws errors. `CustomGenerators` in `defaultGenerators.yml` is no longer removed on every start
- **Config backups** - `config.yml`, the language files, `bossBars.yml`, `scoreboards.yml` and `defaultGenerators.yml` are copied to `data/backups/config/<version>/` once per plugin version before they are updated
- **Map resets, templates and backups** - The legacy rollback (`Rollback.UseLegacySystem: true` or an arena without a template) really resets the map now. Map templates are only replaced once the new one is complete, and backup and export zips are named only when complete, so an interrupted update is undone and a failed backup is never restored. A restore applies the map before the arena file; an arena whose map can't be restored, or whose world folder is missing with no template, is loaded disabled with the reason. "Could not load arena" names the file
- **Arena maintenance** - Enabling, deleting and copying arenas do their file work in the background. While a map is saved (`/bw save` or enabling), it can't be built on, blown up or changed by pistons, water or lava, and `/bw save` and enabling can't save it twice at once. Deleting is refused while an arena is enabling or resetting, `/bw arena create` is refused while that name is still being deleted, two copies to one name (a double click in the panel) no longer delete each other's files, and a copy or import cut off by a crash is restored on the next start. Restoring or deleting an arena after a failed reset works, and a deleted arena leaves its group
- **Server shutdown** - Stopping the server waits up to 30 seconds for a running map reset, enable, copy, import, restore or delete; a map that isn't finished is restored on the next start. An error in one arena or during a map reset no longer skips saving the lobby and player data, nothing throws errors when the plugin is already disabled, and players in setup mode get a translated kick message (`ShutdownKick`)
- **MySQL** - Database work runs one save at a time (on the plugin and the proxy), so saves no longer fail under load. The move to the UUID tables happens completely or not at all. Briefly failed stat saves are tried again for a few minutes but never after they may already have been written, so a game can't be counted twice. On shutdown pending saves get up to 15 seconds (5 before) without freezing on a database that doesn't answer, and every lost save is written to the console. Leaderboards are read once per game end and the stats table has indexes for them; players who disconnect are removed from memory
- **Scoreboard** - The status and "YOU" labels are translated (`Scoreboard*` keys), and lines that did not change no longer flicker
- **Proxy `config.yml`** - Values written as text (`"true"`, `"10"`) are accepted, and unreadable values are reported with the setting name
- **Players who wander into a game** - A player who reaches a running or waiting arena by `/tpa`, `/home`, a portal or another plugin without joining is sent to the lobby (new message `NotInThisArena`) and can't build or break blocks there. Admins (`newbedwars.bw.admin`) and players setting the arena up are not affected
- **Leaving during the end countdown** - A player who left the arena world (for example `/spawn`) while the game was ending stayed listed as a spectator, and when that arena reset their inventory and permissions were wiped, even in the middle of their next game
- **Fireball jumping** - A player's own fireball now pushes them like TNT does, and a teammate's fireball pushes without damage when `AllowTeamDamage` is off. Before, the hit was cancelled and gave no knockback
- **Spectators** - Players who respawn or rejoin no longer see the spectators, and arrows, fireballs and pearls fly through spectators instead of hitting them
- **Translated texts** - The alarm trap title, the shop, upgrades, golem and dragon name tags (`AlarmTrapTitle`, `AlarmTrapSubtitle`, `ShopNpcName`, `UpgradesNpcName`, `GolemName`, `DragonName`) and the `/bw help` lines for `npc`, `group`, `update` and `help` now come from the language files
- **Arenas from older servers** - Shop and upgrade NPCs saved as `MUSHROOM_COW` or `SNOWMAN` (Minecraft 1.17 to 1.20.4) work on 1.20.5 and newer instead of breaking the game start; an unknown NPC type becomes a villager with a console warning
- **Arenas disabled at start** - An arena that is loaded disabled because of a problem (a broken generator config, a failed map restore) keeps its saved state, so it is enabled again on the next start once the problem is fixed. Before, the next save disabled it for good and reset its setup. `/bw arena disable` still disables it permanently
- **Arena chat under lag** - When the server lagged, arena chat could reach the whole server and then be shown in the arena a second time, and lobby chat could reach players in games. Chat no longer waits for the server and is always kept apart
- **Backups, exports and force stop** - A backup or export can no longer be mixed with a `/bw save`, enable or restore started while it is being written, and force stop from the web panel or an addon is refused while an arena is resetting or disabled
- **Network: players stuck on a game server** - Without `LocalLobby`, a player whose move to the lobby failed (lobby down or full) is sent again a few times instead of staying invisible on the game server
- **Web panel: live view** - Switching pages while the panel was refreshing no longer starts extra refresh loops
- **Shop categories** - Switching between shop categories no longer moves the mouse cursor back to the middle of the screen ([#5](https://github.com/l299l/NewBedwars-Free/issues/5))
- **Arenas with their own generator config** - An arena set to another generator config with `/bw setGeneratorLeveling` still used the number of levels of the default config: forge upgrades stopped too early, or failed with an error when the arena's config had fewer levels, and the first game after a start used the default spawn rates. Generators now always follow the arena's own config, and a change applies at once when no game is running

### Security
- **Imported arenas** - The resource pack link is removed, and an `arena.json` over 8 MB or with team names other than letters, digits, `_`, `.` or `-` is refused. The docs now say that importing arenas is as trusted as the console (maps can contain command blocks and datapacks) and that config backups keep old passwords and secrets
- **Shop commands with unusual player names** - On offline-mode servers a console shop command with `{player}` is skipped (with a warning) when the name is not a normal Minecraft name, so it can't be used as a target selector
- **Web panel: account and certificate commands** - `/nbwpanel user create`, `reset`, `delete` and `network forget` need op in game, even when a permissions plugin gives `newbedwars.panel.manage` to someone else, and command blocks can't use them
- **Web panel: damaged `peers.json`** - A damaged certificate pin file is moved aside, readable pins are kept, and the gateway doesn't trust a new node certificate by itself, also after a restart, until `/nbwpanel network forget <node>` (the lock is kept in `peers.json.locked`; delete it to lift it for all nodes)
- **Web panel: login attempts** - Requests against a locked login are written to the activity log and the console once per lock instead of every time, the activity log is rotated at 5 MB while the server runs, user names with line breaks can no longer fake console lines, and a deeply nested request body is refused instead of crashing a panel thread
- **Web panel: slow connections** - A connection that sends its request too slowly (more than 10 seconds for the request itself, or an upload that stops for 30 seconds) is closed, so a few slow clients can no longer block the panel for everyone. The panel also handles more requests at once

### Changed
- **Game phases and modes have their own file** - `GamePhases` and `Modes` moved from `config.yml` to `modes.yml` (see [Game modes and phases](https://l299l.github.io/NewBedwars-Free/game-modes/)). Existing servers are moved automatically on first start (the old `config.yml` is backed up), missing built-in modes are added back, and your own modes and comments are kept
- **`DefaultGenerators` and the `Default` phase list are deprecated** - They still work and existing arenas keep using them, but new arenas use the built-in modes (Squads by default)

### Removed
- **`DefaultMaxTeams`** - Removed from `config.yml`. It never had an effect: an arena has as many teams as are created in setup
- **`RequireJoinPermission` and `RequireLobbyPermission`** - Removed from `config.yml`. The join and lobby permissions are now always checked like every other player permission; deny them with a permissions plugin to block players
- **`CloseOnTransaction`** - Removed from shop and upgrade menu files and from the web panel's menu editor. It never had an effect; shops stay open after a purchase as before. Old menu files keep loading

### Addon API
- **Shop menu item prices** - `ShopGui.itemPrices()` and the `ItemPrice` record (price, currency, amount) read and change the `ItemPrices` section of shop menus; the old `ShopGui` constructors still work
- **Game modes** - `getModes()` returns a `ModeService` that lists the modes, phase lists and generator configs of `modes.yml` and applies a mode to a waiting arena (`applyMode`, with a `ModeResult`), like `/bw setMode`; `ArenaView` has `getGeneratorConfig()` and `getGamePhases()`
- **API version and compatibility rules** - `getApiVersion()` returns a number that goes up with every release that adds to the API (1 = 2.8-beta), so addons can check for a feature without parsing the version name. `docs/api.md` now describes what stays compatible from the first stable release (3.0.0) on: additions only in minor releases, removals only in major releases after a deprecation
- **Shop items with commands, menus, permissions and any-item prices** - `ShopItem` has `commands`, `openGui`, `givePermission` and `priceMaterial`; the old constructor still works
- **Marked for removal in 3.0.0** - `ShopGui.closeOnTransaction` (always `false`) and `CommandContext.SETUP_BUILDING` (use `SETUP_AUTOMATIC`) are now `@Deprecated(forRemoval = true)` and will be removed in the first stable release
- **Players** - `join`, `spectate`, `leave` and `sendToLobby` work like the commands (parties, rejoining, messages) and tell the addon what happened (`JoinResult`), so join menus and NPC addons no longer have to run commands for the player
- **Parties, languages and messages** - `getParty(Player)` and `PartyChangeEvent`; `getLanguage`, `getMessage` and `registerMessages` let addons show their own texts in the language each player chose with `/lang`
- **Stats** - `getStats(UUID)`, `loadStats(UUID)` and `addStat`, which saves stats for addon game modes like a normal game (also to MySQL and the leaderboards)
- **Game control** - `destroyBed`, `eliminateTeam` and `setUpgradeLevel` for running games
- **New events** - `ArenaStateChangeEvent` (including the start and cancel of the countdown), `TeamsAssignedEvent`, `PlayerRespawnArenaEvent`, `PlayerSpectateEvent` and `TrapTriggerEvent` (both can be cancelled) and `PartyChangeEvent`. Addons can change the price of a purchase (`ShopPurchaseEvent.setPrice`) and see the buyer's team
- **Custom generators** - Addons now see custom generators as `CUSTOM` with their id instead of `BASIC`, and can place them (`ArenaEditor.addCustomGenerator`)
- **Team display names** - `TeamEditor.setDisplayName` sets the name players see for a team (chat, titles, scoreboard, NPCs, `%newbedwars_team%`) without renaming it, for addons that change team colors
- **Shop items per player** - `ShopItemStackEvent` lets addons change or replace the item a shop shows and gives, for example mangrove planks for the red team, and the shop shows the same item
- **Smaller additions** - `ArenaView.getMaxPlayers()`, `getTeam(Player)`, `isSpectator(Player)`, and shop item logic can tell which shop item was used
- **Addon API documentation** - Corrected descriptions that were wrong, contradicted each other or were hard to understand, documented what was missing (threads, null values, errors, the meaning of each reason and purchase type), and replaced the "Available from" notes with version tags that IDEs show


## [2.7-beta] - 16.11.2026

### Added
- **`/bw game broadcast <message>`** - Admins in a game can send a highlighted message (with `&` colours) to every player and spectator of their arena
- **Smarter Ender Dragons** - Dragons from the Dragon Buff upgrade now patrol over the middle of the map, hunt enemy players (first those near a bed), dive at them and climb back up, aim their fireballs ahead of moving targets, and never fly off the map. Dragons break every block they fly through except beds and the blocks listed in `DragonIndestructibleBlocks` (the map is restored after the game). A player killed by a dragon gets a "killed by the <team> dragon" message; nobody gets a kill for it. A dragon never hurts its own team and disappears as soon as its team is out. Speed, turn rate, patrol height and radius, margin, target and attack range and fireball interval/speed can be changed in the new `Dragon` section of `config.yml`
- **Update notifications** - NewBedwars looks for new versions on Modrinth (GitHub as a fallback) every 12 hours and writes one console line when one is out; admins (`newbedwars.update.notify`) get one chat line with a download link when they join. `/bw update` checks now, `/bw update download` downloads the new jar (checksum verified) into `plugins/update/` so the server installs it on the next restart. Optional `Updates.AutoDownload: true` does that automatically; `Updates.Notify` and `Updates.Check` turn the messages or the whole check off, `Updates.Channel` picks RELEASE, BETA or ALPHA versions. The proxy plugin reports new proxy versions in its console and in `/nbwproxy` (`updates:` in its config)
- **Web panel (new addon `NewBedwars-Panel`)** - A separate jar that adds a browser admin panel on port 8150 (HTTPS with a self-signed certificate by default). Shows every arena live (state, players, phase, game time, teams and beds), online players, TPS and memory, and lets you enable, disable, back up, restore, reset, copy, export (download) and delete arenas and import uploaded arena zips. Player stats and leaderboards, game control (start, stop, skip phase, broadcast) and a live game log per arena (chat, team chat, kills, beds, phases, and commands used inside the arena such as `/bw game` or `/gamemode`; player commands like `/lobby`, `/party` or `/msg` are left out, see `GameLog.IgnoredCommands`). A shop editor: edit shop items (material, amount, price, what buying does, item logic, enchantments, flags, permission) and their name and descriptions in every language with a live colour preview; build shop and upgrade GUIs on a visual slot grid with drag and drop, categories and header/footer rows; pick the shop and upgrade GUI of each waiting arena. Changes apply at once and items and GUIs still in use can not be deleted; the previous version of every changed file is kept in `data/backups/shop/`. Accounts are created from the console with `/nbwpanel user create <name> <role>` (one-time password, changed at the first login); `/nbwpanel status` shows the address and certificate. Built-in roles `admin` and `staff` (view only) plus your own roles with chosen permissions, a lock after 5 wrong passwords, and an activity log of every action. English and Polish. **Network mode**: run one panel on the lobby server (`Network.Role: GATEWAY`) and a panel on every game server (`Network.Role: NODE`), connected with a shared `Network.Secret` (`/nbwpanel network secret`) and a `Network.Peers` list. The gateway shows every server (status, players, TPS, arenas; servers that do not answer are marked offline) and all their arenas in one list, and opens any arena, game log, shop editor, stats and arena import/export of another server through the same login. The built-in `staff` role can look at every server of the network. The shop editor's Items and GUIs tabs have buttons that copy the items or GUIs of the selected server to all other servers when pressed. Game servers only answer their gateway (their own logins can be allowed with `Network.NodeLogin`), every action is recorded in the game server's activity log as `user@gateway`, and the gateway remembers each game server's certificate on first contact (`/nbwpanel network forget <server>` after a certificate change). Panels without `Network.Role` behave as before. Setup: [docs/panel.md](docs/panel.md)
- **Upgrade prices per level with their own currency** - Upgrade and trap prices are now in `data/upgrades.yml`: every level has its own price and currency (iron, gold, diamond or emerald), and the number of levels in the list is the number of levels a team can buy (up to what the upgrade supports). An upgrade GUI file can override the prices for the arenas that use it (`UpgradePrices:` section), for example cheaper upgrades in solo arenas. The prices from `config.yml` are moved there automatically on the first start. The web panel has a new **Upgrades** tab to edit them, price overrides in the GUI editor and a button to copy the prices to the other servers of a network

- **Automatic arena setup** - The "Automatic" setup mode now works. Build the arena, run `/bw startAutomaticSetup [radius]` and the plugin scans the map for beds (one team per bed color), diamond/emerald blocks (arena generators), iron/gold blocks next to a bed (team generator) and marker signs (`[spawn]`, `[shop]`, `[upgrades]`, `[gen]` with an optional team color), guesses the base and block-protection areas, and then walks you through what is still missing with clickable chat buttons. Settings are under `AutomaticSetup` in `config.yml`

- **`/bw info [gui]`** - The setup status command was renamed from `/bw arena` to `/bw info` (it only exists while you are in setup mode)

- **Complete German, Spanish, French and Russian translations** - About 380 messages that were still shown in English in these languages (setup, death causes, map voting, network mode, stats and more) are now translated

- **Tidier `config.yml`** - The settings are grouped in a clearer order (general, arena defaults, gameplay, chat, phases, dragons, maps, setup, NPCs, storage, updates, network) with one short comment each. Nothing was renamed: your existing `config.yml` is put into the new order on the next start and keeps all its values

### Fixed
- **Setup admins no longer fall** - Admins teleported into an arena by `/bw arena create` or `/bw arena setup` start flying, so they don't drop into the void when there are no blocks below
- **Upgrade icons bought as shop items** - An upgrade icon placed in a GUI as a normal item (`ci:`) no longer sells the upgrade for free or for one flat price; it costs the same as the upgrade slot
- **Upgrade prices changed in the web panel had no effect** - Upgrade icons no longer have a price of their own; the price shown and charged always comes from the upgrade prices
- **Shop files are no longer rewritten** - `items.yml` and the GUI files in `data/guis/` are only written when something is changed (shop editor, new upgrade icons), not on every `/bw reload`, so hand edits are kept. Saving a GUI no longer loses category descriptions, the `Fill`/`Empty` settings or names that start with `&`, and keeps the GUI in its own file
- **One broken shop entry no longer breaks the whole shop** - An item with an unknown material, enchantment or flag (for example from a newer Minecraft version) is skipped with a console warning and kept in `items.yml` unchanged; a GUI file that can not be read no longer stops the other GUIs from loading, and a missing item in a GUI only empties that slot
- **Potion items lost their logic** - Items with `itemLogic: POTION` kept it only until the next save
- **Duplicate shop items after `/bw reload`** - Reloading no longer keeps the old item list next to the new one
- **Item names in other languages** - Shop items without a translation now show the English name instead of the internal item name
- **Addon commands in a game** - Addon `/bw` subcommands used by players inside a game now get the player's arena instead of none
- **Dragons breaking protected blocks** - Blocks listed in `DragonIndestructibleBlocks` (e.g. obsidian) are no longer destroyed when an Ender Dragon flies through them
- **Death reasons in the web panel** - The game log showed "custom" for players who fell below the quick-void height, and "explosion" or "hit" for Ender Dragon kills. It now shows void and dragon, also when another player knocked the victim into the void (the kill is still credited to that player)
- **Death messages in the player's language** - Deaths without a killer (fall, fire, lava, explosions, ...) showed the cause in English; the cause now comes from the language files (`DeathCause-*` keys)
- **Stats kept after a name change** - Player stats, language and quick-buy settings are now stored by UUID instead of player name, so a player who changes their Minecraft name keeps everything and a new player who takes an old name no longer inherits its stats. Existing data is moved automatically on the first start (old JSON file kept as `playerStats.json.before-uuid`; MySQL data is copied to the new tables `nbw_player_data` and `nbw_player_stats`, the old tables stay as a backup). Programs that read `nbw_stats` directly must switch to `nbw_player_stats`
- **Arena import and restore no longer freeze the server** - Unpacking and copying the map of `/bw arena import` and `/bw arena restore` (and the same actions in the web panel) now happens in the background
- **Arenas with incomplete files** - An arena file with missing fields (no teams, generators, gamerules, times or world name) now loads with defaults instead of not loading at all; broken generator entries are skipped and the arena is loaded disabled with a reason
- **Setup markers** - Moving or removing a team's spawn, bed, shop, upgrades or generator no longer deletes the floating setup labels of other points next to it
- **Potions in the shop** - Potion items could turn into water bottles on some server versions
- **Shop and upgrade NPCs pushed away** - Villagers that were pushed by players are put back on their spot within a second
- **Web panel hardening** - Arena imports are refused when the zip has more than 200 000 files, and two uploads with the same file name at the same time can no longer overwrite each other
- **Players stuck after a death** - A death the game did not catch (for example a player falling out of the world without the quick-void check firing, or a death caused by another plugin) could leave the player as a spectator who never respawned. Deaths in a game are now always handled by NewBedwars, players below the quick-void line are caught even when they do not move, and a player whose respawn countdown stopped gets a new one
- **`/bw game forcestop` tab completion** - Admins now get `forcestop` suggested while a game is ending

### Removed
- **`UpgradePrices` in `config.yml`** - Moved to `data/upgrades.yml` (done automatically on the first start)
- **Team accessories location** - `/bw setTeamAccessories` and its entry in the arena info were removed (it never did anything). Existing arenas load as before

### Addon API
- **Shop items from addons** - `registerShopItem(owner, item[, texts])` adds an `items.yml` entry (material, price, item logic) when it does not exist yet, so server owners no longer add addon items by hand
- **Dragon kills** - `PlayerKillEvent.getDragonTeam()` tells which team's dragon killed the player, and `getCause()` is `VOID` for every void death (also the quick-void height and knock-offs)
- **Setup and arena details** - `getSetupArena(player)` / `getSetupPlayers()` (who is in setup mode and for which arena), `ArenaView.getSetupPlayers()` / `getQuickVoidY()` and `TeamView.getBed()`
- **Upgrade prices** - `getShop().getUpgradePrices()` / `saveUpgradePrices(upgrade, tiers)` read and change the prices per level, `ShopGui.upgradePrices()` the price overrides of a GUI. Prices given to `registerUpgrade` are written to `data/upgrades.yml` on the first registration and can be changed there afterwards
- **Statistics, leaderboards and game control** - `getLeaderboard`, `loadStats` (offline players too, from MySQL when used) and `findPlayers`; `getGameControl()` starts, stops, skips phases and broadcasts like `/bw game`; `ArenaView` shows the next phase and the time left in the current one
- **Shop editor** - `getShop()` reads and changes shop items, their texts per language and shop GUIs, and the shop GUIs of arenas (`ArenaView.getShopGui()` / `getUpgradeGui()`, `ArenaEditor.setShopGui()` / `setUpgradeGui()`)
- **New events** - `GamePhaseChangeEvent`, `ArenaChatEvent`, `BedsDestroyedEvent`, `TeamEliminatedEvent` and `GeneratorUpgradeEvent`
- **Subcommands in several places** - `registerCommand(owner, command, LOBBY, GAME, CONSOLE, ...)` registers one `/bw` subcommand in several contexts at once, instead of one name per context. New `CONSOLE` context: the command also works in the server console (`AddonCommand.performConsole` / `tabCompleteConsole`), and `bw` alone in the console lists those commands. Two commands may now share a name when they are used in different places (lobby, game, setup, console)
- **Arena maintenance** - Addons can enable, disable, back up, restore, export, import, copy, reset and delete arenas like the `/bw arena` commands (`NewBedwarsAPI.getMaintenance()`), and see why an arena was loaded disabled (`ArenaView.getLoadProblems()`). See `docs/api.md`


## [2.6-beta] - 02.11.2026

### Added
- <span style="color: yellow;">**Network mode (Velocity)** - Run lobby servers and game servers behind a Velocity proxy with the new `NewBedwars-Proxy` plugin. Lobby servers (`Network.Mode: LOBBY`) list the arenas of all game servers in `/bw join`, join NPCs and group placeholders, and send players and parties to the fullest free arena. Game servers (`Network.Mode: GAME`) put arriving players straight into their arena and send them back to a lobby after the game. Parties are handled by the proxy and survive server switches. Optional MySQL status table so empty game servers are still known, and `Network.RestartAfterGames` to restart game servers after a number of games. New permission `newbedwars.network.bypass`. See `docs/network.md`. Single servers (`Network.Mode: NONE`, the default) work as before </span>
- **MySQL storage** - `Storage.Type: MYSQL` keeps player data (language, quick buy) and stats in a MySQL/MariaDB database (`MySQL` section in `config.yml`). Use it on every server of a network and stats, leaderboards and languages are shared everywhere; games on different servers never overwrite each other's results. On the first start with an empty database, the existing JSON data is imported
- **Group placeholders** - `%newbedwars_group%` (group of the player's arena) and `%newbedwars_group_<name>_players|playing|waiting|max_players|arenas|arenas_free|best_arena%` for scoreboards, holograms and menus
- **Map voting** - Put arenas with the same team size into groups (`/bw group`). Players can join a group with `/bw join <group>` or a join NPC. When a game in a grouped arena ends, everyone still there gets a vote item and votes for the next map among the group's other free arenas; when the after-game time is up, everyone joins the map with the most votes (ties are random, parties stay together). Turn it off for single arenas with the `MapVote` gamerule. New permission `newbedwars.bw.group`. The default `AfterGameWatchTime` is now 30 seconds
- **MakeToolsPermanent gamerule** - New arena gamerule (default `true`, also under `DefaultGamerules` in `config.yml`). With `false`, pickaxe, axe and shears are lost on death and must be bought again. Tools were rebalanced: the stone and gold pickaxes are gone from the default shop and the iron pickaxe costs 7 gold
- **Copy arenas** - `/bw arena copy <arena> <newName>` copies an arena with its settings and map, so one map can run several games at once. The copy joins the same group
- **Lobby join NPCs** - Click an NPC in the lobby to join the fullest free arena of a group, join a specific arena or open the arena selector, with a live hologram above it showing players and free arenas. Make the NPC with Citizens and attach it with `/bw npc set`, or without Citizens spawn a simple one with `/bw npc create`. Parties follow the same rules as `/bw join`. New permission `newbedwars.bw.npc`, hologram lines under `JoinNpc` in `config.yml`
- **More translated texts** - The setup info (`/bw arena` in setup mode and its menu), the arena selector, spectate selector, profile, fast-buy editor and upgrade menus, the party help and invite buttons, the `/bw game` messages and the reload kick message now use the language files (English and Polish included; other languages fall back to English)

### Fixed
- **Stats for eliminated and timed-out players** - Players who were eliminated, or whose rejoin time ran out, now get their kills, deaths, losses and games played saved when the game ends
- **Players who already left no longer get pulled back** - Players waiting to rejoin a game that ends are no longer sent to the lobby a second time or have their inventory cleared; spectating another arena also gives up their rejoin slot
- **Player data is no longer overwritten after a failed load** - If the player data file (or database) cannot be read, the broken file is moved aside and saves are paused for that player instead of replacing the real data. Failures are logged
- **Chat and colour codes** - Arena chat respects mutes from other plugins, and players can only use `&` colour codes in arena and team chat with the new permission `newbedwars.chat.color`
- **Party invites** - Accepting an invite while already in a party is refused, an invite to a party that no longer exists fails cleanly, and one party can have at most 10 pending invites
- **Language changes** - `/bw lang` has a short cooldown, and player data is saved in the background without rewriting the file when nothing changed
- **Schematic names** - `/bw arena create ... -sche` only accepts plain schematic names (letters, digits, `_`, `-`, `.`)
- **Deleting an arena** - If some files cannot be deleted, the admin is told and the problem is logged instead of reporting success
- **Arena import limits** - `/bw arena import` refuses archives that would unpack to more than 2 GB or that contain files outside the arena folder
- **Safe loading of item and GUI files** - `items.yml` and the GUI files in `data/guis/` can no longer run code through special YAML tags; a file that contains them fails to load with a logged error
- **Imported arenas** - Importing an arena no longer leaves half-imported files behind when it fails, and imported maps no longer cause duplicate-world errors on Paper
- **Texts in your language** - The game start countdown, respawn, bed destroyed, draw, game ended and arena reset titles, plus several setup messages, now use the language files. Win fireworks use the winning team's color
- **Leave item missing after a game ends** - A player who was waiting to respawn when the game ended now also gets the leave item (bed)
- **Leaving while respawning** - Leaving the game (lobby command, leave item, disconnect) while waiting to respawn no longer keeps the respawn countdown running and throws an error
- **One game to rejoin** - Joining another arena while you could still rejoin a game now gives up that game (you get a message) instead of keeping both, so nobody ends up with several games to rejoin. The arena selector now uses the same checks as `/bw join`
- **Broken arena maps after saving** - Saving or enabling an arena right after changes could copy half-written world data into its map copy ("Unexpected end of ZLIB input stream" after the next reset). Incomplete files are now copied again until they are complete
- **Spectators falling into the void** - Spectators (during the end of a game) and players in the waiting lobby are sent back to the waiting spawn when they fall below the Quick Void level or the bottom of the world, instead of dying and breaking their state. Spectators can no longer take damage
- **Team area setup commands** - `/bw setTeamBasePos1/2` and `/bw setTeamBuildProtAreaPos1/2` now accept `<x> <y> <z>` like the other position commands (tab completion already suggested them). The help of `/bw setWaitingPos1/2` and `/bw setWaitingSpawn` shows the right command name
- **Arena info** - `/bw arena` in setup mode (chat view) now also lists the `MapVote` gamerule, like the GUI. Its "ready" hint now names the right command (`/bw arena enable <arena>`)
- **Special characters in default files** - Bundled language, boss bar, scoreboard and generator files are read as UTF-8, so accents and symbols are no longer garbled on servers with another default charset

### Removed
- **`NETWORK` join NPC action** - `GROUP`, `ARENA` and `SELECTOR` NPCs now work on network lobby servers too. NPCs that still use `NETWORK` keep working as `GROUP` NPCs

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
