# NewBedwars Addon API

Everything in the `com.l299l.newbedwars.api` package (and `api.events`) is the stable contract for addons. All other packages are internal and can change in any update.

- [Setup](#setup)
- [Reading data](#reading-data)
- [Events](#events)
- [Registering your own content](#registering-your-own-content)
- [Extra data](#extra-data)
- [Editing arenas](#editing-arenas)
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

**`ArenaView`**: `getName`, `getState` (`ArenaState`), `isEnabled`, `getWorld`, `getMinPlayers`, `getMaxInTeam`, `getPlayers`, `getSpectators`, `getTeams`, `getTeam(name)`, `getGamePlayer(UUID)`, `getCurrentPhase`, `getGameTime` (seconds), `getGenerators`, `getExtra(Plugin)`.

**`TeamView`**: `getName`, `getColor`, `getPlayers`, `isAlive`, `isBedDestroyed`, `getSpawn` (may be null), `getUpgradeLevel(String upgrade)`, `getExtra(Plugin)`.

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
| `PlayerSendToLobbyEvent` | yes | whenever a player is sent to the lobby | `getPlayer()`, `getReason()`, `getTarget()`/`setTarget()` |

`PlayerSendToLobbyEvent` is the hook for networks: cancel it and send the player to a lobby server yourself. Reasons: `LEFT_ARENA`, `LOBBY_COMMAND`, `JOINED_IN_ARENA_WORLD`, `MAP_RESET`, `ARENA_UNLOADED`, `ARENA_DELETED`, `SETUP_ENDED`, `SERVER_SHUTDOWN`, `RELOAD`.

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

Adds `/bw <name>`, listed in `/bw help`. `CommandContext` says where it is available: `LOBBY`, `LOBBY_ADMIN`, `GAME`, `GAME_ADMIN`, `SETUP_BASIC`, `SETUP_NORMAL`, `SETUP_ADVANCED`, `SETUP_BUILDING`.

Implement `AddonCommand` (`getName`, `getDescription`, `getSyntax`, `getExample`, `perform(player, args, arena)`, optional `tabComplete`) and register it:

```java
api.registerCommand(this, new MyCommand(), CommandContext.GAME);
```

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

## Rules and good practice

- Call the API from the main thread.
- Events fire on the main thread; keep handlers fast.
- Do not teleport players to the lobby yourself: NewBedwars does it through `PlayerSendToLobbyEvent`, so network setups keep working.
- Do not hold on to worlds from `ArenaView#getWorld()`; fetch it again after `ArenaResetEvent` stage `POST`.
- Registrations are tied to your plugin; no cleanup code is needed on disable.

## Full example

See the `NewBedwarsEventLogger` test addon (a minimal plugin that logs every event).
