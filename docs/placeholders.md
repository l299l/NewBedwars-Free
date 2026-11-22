# PlaceholderAPI placeholders

If PlaceholderAPI is installed, all of the placeholders below become available under the prefix `%newbedwars_<placeholder>%`.

## In-game (per player)

| Placeholder | Description |
|---|---|
| `%newbedwars_arena%` | Arena name the player is in, or `""` |
| `%newbedwars_group%` | Group of the arena the player is in, or `""` |
| `%newbedwars_status%` | Game status: `waiting` / `starting` / `playing` / `ending` / `restarting`, or `lobby` |
| `%newbedwars_team%` | Team name (the display name when an addon set one), or `""` |
| `%newbedwars_team_color%` | Team ChatColor code, or `""` |
| `%newbedwars_kills%` | Kills in the current game |
| `%newbedwars_deaths%` | Deaths in the current game |
| `%newbedwars_final_kills%` | Final kills in the current game |
| `%newbedwars_beds_broken%` | Beds broken in the current game |
| `%newbedwars_players%` | Current player count in the arena |
| `%newbedwars_max_players%` | Max players in the arena |
| `%newbedwars_is_spectator%` | `true` / `false` |
| `%newbedwars_phase%` | Current phase name, or `""` |
| `%newbedwars_game_time%` | Seconds elapsed since game start, or `0` |

## Lifetime stats (per player)

| Placeholder | Description |
|---|---|
| `%newbedwars_stat_wins%` | Total wins |
| `%newbedwars_stat_losses%` | Total losses |
| `%newbedwars_stat_kills%` | Total kills |
| `%newbedwars_stat_deaths%` | Total deaths |
| `%newbedwars_stat_final_kills%` | Total final kills |
| `%newbedwars_stat_beds%` | Total beds broken |
| `%newbedwars_stat_games%` | Total games played |
| `%newbedwars_stat_kd%` | Kill/death ratio (1 decimal, e.g. `2.3`) |
| `%newbedwars_stat_wl%` | Win/loss ratio (1 decimal) |

## Leaderboards

The best players of all time, for leaderboard holograms, signs or menus (DecentHolograms, FancyHolograms, ...). They work without a player.

| Placeholder | Description |
|---|---|
| `%newbedwars_top_<stat>_<position>_name%` | Name of the player at that place |
| `%newbedwars_top_<stat>_<position>_value%` | Their value |

`<stat>` is `wins`, `kills`, `final_kills` or `beds`, and `<position>` is `1` to `10`. Example: `%newbedwars_top_wins_1_name%` - `%newbedwars_top_wins_1_value%` wins.

An empty place shows `-` as the name (`LeaderboardPlaceholderEmpty` in the language files) and `0` as the value. The lists are refreshed every `Leaderboards.RefreshSeconds` seconds (60 by default, in `config.yml`). With `Storage.Type: MYSQL` they show the whole network.

## Per arena

| Placeholder | Description |
|---|---|
| `%newbedwars_arena_<name>_status%` | Game status of the named arena |
| `%newbedwars_arena_<name>_players%` | Current player count |
| `%newbedwars_arena_<name>_max_players%` | Max players |
| `%newbedwars_arena_<name>_phase%` | Current phase name |

## Per group

Arena groups from `/bw group`. Group names may contain `_`.

| Placeholder | Description |
|---|---|
| `%newbedwars_group_<name>_players%` | Players in all enabled arenas of the group |
| `%newbedwars_group_<name>_playing%` | Players in running games (playing / ending) |
| `%newbedwars_group_<name>_waiting%` | Players waiting in lobbies (waiting / starting) |
| `%newbedwars_group_<name>_max_players%` | Total slots of the group's enabled arenas |
| `%newbedwars_group_<name>_arenas%` | Number of arenas in the group |
| `%newbedwars_group_<name>_arenas_free%` | Waiting arenas that still have room |
| `%newbedwars_group_<name>_best_arena%` | Arena `/bw join <group>` would pick now (fullest free one), or `""` |

An unknown group returns `""`.

## In shop items

**(2.8-beta)** Placeholders also work the other way round: the shop names and descriptions of shop items, upgrades and traps, and the names and descriptions of menu categories can use any PlaceholderAPI placeholder, and the player who opens the menu sees their own values. The item a player receives when buying keeps its normal text on purpose, so bought blocks still stack. See [Shop items](shop-and-upgrades.md#shop-items-itemsyml).
