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
| `%newbedwars_picked_team%` | Coloured name of the team the player picked in the waiting lobby, or `""` when not picked or after the game started (use `%newbedwars_team%` during the game) |
| `%newbedwars_kills%` | Kills in the current game |
| `%newbedwars_deaths%` | Deaths in the current game |
| `%newbedwars_final_kills%` | Final kills in the current game |
| `%newbedwars_beds_broken%` | Beds broken in the current game |
| `%newbedwars_players%` | Current player count in the arena |
| `%newbedwars_max_players%` | Max players in the arena |
| `%newbedwars_is_spectator%` | `true` / `false` |
| `%newbedwars_phase%` | Current phase name, or `""` |
| `%newbedwars_game_time%` | Seconds elapsed since game start, or `0` |
| `%newbedwars_game_time_formatted%` | Time since game start as `m:ss`, or `""` |
| `%newbedwars_map%` | Same as `%newbedwars_arena%` |
| `%newbedwars_in_arena%` | `true` when the player is in an arena (as player or spectator) |
| `%newbedwars_team_colored%` | Team name in the team colour, or `""` |
| `%newbedwars_team_short%` | First letter of the team name, or `""` |
| `%newbedwars_team_players%` | Players in the player's team |
| `%newbedwars_team_alive%` | `true` while the player's team is not eliminated |
| `%newbedwars_team_bed%` | `alive` / `destroyed` (bed of the player's team), or `""` |
| `%newbedwars_teams_total%`, `%newbedwars_teams_alive%` | Teams in the arena / teams still in the game |
| `%newbedwars_min_players%` | Players needed to start |
| `%newbedwars_spectators%` | Spectators in the arena |
| `%newbedwars_next_phase%` | Name of the next game phase, or `""` |
| `%newbedwars_phase_time%`, `%newbedwars_phase_time_formatted%` | Time until the next phase (seconds, or `m:ss`) |

## Tab list text (for TAB and other tab list plugins)

The text the plugin's own tab list (`tablists.yml`) would show the player, with all its placeholders already filled in. It works even when `Tablist.Enabled` is `false`, so you can switch the plugin's tab list off and let TAB show the same text. They are empty when the layout has no such text (for example in the lobby while `Lobby.Enabled` is `false`).

| Placeholder | Description |
|---|---|
| `%newbedwars_tablist_name%` | The `PlayerFormat` of the player's layout (team colour, short team name...) |
| `%newbedwars_tablist_header%`, `%newbedwars_tablist_footer%` | Header and footer, lines joined with a new line |

## Server

Work without a player.

| Placeholder | Description |
|---|---|
| `%newbedwars_server_name%` | `ServerName` from `config.yml` |
| `%newbedwars_lobby_players%` | Players not in an arena |
| `%newbedwars_total_players%` | Players in all enabled arenas |
| `%newbedwars_total_arenas%` | Enabled arenas |
| `%newbedwars_arenas_waiting%`, `%newbedwars_arenas_playing%` | Arenas in waiting/starting and in playing/ending |

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
| `%newbedwars_arena_<name>_min_players%` | Players needed to start |
| `%newbedwars_arena_<name>_spectators%` | Spectators |
| `%newbedwars_arena_<name>_group%` | Group of the arena, or `""` |
| `%newbedwars_arena_<name>_enabled%` | `true` / `false` |

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
