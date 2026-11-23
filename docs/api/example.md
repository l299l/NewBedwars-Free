# Full example

This page walks through a complete addon, [`docs/examples/event-logger`](https://github.com/l299l/NewBedwars-Free/tree/master/docs/examples/event-logger). Every snippet below is copied from its source, [`EventLoggerPlugin.java`](https://github.com/l299l/NewBedwars-Free/blob/master/docs/examples/event-logger/src/main/java/com/example/nbwlogger/EventLoggerPlugin.java), so you can find it there. The other pages explain each API in detail; this one shows them working together.

## What the addon does

- Logs every API event to the console and to the chat of players with `nbwlogger.use`.
- Registers a phase action (`NBWLOG`), shop item logic (`NBWLIGHTNING`), an armor tier (`NBWTEST`), a team upgrade (`NBWSPEED`), a trap (`NBWBLIND`) and one subcommand (`/bw nbwinfo`).
- Stores its own data in arenas and teams (extra data) and edits an arena with the arena editor.
- Adds a placeholder `logger_events`, an addon boss bar, a scoreboard line and a tab list footer line.
- Adds `/nbwlog` (`chat`, `modes`, `edit`) to print what the read-only API sees.

To build it, run `mvn package` in the example folder (the NewBedwars API comes from JitPack) and copy `target/NewBedwarsEventLogger-1.0.jar` into the server's `plugins/` folder next to NewBedwars. It needs NewBedwars 2.8-beta or newer.

## Project setup

`pom.xml`: the JitPack repository and the API as a `provided` dependency, because NewBedwars supplies the classes at runtime. Never shade the API into your jar. The example compiles against Paper 1.17.1, the oldest supported version; see [Getting started](index.md#setup).

```xml
    <repositories>
        <repository>
            <id>papermc-repo</id>
            <url>https://repo.papermc.io/repository/maven-public/</url>
        </repository>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```

```xml
        <dependency>
            <groupId>com.github.l299l.NewBedwars-Free</groupId>
            <artifactId>newbedwars-api</artifactId>
            <version>v2.8-beta</version>
            <scope>provided</scope>
        </dependency>
```

`plugin.yml` has to list NewBedwars in `depend`, so it loads first and its API service exists when your `onEnable` runs:

```yaml
name: NewBedwarsEventLogger
version: 1.0
main: com.example.nbwlogger.EventLoggerPlugin
api-version: "1.17"
depend: [NewBedwars]
description: Logs every NewBedwars API event for testing.
commands:
  nbwlog:
    description: "NewBedwarsEventLogger: /nbwlog [chat|modes|edit]"
    usage: /nbwlog [chat|modes|edit]
    permission: nbwlogger.use
permissions:
  nbwlogger.use:
    default: op
```

## Getting the API

`NewBedwarsAPI.get()` returns the API, and throws when NewBedwars is not enabled. The addon also checks the [API version](index.md#api-version): the display features need API version 1 (2.8-beta), so on anything older it disables itself instead of failing later. `getApiVersion()` itself exists from 2.8-beta; to support older servers too, catch `NoSuchMethodError` as described in Getting started.

```java
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        NewBedwarsAPI api = NewBedwarsAPI.get();
        getLogger().info("API version of NewBedwars: " + api.getVersion());
        if (api.getApiVersion() < 1) {
            getLogger().warning("NewBedwars 2.8-beta or newer is needed for the display APIs, disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        registerAll(api);
    }
```

## Listening to events

NewBedwars events are normal Bukkit events, so the addon is a `Listener` and registers itself on the first line of `onEnable`. Most handlers only read data and use `EventPriority.MONITOR`, which runs after other plugins have decided and sees the final result:

```java
    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(PlayerKillEvent e) {
        String killer = e.getKiller() == null ? "none" : e.getKiller().getName();
        log("Kill victim=" + e.getVictim().getName() + " killer=" + killer + " final=" + e.isFinalKill() + " cause=" + e.getCause());
    }
```

Events that can be cancelled are handled earlier, at the default priority. The example has a `CANCEL_TEST` switch for trying that: with it set to `true`, bed breaks of the team "Red" and purchases of "Wool" are cancelled.

```java
    @EventHandler
    public void onBed(BedBreakEvent e) {
        if (CANCEL_TEST && e.getTeam().getName().equalsIgnoreCase("Red")) e.setCancelled(true);
        String breaker = e.getBreaker() == null ? "the server" : e.getBreaker().getName();
        log("BedBreak team=" + e.getTeam().getName() + " by " + breaker + " cancelled=" + e.isCancelled());
        if (!e.isCancelled()) e.getTeam().getPlayers().forEach(this::showBedBar);
    }
```

The addon handles these events. Details of every getter are in [Events](data-and-events.md#events).

| Event | Meaning |
|---|---|
| `GameStartEvent` | The game starts, teams are assigned |
| `GameEndEvent` | The game ends; `getWinner()` is `null` for a draw |
| `ArenaResetEvent` | Before (`PRE`) and after (`POST`) a map reset |
| `ArenaStateChangeEvent` | The arena changes state, for example WAITING to STARTING |
| `TeamsAssignedEvent` | Players were distributed into teams |
| `PlayerJoinArenaEvent` | A player joins (also a rejoin); can be cancelled |
| `PlayerLeaveArenaEvent` | A player leaves, with the reason (command, quit, ...) |
| `PlayerSpectateEvent` | A player becomes a spectator |
| `PlayerRespawnArenaEvent` | A player respawns |
| `PlayerKillEvent` | A player dies: victim, killer, final kill, cause |
| `BedBreakEvent` | A bed is destroyed; can be cancelled |
| `ShopPurchaseEvent` | A player buys a shop item; can be cancelled |
| `ShopItemStackEvent` | An item is built or given from the shop |
| `ArmorEquipEvent` | A player's armor tier changes |
| `TeamUpgradeEvent` | A team buys an upgrade level |
| `TrapTriggerEvent` | An enemy triggers a team trap |
| `PlayerSendToLobbyEvent` | A player is sent to the lobby |
| `PartyChangeEvent` | A party is created, changed or disbanded |

## Registering content

Everything is registered in one method. All `register*` calls take your plugin as `owner`; see [Registering your own content](registering.md) for the rules.

### Phase action

A phase action is a name you can put in the `actions` of a game phase. This one tells everyone in the arena a message and logs it. See [Phase actions](registering.md#phase-actions).

```java
        // Phase action: put "NBWLOG:hello" in a phase's actions in config.yml.
        api.registerPhaseAction(this, "NBWLOG", (arena, arg) -> {
            log("PhaseAction NBWLOG arena=" + arena.getName() + " phase=" + arena.getCurrentPhase() + " arg='" + arg + "'");
            arena.getPlayers().forEach(p -> p.sendMessage(ChatColor.AQUA + "[NBWLOG] " + arg));
        });
```

Try it: add `NBWLOG:hello` to the actions of a phase in `config.yml`. When the phase runs, players get `[NBWLOG] hello`.

### Shop item logic

The key you register is used as `itemLogic` on a shop item. Returning `true` consumes one item. See [Shop item logic](registering.md#shop-item-logic).

```java
        // Shop logic: set "itemLogic: NBWLIGHTNING" on a shop item in items.yml.
        api.registerShopLogic(this, "NBWLIGHTNING", (player, arena) -> {
            player.getWorld().strikeLightningEffect(player.getLocation());
            log("ShopLogic NBWLIGHTNING used by " + player.getName() + " in " + arena.getName());
            return true;
        });
```

Try it: set `itemLogic: NBWLIGHTNING` on an item in `items.yml`, buy it and right-click: a lightning effect, and the item is used up.

### Armor tier

A tier is a name, a rank and the four pieces. Buying an item with `onBuyEvent: GIVE_ARMOR` and one of the tier's pieces gives the whole tier, and a lower-ranked tier never replaces a higher one. See [Armor tiers](registering.md#armor-tiers).

```java
        // Armor tier: a shop item with onBuyEvent GIVE_ARMOR and e.g. TURTLE_HELMET gives the whole tier.
        api.registerArmorTier(this, new ArmorTierDefinition("NBWTEST", 2, Material.DIAMOND_BOOTS,
                Material.IRON_LEGGINGS, Material.GOLDEN_CHESTPLATE, Material.TURTLE_HELMET));
```

Try it: a shop item with `onBuyEvent: GIVE_ARMOR` and `TURTLE_HELMET` gives diamond boots, iron leggings, a golden chestplate and a turtle helmet.

### Upgrade and trap

Upgrades and traps share one definition. `NBWSPEED` has two levels costing 2 and 4 diamonds, applies Speed to the team and is applied again after a respawn. `NBWBLIND` is a trap that blinds the enemy who enters the base. See [Upgrades and traps](registering.md#upgrades-and-traps).

```java
        // Upgrade (use "upg:NBWSPEED" in the upgrade GUI file) and trap ("upg:NBWBLIND").
        api.registerUpgrade(this, UpgradeDefinition.upgrade("NBWSPEED", 2, Material.SUGAR, List.of(2, 4),
                (player, team, level) -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, level - 1));
                    log("Upgrade effect NBWSPEED level " + level + " on " + player.getName());
                }).reapplyOnRespawn());
        api.registerUpgrade(this, UpgradeDefinition.trap("NBWBLIND", 1, Material.INK_SAC, List.of(1),
                (enemy, team, arena) -> {
                    enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                    log("Trap NBWBLIND triggered on " + enemy.getName() + " at team " + team.getName());
                }));
```

Try it: put `upg:NBWSPEED` and `upg:NBWBLIND` in the upgrade GUI file and buy them in a game.

### Subcommand

`registerCommand` adds a subcommand of `/bw`. The same command can live in several contexts; here it works in a game, for admins in the lobby and from the console (the console methods are `performConsole` and `tabCompleteConsole`). It also appears in `/bw help`. See [Subcommands](registering.md#subcommands).

```java
        // One subcommand in three places: /bw nbwinfo in a game, as a lobby admin, and from the console.
        api.registerCommand(this, new InfoCommand("nbwinfo"), CommandContext.GAME, CommandContext.LOBBY_ADMIN, CommandContext.CONSOLE);
```

The command class implements `AddonCommand`: name, description, syntax, example and what to do when a player or the console runs it.

```java
    /** Prints the arena's extra data and the team upgrade levels. */
    private final class InfoCommand implements AddonCommand {
        private final String name;

        InfoCommand(String name) {
            this.name = name;
        }

        @Override public String getName() { return name; }
        @Override public String getDescription() { return "Logger addon: shows extra data and upgrade levels"; }
        @Override public String getSyntax() { return "/bw " + name; }
        @Override public String getExample() { return "/bw " + name; }

        @Override
        public void perform(Player player, String[] args, ArenaView arena) {
            player.sendMessage("Command " + name + " works. Arena: " + (arena == null ? "none" : arena.getName()));
            if (arena != null) describe(player, arena);
        }

        @Override
        public void performConsole(CommandSender sender, String[] args) {
            if (args.length < 2) {
                sender.sendMessage("Usage: bw " + name + " <arena>");
                return;
            }
            NewBedwarsAPI.get().getArena(args[1]).ifPresentOrElse(arena -> describe(sender, arena),
                    () -> sender.sendMessage("No arena " + args[1]));
        }

        @Override
        public List<String> tabCompleteConsole(CommandSender sender, String[] args) {
            return args.length == 2 ? NewBedwarsAPI.get().getArenas().stream().map(ArenaView::getName).collect(Collectors.toList()) : List.of();
        }
    }
```

Try it: run `/bw nbwinfo` in a game, as an admin in the lobby, and `bw nbwinfo <arena>` in the console, where the arena name tab-completes.

## Extra data and the arena editor

Extra data is your own storage inside an arena or a team. It is saved in the arena file, namespaced by your plugin name, and NewBedwars never touches it. The addon counts games on the arena when a game starts and wins on the winning team. See [Extra data](data-and-events.md#extra-data).

```java
    @EventHandler(priority = EventPriority.MONITOR)
    public void onStart(GameStartEvent e) {
        String teams = e.getArena().getTeams().stream()
                .map(t -> t.getName() + "=" + t.getPlayers().size()).collect(Collectors.joining(", "));
        ExtraData extra = e.getArena().getExtra(this);
        extra.set("games", extra.getInt("games", 0) + 1);
        log("GameStart " + a(e) + " teams: " + teams + " (games played with this addon: " + extra.getInt("games", 0) + ")");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnd(GameEndEvent e) {
        TeamView w = e.getWinner();
        if (w != null) {
            ExtraData extra = w.getExtra(this);
            extra.set("wins", extra.getInt("wins", 0) + 1);
        }
        log("GameEnd " + a(e) + " winner=" + (w == null ? "DRAW" : w.getName()));
    }
```

The values are printed by the addon's `describe` method, which `/bw nbwinfo` and `/nbwlog` use, and survive a restart (`extra` in the arena JSON).

The arena editor changes arenas that are being set up or waiting for players. `/nbwlog edit` creates a team, adds and removes a generator, removes the team and saves, all with the position of the player running it. It refuses while a game is running (the editor throws, and the addon reports the exception). See [Editing arenas](arenas.md#editing-arenas).

```java
    private void runEditorTest(Player player) {
        NewBedwarsAPI api = NewBedwarsAPI.get();
        ArenaView arena = api.getArena(player.getWorld()).orElse(null);
        if (arena == null) {
            player.sendMessage("Stand in the arena world (setup or waiting) first.");
            return;
        }
        try {
            ArenaEditor editor = api.edit(arena);
            int generatorsBefore = arena.getGenerators().size();
            TeamEditor team = editor.createTeam("NbwTest", ChatColor.LIGHT_PURPLE);
            team.setSpawn(player.getLocation());
            team.setColor(ChatColor.AQUA);
            player.sendMessage("Created team NbwTest, teams now: " + arena.getTeams().stream().map(TeamView::getName).toList());
            editor.addGenerator(GeneratorKind.DIAMOND, player.getLocation());
            player.sendMessage("Generators " + generatorsBefore + " -> " + arena.getGenerators().size());
            player.sendMessage("removeGenerator: " + editor.removeGenerator(arena.getGenerators().size() - 1));
            player.sendMessage("removeTeam: " + editor.removeTeam("NbwTest"));
            editor.save();
            player.sendMessage("Editor OK. Teams: " + arena.getTeams().size() + ", generators: " + arena.getGenerators().size());
        } catch (RuntimeException ex) {
            player.sendMessage(ChatColor.RED + "Editor refused: " + ex);
        }
    }
```

## Display

The display APIs are new in 2.8-beta. See [Display](display.md) for all of them.

### Placeholder

One registration works in `tablists.yml` as `{logger_events}`, and in `scoreboards.yml` and `bossBars.yml` as `/logger_events/`. See [Placeholders](display.md#placeholders). The counter is incremented by the addon's `log` method, which every handler calls.

```java
        // Placeholder: {logger_events} in tablists.yml, /logger_events/ in scoreboards.yml and bossBars.yml.
        api.registerPlaceholder(this, "logger_events", (viewer, arena) -> String.valueOf(loggedEvents.get()));
```

### Boss bar

When a bed is broken, every player of that team gets a red, full boss bar for five seconds. The bar is shown with `getBossBars().show(...)` and removed by a normal Bukkit scheduler task; NewBedwars also removes it if the player leaves first. See [Boss bars](display.md#boss-bars). The call is at the end of `onBed`, shown earlier.

```java
    /** Addon boss bar: red, full, removed again after 5 seconds (NewBedwars also removes it when the player leaves). */
    private void showBedBar(Player player) {
        AddonBossBar bar = NewBedwarsAPI.get().getBossBars()
                .show(this, player, "&cYour bed is gone!", BarColor.RED, BarStyle.SOLID, 1.0);
        Bukkit.getScheduler().runTaskLater(this, bar::remove, 5 * 20L);
    }
```

### Scoreboard and tab list

Both events are fired just before a changed sidebar or tab list is sent, and only when something changed. The scoreboard listener appends a line when there are fewer than 15; the tab list listener adds to the footer. See [Scoreboards](display.md#scoreboards) and [Tab list](display.md#tab-list).

```java
    /** Appends one line to the sidebar when there is room. */
    @EventHandler
    public void onScoreboard(ScoreboardUpdateEvent e) {
        List<String> lines = e.getLines();
        if (lines.size() >= ScoreboardUpdateEvent.MAX_LINES) return;
        lines.add(ChatColor.GRAY + "Logged: " + loggedEvents.get());
        e.setLines(lines);
    }

    /** Adds a line to the footer of the tab list. */
    @EventHandler
    public void onTablist(TablistUpdateEvent e) {
        String footer = e.getFooter();
        String line = ChatColor.GRAY + "Events logged: " + loggedEvents.get();
        e.setFooter(footer.isEmpty() ? line : footer + "\n" + line);
    }
```

!!! note
    The values only change when NewBedwars sends a changed sidebar or tab list. The counter alone does not cause a new send, so it can lag behind until something else changes. Call `ScoreboardService#refresh` or `TablistService#refresh` if you need it to update at once.

### Reading game modes

`api.getModes()` returns the [game modes](shop-and-modes.md#game-modes) of `modes.yml`. The methods return a `CompletableFuture` that completes on the server thread, so the addon sends its messages directly from the callback. `/nbwlog modes` lists the modes:

```java
        if (args.length > 0 && args[0].equalsIgnoreCase("modes")) {
            api.getModes().getModes().thenAccept(modes -> modes.forEach(m -> sender.sendMessage(
                    " mode " + m.id() + " maxInTeam=" + m.maxInTeam() + " phases=" + m.gamePhases() + " builtIn=" + m.builtIn())));
            return true;
        }
```

## Cleaning up

NewBedwars removes everything registered with `owner` when your plugin is disabled, or when you call `api.unregisterAll(plugin)`: the phase action, shop logic, armor tier, upgrades and traps, subcommands, placeholders and boss bars the addon showed. The addon therefore has no `onDisable`.

What the addon has to do itself:

- Bukkit listeners are removed by Bukkit when the plugin is disabled, so nothing is needed there. Repeating tasks and anything you hold on to (threads, files, connections) are yours to stop.
- Extra data stays in the arena file on purpose. Remove your keys yourself if an addon should leave no trace.
- Keep no references to `ArenaView`, `TeamView` or `Player` objects after the game ended.

## Testing checklist

Start a test server with NewBedwars and the addon, make yourself op (or give yourself `nbwlogger.use`) and add the config lines from the sections above.

1. Start the server: the console shows the API version and `Registered phase action, ...`.
2. Run `/nbwlog`: arenas, teams and your stats are printed. `/nbwlog chat` toggles chat output.
3. Join an arena, leave, join again: `Join` and `Leave` lines appear (try the command and quitting the game).
4. Wait for the start: `State`, `TeamsAssigned` and `GameStart` lines appear; `games` goes up in `/nbwlog`.
5. With `NBWLOG:hello` in a phase, check the chat message when the phase runs.
6. Buy items, armor and upgrades: `Shop`, `ShopItem`, `ArmorEquip` and `Upgrade` lines. Test `NBWLIGHTNING`, `NBWTEST` armor and `NBWSPEED` (also after dying).
7. Let an enemy enter a base with `NBWBLIND`: Blindness and a `Trap` line.
8. Break a bed: `BedBreak`, and the red boss bar for the team for five seconds.
9. Kill players (also by void and mob): `Kill` lines. Spectate after the final death.
10. Check the sidebar line `Logged: N` and the tab list footer `Events logged: N`.
11. Run `/bw nbwinfo` in a game, in the lobby as admin and `bw nbwinfo <arena>` from the console.
12. Finish the game: `GameEnd` and `ArenaReset` PRE and POST; `wins` goes up for the winner. Use `/lobby` to see `SendToLobby`.
13. Stand in a WAITING arena and run `/nbwlog edit`; then run it during a game, where it must refuse.
14. Run `/nbwlog modes`.
15. Restart: the `games` and `wins` values must still be there.
16. Disable the addon (`/plugman unload NewBedwarsEventLogger`): the action, item logic, upgrades, command, placeholder and bars disappear without errors, and registering works again after loading it.

The README of the example has the same list as a table of features and how to test each.
