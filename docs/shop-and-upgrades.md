# Shop and upgrades

The shop is made of three kinds of files in `plugins/NewBedwars/data/`:

| File | Contents |
|---|---|
| `items.yml` | Every item that can be sold: material, amount, price, what buying does |
| `guis/*.yml` | Shop and upgrade menus: which item goes into which slot, categories |
| `upgrades.yml` | Upgrade and trap prices per level |

Run `/bw reload` after editing these files (it stops running games, see [Installation](installation.md#files)).

!!! tip "Easier with the web panel"
    The [web panel](panel.md) addon has a visual editor for all three: items with a live colour preview, menus on a drag-and-drop slot grid, and upgrade prices. Changes apply at once and the previous version of every file is backed up.

## Shop items (`items.yml`)

Every entry is one item. The key is the item ID that menus refer to:

```yaml
TNT:
  material: TNT
  amount: 1
  price: 4
  priceType: GOLD
  onBuyEvent: GIVE_ITEM
  itemLogic: TNT
  isPermanent: false
  permission: ''
  description: default
  iconDesc: default
  enchantments: {}
  itemFlags: []
```

| Key | Meaning |
|---|---|
| `material` | Bukkit material name of the item |
| `amount` | How many the player gets |
| `price` / `priceType` | Cost and currency: `IRON`, `GOLD`, `DIAMOND`, `EMERALD`, or `NONE` (free) |
| `onBuyEvent` | What buying does: `GIVE_ITEM` (into the inventory), `GIVE_ARMOR` (upgrade the armor tier), `GIVE_TOOL` (sword or tool that follows the tool rules), `UPGRADE` (a team upgrade icon) |
| `itemLogic` | Special behaviour when the item is used: `NONE`, `TNT`, `FIREBALL`, `BRIDGE_EGG`, `LUCKY_BLOCK`, `POTION`, `BLAST_PROTECTION`, `IRON_GOLEM`, `SILVERFISH`, `PORTABLE_TOWER`. Addons can add their own |
| `isPermanent` | The item is unbreakable. A `GIVE_ITEM` item is also given back on every respawn once bought. See [Permanent items](#permanent-items) |
| `permission` | Only players with this permission can buy it (empty = everyone) |
| `description` / `iconDesc` | `default` takes the name and shop description from the language files (`CustomItemsNames` section: `<ID>-name` and `<ID>-icon-description`) |
| `enchantments` | Enchantments and levels, for example `{SHARPNESS: 1}` |
| `itemFlags` | Item flags, for example `[HIDE_ENCHANTS]` |

An item with an unknown material, enchantment or flag (for example from a newer Minecraft version) is skipped with a console warning and kept in the file unchanged.

### Permanent items

What a player keeps after death depends on `onBuyEvent`, not only on `isPermanent`:

| `onBuyEvent` | Kept after death | What `isPermanent` does |
|---|---|---|
| `GIVE_ITEM` | Only when `isPermanent: true` | Unbreakable, and given back on every respawn |
| `GIVE_TOOL` (swords) | Gamerule `MakeSwordsPermanent` (`false` = back to the starting sword) | Unbreakable only |
| `GIVE_TOOL` (pickaxe, axe, shears) | Gamerule `MakeToolsPermanent` (`false` = lost) | Unbreakable only |
| `GIVE_TOOL` (any other material) | Always | Unbreakable only |
| `GIVE_ARMOR` | Always (the armor tier stays) | Nothing |

The gamerules have their defaults in [`config.yml`](configuration.md#default-gamerules) and can be changed per arena with `/bw gamerule` (advanced setup mode).

The starting sword is the item named in `BasicSword` in [`config.yml`](configuration.md#gameplay).

## Shop and upgrade menus (`guis/*.yml`)

On the first start, `exampleShop.yml` and `exampleUpgrade.yml` are copied into `data/guis/`. Edit those copies, or copy one to make a new menu with another `GuiId`.

```yaml
GuiId: exampleShop
GuiClass: ShopGUI
GuiName: "Example Shop"
Size: 54
Fill: ci:fill
Empty: empty
HeaderSize: 18
FooterSize: 9

Slots:
  1: category:home
  2: category:blocks
  20: ci:wool
  21: ci:ironaxe

Categories:
  - ID: blocks
    Name: "&aBlocks"
    Icon: wool
    Description: "&7Browse building blocks"
    Items:
      - wool
      - EndStone
```

- **`Size`** is a multiple of 9, up to 54. **`HeaderSize`** and **`FooterSize`** are the slots at the top and bottom that stay the same on every category page.
- **`Slots`** keys start at 1 (key 1 is the first slot). Values:
    - `ci:<item>`: an item from `items.yml`
    - `category:<id>` (shop) or `cat:<id>` (upgrades): a button that opens a category
    - `upg:<upgrade>`: an upgrade, for example `upg:sharpness`
    - `fill`: the filler item, `empty`: nothing
- **`Categories`** are the pages behind the category buttons: name, icon, description and the list of items shown on that page. The `home` category shows the default home page; players can change their own quick-buy slots.

Upgrades: `protection`, `sharpness`, `haste`, `forge`, `healpool`, `dragonbuff`, and the traps `alarmtrap`, `blindtrap`, `miningfatiguetrap`. Addons can add more.

**Which menu an arena uses:** new arenas get `DefaultTeamShopGui` and `DefaultTeamUpgradeGui` from [`config.yml`](configuration.md#arena-defaults). In advanced setup mode, `/bw setGui shop <guiId>` and `/bw setGui upgrade <guiId>` choose another menu for one arena.

## Upgrade prices (`upgrades.yml`)

Every upgrade has a list of levels. The first entry is the price of level 1, the second of level 2 and so on, so the length of the list is the number of levels a team can buy:

```yaml
DefaultCurrency: DIAMOND

Upgrades:
  SHARPNESS:
    - {price: 2, currency: DIAMOND}
    - {price: 4, currency: DIAMOND}
  HEALPOOL:
    - {price: 4, currency: DIAMOND}
  ALARMTRAP:
    - {price: 1, currency: DIAMOND}
    - {price: 2, currency: DIAMOND}
```

- `currency` is `IRON`, `GOLD`, `DIAMOND` or `EMERALD`; when it is left out, `DefaultCurrency` is used.
- A shorter list means fewer levels (up to what the upgrade supports). An empty list means the upgrade can't be bought.
- **Different prices for some arenas:** an upgrade menu file can have its own `UpgradePrices:` section in the same format. Arenas that use that menu get those prices, for example cheaper upgrades in solo arenas.

## Generators (`generators/*.yml`)

`generators/defaultGenerators.yml` sets how fast team, diamond and emerald generators drop resources at each level. Make another file with a different `ConfigurationId` for a faster or slower preset, and choose it with `DefaultGeneratorConfig` in `config.yml` or, per arena, `/bw setGeneratorLeveling <configId>` in advanced setup. The comments at the top of the file explain every value.

Diamond and emerald generators are upgraded by the [game phases](configuration.md#game-phases); team generators by the Forge upgrade.
