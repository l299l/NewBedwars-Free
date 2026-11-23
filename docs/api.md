# NewBedwars Addon API

Everything in the `com.l299l.newbedwars.api` package and its subpackages (`view`, `edit`, `command`, `registry`, `events`, `game`, `maintenance`, `shop`) is the stable contract for addons. All other packages are internal and can change in any update.

> [!IMPORTANT]
> **New in 2.8-beta:**
>
> - [API version](#api-version): `NewBedwarsAPI.getApiVersion()`, and the [compatibility rules](#compatibility) for the stable releases.
> - [Shop items](#shop-items): `ShopItem` has `commands`, `openGui`, `givePermission` and `priceMaterial` for the new `COMMANDS`, `OPEN_GUI` and `GIVE_PERM` actions and `priceType` `OTHER` (the 12-argument constructor still works).
> - `ShopGui.closeOnTransaction` and `CommandContext.SETUP_BUILDING` are marked for removal in 3.0.0.
> - [Players](#players-join-spectate-leave): `join`, `spectate`, `leave` and `sendToLobby` with a `JoinResult`.
> - [Parties](#parties): `getParty(Player)` (`PartyView`) and `PartyChangeEvent`.
> - [Messages and languages](#messages-and-languages): `getLanguage`, `getMessage` and `registerMessages` for translated addon texts.
> - [Statistics](#statistics-and-leaderboards): `getStats(UUID)`, `loadStats(UUID)` and `addStat` to count stats for addon game modes.
> - [Game modes](#game-modes): `getModes()` (`ModeService`) lists the modes and phase lists of `modes.yml` and applies a mode to a waiting arena; `ArenaView.getGeneratorConfig()` and `getGamePhases()`.
> - [Game control](#game-control): `destroyBed`, `eliminateTeam` and `setUpgradeLevel`.
> - New [events](#events): `ArenaStateChangeEvent` (also the countdown start and cancel), `TeamsAssignedEvent`, `PlayerRespawnArenaEvent`, `PlayerSpectateEvent`, `TrapTriggerEvent`, `PartyChangeEvent`. `ShopPurchaseEvent` has `getTeam()` and `setPrice(int)`; `BedBreakEvent.getBreaker()` is null when the bed was destroyed through `destroyBed`.
> - Custom generators: `GeneratorKind.CUSTOM`, `GeneratorView.getCustomId()`, `ArenaView.getCustomGeneratorIds()` and `ArenaEditor.addCustomGenerator(id, location)`. Before, custom generators were reported as `BASIC`.
> - Team display names: `TeamEditor.setDisplayName` / `TeamView.getDisplayName()` change the name players see (e.g. `Lime` for a team `Red` that got a lime color) without renaming the team.
> - `ShopItemStackEvent`: change or replace the item stack of a shop item per player, both the menu icon and the item the player gets.
> - `ArenaView.getMaxPlayers()`, `getTeam(Player)` and `isSpectator(Player)`; `ShopLogic.perform(player, arena, itemName)` tells the logic which shop item was used.

- [Compatibility](#compatibility)
- [Setup](#setup)
- [Reading data](#reading-data)
- [Events](#events)
- [Registering your own content](#registering-your-own-content)
- [Extra data](#extra-data)
- [Editing arenas](#editing-arenas)
- [Arena maintenance](#arena-maintenance)
- [Statistics and leaderboards](#statistics-and-leaderboards)
- [Game control](#game-control)
- [Players: join, spectate, leave](#players-join-spectate-leave)
- [Parties](#parties)
- [Messages and languages](#messages-and-languages)
- [Shop editor](#shop-editor)
- [Game modes](#game-modes)
- [Rules and good practice](#rules-and-good-practice)
- [Full example](#full-example)

## Compatibility

From the first stable release (**3.0.0**) NewBedwars uses `MAJOR.MINOR.PATCH` version numbers, and the API follows these rules:

| Release | Example | What can change in the API |
|---|---|---|
| Patch | 3.0.0 → 3.0.1 | Nothing. Bug fixes only |
| Minor | 3.0 → 3.1 | Only additions: new methods, events, enum values, registries, record components (the old record constructor is kept). An addon built for 3.0 keeps working on every 3.x |
| Major | 3.x → 4.0 | Members that were deprecated may be removed |

- **Deprecation first.** Anything that will be removed is marked `@Deprecated(forRemoval = true)` (your compiler warns about it), its javadoc names the replacement, and it stays for at least one minor release. It is removed only in the next major version.
- **Interfaces you implement** (`AddonCommand`, `ShopLogic`, `PhaseActionHandler`, `UpgradeDefinition.Effect`) only get new methods with a default implementation. All other interfaces (`ArenaView`, `ShopEditor`, ...) are implemented by NewBedwars and can get new methods in any minor release, so don't implement them yourself.
- **Enums can get new values** in a minor release (for example a new `CommandContext` or `StatType`). Give every `switch` over an API enum a `default` branch.
- **Not covered:** everything outside `com.l299l.newbedwars.api`, and behaviour that a bug fix corrects.
- **Before 3.0.0** (the 2.x-beta versions) the API could still change between releases. Two deprecated members will be removed in 3.0.0: `ShopGui.closeOnTransaction` (always `false`) and `CommandContext.SETUP_BUILDING` (use `SETUP_AUTOMATIC`).

### API version

`api.getApiVersion()` returns a number that goes up by one in every release that adds something to the API. Check it to find out whether a feature exists on the server your addon runs on:

```java
if (api.getApiVersion() >= 1) {
    // 2.8-beta or newer: ShopItem.commands(), getApiVersion(), ...
}
```

| API version | NewBedwars |
|---|---|
| 1 | 2.8-beta |

`getApiVersion()` itself exists from 2.8-beta. To support older versions, catch `NoSuchMethodError` or compare `api.getVersion()`.

## Setup

Add the dependency from [JitPack](https://jitpack.io/#l299l/NewBedwars-Free) with `provided` scope:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.l299l.NewBedwars-Free</groupId>
        <artifactId>newbedwars-api</artifactId>
        <version>v2.8-beta</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Then depend on the plugin in `plugin.yml`:

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
| `getApiVersion()` **(2.8-beta)** | the [API version](#api-version), a number |
| `getArenas()` | all arenas |
| `getArena(String name)` / `getArena(World)` / `getArena(Player)` | `Optional<ArenaView>` (the player variant also finds spectators) |
| `getGroups()` / `getGroup(String name)` **(2.6-beta)** | arena groups (maps voted between), `Optional<ArenaGroupView>` |
| `getSetupArena(Player)` / `getSetupPlayers()` **(2.7-beta)** | the arena a player is setting up (in setup mode), `Optional<ArenaView>`; everyone in setup mode as `Map<Player, ArenaView>` |
| `getNetworkMode()` **(2.6-beta)** | `NONE`, `GAME` or `LOBBY` (`Network.Mode`) |
| `getNetworkArenas()` **(2.6-beta)** | on `LOBBY` and `GAME` servers: the arenas of all game servers (`NetworkArenaView`: server, name, group, state, players, `isJoinable()`, `getFreeSlots()`), empty otherwise |
| `getServerId()` **(2.6-beta)** | this server's name in the proxy, empty when not in network mode |
| `getNetworkRejoin(Player)` **(2.6-beta)** | on a `LOBBY` server: the network arena the player can still rejoin |
| `getStats(String name)` **(2.6-beta)** / `getStats(UUID)` **(2.8-beta)** | `PlayerStatsView` (wins, losses, kills, deaths, final kills, beds, games; zeros when unknown). With MySQL storage these are network-wide; only online/loaded players are known. The UUID variant still finds a player who changed their name |
| `getParty(Player)` **(2.8-beta)** | `Optional<PartyView>`, see [Parties](#parties) |
| `getLanguage(Player)` / `getMessage(Player, key)` **(2.8-beta)** | the player's language code and a translated message, see [Messages and languages](#messages-and-languages) |

**`ArenaView`**: `getName`, `getState` (`ArenaState`), `isEnabled`, `getWorld`, `getMinPlayers`, `getMaxInTeam`, `getPlayers`, `getSpectators`, `getTeams`, `getTeam(name)`, `getGamePlayer(UUID)`, `getCurrentPhase`, `getGameTime` (seconds), **`getNextPhase` / `getPhaseTimeLeft` (NEW in 2.7-beta)** (next phase name or null; seconds left in the current phase, -1 when none), `getGenerators`, `getExtra(Plugin)`, `getShopGui` / `getUpgradeGui` (ids of the GUIs the arena's shop and upgrade NPCs open), **`getLoadProblems` (NEW in 2.7-beta)** (why the arena was loaded disabled; empty when it loaded fine), **`getQuickVoidY` / `getSetupPlayers` (NEW in 2.7-beta)** (the quick void level, null while not set; online players who are setting the arena up), **`getMaxPlayers` / `getTeam(Player)` / `isSpectator(Player)` / `getCustomGeneratorIds` (NEW in 2.8-beta)** (teams times team size; the team of a player, empty before the teams are assigned; whether the player watches, is out or waits for a respawn; the ids from `CustomGenerators` in the arena's generator file).

**`TeamView`**: `getName` (never changes; identifies the team), **`getDisplayName` (2.8-beta)** (what players see: the display name or `getName`), `getColor`, `getPlayers`, `isAlive`, `isBedDestroyed`, `getSpawn` (may be null), **`getBed` (NEW in 2.7-beta, may be null)**, `getUpgradeLevel(String upgrade)`, `getExtra(Plugin)`.

**`ArenaGroupView`** **(2.6-beta)**: `getName`, `getArenaNames` (as configured, may include missing arenas), `getArenas` (existing ones), `getDisplayItem`, `getProblems` (missing arenas, different team sizes).

**`GamePlayerView`**: `getPlayer`, `getTeam` (empty while the arena is waiting; teams are assigned at game start), `getKills`, `getFinalKills`, `getDeaths`, `getBedsBroken`.

**`GeneratorView`**: `getKind` (`GeneratorKind`: `BASIC`, `DIAMOND`, `EMERALD`, **`CUSTOM` (2.8-beta)**), `getLocation`, **`getCustomId` (2.8-beta)** (the `CustomGenerators` id of a `CUSTOM` generator, null otherwise).

**`PartyView`** **(2.8-beta)**: `getLeader`, `getMembers` (UUIDs, online or not), `getOnlineMembers`, `contains(UUID)`, `size`.

## Events

Normal Bukkit events: register a `Listener` as usual. All arena events extend `ArenaEvent#getArena()`.

| Event | Cancellable | Fired | Useful getters |
|---|---|---|---|
| `GameStartEvent` | no | game starts, teams assigned | |
| `GameEndEvent` | no | game ends, before the winner announcement | `getWinner()` (null = draw) |
| `ArenaResetEvent` | no | before (`PRE`) and after (`POST`) a map reset | `getStage()` |
| `PlayerJoinArenaEvent` | yes | before a player joins or rejoins | `getPlayer()`, `isRejoin()` |
| `PlayerLeaveArenaEvent` | no | a player or spectator leaves | `getPlayer()`, `getReason()` (`LeaveReason`: `COMMAND`, `DISCONNECT`, `KICKED`, `OTHER`) |
| `BedBreakEvent` | yes | before an enemy bed breaks, or `GameControl#destroyBed` destroys it | `getTeam()`, `getBreaker()` (null for `destroyBed`) |
| `PlayerKillEvent` | no | a player dies in a running game | `getVictim()`, `getKiller()` (nullable), `isFinalKill()`, `getCause()` (`VOID` for every void death), `getDragonTeam()` (the team whose dragon killed the victim) |
| `ShopPurchaseEvent` | yes | before a purchase is charged (only when the player can pay the normal price) | `getPlayer()`, `getItemName()`, `getKind()` (`ITEM`, `ARMOR`, `TOOL`, `UPGRADE`), `getPriceType()` (the material for `priceType: OTHER`), `getPrice()`, **`setPrice(int)` (2.8-beta)** (0 = free, never negative), **`getTeam()` (2.8-beta)** |
| `TeamUpgradeEvent` | yes | before an upgrade or trap is bought | `getTeam()`, `getUpgrade()`, `getNewLevel()`, `getBuyer()` |
| `ArmorEquipEvent` | no | before armor is applied (respawn, purchase) | `getPlayer()`, `getArmor()`, `setArmor(ItemStack[])` (boots, leggings, chestplate, helmet) |
| `MapVoteEvent` **(2.6-beta)** | yes | during the after-game time, a player of the ended arena votes for the next map | `getPlayer()`, `getVotedArena()` (null when the arena is on another game server), `getVotedNetworkArena()` |
| `MapChosenEvent` **(2.6-beta)** | no | the after-game time is over and a next map won; the arena then stops and everyone joins the chosen arena | `getGroup()`, `getChosenArena()` (null when on another game server), `getChosenNetworkArena()` |
| `GamePhaseChangeEvent` **(2.7-beta)** | no | the game moves to the next phase (also the first phase at game start) | `getPrevious()` (null at the first phase), `getNext()`, `isForced()` (admin skipped) |
| `ArenaChatEvent` **(2.7-beta)** | no | after a player's arena or team chat message was delivered | `getPlayer()`, `getMessage()` (without colours), `isTeamChat()`, `getTeam()` (nullable) |
| `BedsDestroyedEvent` **(2.7-beta)** | no | the bed destruction phase destroyed the remaining beds | `getTeams()` |
| `TeamEliminatedEvent` **(2.7-beta)** | no | a team is out (no bed, no living players) | `getTeam()` |
| `GeneratorUpgradeEvent` **(2.7-beta)** | no | a game phase upgraded the diamond or emerald generators | `getKind()`, `getLevel()` |
| `ArenaStateChangeEvent` **(2.8-beta)** | no | after the arena state changed: `WAITING`→`STARTING` countdown started, `STARTING`→`WAITING` countdown cancelled, →`PLAYING` game started, `PLAYING`→`ENDING`, `ENDING`→`RESTARTING`→`WAITING` map reset; disabling = `RESTARTING`, enabling = `WAITING` | `getPrevious()`, `getNext()` |
| `TeamsAssignedEvent` **(2.8-beta)** | no | at the game start, after every player got a team and before the teleport to the team spawns and `GameStartEvent` | (read the teams from `getArena()`) |
| `PlayerRespawnArenaEvent` **(2.8-beta)** | no | a player respawned at their team spawn, with armor, items and upgrades given back | `getPlayer()`, `getTeam()` |
| `PlayerSpectateEvent` **(2.8-beta)** | yes | before a player who doesn't play starts spectating (`/bw spectate`, the menu, `api.spectate`); not for dead or eliminated players | `getPlayer()` |
| `TrapTriggerEvent` **(2.8-beta)** | yes | before one trap of a team goes off (one event per trap); cancelled = the trap is kept and does nothing | `getTeam()` (the owner), `getEnemy()`, `getTrap()` (upgrade name) |
| `ShopItemStackEvent` **(2.8-beta)** | no | NewBedwars built the stack of a shop item for one player: a menu icon (`ICON`, shop, upgrades, quick-buy editor) or the item the player gets (`GIVEN`, also permanent items after a respawn); not an `ArenaEvent` | `getPlayer()`, `getItemName()`, `getUse()`, `getItem()` / `setItem(ItemStack)`, `getArena()`, `getTeam()` (both `Optional`) |
| `PartyChangeEvent` **(2.8-beta)** | no | after a party on this server changed (not for changes synced from the proxy; not an `ArenaEvent`) | `getParty()`, `getPlayer()` (null for `DISBANDED`), `getChange()` (`CREATED`, `JOINED`, `LEFT`, `KICKED`, `LEADER_CHANGED`, `DISBANDED`) |
| `TeamPickEvent` **(2.8-beta)** | yes | before a player's pick in the waiting lobby is stored (team selector item or `/bw team`); cancelled = the old pick is kept | `getPlayer()`, `getTeam()` (`Optional<TeamView>`, empty = pick cleared / random), `getPreviousTeam()` (`Optional<TeamView>`), `getMovedPlayers()` (the whole party when the leader picks) |
| `PlayerSendToLobbyEvent` | yes | whenever a player is sent to the lobby | `getPlayer()`, `getReason()` (`ADDON` for `api.sendToLobby`, from 2.8-beta), `getTarget()`/`setTarget()` |
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

Usable in `GamePhases` in [`modes.yml`](game-modes.md#game-phases) as `NAME` or `NAME:argument` (everything after the first colon is the argument).

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

**(2.8-beta)** When one logic serves several items, override `perform(player, arena, itemName)` to know which `items.yml` entry was used (the lambda form above only implements the two-argument method):

```java
api.registerShopLogic(this, "SPELL", new ShopLogic() {
    @Override
    public boolean perform(Player player, ArenaView arena) {
        return false;
    }

    @Override
    public boolean perform(Player player, ArenaView arena, String itemName) {
        return castSpell(player, itemName);
    }
});
```

### Shop items

`registerShopItem` writes an item into `items.yml` when no item with that name exists yet, so server owners do not have to add it by hand. An existing item is never changed (the owner may have edited it) and stays when your addon is disabled. Register the item logic first, call it on the server thread (e.g. in `onEnable`), and note that the item still has to be put into a shop GUI (the panel or a GUI file) to be sold. It returns `true` when the item was added and throws `IllegalArgumentException` for invalid values (same rules as `ShopEditor#saveItem`). **(2.7-beta)**

```java
api.registerShopLogic(this, "EMERALD_GOLEM", (player, arena) -> { /* spawn it */ return true; });
api.registerShopItem(this,
        ShopItem.of("EmeraldGolemEgg", "SNOWBALL", 1, 24, "IRON", "EMERALD_GOLEM"),
        Map.of("en", new ItemTexts("&fEmerald Golem", "&7Spawns a emerald golem;&7for /price/", ""),
               "pl", new ItemTexts("&fSmaragdowy golem", "", "")));
```

`ShopItem.of(name, material, amount, price, priceType, logic)` covers the common case; use the full `ShopItem` constructor for permanent items, enchantments, flags or another `onBuy` action. The texts are optional (without them the item is named after its `name`). From 2.8-beta the full constructor also takes `commands`, `openGui`, `givePermission` and `priceMaterial` for the `COMMANDS`, `OPEN_GUI` and `GIVE_PERM` actions and `priceType` `OTHER` (see [Commands, menus and permissions](shop-and-upgrades.md#commands-menus-and-permissions)); the 12-argument constructor is kept and leaves them empty.

### Armor tiers

A shop item with `onBuyEvent: GIVE_ARMOR` and a piece of the tier gives the buyer the whole tier; higher rank replaces lower. Built-in ranks: leather 0, gold 1, chainmail 2, iron 3, diamond 4, netherite 5.

```java
api.registerArmorTier(this, new ArmorTierDefinition("COPPER", 1,
        Material.COPPER_INGOT /* boots, leggings, chestplate, helmet materials */, ...));
```

### Upgrades and traps

Use in the upgrade GUI files as `upg:<name>`; its prices are added to `data/upgrades.yml` on the first registration and are changed there (or in the panel) afterwards. Read the level with `TeamView#getUpgradeLevel`.

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
editor.addCustomGenerator("gapple", appleLoc); // an id from ArenaView#getCustomGeneratorIds() (2.8-beta)
editor.removeGenerator(0);                   // index in ArenaView#getGenerators()
editor.removeTeam("Blue");
```

`ArenaEditor`: `setMinPlayers`, `setMaxInTeam`, `setWaitingSpawn`, `createTeam`, `getTeam`, `removeTeam`, `addGenerator` (`DIAMOND`/`EMERALD`), `addCustomGenerator(id, location)` **(2.8-beta)** (`IllegalArgumentException` when the arena's generator file has no such id), `removeGenerator`, `setShopGui(guiId)` / `setUpgradeGui(guiId)` (the GUI of `data/guis/` the shop and upgrade NPCs of the arena open), `save`.
`TeamEditor`: `setColor`, `setDisplayName` **(2.8-beta)** (1 to 32 characters without colour codes or line breaks, `null` = show the name again; saved with the arena), `setSpawn`, `setBed`/`clearBed`, `setShop`/`clearShop`, `setUpgrades`/`clearUpgrades`, `setGenerator`/`clearGenerator`.

### Team colors and display names **(2.8-beta)**

A team keeps the name it was created with (`getName()`): the arena file, commands and upgrades use it. When your addon gives teams other colors, set a display name too, so players don't see a lime team called "Red". Chat, titles, the scoreboard, NPC names and `%newbedwars_team%` show the display name.

```java
TeamEditor red = api.edit(arena).getTeam("Red").orElseThrow();
red.setColor(ChatColor.GREEN);
red.setDisplayName("Lime");
```

### Changing shop items per player **(2.8-beta)**

`ShopItemStackEvent` lets you change what a shop item shows and gives, per player. It fires for the menu icon (`Use.ICON`) and for the item the player gets (`Use.GIVEN`), so both match. Keep the item meta when you replace the stack: it holds the name, the description and the key NewBedwars uses to recognise its items.

```java
@EventHandler
public void onShopItem(ShopItemStackEvent e) {
    if (!e.getItemName().equals("Wood")) return;
    if (e.getTeam().map(t -> t.getColor() == ChatColor.RED).orElse(false)) {
        ItemStack planks = new ItemStack(Material.MANGROVE_PLANKS, e.getItem().getAmount());
        planks.setItemMeta(e.getItem().getItemMeta());
        e.setItem(planks);
    }
}
```

Wool, glass, terracotta and the other team-colored blocks are already in the team color.

## Arena maintenance

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

| Method | Returns |
|---|---|
| `getLeaderboard(StatType, limit)` | `CompletableFuture<List<LeaderboardEntry>>` (`getName()`, `getStats()`), best first, ties by name; `limit` 1 to 100 |
| `loadStats(name)` | `CompletableFuture<Optional<PlayerStatsView>>`, also for offline players; empty when the player never played |
| `findPlayers(prefix, limit)` | `CompletableFuture<List<String>>` names starting with `prefix` (case-insensitive) |
| `loadStats(UUID)` **(2.8-beta)** | like `loadStats(name)`, by UUID |
| `addStat(UUID, name, StatType, amount)` **(2.8-beta)** | adds `amount` (1 or more) to one stat and saves it like the stats of a game: `playerStats.json`, or MySQL (network-wide, in the leaderboards). Stats can't be lowered. Server thread only |

`StatType`: `WINS`, `LOSSES`, `KILLS`, `DEATHS`, `FINAL_KILLS`, `BEDS_BROKEN`, `GAMES_PLAYED` (`stat.of(view)` reads the value). With JSON storage the data comes from memory (the future completes on the server thread); with MySQL storage the database is asked on its own thread, so the results cover the whole network. Call them from any thread and never block the server thread on the future.

```java
api.getLeaderboard(StatType.WINS, 10).thenAccept(top ->
        top.forEach(e -> getLogger().info(e.getName() + ": " + e.getStats().getWins())));

// an addon game mode counts its own wins (2.8-beta)
api.addStat(winner.getUniqueId(), winner.getName(), StatType.WINS, 1);
```

`addStat` changes only the stat you pass; a win doesn't add a game played, so add `GAMES_PLAYED` yourself when it should count as a game.

## Game control

`api.getGameControl()` returns `GameControl`, the actions of `/bw game` without a player in the arena. `actor` is the name players see in the messages (e.g. `"Panel: Alex"`).

| Method | Does | Refused (`NOT_READY`) when |
|---|---|---|
| `start(arena, actor)` | starts the countdown | the arena is not waiting |
| `forceStart(arena, actor)` | starts the game now | the arena is not waiting or starting |
| `stop(arena, actor)` | ends the game as a draw (stats saved) | no game runs |
| `forceStop(arena, actor)` | stops and resets the arena at once | never |
| `nextPhase(arena, actor)` | skips to the next game phase | no game runs |
| `broadcast(arena, actor, message)` | sends `message` (`&` colours, 1 to 256 characters, `INVALID_NAME` otherwise) to all players and spectators of the arena | |
| `destroyBed(arena, team, actor)` **(2.8-beta)** | destroys the team's bed as if an enemy broke it (`BedBreakEvent` with a null breaker, can be cancelled: `FAILED`); `ALREADY_DONE` when the bed is gone | no game runs |
| `eliminateTeam(arena, team, actor)` **(2.8-beta)** | destroys the bed, makes the team's players spectators and runs the win check (the game ends when one team is left); `ALREADY_DONE` when the team is out | no game runs |
| `setUpgradeLevel(arena, team, upgrade, level, actor)` **(2.8-beta)** | sets a team upgrade or the number of a trap for free; raising applies each level like a purchase, only traps can be lowered (`INVALID_NAME` otherwise and for a level above the upgrade's maximum, `NOT_FOUND` for an unknown upgrade). `TeamUpgradeEvent` is not fired | no game runs |

Team names are matched without case; an unknown team is `NOT_FOUND`. All return `CompletableFuture<MaintenanceResult>` like [arena maintenance](#arena-maintenance) (`NOT_FOUND` for an unknown arena), can be called from any thread and run on the server thread.

## Players: join, spectate, leave

> [!IMPORTANT]
> **New in 2.8-beta.**

The same paths as the commands, so parties, rejoining, events and messages work as usual. Call them on the server thread with an arena of this server (`IllegalArgumentException` otherwise; in network mode use [`requestNetworkJoin`](#network-actions-lobby-servers-26-beta) on a lobby server).

| Method | Does |
|---|---|
| `join(player, arena)` | like `/bw join <arena>`: a party leader brings the party, a player who left this arena's running game rejoins. The `newbedwars.bw.join` permission is **not** checked, so your addon decides who may join |
| `spectate(player, arena)` | like `/bw spectate <arena>`, without the `newbedwars.bw.spectate` permission |
| `leave(player)` | takes the player out of the arena they play, wait or spectate in and sends them to the lobby (during a game they can rejoin when rejoining is on); `false` when they are in no arena |
| `sendToLobby(player)` | `leave` for a player in an arena, otherwise a teleport to the lobby through `PlayerSendToLobbyEvent` (reason `ADDON`); `false` when there is no lobby or the teleport was cancelled |

`join` and `spectate` return a `JoinResult`, and the player also gets the usual chat message:

| `JoinResult` | Meaning |
|---|---|
| `JOINED`, `REJOINED`, `SPECTATING` | done (`isSuccess()` is `true`) |
| `NOT_PARTY_LEADER`, `PARTY_TOO_BIG`, `PARTIES_NOT_ALLOWED` | party rules |
| `ALREADY_IN_ARENA`, `NOT_ENABLED`, `FULL` | the player or the arena |
| `RUNNING`, `ENDING`, `RESTARTING` | the game state does not allow joining (spectate a `RUNNING` game instead) |
| `NOT_RUNNING`, `SPECTATORS_DISABLED` | spectating only |
| `CANCELLED` | an addon cancelled `PlayerJoinArenaEvent` or `PlayerSpectateEvent` |
| `FAILED` | anything else, e.g. the rejoin place is gone or the player plays in another arena |

```java
api.getArena("Castle").ifPresent(arena -> {
    JoinResult result = api.join(player, arena);
    if (result == JoinResult.RUNNING) api.spectate(player, arena);
});
```

## Parties

> [!IMPORTANT]
> **New in 2.8-beta.**

`api.getParty(player)` returns the player's `PartyView` (`getLeader`, `getMembers`, `getOnlineMembers`, `contains`, `size`). It is a live view: the getters always show the current members. In network mode the proxy keeps the parties and this is the copy it sends to the server.

`PartyChangeEvent` fires after a party on this server changed, with `getChange()`: `CREATED` (`getPlayer()` is the leader), `JOINED`, `LEFT`, `KICKED`, `LEADER_CHANGED` (`getPlayer()` is the new leader) and `DISBANDED` (`getPlayer()` is null, the party is empty). Changes that arrive from the proxy don't fire it.

```java
@EventHandler
public void onParty(PartyChangeEvent e) {
    if (e.getChange() == PartyChangeEvent.Change.JOINED) {
        e.getParty().getOnlineMembers().forEach(p -> p.sendMessage("A new member joined your party"));
    }
}
```

## Messages and languages

> [!IMPORTANT]
> **New in 2.8-beta.**

Players choose their language with `/lang`. Addons can use the same language for their own texts:

| Method | Returns |
|---|---|
| `getLanguage(player)` | `en`, `pl`, `de`, `es`, `fr` or `ru` |
| `getMessage(player, key)` | the message in the player's language: one your addon registered, or a NewBedwars message from the language files; missing translations fall back to English, unknown keys give `""`. `&` colours are translated |
| `getMessage(player, key, replacements)` | the same with placeholders replaced, e.g. `Map.of("/player/", name)` |
| `registerMessages(owner, language, messages)` | adds your messages for one language (calling it again replaces that language). Register `en` at least. Removed when your plugin is disabled |

```java
api.registerMessages(this, "en", Map.of("MyAddon-Welcome", "&aWelcome, /player/!"));
api.registerMessages(this, "pl", Map.of("MyAddon-Welcome", "&aWitaj, /player/!"));

player.sendMessage(api.getMessage(player, "MyAddon-Welcome", Map.of("/player/", player.getName())));
```

Addon messages win over NewBedwars messages with the same key, so give your keys a prefix.

## Shop editor

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
| `getUpgradePrices()` / `saveUpgradePrices(upgrade, tiers)` | `UpgradePrice`s of `data/upgrades.yml` (`name`, `tiers`, `maxLevel`, `builtIn`); a tier (`UpgradeTier`) is a `price` and a `currency` |
| `getOptions()` | materials, enchantments, flags, logic keys, upgrades, price types, buy actions and languages of this server |

`ShopGui.itemPrices()` **(2.8-beta)** is a map of shop item id to `ItemPrice` (`price`, `currency`, `amount`; the last two may be null = the item's own), the `ItemPrices` section of the file (see [Shop and upgrades](shop-and-upgrades.md)). When saving, `null` keeps the current prices and an empty map removes them. The older `ShopGui` constructors still work.

Changes return a `ShopResult` (`OK`, `NOT_FOUND`, `INVALID` with the field in `detail`, `IN_USE` with the places in `detail`, `BUSY`, `NAME_TAKEN`, `FAILED`).

> [!WARNING]
> **`ShopGui.closeOnTransaction` is deprecated and will be removed in the first stable release (3.0.0).** The `CloseOnTransaction` setting of GUI files never had an effect (shops always stay open after a purchase), so it was removed from GUI files and the web panel in 2.8-beta. The record component is still there so existing addons keep compiling, but it is always `false` and ignored when saving. Pass `false` and don't read it.

```java
ShopEditor shop = api.getShop();
shop.getItem("Wool").thenAccept(item -> item.ifPresent(wool ->
        shop.saveItem(new ShopItem(wool.name(), wool.material(), 32, wool.price(), wool.priceType(), wool.onBuy(),
                wool.logic(), wool.permanent(), wool.permission(), wool.enchantments(), wool.flags(), List.of()))));
```

## Game modes

> **New in 2.8-beta.**

`api.getModes()` returns `ModeService`, the data behind the web panel's Modes page and its **Apply mode** picker. Every method can be called from any thread and returns a `CompletableFuture`; the work runs on the server thread and the futures complete there.

| Method | Does |
|---|---|
| `getModes()` | `ModeInfo`s in the order of `modes.yml`, then built-in modes the file doesn't list: `id`, `generators`, `gamePhases`, `shopGui`, `upgradeGui` (null = the mode leaves it unchanged), `maxInTeam` (0 = unchanged), `teams` (a hint, never applied; 0 = not set) and `builtIn` |
| `getPhaseLists()` | `PhaseListInfo`s: the `id` and its `phases` (`PhaseInfo`: `name`, `duration` in seconds until the next phase, 0 for the last one, and `actions` as written in `modes.yml`, e.g. `DIAMOND_GENERATOR:2`) |
| `getArenaModes()` | `ArenaModeInfo` per arena, sorted by name: `arena`, `waiting` (only then can a mode be applied), `generators`, `gamePhases`, `shopGui`, `upgradeGui` (null when not set), `maxInTeam` and `teams` (team count) |
| `getGeneratorConfigs()` | ids of every loaded generator config, sorted |
| `applyMode(arena, mode)` | copies the mode into the arena once, like `/bw setMode` (generator config, phase list, GUIs and team size, each only when the mode sets it) and saves the arena. The mode name is matched ignoring case. Later edits to the mode don't change the arena |

`applyMode` needs the arena to be waiting for players. It returns a `ModeResult` with a `code`: `OK`, `NOT_FOUND` (no such arena or mode), `MISSING_PARTS` (the mode names generator configs, phase lists or GUIs that don't exist; they are in `missing` and nothing was changed) or `BUSY` (a game is starting or running). With `OK`, `teamsDiffer` is `true` when the arena's team count differs from the mode's `teams`.

`ArenaView.getGeneratorConfig()` and `getGamePhases()` return the ids of the generator config and the phase list an arena uses (null when it has none).

```java
ModeService modes = api.getModes();
modes.applyMode("Island", "Duels").thenAccept(result -> {
    if (result.code() == ModeResult.Code.OK && result.teamsDiffer()) {
        getLogger().warning("Island has a different team count than Duels");
    }
});
```

## Rules and good practice

- Call the API from the main thread (`join`, `spectate`, `leave`, `sendToLobby`, `addStat` and `registerShopItem` throw `IllegalStateException` on another thread). Exceptions that may be called from any thread: `getMaintenance()`, `getGameControl()` and `getShop()` (the work runs on the main thread, the futures complete there), and the stats futures `getLeaderboard`, `loadStats` and `findPlayers` (with MySQL they complete on a database thread, so switch to the main thread before using Bukkit).
- Events fire on the main thread; keep handlers fast.
- Do not teleport players to the lobby yourself: NewBedwars does it through `PlayerSendToLobbyEvent`, so network setups keep working.
- Do not hold on to worlds from `ArenaView#getWorld()`; fetch it again after `ArenaResetEvent` stage `POST`.
- Registrations are tied to your plugin; no cleanup code is needed on disable.

## Full example

[`docs/examples/event-logger`](https://github.com/l299l/NewBedwars-Free/tree/master/docs/examples/event-logger) is a complete, buildable addon (`pom.xml`, `plugin.yml`, [`EventLoggerPlugin.java`](https://github.com/l299l/NewBedwars-Free/blob/master/docs/examples/event-logger/src/main/java/com/example/nbwlogger/EventLoggerPlugin.java)). It logs every API event and registers a phase action, shop logic, armor tier, upgrade, trap and subcommands, and uses extra data and the arena editor. Its README says how to try each one.
