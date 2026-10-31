# Player data storage: JSON or MySQL

NewBedwars keeps two kinds of player data:

- **Player data:** language and quick-buy layout.
- **Stats:** wins, losses, kills, deaths, final kills, beds broken and games played. Leaderboards and the stats placeholders use these.

`Storage.Type` in `config.yml` decides where they are kept:

| `Storage.Type` | Where | Needs |
|---|---|---|
| `JSON` (default) | `plugins/NewBedwars/data/playersData.json` and `playerStats.json` | Nothing |
| `MYSQL` | Tables `nbw_players` and `nbw_stats` in a MySQL/MariaDB database | A database and the `MySQL:` section in `config.yml` |

Arenas, groups, GUIs and other settings always stay in files. Only player data and stats move to the database.

## Do I need MySQL?

**On one server: no.** No feature or gameplay change needs it, and `JSON` is the simplest choice: nothing to install, and the data sits in the plugin folder.

MySQL is still worth it on a single server when:

1. **You want stats outside Minecraft.** A website leaderboard, a Discord bot or a stats page can read `nbw_stats` directly instead of parsing a JSON file in the server folder.
2. **Several separate servers should share stats.** Servers that are not a NewBedwars network (no Velocity proxy) but use the same database share stats, leaderboards and player settings.
3. **You have many players.** With JSON, all stats are held in memory and the whole file is rewritten on save. With MySQL, a player's row is loaded when they join, and a game result only adds to their numbers.
4. **You want database tools.** You can make dumps and scheduled backups, or fix one player's stats with a query while the server is running.

**On a Velocity network:** use `MYSQL` on every server if stats and leaderboards should be the same everywhere. Without it, each server counts its own stats. Network mode has one more optional database feature, the server status table. See [NETWORK.md](NETWORK.md#is-mysql-required).

## Switching to MySQL

1. Create a database and a user for it (MySQL or MariaDB).
2. Fill in the `MySQL:` section of `config.yml` (`Host`, `Port`, `Database`, `User`, `Password`) and set `Storage.Type: MYSQL`.
3. Restart the server. The tables are created automatically. On the first start with an empty database, the server's JSON data is imported once. The JSON files are left in place, but they are no longer updated.

The plugin uses the MySQL driver that ships with the server, so nothing else needs to be installed.

## Tables

| Table | Columns |
|---|---|
| `nbw_players` | `name` (key), `uuid`, `language`, `fast_buy`, `updated` |
| `nbw_stats` | `name` (key), `wins`, `losses`, `kills`, `deaths`, `final_kills`, `beds_broken`, `games_played` |

Rows are keyed by player name. Reading these tables from other programs is safe. The plugin only ever adds to stats (`wins = wins + 1`) and never overwrites them, so editing a stat by hand is safe too. An online player sees the new value after they rejoin.
