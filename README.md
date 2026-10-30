![NewBedwars Banner](https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/banner_main.svg)

<div align="center">

[![Modrinth](https://img.shields.io/badge/Modrinth-Download-1BD96A?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/new-bedwars) [![GitHub](https://img.shields.io/badge/GitHub-Source-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/l299l/NewBedwars-Free)

</div>

---
# NewBedwars

A modern, fully configurable Bedwars plugin built from the ground up for **Paper 1.17+**. You get the classic Bedwars experience (teams, beds, generators, shops) plus a proper in-game setup system, YAML-driven GUIs, multi-arena support, and deep gamerule control. No extra plugins needed.

Whether you're running a small survival server and just want to add a Bedwars minigame, or building a full-blown Bedwars network, NewBedwars gives you the tools to build polished arenas without a headache.

> **This is a beta release.** Core gameplay is stable and feature-complete. Please report any issues via the GitHub issue tracker.

---

## Why this exists

A lot of free Bedwars plugins make you pick one: simple but bare-bones, or powerful but bloated with dependencies and a config file the size of a novel. NewBedwars tries to skip that trade-off. You set up an arena entirely in-game through a guided setup flow, so there's no hand-editing YAML just to place a bed or a generator. Everything from the shop GUI to the scoreboard is defined in YAML you can reshape without touching a single line of Java. PlaceholderAPI and WorldEdit are supported if you have them, but you don't need either one. Install the jar, start the server, and you've got a fully working Bedwars setup.

---

## Highlights

**Gameplay**
- Multi-arena support, so you can run as many arenas as you want at once, each in its own world
- Configurable team count, colors, and player limits per arena
- Quick Void: instant death below a configurable Y level
- Per-team build protection zones so the opposing side can't place blocks where they shouldn't
- Rejoin system, so players can reconnect to a game if they get disconnected
- Map voting: put arenas into groups; at the end of a game players vote for the next map and move there together. `/bw arena copy` clones an arena so one map can run several games
- Party system: group up with `/party`, invite friends with clickable chat buttons, and let the party admin lead everyone into an arena together

**Chat & visibility**
- Arena chat isolation, so each arena has its own chat completely separate from the lobby and other arenas
- Team chat via `!message`, toggleable per arena
- Optional arena-wide global chat channel
- Tab list isolation, so lobby and arena players never see each other

**Display & HUD**
- YAML-templated bossbars for waiting, playing, and ending states
- Per-player scoreboards with full placeholder support
- Spectator mode with flight and a compass that teleports you to live players
- Six languages out of the box: English, Polish, German, Spanish, French, and Russian. Missing keys just fall back to English automatically
- Per-arena resource packs, sent automatically on join and cleared on leave

**Integrations**
- PlaceholderAPI: 34 placeholders under `%newbedwars_*%` (players, stats, arenas and arena groups) for scoreboards, chat formatters, and other plugins. Totally optional, the plugin loads fine without it
- WorldEdit / FastAsyncWorldEdit: build an arena straight from a `.schem` file instead of placing blocks by hand. Also optional, manual setup works fine without it
- Citizens: turn any Citizens NPC into a lobby join NPC with a live hologram (join a group or arena, or open the arena selector). Without Citizens, `/bw npc create` spawns a simple NPC instead

---

## Requirements

- **Server:** Paper 1.17 – 26.3 *(Spigot will probably work too, but it's untested, so Paper is recommended)*
- **Java:** 16 or higher

Optional: **PlaceholderAPI**, if you want `%newbedwars_*%` placeholders elsewhere on your server, **WorldEdit** or **FastAsyncWorldEdit**, if you'd rather build arenas from a schematic than by hand, and **Citizens**, for skinned lobby join NPCs. None of them is required, NewBedwars runs fine standalone.

---

## Getting started

1. Grab the jar from the **[latest Modrinth release](https://modrinth.com/plugin/new-bedwars/versions)** and drop it into your server's `/plugins/` folder.
2. Start the server. Config files generate automatically.
3. Set your lobby spawn: `/bw setLobby`
4. Create your first arena: `/bw arena create myArena`
5. Follow the setup checklist shown by `/bw arena`.
6. Save the arena: `/bw save`
7. Enable it when ready: `/bw arena enable myArena`

### Building an arena from a schematic

If you'd rather paste in a finished map than build one from scratch in-game:

1. Save your map as a `.schem` file in WorldEdit's schematics folder (`plugins/WorldEdit/schematics/` or `plugins/FastAsyncWorldEdit/schematics/`).
2. Run `/bw arena create myArena -sche myMap`.
3. The map gets pasted automatically at world origin. Continue with the normal setup checklist from there.

---

## Commands and permissions

Players mostly use `/bw join`, `/bw lobby`, `/bw spectate`, `/bw stats` and `/party`. Admins manage arenas with `/bw arena ...` (create, setup, enable, backup, export...) and `/bw setlobby`; `/bw help` and `/bw setupHelp` list what is available to you right now.

The full list of commands, setup commands and permissions is in **[docs/COMMANDS.md](docs/COMMANDS.md)**.

---

## PlaceholderAPI

If PlaceholderAPI is installed, NewBedwars registers 26 placeholders under `%newbedwars_<placeholder>%` for the current game, lifetime stats and individual arenas. The full list is in **[docs/PLACEHOLDERS.md](docs/PLACEHOLDERS.md)**.

---

## Addon developers

[![](https://jitpack.io/v/l299l/NewBedwars-Free.svg)](https://jitpack.io/#l299l/NewBedwars-Free)

**Full documentation: [docs/API.md](docs/API.md)** (reading data, events, registering shop items/upgrades/armor/commands/phase actions, extra data, editing arenas).

NewBedwars has a small API with events, so other plugins can hook into it without touching internals. Only the `com.l299l.newbedwars.api` package is stable; everything else can change in any update.

**1. Add the dependency** (published through [JitPack](https://jitpack.io/#l299l/NewBedwars-Free); available from the first release that contains the API):

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.l299l</groupId>
        <artifactId>NewBedwars-Free</artifactId>
        <version>v2.5-beta</version> <!-- a release tag -->
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**2. Depend on it** in your `plugin.yml`: `depend: [NewBedwars]`.

**3. Use it:**

```java
public class MyAddon extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        NewBedwarsAPI api = NewBedwarsAPI.get(); // read-only views of arenas, teams and players
        getLogger().info("Arenas: " + api.getArenas().size());
    }

    @EventHandler
    public void onBedBreak(BedBreakEvent e) {
        e.getArena().getPlayers().forEach(p -> p.sendMessage(e.getBreaker().getName() + " broke " + e.getTeam().getName() + "'s bed!"));
    }

    @EventHandler
    public void onKill(PlayerKillEvent e) {
        if (e.isFinalKill()) getLogger().info(e.getVictim().getName() + " was eliminated");
    }
}
```

| Event | Cancellable | Fired |
|---|---|---|
| `GameStartEvent` / `GameEndEvent` | no | game starts (teams assigned) / ends (winner or draw) |
| `ArenaResetEvent` | no | before (`PRE`) and after (`POST`) the map is reset |
| `PlayerJoinArenaEvent` | yes | before a player joins or rejoins |
| `PlayerLeaveArenaEvent` | no | a player or spectator leaves (with a `LeaveReason`) |
| `BedBreakEvent` | yes | before an enemy bed is destroyed |
| `PlayerKillEvent` | no | a player dies in a running game |
| `ShopPurchaseEvent` | yes | before a shop purchase is charged |
| `TeamUpgradeEvent` | yes | before a team upgrade or trap is bought |
| `ArmorEquipEvent` | no (armor can be replaced) | before armor is applied |
| `PlayerSendToLobbyEvent` | yes (target can be changed) | whenever the plugin sends a player to the lobby, e.g. to redirect to a lobby server |

---

## Roadmap

NewBedwars will leave beta once stable operation is verified across all supported server versions (Paper 1.17 – 26.3).

- [x] Party support
- [x] PlaceholderAPI support
- [x] WorldEdit / FAWE schematic-based arena creation
- [x] Per-arena resource pack forcing
- [x] More languages (DE, ES, FR, RU) (partial support)
- [x] More custom items (Iron Golem, Silverfish, etc.)
- [x] Arena statistics and leaderboards
- [x] Player profile GUI with stats history
- [x] Improved help
- [x] Addon API and events
- [x] Citizens2 support (lobby join NPCs)
- [x] In-game map voting system
- [ ] More languages (full support)
- [ ] Network / BungeeCord / Velocity mode
- [ ] Website panel

---

## Screenshots

<details>
<summary><b>Joining & Waiting</b></summary>

<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_110222.png" width="100%"/><br><sub>Arena selector GUI</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_110543.png" width="100%"/><br><sub>Waiting lobby with bossbar & scoreboard</sub></td>
  </tr>
</table>

</details>

<details>
<summary><b>In-Game HUD & Events</b></summary>

<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_114731.png" width="100%"/><br><sub>In-game HUD — scoreboard, phase info, NPC labels</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121249.png" width="100%"/><br><sub>Respawn countdown</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_120913.png" width="100%"/><br><sub>Bed destroyed announcement</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_1205111.png" width="100%"/><br><sub>Trap alarm</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121425.png" width="100%"/><br><sub>End game — spectator view with bossbar</sub></td>
    <td></td>
  </tr>
</table>

</details>

<details>
<summary><b>Shop & Upgrades</b></summary>

<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_114843.png" width="100%"/><br><sub>Shop GUI</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_114904.png" width="100%"/><br><sub>Bridge Egg — shop tooltip (cost: 4 Gold)</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_114919.png" width="100%"/><br><sub>Team Upgrades shop</sub></td>
    <td></td>
  </tr>
</table>

</details>

<details>
<summary><b>Resource Generators</b></summary>

<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_115559.png" width="100%"/><br><sub>Emerald Generator — hologram</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_120327.png" width="100%"/><br><sub>Diamond Generator — hologram</sub></td>
  </tr>
</table>

</details>

<details>
<summary><b>Arena Setup</b></summary>

<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121511.png" width="100%"/><br><sub>Help command output</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121720.png" width="100%"/><br><sub>Arena setup checklist (NORMAL_SETUP)</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_121727.png" width="100%"/><br><sub>Team configuration checklist — missing fields highlighted</sub></td>
    <td></td>
  </tr>
</table>

</details>

<details>
<summary><b>Map & Environment</b></summary>

<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_122307.png" width="100%"/><br><sub>Arena map — aerial overview</sub></td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_123438.png" width="100%"/><br><sub>Live gameplay — combat near a bed 1</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_122037.png" width="100%"/><br><sub></sub>Live gameplay — bed protection</td>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_122528.png" width="100%"/><br><sub>Live gameplay — building bridge</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/l299l/NewBedwars-Free/refs/heads/master/assets/screenshots/Screenshot_20260611_123625.png" width="100%"/><br><sub>Live gameplay — combat near a bed 2</sub></td>
    <td></td>
  </tr>
</table>

</details>

---

## Support & Bug Reports

Found a bug or have a suggestion? Please open an issue on **[GitHub](https://github.com/l299l/NewBedwars-Free/issues)** or use the platform's discussion section. Most problems can be resolved quickly with a good bug report.

---

## License

See [LICENSE.txt](https://github.com/l299l/NewBedwars-Free/blob/master/LICENSE.txt).

---

<div align="center">

[![Latest version](https://img.shields.io/modrinth/v/new-bedwars?label=latest%20version&style=flat-square)](https://modrinth.com/plugin/new-bedwars/versions)

**NewBedwars** - Developed by **l299l** - Paper 1.17–26.3

</div>
