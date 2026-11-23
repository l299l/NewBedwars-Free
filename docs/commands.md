# Commands and permissions

Commands depend on where you are: outside any arena (lobby), inside a game or spectating, or in arena setup mode. `/bw help` shows what you can use right now, and `/bw setupHelp [page] [command]` shows the setup commands of your current setup mode.

- [Player commands](#player-commands)
- [Party commands](#party-commands)
- [Network commands](#network-commands)
- [Admin commands](#admin-commands)
- [Web panel commands](#web-panel-commands)
- [Setup commands](#setup-commands)
- [Permissions](#permissions)

## Player commands

| Command | Where | Description |
|---|---|---|
| `/bw join [arena\|group]` | lobby | Open the arena selector, join a specific arena, or join the fullest waiting arena of a group. On a network lobby server `<server>:<arena>` joins that server's arena |
| `/bw rejoin` | lobby | Rejoin the game you disconnected from |
| `/bw spectate <arena>` | lobby | Join a running arena as a spectator |
| `/bw team [team\|random]` | game (waiting) | Open the team menu, pick a team (name, any case) or clear your pick with `random`. Needs the gamerule `TeamSelection`; only while the arena is waiting or starting, not for spectators |
| `/bw lobby` | lobby, game | Leave the arena and return to the lobby |
| `/bw lang <en\|pl\|de\|es\|fr\|ru>` | lobby, game | Change your language |
| `/bw help [page]` | lobby, game | Show the commands you can use |
| `/bw stats [player]` | lobby | Show a player's stats |
| `/bw profile [player]` | lobby | Open a player's profile GUI (also fast-buy customization) |
| `/bw leaderboard [wins\|kills\|beds\|fk]` | lobby | Top 10 players by a stat |
| `/lobby` | anywhere | Shortcut for `/bw lobby` |
| `/lang <en\|pl\|de\|es\|fr\|ru>` | anywhere | Shortcut for `/bw lang` |

## Party commands

`/party` (alias `/p`). All of them need `newbedwars.party`. In network mode (see [network.md](network.md)) the same commands are handled by the NewBedwars-Proxy plugin on Velocity.

| Command | Description |
|---|---|
| `/party invite <player>` | Invite a player to your party |
| `/party accept` / `/party deny` | Accept or deny a pending invite (clickable in chat) |
| `/party leave` | Leave your party |
| `/party list` | List the party members |
| `/party kick <player>` | Kick a member (party admin only) |
| `/party admin <player>` | Give the party admin role to another member (party admin only) |

## Network commands

Only used in [network mode](network.md). In network mode `/party` is also handled by the proxy (see [Party commands](#party-commands)).

| Command | Where | Permission | Description |
|---|---|---|---|
| `/nbwproxy` | Velocity proxy | `newbedwars.proxy.admin` | List the game servers and arenas the proxy knows, and whether a newer proxy version exists |
| `/bw join <server>:<arena>` | lobby server | `newbedwars.bw.join` | Join that arena on that game server |
| `/bw arena list` | lobby server | `newbedwars.bw.arena.list` | The arenas of every game server (server, group, state, players) |
| `/bw arena info` | lobby server | `newbedwars.bw.arena` | How to set arenas up on a network. All other `/bw arena` commands are refused on a lobby server; run them on the game server that has the arena |
| `/bw group ...` | lobby server | `newbedwars.bw.group` | Arena groups for the whole network; `<server>:<arena>` adds only that server's arena. Game servers can only list groups |
| `/nbwpanel network status` / `secret` / `forget <server>` | any server with the panel | `newbedwars.panel.manage` | Panel network mode, see [Web panel commands](#web-panel-commands) |

`newbedwars.network.bypass` lets staff stay on a game server without a join request (setup, testing).

## Admin commands

Available outside arenas to players with the permission shown.

| Command | Permission | Description |
|---|---|---|
| `/bw arena create <name> [-sche <schematic>] [-n]` | `newbedwars.bw.arena.create` | Create an arena, optionally from a WorldEdit schematic (`-n`: do not teleport to it) |
| `/bw arena list` | `newbedwars.bw.arena.list` | List arenas and their status (with problems of disabled arenas) |
| `/bw arena delete <name>` | `newbedwars.bw.arena.delete` | Permanently delete an arena, its world and saved data |
| `/bw arena enable <name>` | `newbedwars.bw.arena.enable` | Enable a fully configured arena |
| `/bw arena disable <name>` | `newbedwars.bw.arena.disable` | Disable an arena |
| `/bw arena setup <name> [-l <players>]` | `newbedwars.bw.arena.setup` | Enter setup mode for an arena |
| `/bw arena backup <name>` | `newbedwars.bw.arena.backup` | Back up the arena file and map now |
| `/bw arena backups <name>` | `newbedwars.bw.arena.backups` | List the backups of an arena |
| `/bw arena restore <name> [backupId]` | `newbedwars.bw.arena.restore` | Restore the arena file and map from a backup |
| `/bw arena export <name>` | `newbedwars.bw.arena.export` | Export the arena (file and map) to a zip in `data/exports` |
| `/bw arena import <file>` | `newbedwars.bw.arena.import` | Import an arena from a zip in `data/exports` (loaded disabled) |
| `/bw arena copy <name> <newName>` | `newbedwars.bw.arena.copy` | Copy an arena (settings and map) to a new arena with the same state; it joins the same group |
| `/bw arena resetmap <name>` | `newbedwars.bw.arena.resetmap` | Put the arena map back to its saved state now |
| `/bw setlobby` | `newbedwars.bw.setlobby` | Set the lobby spawn |
| `/bw group create <name> [item]` | `newbedwars.bw.group.create` | Create an arena group (players vote between its maps); `item` is the vote menu icon |
| `/bw group delete <group>` | `newbedwars.bw.group.delete` | Delete a group |
| `/bw group add <group> <arena>` | `newbedwars.bw.group.add` | Add an arena (all arenas of a group need the same team count and team size; one group per arena). On a network lobby server: `<arena>` = that arena on every game server, `<server>:<arena>` = only that one; game servers can only list groups |
| `/bw group remove <group> <arena>` | `newbedwars.bw.group.remove` | Remove an arena from a group |
| `/bw group list` / `info <group>` | `newbedwars.bw.group.list` / `.info` | List groups / show a group and its problems |
| `/bw group setitem <group> <item>` | `newbedwars.bw.group.setitem` | Change the vote menu icon |
| `/bw arena info` | `newbedwars.bw.arena` | Lobby server of a network only: how to set arenas up on the game servers (`/bw arena list` shows all servers' arenas there) |
| `/bw npc set <action> [target]` | `newbedwars.bw.npc` | Turn the Citizens NPC you selected (`/npc select`) into a join NPC |
| `/bw npc clear` | `newbedwars.bw.npc` | Make the selected Citizens NPC a normal NPC again |
| `/bw npc info [id]` | `newbedwars.bw.npc` | Show the action of the selected Citizens NPC, or of a simple NPC by id |
| `/bw npc list` | `newbedwars.bw.npc` | List all join NPCs with their action and location |
| `/bw npc create <entityType> <action> [target]` | `newbedwars.bw.npc` | Without Citizens: spawn a simple join NPC (e.g. a villager) where you stand |
| `/bw npc remove <id>` | `newbedwars.bw.npc` | Remove a simple join NPC |
| `/bw reload` | `newbedwars.bw.admin` | Save arenas, send everyone to the lobby and reload the configuration. Parties are disbanded. Refused while an arena map is being reset |
| `/bw update [check\|download]` **(2.7-beta)** | `newbedwars.bw.update` | Look for a new NewBedwars version on Modrinth/GitHub; `download` saves it (checksum verified) to `plugins/update/`, installed on the next restart |
| `/bw game <start\|forcestart\|stop\|forcestop\|nextPhase>` | `newbedwars.bw.managegame` | Manage the running game you are in or spectating (start, stop, skip a phase) |
| `/bw game broadcast <message>` | `newbedwars.bw.managegame` | Send a highlighted message (`&` colours allowed) to everyone in your arena. **Available from 2.7-beta** |

`/bw join <group>` joins the fullest waiting arena of a group, so players gather in one lobby. When a game in a grouped arena ends, everyone still there gets a vote item (`MapVote.Item` in `config.yml`, slot 2 by default) and votes for the next map among the group's other waiting arenas with room for everyone. When `AfterGameWatchTime` is up, everyone joins the waiting lobby of the map with the most votes (ties and no votes pick randomly; parties stay together). If no map is free, everyone goes to the lobby. Turn voting off for one arena with the gamerule `MapVote`.

> [!NOTE]
> `/bw update` or `/bw update check` checks now; `/bw update download` downloads the new jar into `plugins/update/` and the server installs it on the next restart. It only works in-game; the console gets an automatic line when a new version is found. Settings are under `Updates` in `config.yml` (see [Configuration](configuration.md#updates)).

Join NPC actions: `GROUP <group>` joins the fullest free arena of a group, `ARENA <arena>` joins that arena, and `SELECTOR [group]` opens the arena selector. On a network lobby server all three send players to the game servers. A group is a `/bw group` name; otherwise it is an arena name pattern (`solo_*`) or a team size (`4x1` = 4 teams of 1). Hologram lines and update speed are set under `JoinNpc` in `config.yml`.

## Web panel commands

> [!NOTE]
>, only with the `NewBedwars-Panel` addon installed. See [panel.md](panel.md).

| Command | Permission | Description |
|---|---|---|
| `/nbwpanel status` | `newbedwars.panel.manage` | Panel address, certificate fingerprint, accounts and logged-in sessions |
| `/nbwpanel user create <name> <role>` | `newbedwars.panel.manage` | Create a panel account; prints a one-time password |
| `/nbwpanel user reset <name>` | `newbedwars.panel.manage` | New one-time password; the account is logged out |
| `/nbwpanel user delete <name>` | `newbedwars.panel.manage` | Delete a panel account (not the last admin) |
| `/nbwpanel user list` | `newbedwars.panel.manage` | List panel accounts and roles |
| `/nbwpanel network status` | `newbedwars.panel.manage` | Network role of the panel and, on a gateway, the state of every server |
| `/nbwpanel network secret` | `newbedwars.panel.manage` | Print a new random network secret (not saved) |
| `/nbwpanel network forget <server>` | `newbedwars.panel.manage` | Gateway: forget the remembered certificate of a server |

## Setup commands

Used inside the arena world after `/bw arena setup <name>`. Choose the mode with `/bw setupMode`; each mode includes the commands of the ones before it. Arguments in `( )` are optional; many position commands use your location when you give no coordinates.

### All modes

| Command | Description |
|---|---|
| `/bw info [gui]` | Show the setup status of the arena |
| `/bw setupMode` | Open the setup mode selection GUI |
| `/bw setupHelp [page] [command]` | Show the setup commands of your mode, or the details of one |
| `/bw save` | Save the arena (also updates the map copy used for resets) |
| `/bw remove <team\|generator> <teamName\|index> [bed\|spawn\|shop\|upgrades\|buildprotpos1\|buildprotpos2\|pos1\|pos2]` | Remove a team, a generator, or one property of a team |
| `/bw leave [-all]` | Leave setup mode and go back to the lobby |

### Normal setup

| Command | Description |
|---|---|
| `/bw createTeam <color> <name>` | Create a team |
| `/bw setTeamSpawn <team> [x y z]` | Team spawn |
| `/bw setTeamBed <team> [x y z]` | Team bed |
| `/bw setTeamShop <team> <entityType> [x y z]` | Team shop NPC |
| `/bw setTeamUpgrades <team> <entityType> [x y z]` | Team upgrades NPC |
| `/bw setGenerator <type> [team\|id] [x y z]` | Diamond, emerald or team generator; in advanced setup also `OTHER <id>`, a [custom generator](shop-and-upgrades.md#custom-generators) |
| `/bw setMinPlayers <n>` | Minimum players to start |
| `/bw setMaxInTeam <n>` | Players per team |
| `/bw setWaitingSpawn [x y z]` | Waiting lobby spawn |
| `/bw setWaitingPos1` / `setWaitingPos2` | Corners of the waiting region |
| `/bw setWaitingTime <seconds>` | Countdown before the start |
| `/bw setQuickVoidY [y]` | Y level where players die instantly |
| `/bw setTeamBasePos1 <team>` / `setTeamBasePos2 <team>` | Corners of the team base (traps, heal pool) |
| `/bw setTeamBuildProtAreaPos1 <team>` / `...Pos2 <team>` | Corners of the zone where blocks cannot be placed |

### Advanced setup

| Command | Description |
|---|---|
| `/bw gamerule <name> <true\|false>` | Gamerule of this arena |
| `/bw setGui <shop\|upgrade> <guiId>` | Shop or upgrade GUI used on this arena |
| `/bw setGeneratorLeveling <configId>` | Generator leveling configuration |
| `/bw setGamePhases <id>` | Game phase list of this arena (an ID from `GamePhases` in `modes.yml`, tab-completed) |
| `/bw setTablist <layout>` | Tab list layout of this arena (a layout from `tablists.yml`, tab-completed) |
| `/bw setMode <mode>` | Copies a mode from `Modes` in `modes.yml` (generators, game phases, shop and upgrade menu, team size) into this arena once; nothing is changed if a part of the mode is missing (tab-completed) |
| `/bw setResourcePack <url\|clear> [sha1]` | Per-arena resource pack |

### Automatic setup

Pick **Automatic** in the setup menu (or `/bw arena create`), build the arena, stand in the middle of it and run:

| Command | Description |
|---|---|
| `/bw startAutomaticSetup [radius]` | Scans the blocks around you (default `AutomaticSetup.DefaultRadius`, at most `AutomaticSetup.MaxRadius`) and sets up what it finds, then asks for the rest |
| `/bw startAutomaticSetup next` | Shows the current step again (after you changed something by hand) |
| `/bw startAutomaticSetup skip` | Skips the current step |
| `/bw startAutomaticSetup cancel` | Stops the scan or the step-by-step help; what was already set stays |

What the scan finds:

- **Beds** (the head half): every bed color becomes a team (`Red`, `Blue`, `LightBlue`, ...) with its bed set.
- **Generator blocks**: diamond and emerald blocks become arena generators (touching blocks make one). The nearest iron or gold block within `AutomaticSetup.GeneratorSearchRadius` blocks of a bed becomes that team's generator.
- **Marker signs** place the rest. Line 1 is the type, line 2 the team color (left empty = the team of the nearest bed), line 3 an optional mob type for shops (default `VILLAGER`):

| Line 1 | Sets |
|---|---|
| `[spawn]` | Team spawn (faces the middle of the map) |
| `[shop]` | Item shop |
| `[upgrades]` | Upgrades shop |
| `[gen]` | Team generator (wins over a found iron or gold block). With `DIAMOND` or `EMERALD` on line 2 it adds an arena generator instead |

Used signs are removed. The lowest block of the scanned area minus 10 becomes the quick void Y, and the base area and block protection area of every team are guessed from its points (padding in `AutomaticSetup`). Guessed areas follow the team's points until you set them yourself.

Steps still missing are shown in chat with clickable **[Set here]** and **[Skip]** buttons that run the normal setup commands. Things that are already set are never overwritten by a new scan. When no step is left, the plugin says whether the arena can be enabled; save with `/bw save`.

## Permissions

Parent permissions grant their children. Everything not marked `true` defaults to `op`.

### Admin

| Permission | Default | Description |
|---|---|---|
| `newbedwars.bw.admin` | op | All admin commands (grants `arena`, `setlobby`, `managegame`, `npc`, `group`, `update`, `update.notify`) |
| `newbedwars.bw.arena` | op | All `/bw arena ...` commands (grants the `arena.*` permissions) |
| `newbedwars.bw.arena.create` / `.delete` / `.enable` / `.disable` / `.setup` / `.list` | op | The matching `/bw arena <action>`. Holding only one of these is enough: the player can use that action (and sees only the actions they may use in `/bw help` and tab completion). `.setup` also allows the setup-mode commands |
| `newbedwars.bw.arena.backup` / `.backups` / `.restore` / `.export` / `.import` / `.copy` / `.resetmap` | op | The matching `/bw arena <action>`. `.import` is trusted like console access: an imported map can contain command blocks (they run when `enable-command-block=true` in `server.properties`) and datapacks. Grant it only to people you would give console access |
| `newbedwars.bw.setlobby` | op | `/bw setlobby` |
| `newbedwars.bw.managegame` | op | `/bw game ...` |
| `newbedwars.bw.npc` | op | `/bw npc ...` |
| `newbedwars.bw.update` | op | `/bw update` |
| `newbedwars.update.notify` | op | One chat line on join when a new NewBedwars version is available |
| `newbedwars.network.bypass` | op | Network GAME mode: stay on a game server without a join request (staff, setup) |
| `newbedwars.proxy.admin` | - | Velocity: `/nbwproxy` (list the game servers and arenas the proxy knows, and whether a newer proxy version exists) |
| `newbedwars.panel.manage` | op | `/nbwpanel` (web panel addon: status and panel accounts) |
| `newbedwars.bw.group` | op | All `/bw group ...` commands (grants `group.create` / `.delete` / `.add` / `.remove` / `.list` / `.info` / `.setitem`; each one alone allows only that `/bw group <sub>`) |
| `newbedwars.bw.bypass` | op | Bypass setup-mode restrictions (for example entering setup worlds) |
| `newbedwars.chat.color` | op | Use `&` colour codes in arena and team chat messages (others have them stripped) |

### Player

| Permission | Default | Description |
|---|---|---|
| `newbedwars.bw.player` | true | Bundle that grants all player permissions below |
| `newbedwars.bw.join` | true | Join arenas |
| `newbedwars.bw.lobby` | true | Go to the lobby |
| `newbedwars.bw.rejoin` | true | Rejoin after a disconnect |
| `newbedwars.bw.spectate` | true | Spectate arenas |
| `newbedwars.bw.lang` | true | Change language |
| `newbedwars.bw.help` | true | `/bw help` |
| `newbedwars.bw.stats` | true | `/bw stats` |
| `newbedwars.bw.profile` | true | `/bw profile` |
| `newbedwars.bw.leaderboard` | true | `/bw leaderboard` |
| `newbedwars.party` | true | All `/party` commands |
| `newbedwars.teamselect` | true | Team selector item and `/bw team` (also part of `newbedwars.bw.player`) |

All player permissions default to `true`, so everyone can use these commands until you deny a permission with a permissions plugin. Ops always bypass the checks. `/lobby` and `/lang` need the same permissions as `/bw lobby` and `/bw lang`.

To stop a group from picking teams, take `newbedwars.teamselect` away with a permissions plugin, for example with LuckPerms: `lp group default permission set newbedwars.teamselect false`. Those players get no selector item, `/bw team` is refused and they are always placed automatically. Operators always keep it.
