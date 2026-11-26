# Display

See [Getting started](index.md) for how to get `NewBedwarsAPI api` and the dependency.

## Placeholders

> [!IMPORTANT]
> **New in 2.8-beta.**

`api.registerPlaceholder(owner, name, placeholder)` adds one custom placeholder that works everywhere NewBedwars renders text: `{name}` in `tablists.yml`, and `/name/` in `scoreboards.yml`, `bossBars.yml` and the boss bars of addons (`BossBarService#show`).

```java
api.registerPlaceholder(this, "rank", (viewer, arena) -> ranks.of(viewer));
```

`TextPlaceholder#resolve(Player viewer, ArenaView arenaOrNull)` is called on the server thread every time a text using it is rendered, so keep it fast. `viewer` is the player the text is for (in a tab list `PlayerFormat` the player being listed). It is `null` for the boss bars of an arena and the lobby boss bar, because all players of the arena share one bar. The arena is `null` outside arenas. `&` colours in the result are translated afterwards; `null` counts as empty.

A name is 1 to 32 letters, digits or `_`. `registerPlaceholder` throws `IllegalArgumentException` for other names, for names any of the three systems uses itself (compared ignoring case, e.g. `player`, `arena`, `teamColor`, `map`, `status`, `arena_name`) and for names that are already registered. An exception thrown by your placeholder is logged once per name and renders as empty text. Placeholders are removed when your plugin is disabled or on `unregisterAll(owner)`; they also survive `/bw reload`. Scoreboard lines and titles also apply PlaceholderAPI when it is installed.

The tab list has a shortcut, `api.getTablists().registerPlaceholder`, that adds `{name}` for the tab list only:

```java
api.getTablists().registerPlaceholder(this, "rank", (viewer, arena) -> ranks.of(viewer));
```

`TablistPlaceholder` has the same `resolve(Player viewer, ArenaView arenaOrNull)` as `TextPlaceholder`. In the `PlayerFormat` of a tab list the player is the one whose name is being listed, and the arena is `null` in the lobby.

## Tab list

> [!IMPORTANT]
> **New in 2.8-beta.**

`api.getTablists()` returns `TablistService`, a view of the custom tab list (`tablists.yml`). Call it on the server thread.

| Method | Returns / does |
|---|---|
| `isEnabled()` | `Tablist.Enabled` from config.yml |
| `getLayouts()` | ids of the layouts in `tablists.yml`, sorted |
| `getDefaultLayout()` | `DefaultTablist` from config.yml |
| `getEffectiveLayout(arena)` | the layout an arena's players get: the arena's own (`ArenaView#getTablist()`), else the default, and `Default` when that layout doesn't exist. `null` gives the default |
| `isLobbyEnabled()` | whether players outside arenas get the `Lobby` tab list |
| `registerPlaceholder(owner, name, placeholder)` | adds `{name}` for the Header, Footer and PlayerFormat lines |
| `setPlayerLayout(player, layoutId)` / `clearPlayerLayout(player)` | a layout for one player |
| `refresh(player)` / `refresh(arena)` | renders now instead of at the next 1 second tick |

Custom placeholders for the tab list: see [Placeholders](#placeholders) above.

### Layouts per player

`setPlayerLayout(player, "Fancy")` makes the player see that layout instead of the arena's or the default one. It applies inside arenas only and is removed when the player leaves the arena, quits, or you call `clearPlayerLayout(player)`; it survives `/bw reload`. An unknown layout id throws `IllegalArgumentException`.

### Arena layout and modes

`ArenaView#getTablist()` is the layout id set on the arena (`/bw setTablist`), `null` = server default. `ArenaEditor#setTablist(layoutId)` changes it while the arena is waiting (`null` = default, an unknown id throws `IllegalArgumentException`); like the other setters it is saved with `save()` and it refreshes the tab list of the arena's players. Game modes carry it as `ModeInfo#tablist` (see [Game modes](shop-and-modes.md#game-modes)).

### Changing what is sent

`TablistUpdateEvent` is fired just before a changed header, footer or list name is sent to a player. It is not fired while the rendered tab list is unchanged, so it never runs every tick. Set the texts you want with `setHeader`, `setFooter` and `setPlayerListName` (`null` header/footer count as empty, a `null` list name shows the vanilla name), or cancel it to send nothing for this change. What your listener decides is only asked again when the rendered tab list changes or after `refresh`, so keep the result a function of the event's values or call `refresh` when your data changes.

```java
@EventHandler
public void onTablist(TablistUpdateEvent e) {
    e.setFooter(e.getFooter() + "\n&7Play on " + host);
}
```

## Scoreboards

> [!IMPORTANT]
> **New in 2.8-beta.**

`api.getScoreboards()` returns `ScoreboardService`, a view of the sidebar scoreboards of `scoreboards.yml`. Call it on the server thread.

| Method | Returns / does |
|---|---|
| `getScoreboards()` | ids in `scoreboards.yml`, sorted |
| `isEnabled(id)` | whether the scoreboard exists and has `Enabled: true` |
| `getCurrentScoreboard(player)` | the id shown to the player now, `null` outside arenas, when hidden or disabled |
| `setPlayerScoreboard(player, id)` / `clearPlayerScoreboard(player)` | a scoreboard for one player instead of the one the arena state picks; an unknown id throws `IllegalArgumentException` |
| `hide(player)` / `show(player)` | removes the NewBedwars sidebar for the player (for addons that draw their own), or brings it back |
| `refresh(player)` | renders now instead of at the next 1 second tick |

The override and `hide` apply inside arenas only and are removed when the player leaves the arena or quits (`hide` also when you call `show`). An override for a scoreboard with `Enabled: false` shows nothing.

`ScoreboardUpdateEvent` is fired just before a changed title or lines are sent to a player. It is not fired while the rendered scoreboard is unchanged, so it never runs every tick. Set what you want with `setTitle` and `setLines` (at most 15 lines, otherwise `IllegalArgumentException`; equal lines are made unique automatically), or cancel it to send nothing for this change. The title in the event is already resolved for the player (colours, `&#RRGGBB` and PlaceholderAPI). A title you change replaces the animated `TitleEffect`. What your listener decides is only asked again when the rendered scoreboard changes or after `refresh`, so keep the result a function of the event's values or call `refresh` when your data changes.

```java
@EventHandler
public void onScoreboard(ScoreboardUpdateEvent e) {
    List<String> lines = e.getLines();
    if (lines.size() < 15) lines.add("&7" + host);
    e.setLines(lines);
}
```

## Boss bars

> [!IMPORTANT]
> **New in 2.8-beta.**

`api.getBossBars()` returns `BossBarService`. Call it on the server thread.

| Method | Returns / does |
|---|---|
| `getBossBars()` | ids in `bossBars.yml`, sorted |
| `isEnabled(id)` | whether the boss bar exists and has `Enabled: true` |
| `show(owner, player, text, color, style, progress)` | shows your own boss bar to one player and returns an `AddonBossBar` |

```java
AddonBossBar bar = api.getBossBars().show(this, player, "&eEvent in /players/ players", BarColor.YELLOW, BarStyle.SOLID, 1.0);
bar.setProgress(0.5);
bar.remove();
```

The text supports `&` colours, the `/name/` placeholders of `bossBars.yml`, placeholders registered with `registerPlaceholder` and PlaceholderAPI when it is installed; a text with placeholders is resolved again every second. `AddonBossBar` has `getPlayer()`, `setText`, `setColor`, `setStyle`, `setProgress` (clamped to 0..1), `isVisible()` and `remove()`. A bar is removed when the player quits, leaves or changes the arena (if it was shown while the player was in one), when your plugin is disabled, on `unregisterAll(owner)` and when NewBedwars is disabled; `isVisible()` is then `false`.

`BossBarUpdateEvent` is fired when a boss bar of `bossBars.yml` switches to a new text (a step of its `Texts` cycle, or an arena starts showing it at a phase change), not for addon boss bars. Change `setText`, `setColor` and `setStyle`, or cancel it to keep the previous display for this step. A text you change is no longer refreshed every second until the next step. When PlaceholderAPI is installed and a text of the bar contains `%`, every player gets an own bar; the event is still fired once per step, its text then still contains the `%...%` placeholders and PlaceholderAPI is applied for each viewer afterwards (also to a text you set). Otherwise all players share one bar and the text has no viewer.
