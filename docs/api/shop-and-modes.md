# Shop editor and game modes

See [Getting started](index.md) for how to get `NewBedwarsAPI api` and the dependency.

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

`ShopGui.itemPrices()` **(2.8-beta)** is a map of shop item id to `ItemPrice` (`price`, `currency`, `amount`; the last two may be null = the item's own), the `ItemPrices` section of the file (see [Shop and upgrades](../shop-and-upgrades.md)). When saving, `null` keeps the current prices and an empty map removes them. The older `ShopGui` constructors still work.

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
| `getModes()` | `ModeInfo`s in the order of `modes.yml`, then built-in modes the file doesn't list: `id`, `generators`, `gamePhases`, `shopGui`, `upgradeGui` (null = the mode leaves it unchanged), `maxInTeam` (0 = unchanged), `teams` (a hint, never applied; 0 = not set), `builtIn` and `tablist` (tab list layout id, null = unchanged; `saveMode` rejects an unknown layout with `unknown tablist: X`; the old eight-argument `ModeInfo` constructor still exists and sets it to null) |
| `getPhaseLists()` | `PhaseListInfo`s: the `id` and its `phases` (`PhaseInfo`: `name`, `duration` in seconds until the next phase, 0 for the last one, and `actions` as written in `modes.yml`, e.g. `DIAMOND_GENERATOR:2`) |
| `getArenaModes()` | `ArenaModeInfo` per arena, sorted by name: `arena`, `waiting` (only then can a mode be applied), `generators`, `gamePhases`, `shopGui`, `upgradeGui` (null when not set), `maxInTeam` and `teams` (team count) |
| `getGeneratorConfigs()` | ids of every loaded generator config, sorted |
| `applyMode(arena, mode)` | copies the mode into the arena once, like `/bw setMode` (generator config, phase list, GUIs and team size, each only when the mode sets it) and saves the arena. The mode name is matched ignoring case. Later edits to the mode don't change the arena |

`applyMode` needs the arena to be waiting for players. It returns a `ModeResult` with a `code`: `OK`, `NOT_FOUND` (no such arena or mode), `MISSING_PARTS` (the mode names generator configs, phase lists or GUIs that don't exist; they are in `missing` and nothing was changed) or `BUSY` (a game is starting or running). With `OK`, `teamsDiffer` is `true` when the arena's team count differs from the mode's `teams`.

### Editing modes, phase lists and generator configs

The edit methods return an `EditResult` with a `code`, `problems` (short English reasons) and `usedBy`. File work runs off the main thread; the futures still complete on the server thread, after the change was written and loaded. Edits never change arenas: an arena keeps the settings and the generator config object it already has until you apply a mode again.

| Method | Does |
|---|---|
| `saveMode(ModeInfo)` | creates a mode or replaces the one with the same id (ignoring case, the existing spelling is kept); `builtIn` is ignored. Only that block of `modes.yml` is rewritten, comments elsewhere stay. The id must match `[A-Za-z0-9_-]{1,32}`; `generators`, `gamePhases`, `shopGui` and `upgradeGui` must be null/blank (unchanged) or exist; `maxInTeam` and `teams` must be 0 to 100 |
| `deleteMode(id)` | removes a mode. Built-in modes (Solo, Doubles, Squads, Rush, Duels) can be edited but not deleted |
| `savePhaseList(PhaseListInfo)` | creates or replaces a phase list: at least one phase, named, duration of 0 or more, every action registered (`DIAMOND_GENERATOR` and `EMERALD_GENERATOR` need a level) |
| `deletePhaseList(id)` | removes a phase list; refused while a mode or an arena uses it, and for the lists that ship with NewBedwars |
| `getGeneratorConfigFiles()` | `GeneratorConfigInfo` (`id`, `fileName`, `builtIn`) for every file in `generators/`, sorted by id |
| `getGeneratorConfigText(id)` | the raw YAML of that config, or `null` when there is none |
| `saveGeneratorConfig(id, yaml)` | creates `generators/<id>.yml` or replaces the file of that id, then reloads the generator configs. The text must be YAML of at most 64 KB, its `ConfigurationId` must equal `id` and it must load as a valid generator config |
| `deleteGeneratorConfig(id)` | deletes the file and reloads; refused while a mode or an arena uses it, and for the bundled configs |

`EditResult.code` is `OK`, `NOT_FOUND`, `INVALID` (see `problems`; nothing was changed), `IN_USE` (see `usedBy`, e.g. `mode Squads` or `arena castle`), `BUILT_IN` (a delete of something that ships with NewBedwars) or `FAILED` (the file could not be written; `problems` holds the error).

`ArenaView.getGeneratorConfig()` and `getGamePhases()` return the ids of the generator config and the phase list an arena uses (null when it has none).

```java
ModeService modes = api.getModes();
modes.applyMode("Island", "Duels").thenAccept(result -> {
    if (result.code() == ModeResult.Code.OK && result.teamsDiffer()) {
        getLogger().warning("Island has a different team count than Duels");
    }
});
```
