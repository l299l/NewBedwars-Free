# Players

See [Getting started](index.md) for how to get `NewBedwarsAPI api` and the dependency.

## Players: join, spectate, leave

> [!IMPORTANT]
> **New in 2.8-beta.**

The same paths as the commands, so parties, rejoining, events and messages work as usual. Call them on the server thread with an arena of this server (`IllegalArgumentException` otherwise; in network mode use [`requestNetworkJoin`](data-and-events.md#network-actions-lobby-servers-26-beta) on a lobby server).

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
