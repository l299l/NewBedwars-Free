# Reading data and events

See [Getting started](index.md) for how to get `NewBedwarsAPI api` and the dependency.

## Reading data

`NewBedwarsAPI` hands out read-only views. Views stay valid while the arena exists, but **never keep `ArenaView#getWorld()`**: the world is replaced after every map reset.

| Method | Returns |
|---|---|
| `getVersion()` | plugin version |
| `getApiVersion()` **(2.8-beta)** | the [API version](index.md#api-version), a number |
| `getArenas()` | all arenas |
| `getArena(String name)` / `getArena(World)` / `getArena(Player)` | `Optional<ArenaView>` (the player variant also finds spectators) |
| `getGroups()` / `getGroup(String name)` **(2.6-beta)** | arena groups (maps voted between), `Optional<ArenaGroupView>` |
| `getSetupArena(Player)` / `getSetupPlayers()` **(2.7-beta)** | the arena a player is setting up (in setup mode), `Optional<ArenaView>`; everyone in setup mode as `Map<Player, ArenaView>` |
| `getNetworkMode()` **(2.6-beta)** | `NONE`, `GAME` or `LOBBY` (`Network.Mode`) |
| `getNetworkArenas()` **(2.6-beta)** | on `LOBBY` and `GAME` servers: the arenas of all game servers (`NetworkArenaView`: server, name, group, state, players, `isJoinable()`, `getFreeSlots()`), empty otherwise |
| `getServerId()` **(2.6-beta)** | this server's name in the proxy, empty when not in network mode |
| `getNetworkRejoin(Player)` **(2.6-beta)** | on a `LOBBY` server: the network arena the player can still rejoin |
| `getStats(String name)` **(2.6-beta)** / `getStats(UUID)` **(2.8-beta)** | `PlayerStatsView` (wins, losses, kills, deaths, final kills, beds, games; zeros when unknown). With MySQL storage these are network-wide; only online/loaded players are known. The UUID variant still finds a player who changed their name |
| `getParty(Player)` **(2.8-beta)** | `Optional<PartyView>`, see [Parties](players.md#parties) |
| `getLanguage(Player)` / `getMessage(Player, key)` **(2.8-beta)** | the player's language code and a translated message, see [Messages and languages](players.md#messages-and-languages) |

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
| `TablistUpdateEvent` **(2.8-beta)** | yes | just before a changed tab list (header, footer or list name) is sent to a player; not fired while nothing changed; cancelled = nothing is sent for this change; not an `ArenaEvent` | `getPlayer()`, `getArena()` (`Optional`, empty in the lobby), `getLayout()` (null in the lobby), `getHeader()` / `setHeader(String)`, `getFooter()` / `setFooter(String)`, `getPlayerListName()` / `setPlayerListName(String)` |
| `ScoreboardUpdateEvent` **(2.8-beta)** | yes | just before a changed sidebar scoreboard (title or lines) is sent to a player; not fired while nothing changed; cancelled = nothing is sent for this change; not an `ArenaEvent` | `getPlayer()`, `getArena()` (`Optional`), `getScoreboard()` (id), `getTitle()` / `setTitle(String)`, `getLines()` (a copy) / `setLines(List<String>)` (at most 15 lines) |
| `BossBarUpdateEvent` **(2.8-beta)** | yes | when a boss bar of `bossBars.yml` switches to a new text (a step of its `Texts` cycle, or an arena starts showing it at a phase change); not for addon boss bars; cancelled = the previous display is kept for this step (the first display is shown as configured); not an `ArenaEvent` | `getArena()` (`Optional`, empty for `MainLobby`), `getBossBar()` (id), `getText()` / `setText(String)`, `getColor()` / `setColor(BarColor)`, `getStyle()` / `setStyle(BarStyle)` |
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

## Extra data

Store your own settings inside an arena or team. They are saved in the arena file and never touched by NewBedwars. Each addon has its own namespace (its plugin name).

```java
ExtraData data = arena.getExtra(this);
data.set("difficulty", 3);
int d = data.getInt("difficulty", 1);
```

Getters take a default; setters exist for `String`, `int`, `double`, `boolean`; also `has`, `remove`, `keys`. Changes are written to disk shortly after (no manual save).
