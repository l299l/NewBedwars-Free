# Network mode (Velocity)

NewBedwars can run on a proxy network: **lobby servers** where players choose a game, and **game servers** that host the arenas. Players and parties are moved between them automatically.

By default (`Network.Mode: NONE`) NewBedwars is a single server and nothing on this page applies.

## What you need

| Where | What |
|---|---|
| Velocity proxy | `NewBedwars-Proxy-<version>.jar` in `plugins/` (Velocity 4.x, Java 25) |
| Every lobby and game server | `NewBedwars-<version>.jar` (same version on all of them) |
| Optional | A MySQL/MariaDB database that game servers and the proxy can reach |

Set up Velocity forwarding as usual (`player-info-forwarding-mode = "MODERN"` in `velocity.toml`, `proxies.velocity` in each Paper server's `config/paper-global.yml`).

> **Install the proxy plugin.** Besides routing, it blocks players from sending fake NewBedwars network messages to your servers. Don't run `Network.Mode: GAME/LOBBY` without it.

## Scoreboard
`/server/` in `scoreboards.yml` shows the server's name (`Network.ServerId`); the default waiting scoreboard has a `Server:` line. A line with `/server/` is left out on a single server. Existing `scoreboards.yml` files are not changed by an update: add the line yourself.

## Is MySQL required?
(For a single server see [STORAGE.md](STORAGE.md).)

No. Every feature works without a database, on one server or on a network. MySQL/MariaDB only adds:

| Feature | Without MySQL | With MySQL |
|---|---|---|
| Stats and player data (`Storage.Type: MYSQL`) | Each server keeps its own stats, leaderboards and player settings in JSON files | Shared by every server of the network |
| Server status (`Network.MySQL.Enabled` on game servers + `mysql:` in the proxy config) | The proxy only knows a game server's arenas while a player is online on it; a server it has not heard from is still tried and picks an arena itself | The proxy always knows every game server's arenas, also right after a restart |
| Rejoin notice on the lobby | Works when somebody else is still online on that game server to carry the message | Always works (the status table carries the rejoin places) |

Joining through the proxy, parties, rejoining, map voting, `RestartAfterGames`, NPCs and placeholders all work without MySQL.

## Server config (`plugins/NewBedwars/config.yml`)

```yaml
Network:
  Mode: GAME          # GAME on game servers, LOBBY on lobby servers
  ServerId: "game1"   # this server's name in velocity.toml [servers]
  LobbyServer: ""     # GAME: lobby to send players to; empty = the proxy picks the least full lobby
  LocalLobby: false   # GAME: false = no /bw setLobby needed on game servers (see below)
  LobbyJoinMessages: true  # LOBBY: show "<player> joined/left the game" (game servers never show it)
  RestartAfterGames: 0
  TicketTimeoutSeconds: 10
  StatePublishSeconds: 5
  MySQL:
    Enabled: false      # GAME: write the server status to the shared table (uses the MySQL section)
    HeartbeatSeconds: 5

Storage:
  Type: MYSQL           # share stats, leaderboards and languages across the network
MySQL:
  Host: "localhost"
  Port: 3306
  Database: "newbedwars"
  User: "root"
  Password: ""
```

### Game servers (`Mode: GAME`)
- Set up arenas as on a single server. One server can host one or many arenas. Arena groups are **not** set here (see the lobby below); `/bw group` on a game server only lists them, and its own `data/groups.json` is not used.
- **No lobby needed** (`LocalLobby: false`, default): players who arrive and wait for their arena, or who leave a game, wait for that moment invisibly in spectator mode at the world spawn, then go into the arena or on to a lobby server. With `LocalLobby: true` they are teleported to this server's `/bw setLobby` location instead (the old behaviour).
- Players who arrive from the proxy go straight into the arena the proxy picked (parties together). A player who arrives without a join request is sent back to the lobby after `TicketTimeoutSeconds`. Staff with `newbedwars.network.bypass` (default: op) can stay, e.g. to set up arenas.
- After a game (or `/bw lobby`), players go to a lobby server. With map voting, players who vote move to the next arena on the same server.
- `RestartAfterGames: <n>` (0 = off): after `n` finished games the server is marked "restarting": the proxy and lobbies send no new players there, running games finish normally, and once no arena has players everyone left is sent to a lobby and the server shuts down. Your start script (or panel) has to start it again; the plugin only stops it. Useful to clear memory and old chunks of game servers that run for days.
- **Map voting across servers:** at the end of a game the vote offers the free arenas of the same group on **every** game server (the item shows the server). If an arena on another server wins, everyone (parties together) is moved there through the proxy.

### Lobby servers (`Mode: LOBBY`)
- No arenas are loaded.
- `/bw join` opens the arena selector with the arenas of all game servers; clicking an arena takes you to exactly that server's arena. `/bw join <group|arena>` asks the proxy for the best game: an arena name that exists on several servers means the fullest of them that has room. `/bw join <server>:<arena>` (and `ARENA <server>:<arena>` join NPCs) go only to that server's arena.
- **Arena groups are made here** with `/bw group` (see [COMMANDS.md](COMMANDS.md)). `/bw group add <group> <arena>` adds that arena on **every** game server that has it; `/bw group add <group> <server>:<arena>` only one server's arena. Problems and team sizes are checked against the arenas the game servers report. The proxy uses them right away for routing and the arena lists, also for game servers nobody is on, and sends them to every game server (each keeps the last copy in `data/network-groups.json`, so it still knows them after a restart); joining a group, its NPCs and placeholders and the map vote across servers all use them. With several lobby servers the last edit wins and is copied to the other lobbies.
- `/bw arena list` shows the arenas of all game servers (server, group, state, players); `/bw arena info` explains how to set arenas up on a network. All other `/bw arena` subcommands (create, setup, backup, …) are refused here: run them on the game server that has the arena.
- `/bw spectate` opens a menu of the running games on all game servers; `/bw spectate <arena>` watches that game.
- Join NPCs: `GROUP`, `ARENA` and `SELECTOR` send players to game servers (the old `NETWORK` action was removed; NPCs that still have it work as `GROUP`). Holograms and the `%newbedwars_group_<name>_...%` placeholders show the network's numbers.
- Only the party admin can join; the whole party is moved.

## Proxy config (`plugins/newbedwars-proxy/config.yml`)

```yaml
game-servers: [game1, game2]   # names from velocity.toml
lobby-servers: [lobby]
ticket-timeout-seconds: 10
state-stale-seconds: 15
party-invite-seconds: 60
mysql:
  enabled: false
  host: "localhost"
  port: 3306
  database: "newbedwars"
  user: "root"
  password: ""
  poll-seconds: 5
```

- The proxy picks the fullest arena that still has room (the same rule as `/bw join` on one server). If the game server refuses (the arena filled up in the meantime), the proxy tries one other server. If that fails too, the players go back to the lobby.
- Game servers can only report their arenas while a player is online on them. A game server the proxy has not heard from recently is still tried, and it then picks an arena itself. With MySQL enabled on the game servers and on the proxy, the proxy always knows every server's arenas.
- `/nbwproxy` (permission `newbedwars.proxy.admin`) lists the game servers and arenas the proxy knows.
- Proxy texts are in `plugins/newbedwars-proxy/lang/en.yml` and `pl.yml` and are chosen by the player's client language.

## Stats and player data
With `Storage.Type: MYSQL` on every server (same database), stats, leaderboards (`/bw leaderboard`, placeholders), languages and quick-buy layouts are shared by the whole network. A game's result is added to the stats on the database, so games ending on several servers at once never overwrite each other. A player's data is read when they join a server; leaderboards and online players' stats are refreshed every minute. On the first start with an empty database, that server's JSON data is imported once.

## Rejoining
When a player leaves a running game while their team is still playing (disconnect or `/bw lobby`), the game server keeps their place for `RejoinTime` minutes and tells the network about it. On a lobby server the player gets a title, a sound, a clickable "Click to rejoin" button and an action bar reminder with the time left. `/bw rejoin` (or the button) sends them back to that game server and arena. A player who was the last of their team has no place to go back to (their team is out).

## Parties
In network mode, `/party` (alias `/p`) is handled by the proxy, so parties survive server switches. The commands are the same as on a single server.

## Limits
- BungeeCord is not supported yet (Velocity only).
