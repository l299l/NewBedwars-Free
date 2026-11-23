# Arenas

See [Getting started](index.md) for how to get `NewBedwarsAPI api` and the dependency.

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
