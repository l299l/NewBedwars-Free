# NewBedwarsEventLogger

Example addon for the NewBedwars API (see [docs/API.md](../../API.md)). It logs every API event (console + chat of ops) and uses the registration, extra data and editor APIs, so it doubles as a test of each in a real game.

Build: `mvn package` here (the NewBedwars API comes from JitPack), copy `target/NewBedwarsEventLogger-1.0.jar` to the server's `plugins/` next to NewBedwars.

- `/nbwlog` prints what the read-only API sees (arenas, teams, your stats). `/nbwlog chat` toggles chat output.
- Set `CANCEL_TEST = true` in `EventLoggerPlugin` to also test cancelling: Wool purchases and bed breaks of team "Red" get cancelled.

What to check in a round: join / leave (command + quit), start, bed break, kill (player, void, mob), shop item/armor/upgrade, trap, end, map reset PRE/POST, `/lobby`.

## Testing the registration, extra data and editor APIs

On enable the addon registers (all removed automatically when the addon is disabled):

| Feature | How to test |
|---|---|
| Phase action `NBWLOG` | add `NBWLOG:hello` to a phase's actions in `config.yml`; players get a message and the console logs it when the phase runs |
| Shop logic `NBWLIGHTNING` | set `itemLogic: NBWLIGHTNING` on a shop item in `items.yml`; right-click it: lightning effect, item consumed |
| Armor tier `NBWTEST` (rank 2) | shop item with `onBuyEvent: GIVE_ARMOR` and `TURTLE_HELMET`: you get diamond boots, iron leggings, golden chestplate, turtle helmet (a lower tier like leather must not replace it) |
| Upgrade `NBWSPEED` (2 levels, 2/4 diamonds) | put `upg:NBWSPEED` in the upgrade GUI file; buy it: Speed for the whole team, re-applied after respawn; level shown by `/bw nbwinfo` |
| Trap `NBWBLIND` | put `upg:NBWBLIND` in the upgrade GUI; an enemy entering the base gets Blindness and the console logs it |
| Subcommands | `/bw nbwinfo` in a game (shows arena extra data, generators, upgrade levels); `/bw nbwlobby` as admin in the lobby; both appear in `/bw help` |
| Extra data | the addon counts `games` on the arena at game start and `wins` on the winning team; check with `/nbwlog` and that the values survive a restart (`extra` in the arena JSON) |
| Editor | stand in a WAITING arena (as an op) and run `/nbwlog edit`: it creates team `NbwTest`, adds and removes a diamond generator, removes the team. Run it during a game: it must refuse |
| Cleanup | `/plugman unload NewBedwarsEventLogger` (or disable): the action, item logic, upgrades and commands must disappear without errors; reload it and registration works again |
