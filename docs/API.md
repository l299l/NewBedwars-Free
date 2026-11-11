# NewBedwars Addon API

Everything in the `com.l299l.newbedwars.api` package and its subpackages (`view`, `edit`, `command`, `registry`, `events`) is the stable contract for addons. All other packages are internal and can change in any update.

> [!IMPORTANT]
> **New in 2.7-beta** (not released yet):
> - [Arena maintenance](#arena-maintenance): `NewBedwarsAPI.getMaintenance()` enables, disables, backs up, restores, exports, imports, copies, resets and deletes arenas like `/bw arena`.
> - `ArenaView.getLoadProblems()`: why an arena was loaded disabled.
> - [Statistics and leaderboards](#statistics-and-leaderboards): `getLeaderboard`, `loadStats` (offline players too), `findPlayers`.
> - [Game control](#game-control): `NewBedwarsAPI.getGameControl()` starts, stops, skips phases and broadcasts like `/bw game`; `ArenaView.getNextPhase()` / `getPhaseTimeLeft()`.
> - [Shop editor](#shop-editor): `NewBedwarsAPI.getShop()` reads and changes shop items, their texts and shop GUIs; `ArenaView.getShopGui()` / `getUpgradeGui()`, `ArenaEditor.setShopGui()` / `setUpgradeGui()`.
> - New [events](#events): `GamePhaseChangeEvent`, `ArenaChatEvent`, `BedsDestroyedEvent`, `TeamEliminatedEvent`, `GeneratorUpgradeEvent`.
> - [Subcommands](#subcommands) in several contexts at once (`registerCommand(owner, command, LOBBY, GAME, CONSOLE)`), and the new `CONSOLE` context with `AddonCommand.performConsole` / `tabCompleteConsole`.
>
> Depend on `v2.7-beta` (or newer) to use them; on older NewBedwars versions these methods do not exist.

- [Setup](#setup)
- [Reading data](#reading-data)
- [Events](#events)
- [Registering your own content](#registering-your-own-content)
- [Extra data](#extra-data)
- [Editing arenas](#editing-arenas)
- [Arena maintenance](#arena-maintenance)
- [Rules and good practice](#rules-and-good-practice)
- [Full example](#full-example)

## Setup

Add the dependency (JitPack, see the README for the repository block) with `provided` scope and depend on the plugin in `plugin.yml`:

```yaml
name: MyAddon
main: com.example.MyAddon
version: 1.0
api-version: "1.17"
depend: [NewBedwars]
```

Get the API in `onEnable` (it throws `IllegalStateException` when NewBedwars is not enabled). It is also registered in Bukkit's `ServicesManager`:

```java
NewBedwarsAPI api = NewBedwarsAPI.get();
// or: Bukkit.getServicesManager().load(NewBedwarsAPI.class)
```

## Reading data

`NewBedwarsAPI` hands out read-only views. Views stay valid while the arena exists, but **never keep `ArenaView#getWorld()`**: the world is replaced after every map reset.

| Method | Returns |
|---|---|
| `getVersion()` | plugin version |
| `getArenas()` | all arenas |
| `getArena(String name)` / `getArena(World)` / `getArena(Player)` | `Optional<ArenaView>` (the player variant also finds spectators) |
| `getGroups()` / `getGroup(String name)` **(2.6-beta)** | arena groups (maps voted between), `Optional<ArenaGroupView>` |
| `getNetworkMode()` **(2.6-beta)** | `NONE`, `GAME` or `LOBBY` (`Network.Mode`) |
| `getNetworkArenas()` **(2.6-beta)** | on `LOBBY` and `GAME` servers: the arenas of all game servers (`NetworkArenaView`: server, name, group, state, players, `isJoinable()`, `getFreeSlots()`), empty otherwise |
| `getServerId()` **(2.6-beta)** | this server's name in the proxy, empty when not in network mode |
| `getNetworkRejoin(Player)` **(2.6-beta)** | on a `LOBBY` server: the network arena the player can still rejoin |
| `getStats(String name)` **(2.6-beta)** | `PlayerStatsView` (wins, losses, kills, deaths, final kills, beds, games; zeros when unknown). With MySQL storage these are network-wide; only online/loaded players are known |

**`ArenaView`**: `getName`, `getState` (`ArenaState`), `isEnabled`, `getWorld`, `getMinPlayers`, `getMaxInTeam`, `getPlayers`, `getSpectators`, `getTeams`, `getTeam(name)`, `getGamePlayer(UUID)`, `getCurrentPhase`, `getGameTime` (seconds), **`getNextPhase` / `getPhaseTimeLeft` (NEW in 2.7-beta)** (next phase name or null; seconds left in the current phase, -1 when none), `getGenerators`, `getExtra(Plugin)`, **`getLoadProblems` (NEW in 2.7-beta)** (why the arena was loaded disabled; empty when it loaded fine).

**`TeamView`**: `getName`, `getColor`, `getPlayers`, `isAlive`, `isBedDestroyed`, `getSpawn` (may be null), `getUpgradeLevel(String upgrade)`, `getExtra(Plugin)`.

**`ArenaGroupView`** **(2.6-beta)**: `getName`, `getArenaNames` (as configured, may include missing arenas), `getArenas` (existing ones), `getDisplayItem`, `getProblems` (missing arenas, different team sizes).

**`GamePlayerView`**: `getPlayer`, `getTeam` (empty while the arena is waiting; teams are assigned at game start), `getKills`, `getFinalKills`, `getDeaths`, `getBedsBroken`.

**`GeneratorView`**: `getKind` (`GeneratorKind`: `BASIC`, `DIAMOND`, `EMERALD`), `getLocation`.

## Events

Normal Bukkit events: register a `Listener` as usual. All arena events extend `ArenaEvent#getArena()`.

| Event | Cancellable | Fired | Useful getters |
|---|---|---|---|
| `GameStartEvent` | no | game starts, teams assigned | |
| `GameEndEvent` | no | game ends, before the winner announcement | `getWinner()` (null = draw) |
| `ArenaResetEvent` | no | before (`PRE`) and after (`POST`) a map reset | `getStage()` |
| `PlayerJoinArenaEvent` | yes | before a player joins or rejoins | `getPlayer()`, `isRejoin()` |
| `PlayerLeaveArenaEvent` | no | a player or spectator leaves | `getPlayer()`, `getReason()` (`LeaveReason`: `COMMAND`, `DISCONNECT`, `KICKED`, `OTHER`) |
| `BedBreakEvent` | yes | before an enemy bed breaks | `getTeam()`, `getBreaker()` |
| `PlayerKillEvent` | no | a player dies in a running game | `getVictim()`, `getKiller()` (nullable), `isFinalKill()`, `getCause()` |
| `ShopPurchaseEvent` | yes | before a purchase is charged | `getPlayer()`, `getItemName()`, `getKind()` (`ITEM`, `ARMOR`, `TOOL`, `UPGRADE`), `getPriceType()`, `getPrice()` |
| `TeamUpgradeEvent` | yes | before an upgrade or trap is bought | `getTeam()`, `getUpgrade()`, `getNewLevel()`, `getBuyer()` |
| `ArmorEquipEvent` | no | before armor is applied (respawn, purchase) | `getPlayer()`, `setArmor(ItemStack[])` (boots, leggings, chestplate, helmet) |
| `MapVoteEvent` **(2.6-beta)** | yes | during the after-game time, a player of the ended arena votes for the next map | `getPlayer()`, `getVotedArena()` (null when the arena is on another game server), `getVotedNetworkArena()` |
| `MapChosenEvent` **(2.6-beta)** | no | the after-game time is over and a next map won; the arena then stops and everyone joins the chosen arena | `getGroup()`, `getChosenArena()` (null when on another game server), `getChosenNetworkArena()` |
| `GamePhaseChangeEvent` **(2.7-beta)** | no | the game moves to the next phase (also the first phase at game start) | `getPrevious()` (null at the first phase), `getNext()`, `isForced()` (admin skipped) |
| `ArenaChatEvent` **(2.7-beta)** | no | after a player's arena or team chat message was delivered | `getPlayer()`, `getMessage()` (without colours), `isTeamChat()`, `getTeam()` (nullable) |
| `BedsDestroyedEvent` **(2.7-beta)** | no | the bed destruction phase destroyed the remaining beds | `getTeams()` |
| `TeamEliminatedEvent` **(2.7-beta)** | no | a team is out (no bed, no living players) | `getTeam()` |
| `GeneratorUpgradeEvent` **(2.7-beta)** | no | a game phase upgraded the diamond or emerald generators | `getKind()`, `getLevel()` |
| `PlayerSendToLobbyEvent` | yes | whenever a player is sent to the lobby | `getPlayer()`, `getReason()`, `getTarget()`/`setTarget()` |
| `NetworkJoinRequestEvent` **(2.6-beta)** | yes | on a `LOBBY` server, before a join or rejoin request goes to the proxy (`/bw join`, NPCs, selector, `/bw rejoin`, API); cancelled = nothing is sent and NewBedwars sends no message | `getPlayer()`, `getKind()` (`ANY`, `GROUP`, `ARENA`, `REJOIN`, `SPECTATE`), `getTarget()` |
| `NetworkArenasUpdateEvent` **(2.6-beta)** | no | on a `LOBBY` server, when the network arena list changed (not an `ArenaEvent`) | `getArenas()` |

`PlayerSendToLobbyEvent` is the hook for custom networks: cancel it and send the player to a lobby server yourself (the built-in network mode already does this on `GAME` servers). Reasons: `LEFT_ARENA`, `LOBBY_COMMAND`, `JOINED_IN_ARENA_WORLD`, `MAP_RESET`, `ARENA_UNLOADED`, `ARENA_DELETED`, `SETUP_ENDED`, `SERVER_SHUTDOWN`, `RELOAD`.

### Network actions (LOBBY servers) **(2.6-beta)**

```java
NewBedwarsAPI api = NewBedwarsAPI.get();
if (api.getNetworkMode() == NetworkMode.LOBBY) {
    // Same checks and messages as /bw join: permission, only the party admin, ...
    api.requestNetworkJoin(player, "solo");      // group, arena name, or null for any arena
    api.getNetworkRejoin(player).ifPresent(a -> api.requestNetworkRejoin(player));
}
```

Both return `false` when this is not a lobby server, a check failed, a `NetworkJoinRequestEvent` listener cancelled it, or the proxy can not be reached.

## Registering your own content

All `register*` methods take your plugin as `owner`. A name that already exists throws `IllegalArgumentException`. Everything you register is **removed automatically when your plugin is disabled** (or call `api.unregisterAll(plugin)`).

### Phase actions

Usable in `GamePhases` in `config.yml` as `NAME` or `NAME:argument` (everything after the first colon is the argument).

```java
api.registerPhaseAction(this, "ANNOUNCE", (arena, arg) ->
        arena.getPlayers().forEach(p -> p.sendMessage(arg)));
// config.yml: - "ANNOUNCE:Diamonds are now level 2!"
```

### Shop item logic

What a special item does on right-click. Use the key as `itemLogic` in `items.yml`. Return `true` to consume one item.

```java
api.registerShopLogic(this, "LIGHTNING", (player, arena) -> {
    player.getWorld().strikeLightning(player.getTargetBlock(null, 40).getLocation());
    return true;
});
```

### Armor tiers

A shop item with `onBuyEvent: GIVE_ARMOR` and a piece of the tier gives the buyer the whole tier; higher rank replaces lower. Built-in ranks: leather 0, gold 1, chainmail 2, iron 3, diamond 4, netherite 5.

```java
api.registerArmorTier(this, new ArmorTierDefinition("COPPER", 1,
        Material.COPPER_INGOT /* boots, leggings, chestplate, helmet materials */, ...));
```

### Upgrades and traps

Use in the upgrade GUI files as `upg:<name>`; prices can be overridden in `config.yml` under `UpgradePrices.<NAME>`. Read the level with `TeamView#getUpgradeLevel`.

```java
api.registerUpgrade(this, UpgradeDefinition.upgrade("SPEED", 2, Material.SUGAR, List.of(4, 8),
        (player, team, level) -> player.addPotionEffect(
                new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, level - 1)))
        .reapplyOnRespawn());

api.registerUpgrade(this, UpgradeDefinition.trap("BLIND", 1, Material.INK_SAC, List.of(2),
        (enemy, team, arena) -> enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0))));
```

### Subcommands

Adds `/bw <name>`, listed in `/bw help`. `CommandContext` says where it is available: `LOBBY`, `LOBBY_ADMIN`, `GAME`, `GAME_ADMIN`, `SETUP_BASIC`, `SETUP_NORMAL`, `SETUP_ADVANCED`, `SETUP_AUTOMATIC` (`SETUP_BUILDING` is a deprecated alias for it) and `CONSOLE` (2.7-beta).

Implement `AddonCommand` (`getName`, `getDescription`, `getSyntax`, `getExample`, `perform(player, args, arena)`, optional `tabComplete`) and register it:

```java
api.registerCommand(this, new MyCommand(), CommandContext.GAME);
```

**Several contexts (2.7-beta).** Register the same command for every place it should work. `arena` in `perform` is the player's arena, or `null` outside arenas:

```java
api.registerCommand(this, new TrimCommand(), CommandContext.LOBBY, CommandContext.GAME, CommandContext.CONSOLE);
```

`CONSOLE` makes `/bw <name>` work in the server console (and command blocks). It calls `performConsole(sender, args)` and `tabCompleteConsole(sender, args)` instead of the player methods. Both do nothing by default, so override them when you register for `CONSOLE`. Typing `bw` alone in the console lists the console commands.

Names: two different commands may not share a name in the same place. Lobby and lobby admin count as one place, so do game and game admin, all setup modes together, and the console. The same name is allowed in places that never apply at the same time (e.g. one addon's `trim` in `LOBBY` and another's in `GAME`). A clash throws `IllegalArgumentException`.

## Extra data

Store your own settings inside an arena or team. They are saved in the arena file and never touched by NewBedwars. Each addon has its own namespace (its plugin name).

```java
ExtraData data = arena.getExtra(this);
data.set("difficulty", 3);
int d = data.getInt("difficulty", 1);
```

Getters take a default; setters exist for `String`, `int`, `double`, `boolean`; also `has`, `remove`, `keys`. Changes are written to disk shortly after (no manual save).

## Editing arenas

`api.edit(arena)` returns an `ArenaEditor`. It only works while the arena is `WAITING` (otherwise `IllegalStateException`); invalid input throws `IllegalArgumentException`. Locations must be in the arena world. Changes are saved shortly after, or call `save()`.

```java
ArenaEditor editor = api.edit(arena);
editor.setMinPlayers(2);
TeamEditor red = editor.createTeam("Red", ChatColor.RED);
red.setSpawn(spawnLoc);
red.setBed(bedLoc);                          // a bed must be placed there
red.setShop(shopLoc, EntityType.VILLAGER);
red.setUpgrades(upgLoc, EntityType.VILLAGER);
red.setGenerator(genLoc);
editor.addGenerator(GeneratorKind.DIAMOND, diamondLoc);
editor.removeGenerator(0);                   // index in ArenaView#getGenerators()
editor.removeTeam("Blue");
```

`ArenaEditor`: `setMinPlayers`, `setMaxInTeam`, `setWaitingSpawn`, `createTeam`, `getTeam`, `removeTeam`, `addGenerator` (`DIAMOND`/`EMERALD`), `removeGenerator`, `save`.
`TeamEditor`: `setColor`, `setSpawn`, `setBed`/`clearBed`, `setShop`/`clearShop`, `setUpgrades`/`clearUpgrades`, `setGenerator`/`clearGenerator`.

## Arena maintenance

> [!IMPORTANT]
> **New in 2.7-beta.** Needs NewBedwars 2.7-beta or newer; check `api.getVersion()` if your addon also supports older versions.

`api.getMaintenance()` returns `ArenaMaintenance`, the arena actions of `/bw arena` for addons such as a web panel. The same checks as the commands apply (e.g. nothing is changed while players are in the arena).

| Method | Does |
|---|---|
| `enable(arena)` / `disable(arena)` | like `/bw arena enable/disable` (disable sends waiting players out) |
| `backup(arena)` | saves the arena file and map; `detail()` is the backup id |
| `listBackups(arena)` | `List<ArenaBackup>` (`id`, `hasArenaFile`, `hasMap`), newest first |
| `restore(arena, backupId)` | restores file and map; `backupId` null = newest |
| `export(arena)` | zip with arena file + map in `getExportFolder()`; `detail()` is its path |
| `importArena(fileName)` | loads a zip from `getExportFolder()`; the new arena is disabled |
| `copy(arena, newName)` | copy with settings and map, joins the same group |
| `resetMap(arena)` | puts the map back to its saved state (completes when the reset started) |
| `delete(arena)` | deletes the arena file, world and saved map (backups are kept) |
| `getExportFolder()` | `plugins/NewBedwars/data/exports` |

Every action returns a `CompletableFuture<MaintenanceResult>`. The methods can be called from any thread: the work runs on the server thread (file copying in the background) and the future completes on the server thread. A refused action is a normal result, not an exception:

```java
api.getMaintenance().backup("castle").thenAccept(result -> {
    if (result.isSuccess()) getLogger().info("Backup " + result.detail());
    else getLogger().warning(result.code() + ": " + result.message());
});
```

`MaintenanceResult`: `code()` (`OK`, `NOT_FOUND`, `BUSY`, `ALREADY_DONE`, `NOT_READY`, `INVALID_NAME`, `NAME_TAKEN`, `UNAVAILABLE` on a `LOBBY` server, `FAILED`, `PARTIAL` = deleted but some files remain), `isSuccess()`, `arena()`, `message()` (the command's message in the server's default language, without colors) and `detail()` (backup id, export file, copy source or error text; may be null). `listBackups` completes exceptionally with `NoSuchElementException` for an unknown arena.

## Statistics and leaderboards

> [!IMPORTANT]
> **New in 2.7-beta.**

| Method | Returns |
|---|---|
| `getLeaderboard(StatType, limit)` | `CompletableFuture<List<LeaderboardEntry>>` (`getName()`, `getStats()`), best first, ties by name; `limit` 1 to 100 |
| `loadStats(name)` | `CompletableFuture<Optional<PlayerStatsView>>`, also for offline players; empty when the player never played |
| `findPlayers(prefix, limit)` | `CompletableFuture<List<String>>` names starting with `prefix` (case-insensitive) |

`StatType`: `WINS`, `LOSSES`, `KILLS`, `DEATHS`, `FINAL_KILLS`, `BEDS_BROKEN`, `GAMES_PLAYED` (`stat.of(view)` reads the value). With JSON storage the data comes from memory (the future completes on the server thread); with MySQL storage the database is asked on its own thread, so the results cover the whole network. Call them from any thread and never block the server thread on the future.

```java
api.getLeaderboard(StatType.WINS, 10).thenAccept(top ->
        top.forEach(e -> getLogger().info(e.getName() + ": " + e.getStats().getWins())));
```

## Game control

> [!IMPORTANT]
> **New in 2.7-beta.**

`api.getGameControl()` returns `GameControl`, the actions of `/bw game` without a player in the arena. `actor` is the name players see in the messages (e.g. `"Panel: Alex"`).

| Method | Does | Refused (`NOT_READY`) when |
|---|---|---|
| `start(arena, actor)` | starts the countdown | the arena is not waiting |
| `forceStart(arena, actor)` | starts the game now | the arena is not waiting or starting |
| `stop(arena, actor)` | ends the game as a draw (stats saved) | no game runs |
| `forceStop(arena, actor)` | stops and resets the arena at once | never |
| `nextPhase(arena, actor)` | skips to the next game phase | no game runs |
| `broadcast(arena, actor, message)` | sends `message` (`&` colours, 1 to 256 characters, `INVALID_NAME` otherwise) to all players and spectators of the arena | |

All return `CompletableFuture<MaintenanceResult>` like [arena maintenance](#arena-maintenance) (`NOT_FOUND` for an unknown arena), can be called from any thread and run on the server thread.

## Shop editor

> [!IMPORTANT]
> **New in 2.7-beta.**

`api.getShop()` returns `ShopEditor`, the data behind the web panel's shop editor. Every method can be called from any thread and returns a `CompletableFuture`; the work runs on the server thread. Changes apply at once (players see them the next time they open a shop) and the previous file is kept in `data/backups/shop/`.

| Method | Does |
|---|---|
| `getItems()` / `getItem(name)` | `ShopItem`s of `items.yml` (items that could not be loaded on this server are left out) |
| `saveItem(item)` | creates the item when the name is new, changes it otherwise |
| `deleteItem(name)` | refused with `IN_USE` while a GUI or `config.yml` uses it |
| `getItemTexts(name)` / `saveItemTexts(name, texts)` | `ItemTexts` (name, shop icon description, lore) per language code |
| `getGuis()` / `getGui(id)` | `ShopGui`s with slots, categories, unresolved references (`problems`) and `usedBy` |
| `createGui(gui)` / `saveGui(gui)` / `deleteGui(id)` | `deleteGui` is refused while an arena or `config.yml` uses it |
| `getArenaShops()` / `setArenaShops(arena, shopGui, upgradeGui)` | which GUIs each arena opens; only while the arena is waiting (`BUSY` otherwise) |
| `getOptions()` | materials, enchantments, flags, logic keys, upgrades, price types, buy actions and languages of this server |

Changes return a `ShopResult` (`OK`, `NOT_FOUND`, `INVALID` with the field in `detail`, `IN_USE` with the places in `detail`, `BUSY`, `NAME_TAKEN`, `FAILED`).

```java
ShopEditor shop = api.getShop();
shop.getItem("Wool").thenAccept(item -> item.ifPresent(wool ->
        shop.saveItem(new ShopItem(wool.name(), wool.material(), 32, wool.price(), wool.priceType(), wool.onBuy(),
                wool.logic(), wool.permanent(), wool.permission(), wool.enchantments(), wool.flags(), List.of()))));
```

## Rules and good practice

- Call the API from the main thread (`getMaintenance()` methods may be called from any thread).
- Events fire on the main thread; keep handlers fast.
- Do not teleport players to the lobby yourself: NewBedwars does it through `PlayerSendToLobbyEvent`, so network setups keep working.
- Do not hold on to worlds from `ArenaView#getWorld()`; fetch it again after `ArenaResetEvent` stage `POST`.
- Registrations are tied to your plugin; no cleanup code is needed on disable.

## Full example

[`docs/examples/event-logger`](examples/event-logger) is a complete, buildable addon (`pom.xml`, `plugin.yml`, [`EventLoggerPlugin.java`](examples/event-logger/src/main/java/com/example/nbwlogger/EventLoggerPlugin.java)). It logs every API event and registers a phase action, shop logic, armor tier, upgrade, trap and subcommands, and uses extra data and the arena editor. Its README says how to try each one.
