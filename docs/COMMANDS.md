# Commands and permissions

Commands depend on where you are: outside any arena (lobby), inside a game or spectating, or in arena setup mode. `/bw help` shows what you can use right now, and `/bw setupHelp [page] [command]` shows the setup commands of your current setup mode.

- [Player commands](#player-commands)
- [Party commands](#party-commands)
- [Admin commands](#admin-commands)
- [Setup commands](#setup-commands)
- [Permissions](#permissions)

## Player commands

| Command | Where | Description |
|---|---|---|
| `/bw join [arena]` | lobby | Open the arena selector or join a specific arena |
| `/bw rejoin` | lobby | Rejoin the game you disconnected from |
| `/bw spectate <arena>` | lobby | Join a running arena as a spectator |
| `/bw lobby` | lobby, game | Leave the arena and return to the lobby |
| `/bw lang <en\|pl\|de\|es\|fr\|ru>` | lobby, game | Change your language |
| `/bw help [page]` | lobby, game | Show the commands you can use |
| `/bw stats [player]` | lobby | Show a player's stats |
| `/bw profile [player]` | lobby | Open a player's profile GUI (also fast-buy customization) |
| `/bw leaderboard [wins\|kills\|beds\|fk]` | lobby | Top 10 players by a stat |
| `/lobby` | anywhere | Shortcut for `/bw lobby` |
| `/lang <en\|pl\|de\|es\|fr\|ru>` | anywhere | Shortcut for `/bw lang` |

## Party commands

`/party` (alias `/p`). All of them need `newbedwars.party`.

| Command | Description |
|---|---|
| `/party invite <player>` | Invite a player to your party |
| `/party accept` / `/party deny` | Accept or deny a pending invite (clickable in chat) |
| `/party leave` | Leave your party |
| `/party list` | List the party members |
| `/party kick <player>` | Kick a member (party admin only) |
| `/party admin <player>` | Give the party admin role to another member (party admin only) |

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
| `/bw arena resetmap <name>` | `newbedwars.bw.arena.resetmap` | Put the arena map back to its saved state now |
| `/bw setlobby` | `newbedwars.bw.setlobby` | Set the lobby spawn |
| `/bw reload` | `newbedwars.bw.admin` | Save arenas, send everyone to the lobby and reload the configuration |
| `/bw game <start\|forcestart\|stop\|forcestop\|nextPhase>` | `newbedwars.bw.managegame` | Manage the running game you are in or spectating (start, stop, skip a phase) |

## Setup commands

Used inside the arena world after `/bw arena setup <name>`. Choose the mode with `/bw setupMode`; each mode includes the commands of the ones before it. Arguments in `( )` are optional; many position commands use your location when you give no coordinates.

### All modes

| Command | Description |
|---|---|
| `/bw arena [gui]` | Show the setup status of the arena |
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
| `/bw setGenerator <type> [team] [x y z]` | Diamond, emerald or team generator |
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
| `/bw setResourcePack <url\|clear> [sha1]` | Per-arena resource pack |
| `/bw setTeamAccessories <team> [x y z]` | Team accessories location |

### Automatic setup

`/bw startAutomaticSetup` starts the automatic setup (building mode).

## Permissions

Parent permissions grant their children. Everything not marked `true` defaults to `op`.

### Admin

| Permission | Default | Description |
|---|---|---|
| `newbedwars.bw.admin` | op | All admin commands (grants `arena`, `setlobby`, `managegame`) |
| `newbedwars.bw.arena` | op | All `/bw arena ...` commands (grants the `arena.*` permissions) |
| `newbedwars.bw.arena.create` / `.delete` / `.enable` / `.disable` / `.setup` / `.list` | op | The matching `/bw arena <action>` |
| `newbedwars.bw.arena.backup` / `.backups` / `.restore` / `.export` / `.import` / `.resetmap` | op | The matching `/bw arena <action>` |
| `newbedwars.bw.setlobby` | op | `/bw setlobby` |
| `newbedwars.bw.managegame` | op | `/bw game ...` |
| `newbedwars.bw.bypass` | op | Bypass setup-mode restrictions (for example entering setup worlds) |

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

`newbedwars.bw.join` and `newbedwars.bw.lobby` are only checked when `RequireJoinPermission` / `RequireLobbyPermission` is `true` in `config.yml` (both default to `false`, which means everyone can use them).
