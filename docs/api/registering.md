# Registering your own content

See [Getting started](index.md) for how to get `NewBedwarsAPI api` and the dependency.

## Registering your own content

All `register*` methods take your plugin as `owner`. A name that already exists throws `IllegalArgumentException`. Everything you register is **removed automatically when your plugin is disabled** (or call `api.unregisterAll(plugin)`).

### Phase actions

Usable in `GamePhases` in [`modes.yml`](../game-modes.md#game-phases) as `NAME` or `NAME:argument` (everything after the first colon is the argument).

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

`ShopItem.of(name, material, amount, price, priceType, logic)` covers the common case; use the full `ShopItem` constructor for permanent items, enchantments, flags or another `onBuy` action. The texts are optional (without them the item is named after its `name`). From 2.8-beta the full constructor also takes `commands`, `openGui`, `givePermission` and `priceMaterial` for the `COMMANDS`, `OPEN_GUI` and `GIVE_PERM` actions and `priceType` `OTHER` (see [Commands, menus and permissions](../shop-and-upgrades.md#commands-menus-and-permissions)); the 12-argument constructor is kept and leaves them empty.

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
