# Configuration

All main settings are in `plugins/NewBedwars/config.yml`. Every setting also has a short comment in the file itself. After a change, restart the server or run `/bw reload` (this stops running games).

When you update NewBedwars, new settings are added to your `config.yml` with their default values; your own values are kept.

Other files are explained elsewhere: shop items, menus, upgrade prices and generators in [Shop and upgrades](shop-and-upgrades.md), storage in [Storage](storage.md), network mode in [Network mode](network.md).

## General

| Setting | Default | Meaning |
|---|---|---|
| `ServerName` | `"&6&lExample&f&lServer.com"` | Shown on scoreboards and boss bars (placeholders `/servername/` and `/server_name/`) |
| `DefaultLanguage` | `en` | Language for new players and the console: `en`, `pl`, `de`, `es`, `fr` or `ru`. You can add your own language file to the `language` folder |

Players change their own language with `/bw lang`.

## Arena defaults

Used for **new** arenas. Every arena can change them in setup mode.

| Setting | Default | Meaning |
|---|---|---|
| `DefaultMinPlayers` | `4` | Players needed to start the countdown |
| `DefaultMaxInTeam` | `4` | Players per team |
| `DefaultWaitingTime` | `20` | Seconds of countdown in the waiting lobby |
| `DefaultRespawnTime` | `5` | Seconds a dead player waits before respawning |
| `DefaultTeamShopGui` | `exampleShop` | Shop menu (`GuiId` of a file in `data/guis/`) |
| `DefaultTeamUpgradeGui` | `exampleUpgrade` | Upgrade menu |
| `DefaultGeneratorConfig` | `DefaultGenerators` | Generator preset (`ConfigurationId` of a file in `generators/`) |
| `DefaultGamePhase` | `Default` | ID of a phase list from [`GamePhases`](#game-phases) |

## Default gamerules

`DefaultGamerules` applies to all arenas unless an arena overrides a rule with `/bw gamerule <name> <true|false>` (advanced setup mode).

| Gamerule | Default | Meaning |
|---|---|---|
| `RandomTeams` | `true` | Players are shuffled into teams; `false` fills the teams in join order |
| `AllowParties` | `true` | Parties can join together and are kept in one team |
| `AllowSpectators` | `true` | Players can spectate running games |
| `AllowTeamChat` | `true` | Team chat (messages starting with `!`) |
| `AllowGlobalChat` | `true` | Chat with everyone in the arena |
| `AllowPrivateChat` | `true` | `false` blocks private messages (`/msg`, `/tell`, `/w`, `/r` and the EssentialsX variants) from and to players and spectators of a running game. Admins with `newbedwars.bw.admin` can still send them |
| `AllowTeamDamage` | `false` | Team members can hurt each other |
| `MakeSwordsPermanent` | `false` | Bought swords are kept after death |
| `MakeToolsPermanent` | `true` | Pickaxe, axe and shears are kept after death; `false` = lost on respawn |
| `AllowMultipleGolems` | `false` | A team can have more than one iron golem at a time |
| `SplitPartiesToStart` | `true` | When all players would end up in one team (one big party), split the party so the game can start; `false` keeps waiting for more players |
| `MapVote` | `true` | In a grouped arena, players vote for the next map at the end of the game ([Map voting](#map-voting)) |

## Gameplay

| Setting | Default | Meaning |
|---|---|---|
| `BasicSword` | `WoodenSword` | Starting sword (an item ID from `data/items.yml`) |
| `FullArmor` | `false` | Give every player a full set of team-coloured armor at the start |
| `RejoinTimeLimitEnabled` | `true` | Players who disconnect can come back with `/bw rejoin` |
| `RejoinTime` | `5` | Minutes a player has to rejoin |
| `BedLeaveConfirmEnabled` | `true` | Leaving with the bed item asks for a confirmation: click once to start a countdown, again to cancel |
| `BedLeaveConfirmDelay` | `3` | Seconds of that countdown |
| `AfterGameWatchTime` | `30` | Seconds players can look around after the game before the arena resets |
| `TntParticlesEnabled` | `true` | Red particles above players carrying TNT |
| `IronGolemLifetime` | `120` | Seconds a bought iron golem stays alive |

## Chat and tab list

| Setting | Default | Meaning |
|---|---|---|
| `ArenaChatEnabled` | `true` | `false` = players in arenas can't chat at all. The gamerules `AllowTeamChat` and `AllowGlobalChat` also apply. Start a message with `!` to send it to your team only |
| `TablistIsolationEnabled` | `true` | Every arena has its own tab list: players in an arena only see that arena's players, and lobby players don't see arena players |

Players with `newbedwars.chat.color` can use `&` colour codes in arena and team chat.

## Permissions

There are no permission settings in `config.yml`. Every player permission (join, lobby, rejoin, spectate, language, help, stats, profile, leaderboard, party) defaults to `true`, so without a permissions plugin everyone can use these commands. To take one away from some players or groups, deny it with a permissions plugin (LuckPerms or similar). Ops always bypass these checks.

> **Removed in 2.8-beta:** `RequireJoinPermission` and `RequireLobbyPermission`. The join and lobby permissions are now always checked, like all other player permissions. Old `config.yml` files may still contain both keys; they are ignored.

All permissions: [Commands and permissions](commands.md#permissions).

## Game phases

A phase list (`GamePhases`) is what happens during a game. Each phase runs for `Duration` seconds and then executes its `Actions`. Arenas use the list named in `DefaultGamePhase`. Two lists come with the plugin: `Default` (about 35 minutes) and the shorter `Rush`.

```yaml
GamePhases:
  - ID: Default
    Phases:
      - Name: "Starting phase"
        Duration: 300
        Actions: []
      - Name: "Diamonds II"
        Duration: 300
        Actions:
          - "DIAMOND_GENERATOR:2"
      - Name: "Bed destruction"
        Duration: 300
        Actions:
          - "BED_DESTRUCTION"
          - "DISABLE_REGENERATION"
      - Name: "Game end"
        Duration: 0
        Actions:
          - "GAME_END:DRAW"
```

| Action | Effect |
|---|---|
| `DIAMOND_GENERATOR:<level>` | Upgrade all diamond generators |
| `EMERALD_GENERATOR:<level>` | Upgrade all emerald generators |
| `BED_DESTRUCTION` | Destroy all remaining beds |
| `DISABLE_REGENERATION` | Turn off natural health regeneration |
| `ENDER_DRAGON_START` | Spawn Ender Dragons (one per team with the Dragon Buff upgrade) |
| `ENDER_DRAGON_END` | Remove the Ender Dragons |
| `GAME_END:DRAW` | End the game as a draw |
| `GAME_END:MOST_KILLS` | End the game; the team with the most kills wins |

Addons can add their own actions. Admins can skip to the next phase with `/bw game nextPhase`.

## Ender Dragons

Spawned by `ENDER_DRAGON_START` for every team with the Dragon Buff upgrade. Dragons patrol over the middle of the map, hunt enemy players (first those near a bed), dive at them and shoot fireballs. They never hurt their own team and disappear when their team is out.

| Setting | Default | Meaning |
|---|---|---|
| `DragonSpawnHeight` | `10` | Blocks above the team spawn where dragons appear |
| `DragonIndestructibleBlocks` | `OBSIDIAN`, `BEDROCK`, `BARRIER`, `END_PORTAL_FRAME`, `IRON_BARS` | Blocks dragons can't destroy (Bukkit material names). Beds are never destroyed. The map is restored after the game anyway |
| `Dragon.Speed` | `0.6` | Flight speed in blocks per tick |
| `Dragon.TurnRate` | `6.0` | How fast a dragon turns, in degrees per tick |
| `Dragon.PatrolHeight` | `15` | Patrol height above the highest team spawn, bed or generator |
| `Dragon.PatrolRadius` | `0` | Radius of the patrol circle around the map centre (`0` = automatic) |
| `Dragon.Margin` | `10` | How far outside the bases and generators a dragon may fly |
| `Dragon.TargetRange` | `128` | Maximum distance at which a dragon picks a target |
| `Dragon.AttackRange` | `20` | Horizontal distance at which a dragon starts a dive |
| `Dragon.FireballIntervalSeconds` | `[20, 10]` | Seconds between fireballs for Dragon Buff level 1, 2, ... |
| `Dragon.FireballSpeed` | `1.0` | Starting speed of the fireballs in blocks per tick |

## Map voting

Arenas can be put into groups (`/bw group`). When a game in a grouped arena ends, everyone still there gets a vote item and votes for the next map among the group's other free arenas during `AfterGameWatchTime`. When the time is up, everyone joins the map with the most votes (a tie or no votes picks randomly). If no arena of the group is free, everyone goes to the lobby as usual. Turn it off per arena with the `MapVote` gamerule.

| Setting | Default | Meaning |
|---|---|---|
| `MapVote.Item.Slot` | `1` | Hotbar slot of the vote item. Slots 0, 4 and 8 hold spectator items, so they fall back to 2 |
| `MapVote.Item.Material` | `PAPER` | Material of the vote item |

## Maps and backups

The map of every arena is kept as a clean template in `data/maps/<arena>/template`. After each game the arena world is replaced by a fresh copy of it, so a crash or an accidental save can never damage the map. Edit the map in setup mode, then `/bw save`.

| Setting | Default | Meaning |
|---|---|---|
| `Rollback.UseLegacySystem` | `false` | `true` = use the old system (reload the world from the server folder, no templates) |
| `Rollback.AsyncCopy` | `true` | Copy the template on a separate thread (recommended for big maps) |
| `Rollback.RestoreOnShutdown` | `false` | Also restore maps when the server stops (slower stop). When `false`, a map that was in use is restored on the next start |
| `Rollback.UnloadRetryTicks` | `10` | Ticks between attempts when an arena world can't be unloaded yet |
| `Rollback.TemplateExcludes` | `session.lock`, `uid.dat`, `playerdata`, `stats`, `advancements` | Files and folders not copied into the template |
| `Backups.Keep` | `5` | How many automatic backups of the arena file and map are kept per arena |

Before a version update rewrites `config.yml`, a copy of the old one is kept once per version in `data/backups/config/<version>/config.yml`. These copies contain the MySQL password and the network secret as they were. If you change either of them (for example after a leak), delete the old folders in `data/backups/config/` too.

## Automatic arena setup

Settings for `/bw startAutomaticSetup` ([Arena setup](arena-setup.md#automatic-setup)).

| Setting | Default | Meaning |
|---|---|---|
| `AutomaticSetup.DefaultRadius` | `150` | Scan radius in blocks when none is given |
| `AutomaticSetup.MaxRadius` | `400` | Largest radius allowed |
| `AutomaticSetup.GeneratorSearchRadius` | `20` | How many blocks around a bed an iron or gold block counts as that team's generator |
| `AutomaticSetup.BasePadding` | `8` | Blocks added around a team's points on the sides for its base area |
| `AutomaticSetup.BaseBelow` | `5` | Blocks added below |
| `AutomaticSetup.BaseAbove` | `15` | Blocks added above |

## Join NPCs

Lobby NPCs that join a game when clicked. Make one with Citizens (`/npc create`, `/npc select`) and attach it with `/bw npc set <GROUP|ARENA|SELECTOR> [target]`, or without Citizens with `/bw npc create <entityType> <action> [target]`.

| Setting | Default | Meaning |
|---|---|---|
| `JoinNpc.UpdateSeconds` | `2` | How often the holograms above the NPCs update |
| `JoinNpc.LeftClick` | `false` | Also react to a left click (right click always works) |
| `JoinNpc.Hologram.Offset` | `0.0` | Move the hologram up (positive) or down (negative), e.g. `-0.3` when the NPC name is hidden |
| `JoinNpc.Hologram.GROUP` / `ARENA` / `SELECTOR` | 3 lines each | Hologram lines per action (empty list = no hologram) |

Hologram placeholders: `/group/`, `/arena/`, `/playing/`, `/waiting/`, `/arenas_free/`, `/players/`, `/max/`, `/status/`.

## Storage

Where player data (language, quick buy) and stats are kept. Details and when to use MySQL: [Storage](storage.md).

| Setting | Default | Meaning |
|---|---|---|
| `Storage.Type` | `JSON` | `JSON` = files in `data/` (one server). `MYSQL` = the database below; use it on every server of a network so stats are shared. On the first start with an empty database, the JSON data of this server is imported |
| `MySQL.Host` / `Port` / `Database` / `User` / `Password` | `localhost` / `3306` / `newbedwars` / `root` / empty | Connection, also used by `Network.MySQL`. Uses the database driver that ships with the server |
| `MySQL.SslMode` | `PREFERRED` | `DISABLED`, `PREFERRED` (encrypt when the server supports it), `REQUIRED`, `VERIFY_CA` or `VERIFY_IDENTITY` |
| `MySQL.AllowPublicKeyRetrieval` | `false` | Only for MySQL 8 accounts with `caching_sha2_password` on a connection without SSL. Less secure |

## Leaderboards

| Setting | Default | Meaning |
|---|---|---|
| `Leaderboards.RefreshSeconds` | `60` | How often the [leaderboard placeholders](placeholders.md#leaderboards) are refreshed, in seconds (at least 5) |

## Updates

NewBedwars looks for new versions on Modrinth (GitHub as a fallback). Nothing about your server is sent.

| Setting | Default | Meaning |
|---|---|---|
| `Updates.Check` | `true` | `false` = never contact Modrinth or GitHub (also disables `/bw update`) |
| `Updates.Notify` | `true` | Tell the console and admins (`newbedwars.update.notify`, one chat line on join) about a new version |
| `Updates.AutoDownload` | `false` | Download new versions (checksum verified) into `plugins/update/`; installed on the next restart |
| `Updates.Channel` | `BETA` | Versions to offer: `RELEASE`, `BETA` (releases and betas) or `ALPHA` (everything) |
| `Updates.CheckIntervalHours` | `12` | Hours between checks |

## Network mode

Only used with a Velocity proxy and the `NewBedwars-Proxy` plugin. The full guide is [Network mode](network.md).

| Setting | Default | Meaning |
|---|---|---|
| `Network.Mode` | `NONE` | `NONE` = single server (nothing below is used). `GAME` = this server hosts arenas. `LOBBY` = no arenas here; joins send players to game servers |
| `Network.ServerId` | empty | The name of this server in `velocity.toml` (`[servers]`) |
| `Network.LobbyServer` | empty | `GAME`: lobby server to send players to. Empty = the proxy picks the least full lobby |
| `Network.LocalLobby` | `false` | `GAME`: `false` = players waiting to be put into an arena wait invisibly at the world spawn. `true` = they are teleported to this server's `/bw setLobby` location |
| `Network.LobbyJoinMessages` | `true` | `LOBBY`: show "<player> joined/left the game" in the lobby |
| `Network.RestartAfterGames` | `0` | `GAME`: restart the server after this many games, as soon as every arena is empty (`0` = never). Your start script must start it again |
| `Network.TicketTimeoutSeconds` | `10` | `GAME`: how long a player from the proxy waits for their arena before going back |
| `Network.StatePublishSeconds` | `5` | `GAME`: how often the arena list is sent to the proxy while players are online |
| `Network.MySQL.Enabled` | `false` | `GAME`: shared status table so the proxy sees game servers with no players online. Uses the `MySQL` connection |
| `Network.MySQL.HeartbeatSeconds` | `5` | How often that table is updated |
