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
| `price` / `priceType` | Cost and currency: `IRON`, `GOLD`, `DIAMOND`, `EMERALD`, `NONE` (free), or `OTHER` (paid with the item in `priceMaterial`) |
| `priceMaterial` | Only for `priceType: OTHER`: the item players pay with, for example `NETHER_STAR` |
| `onBuyEvent` | What buying does: `GIVE_ITEM` (into the inventory), `GIVE_ARMOR` (upgrade the armor tier), `GIVE_TOOL` (sword or tool that follows the tool rules), `UPGRADE` (a team upgrade icon), `COMMANDS`, `OPEN_GUI` and `GIVE_PERM` (see [Commands, menus and permissions](#commands-menus-and-permissions)), `CANCEL` (nothing, for decoration) |
| `commands` / `gui` / `givePermission` | Only for `COMMANDS`, `OPEN_GUI` and `GIVE_PERM`, see below |
| `itemLogic` | Special behaviour when the item is used: `NONE`, `TNT`, `FIREBALL`, `BRIDGE_EGG`, `LUCKY_BLOCK`, `POTION`, `BLAST_PROTECTION`, `IRON_GOLEM`, `SILVERFISH`, `PORTABLE_TOWER`. Addons can add their own |
| `isPermanent` | The item is unbreakable. A `GIVE_ITEM` item is also given back on every respawn once bought. See [Permanent items](#permanent-items) |
| `permission` | Only players with this permission can buy it (empty = everyone) |
| `description` / `iconDesc` | `default` takes the name and shop description from the language files (`CustomItemsNames` section: `<ID>-name` and `<ID>-icon-description`) |
| `enchantments` | Enchantments and levels, for example `{SHARPNESS: 1}` |
| `itemFlags` | Item flags, for example `[HIDE_ENCHANTS]` |

**PlaceholderAPI (2.8-beta):** when PlaceholderAPI is installed, placeholders are filled in for the player who opens the menu, for example `&7You have won %newbedwars_stat_wins% games`. This works in:

- the shop name and shop description of shop items (`<ID>-name` and `<ID>-icon-description`),
- the same texts of team upgrades and traps in the upgrade menu,
- the `Name` and `Description` of categories in menu files.

Any PlaceholderAPI placeholder works, also those of other plugins. The menu shows the values from the moment it was opened.

> [!NOTE]
> Placeholders are only filled in on the icons in the menus. The item a player receives when buying keeps its normal name and description, on purpose: with player-specific texts, bought blocks such as wool would no longer stack with each other. Put placeholders in `<ID>-icon-description`, not in the item's own description.

An item with an unknown material, enchantment or flag (for example from a newer Minecraft version) is skipped with a console warning and kept in the file unchanged. The same happens to an item with `priceType: OTHER` and no valid `priceMaterial`, and to a `COMMANDS`, `OPEN_GUI` or `GIVE_PERM` item without its `commands`, `gui` or `givePermission`.

### Commands, menus and permissions

Three `onBuyEvent` values do something other than give an item. The price is taken first, as for every item.

```yaml
VipKit:
  material: NETHER_STAR
  price: 1
  priceType: OTHER
  priceMaterial: DIAMOND_BLOCK
  onBuyEvent: COMMANDS
  commands:
  - give {player} golden_apple 2
  - 'player: kit vip'
Specials:
  material: CHEST
  price: 0
  priceType: NONE
  onBuyEvent: OPEN_GUI
  gui: specialShop
DoubleJump:
  material: FEATHER
  price: 8
  priceType: EMERALD
  onBuyEvent: GIVE_PERM
  givePermission: doublejump.use
```

- **`COMMANDS`** runs every line of `commands` (without the `/`). Lines run as the console, so they can do anything the console can; a line that starts with `player:` runs as the buyer instead. `{player}`, `{team}` and `{arena}` are replaced with the buyer's name, team and arena.
- **`OPEN_GUI`** opens the shop menu whose `GuiId` is in `gui`. With price `0` it works as a link to another menu, and no "bought" message is shown.
- **`GIVE_PERM`** gives the buyer the permission in `givePermission` until they leave the game, are eliminated or the game ends. It can't be bought twice in one game. Use it for perks of other plugins that check a permission.

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

Traps don't queue: when an enemy enters a base, every trap the team has bought fires at once and each loses one charge.

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
- **Different prices for some arenas:** an upgrade menu file can have its own `UpgradePrices:` section in the same format. Arenas that use that menu get those prices, for example cheaper upgrades in solo arenas. An entry that can not be read is logged once as a warning (naming the menu and the upgrade) and the prices from `upgrades.yml` are used for it.

## Generators (`generators/*.yml`)

Generators have no limit on how many items lie on the ground; items that nobody picks up stay until the game ends.

`generators/defaultGenerators.yml` sets how fast team, diamond and emerald generators drop resources at each level. Make another file with a different `ConfigurationId` for a faster or slower preset, and choose it with `DefaultGeneratorConfig` in `config.yml` or, per arena, `/bw setGeneratorLeveling <configId>` in advanced setup. The comments at the top of the file explain every value.

Diamond and emerald generators are upgraded by the [game phases](configuration.md#game-phases); team generators by the Forge upgrade.

`ShowNextSpawnTime` in each generator section turns the "next spawn in" countdown in the hologram above the generator on or off. By default it is on for diamond and emerald generators and off for team generators.

### Custom generators

A generator file can also define generators that drop any item, in a `CustomGenerators` section (there is a commented example at the end of `defaultGenerators.yml`):

```yaml
CustomGenerators:
  gapple:
    Item: GOLDEN_APPLE
    Amount: 1
    SpawnDelay: 60
    Display: GOLD_BLOCK
    Name: "&6Golden Apple"
    ShowNextSpawnTime: true
```

| Key | Meaning |
|---|---|
| `Item` | What it drops |
| `Amount` | How many per drop (1 to 64, default 1) |
| `SpawnDelay` | Seconds between drops (default 30) |
| `Display` | The block that spins above it (default: the item) |
| `Name` | Title of its hologram, `&` colours allowed (default: the item name) |
| `ShowNextSpawnTime` | Show the countdown in the hologram (default `true`) |

Place one in advanced setup mode with `/bw setGenerator OTHER <id> [x y z]`, using the generator file of the arena (`/bw setGeneratorLeveling`). Custom generators have one level and are not upgraded by game phases. Like diamond and emerald generators, bridge eggs and portable towers don't build next to them.
