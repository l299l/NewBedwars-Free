# Installation

## Requirements

- **Server:** Paper 1.17 to 26.x (Spigot is untested)
- **Java:** 16 or newer, or whatever your Paper version needs (1.18+ needs Java 17, 1.21+ needs Java 21)
- No other plugins are required. These are optional:

| Plugin | Used for |
|---|---|
| PlaceholderAPI | 34 `%newbedwars_*%` placeholders ([list](placeholders.md)) |
| WorldEdit / FastAsyncWorldEdit | Creating an arena from a `.schem` file |
| Citizens | Lobby join NPCs with a live hologram (without it, `/bw npc create` spawns a simple NPC) |

For network mode you also need Velocity 4.x on Java 25 and the same NewBedwars version on every server. See [Network mode](network.md).

## First start

1. Download `NewBedwars-<version>.jar` from [Modrinth](https://modrinth.com/plugin/new-bedwars/versions) and put it in `plugins/`.
2. Start the server. NewBedwars creates its files in `plugins/NewBedwars/`.
3. Stand at your lobby spawn and run `/bw setLobby`.
4. Create your first arena: `/bw arena create myArena`. Continue with [Arena setup](arena-setup.md).
5. Players join with `/bw join myArena`, the arena selector or a [join NPC](commands.md#admin-commands).

## Files

Everything lives in `plugins/NewBedwars/`:

| File or folder | Contents |
|---|---|
| `config.yml` | Main settings ([Configuration](configuration.md)) |
| `language/*.yml` | Messages in every language; edit them or add your own language |
| `scoreboards.yml`, `bossBars.yml` | Scoreboards and boss bars of the waiting lobby, the game and the end of the game |
| `generators/*.yml` | Generator speeds and levels |
| `data/items.yml` | Shop items ([Shop and upgrades](shop-and-upgrades.md)) |
| `data/upgrades.yml` | Upgrade and trap prices per level |
| `data/guis/*.yml` | Shop and upgrade menus |
| `data/arenas/<name>.json` | Arena settings (written by the setup commands, with automatic backups) |
| `data/maps/<arena>/template/` | Clean copy of every arena map, used to reset it after a game |
| Player data | JSON files in `data/` by default, or MySQL ([Storage](storage.md)) |

`/bw reload` reloads the configuration, messages, scoreboards, boss bars, generators, shop, GUIs and arenas without a server restart.

!!! warning
    `/bw reload` stops every running game and kicks players who are in setup mode. Use it when the arenas are empty.

## Updating

NewBedwars checks Modrinth (GitHub as a fallback) for new versions every 12 hours. When one is out:

- the console gets one line, and admins with `newbedwars.update.notify` get one chat line when they join;
- `/bw update` checks right away, and `/bw update download` downloads the new jar (checksum verified) into `plugins/update/`. The server installs it on the next restart.

`Updates.AutoDownload: true` downloads new versions automatically. `Updates.Channel` chooses between release, beta and alpha versions, and `Updates.Check: false` turns the check off completely. Nothing about your server is sent. See [Updates](configuration.md#updates).

!!! tip
    Versions up to 2.6-beta do not check for updates. Update to 2.7-beta once by hand; after that NewBedwars tells you about new versions itself.

Your configuration, arenas, maps and player data are kept when you update. New settings are added to `config.yml` with their default values, and data that has to move to a new format is moved automatically on the first start (the old files are kept as a backup). Read the [changelog](https://github.com/l299l/NewBedwars-Free/blob/master/CHANGELOG.md) before updating.
