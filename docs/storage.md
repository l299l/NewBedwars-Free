# Player data storage: JSON or MySQL

NewBedwars keeps two kinds of player data:

- **Player data:** language and quick-buy layout.
- **Stats:** wins, losses, kills, deaths, final kills, beds broken and games played. Leaderboards and the stats placeholders use these.

`Storage.Type` in `config.yml` decides where they are kept:

| `Storage.Type` | Where | Needs |
|---|---|---|
| `JSON` (default) | `plugins/NewBedwars/data/playersData.json` and `playerStats.json` | Nothing |
| `MYSQL` | Tables `nbw_player_data` and `nbw_player_stats` in a MySQL/MariaDB database | A database and the `MySQL:` section in `config.yml` |

Arenas, groups, GUIs and other settings always stay in files. Only player data and stats move to the database.

## Do I need MySQL?

**On one server: no.** No feature or gameplay change needs it, and `JSON` is the simplest choice: nothing to install, and the data sits in the plugin folder.

MySQL is still worth it on a single server when:

1. **You want stats outside Minecraft.** A website leaderboard, a Discord bot or a stats page can read `nbw_player_stats` directly instead of parsing a JSON file in the server folder.
2. **Several separate servers should share stats.** Servers that are not a NewBedwars network (no Velocity proxy) but use the same database share stats, leaderboards and player settings.
3. **You have many players.** With JSON, all stats are held in memory and the whole file is rewritten on save. With MySQL, a player's row is loaded when they join, and a game result only adds to their numbers.
4. **You want database tools.** You can make dumps and scheduled backups, or fix one player's stats with a query while the server is running.

**On a Velocity network:** use `MYSQL` on every server if stats and leaderboards should be the same everywhere. Without it, each server counts its own stats. Network mode has one more optional database feature, the server status table. See [network.md](network.md#is-mysql-required).

## Switching to MySQL

1. Create a database and a user for it (MySQL or MariaDB).
2. Fill in the `MySQL:` section of `config.yml` (`Host`, `Port`, `Database`, `User`, `Password`) and set `Storage.Type: MYSQL`.
3. Restart the server. The tables are created automatically. On the first start with an empty database, the server's JSON data is imported once. The JSON files are left in place, but they are no longer updated.

The plugin uses the MySQL driver that ships with the server, so nothing else needs to be installed.

## Tables

| Table | Columns |
|---|---|
| `nbw_player_data` | `uuid` (key), `name`, `language`, `fast_buy`, `updated` |
| `nbw_player_stats` | `uuid` (key), `name`, `wins`, `losses`, `kills`, `deaths`, `final_kills`, `beds_broken`, `games_played` |

Rows are keyed by the player's UUID, so a player who changes their Minecraft name keeps their stats; `name` is the last name the player joined with. Reading these tables from other programs is safe. The plugin only ever adds to stats (`wins = wins + 1`) and never overwrites them, so editing a stat by hand is safe too. An online player sees the new value after they rejoin.

### Upgrading from 2.6-beta or older

Before 2.7-beta, player data and stats were stored by player name (tables `nbw_players` and `nbw_stats`, and name keys in the JSON files). On the first start of 2.7-beta:

- **JSON:** `playerStats.json` is rewritten with UUID keys; the old file is kept as `playerStats.json.before-uuid`. `playersData.json` is rewritten with UUID keys on the next save.
- **MySQL:** when the new tables are empty, the rows of `nbw_players` and `nbw_stats` are copied into `nbw_player_data` and `nbw_player_stats`. The old tables are left untouched as a backup and can be dropped once everything looks right. Programs that read `nbw_stats` (website leaderboards, bots) must switch to `nbw_player_stats`.

Stats of a name with no known UUID (a player who never joined since player data was saved) get the offline-mode UUID of that name. Stats that were split between an old and a new name of the same player are added together.
