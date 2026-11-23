# Getting started

Everything in the `com.l299l.newbedwars.api` package and its subpackages (`view`, `edit`, `command`, `registry`, `events`, `game`, `maintenance`, `shop`) is the stable contract for addons. All other packages are internal and can change in any update.

> [!IMPORTANT]
> **New in 2.8-beta:**
>
> - [API version](#api-version): `NewBedwarsAPI.getApiVersion()`, and the [compatibility rules](#compatibility) for the stable releases.
> - [Shop items](registering.md#shop-items): `ShopItem` has `commands`, `openGui`, `givePermission` and `priceMaterial` for the new `COMMANDS`, `OPEN_GUI` and `GIVE_PERM` actions and `priceType` `OTHER` (the 12-argument constructor still works).
> - `ShopGui.closeOnTransaction` and `CommandContext.SETUP_BUILDING` are marked for removal in 3.0.0.
> - [Players](players.md#players-join-spectate-leave): `join`, `spectate`, `leave` and `sendToLobby` with a `JoinResult`.
> - [Parties](players.md#parties): `getParty(Player)` (`PartyView`) and `PartyChangeEvent`.
> - [Messages and languages](players.md#messages-and-languages): `getLanguage`, `getMessage` and `registerMessages` for translated addon texts.
> - [Statistics](players.md#statistics-and-leaderboards): `getStats(UUID)`, `loadStats(UUID)` and `addStat` to count stats for addon game modes.
> - [Game modes](shop-and-modes.md#game-modes): `getModes()` (`ModeService`) lists the modes and phase lists of `modes.yml` and applies a mode to a waiting arena; `ArenaView.getGeneratorConfig()` and `getGamePhases()`.
> - [Game control](arenas.md#game-control): `destroyBed`, `eliminateTeam` and `setUpgradeLevel`.
> - New [events](data-and-events.md#events): `ArenaStateChangeEvent` (also the countdown start and cancel), `TeamsAssignedEvent`, `PlayerRespawnArenaEvent`, `PlayerSpectateEvent`, `TrapTriggerEvent`, `PartyChangeEvent`. `ShopPurchaseEvent` has `getTeam()` and `setPrice(int)`; `BedBreakEvent.getBreaker()` is null when the bed was destroyed through `destroyBed`.
> - Custom generators: `GeneratorKind.CUSTOM`, `GeneratorView.getCustomId()`, `ArenaView.getCustomGeneratorIds()` and `ArenaEditor.addCustomGenerator(id, location)`. Before, custom generators were reported as `BASIC`.
> - Team display names: `TeamEditor.setDisplayName` / `TeamView.getDisplayName()` change the name players see (e.g. `Lime` for a team `Red` that got a lime color) without renaming the team.
> - `ShopItemStackEvent`: change or replace the item stack of a shop item per player, both the menu icon and the item the player gets.
> - `ArenaView.getMaxPlayers()`, `getTeam(Player)` and `isSpectator(Player)`; `ShopLogic.perform(player, arena, itemName)` tells the logic which shop item was used.

Pages of the addon API documentation:

- [Reading data and events](data-and-events.md): reading data, events, extra data
- [Registering your own content](registering.md): phase actions, shop item logic, shop items, armor tiers, upgrades and traps, subcommands
- [Arenas](arenas.md): editing arenas, arena maintenance, game control
- [Players](players.md): join, spectate and leave, parties, statistics and leaderboards, messages and languages
- [Display](display.md): placeholders, tab list, scoreboards, boss bars
- [Shop editor and game modes](shop-and-modes.md)
- [Full example](example.md)

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

## Rules and good practice

- Call the API from the main thread (`join`, `spectate`, `leave`, `sendToLobby`, `addStat` and `registerShopItem` throw `IllegalStateException` on another thread). Exceptions that may be called from any thread: `getMaintenance()`, `getGameControl()` and `getShop()` (the work runs on the main thread, the futures complete there), and the stats futures `getLeaderboard`, `loadStats` and `findPlayers` (with MySQL they complete on a database thread, so switch to the main thread before using Bukkit).
- Events fire on the main thread; keep handlers fast.
- Do not teleport players to the lobby yourself: NewBedwars does it through `PlayerSendToLobbyEvent`, so network setups keep working.
- Do not hold on to worlds from `ArenaView#getWorld()`; fetch it again after `ArenaResetEvent` stage `POST`.
- Registrations are tied to your plugin; no cleanup code is needed on disable.
