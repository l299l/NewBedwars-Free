# Web panel (NewBedwars-Panel)

> [!NOTE]
> **New in 2.7-beta** (not released yet). The panel needs NewBedwars 2.7-beta or newer on the same server.

`NewBedwars-Panel` is a separate addon jar that adds a browser admin panel to your server. With it you can:

- watch every arena live: its state, players, phase (with the time left and the next phase), game time, and each team's bed;
- control running games: start the countdown, start now, end as a draw, stop and reset, skip to the next phase, and send a message to everyone in the arena;
- read each arena's game log: joins and leaves, chat and team chat, kills and final kills, beds, phases, generator upgrades, eliminated teams and admin actions, updated live;
- look up any player's stats and the leaderboards (top 10, 25 or 100 by wins, kills, final kills, beds, deaths, losses or games);
- see online players, TPS and memory;
- enable, disable, back up, restore, reset, copy, export (download) and delete arenas;
- import arena zips you upload.

Each person logs in with their own account. What they may do depends on their role, and every action is written to an activity log.

The panel runs inside the Paper server (no extra program, no database). It speaks HTTPS by default.

- [Installation](#installation)
- [The first admin account](#the-first-admin-account)
- [Logging in and the certificate warning](#logging-in-and-the-certificate-warning)
- [Accounts and roles](#accounts-and-roles)
- [Commands](#commands)
- [Configuration](#configuration)
- [Using your own certificate](#using-your-own-certificate)
- [Behind a reverse proxy (nginx, Caddy)](#behind-a-reverse-proxy-nginx-caddy)
- [Network mode](#network-mode)
- [Files and backups](#files-and-backups)
- [Security notes](#security-notes)
- [Troubleshooting](#troubleshooting)

## Installation

1. Put `NewBedwars-Panel-<version>.jar` next to `NewBedwars-<version>.jar` in `plugins/`. Use the same version for both.
2. Start the server. The console shows something like:
   ```
   [NewBedwars-Panel] Panel running at https://localhost:8150/
   [NewBedwars-Panel] No panel accounts yet. Create the first admin with: nbwpanel user create <name> admin
   [NewBedwars-Panel] Created a self-signed certificate, SHA-256 fingerprint: 83:3E:87:...
   ```
3. Open port **8150** (TCP) in your firewall or hosting panel if you want to reach the panel from another computer.

Requirements:

- Paper 1.17 to 26.x with NewBedwars 2.7-beta or newer.
- A Java runtime with the built-in web server (module `jdk.httpserver`). Standard JDK/JRE builds (e.g. Eclipse Temurin) have it. Only stripped-down custom runtimes made with `jlink` may lack it. In that case the console says so and the panel stays off.

Changes to `plugins/NewBedwars-Panel/config.yml` take effect after a server restart.

## The first admin account

Accounts are created from the server console, or in game by an op with `newbedwars.panel.manage`:

```
nbwpanel user create Alex admin
```

The answer contains a **one-time password**, shown only this once:

```
[NBW Panel] Account Alex created with role admin.
[NBW Panel] One-time password: xB7VZkhaJktRwFpt
[NBW Panel] It works once: the panel asks for a new password at the first login. It is not shown again.
```

Log in with it. The panel then asks you to choose your own password (10 to 128 characters) before anything else can be used. After that, create further accounts in the panel under **Accounts**, or with the same command.

If somebody forgets their password, run `nbwpanel user reset <name>`, or click **New password** in the panel. They get a new one-time password and are logged out everywhere.

## Logging in and the certificate warning

Open `https://<your-server-address>:8150/`. By default the panel uses a **self-signed certificate**, so the browser warns once ("Your connection is not private" / "Potential security risk").

- To make sure you are talking to your own server, compare the SHA-256 fingerprint in the browser's certificate details with the one in the console or in `nbwpanel status`. If they match, accept the warning.
- If you reach the panel through a domain name or a public IP, add it to `Https.Hostnames` so the certificate lists it.
- To get rid of the warning completely, use [your own certificate](#using-your-own-certificate) or a [reverse proxy](#behind-a-reverse-proxy-nginx-caddy).

The self-signed certificate is valid for 397 days. It is renewed automatically 30 days before it expires, and whenever `Https.Hostnames` or `Bind` changes. The fingerprint changes then, so the browser warns again.

The panel is in English and Polish. It follows the browser language, and you can switch it with the language selector in the top bar. `DefaultLanguage` is used when the browser asks for neither.

## Accounts and roles

Every account has one role. Two roles are built in and can not be changed:

| Role | Can |
|---|---|
| `admin` | everything |
| `staff` | view the arenas and players and the player stats (`dashboard.view`, `stats.view`) |

Under **Roles**, admins (or anyone with `roles.manage`) can create their own roles from these permissions:

| Permission | Allows |
|---|---|
| `dashboard.view` | the arena list, arena details, online players, TPS and memory |
| `arena.enable` / `arena.disable` | enabling / disabling arenas (disabling sends waiting players out) |
| `arena.backup` | making backups and seeing the backup list |
| `arena.restore` | restoring a backup |
| `arena.resetmap` | putting the map back to its saved state |
| `arena.export` | exporting arenas and downloading the zip files |
| `arena.import` | uploading and importing arena zips |
| `arena.copy` | copying an arena |
| `arena.delete` | deleting an arena (file, world and saved map; backups are kept) |
| `users.manage` | creating accounts, new passwords, changing roles, deleting accounts |
| `roles.manage` | creating, changing and deleting roles |
| `audit.view` | the activity log |
| `stats.view` | player stats and leaderboards |
| `game.manage` | start, stop, skip phases of games and broadcast to an arena (like `/bw game`) |
| `shop.edit` | the shop editor: items, their texts, shop GUIs and which GUIs the arenas use |
| `game.log` | the game log of each arena. **It includes team chat**, so give it only to people who may read that |

Rules that always apply:

- Every permission is checked by the server, not just hidden in the page.
- Nobody can give an account or a role a permission they don't have themselves. They also can't change accounts or roles that have more permissions than they do. Someone with only `users.manage` therefore can't make themselves an admin.
- The last `admin` account can't be deleted or given another role.
- A role that is still assigned to an account can't be deleted.
- Arena actions follow the same rules as `/bw arena`. For example, an arena with players in it can't be disabled. A refused action shows the plugin's own message.

## Game control and game log

On an arena's page, **Game** shows only the actions that fit the arena's state (waiting: start countdown, start now; starting: start now, stop; playing: skip phase, end as a draw, stop and reset; ending: stop). Ending as a draw saves the stats; "stop and reset" sends everyone out at once. Messages players see name the panel user (`Panel: Alex`). A broadcast may use `&` colour codes (up to 256 characters); in game, `/bw game broadcast <message>` does the same.

The **game log** is kept in memory only: a finished game stays readable until the next game in that arena starts, and it is lost when the server restarts. Filter it by chat, kills and beds, game events, joins and leaves, and admin actions.

Commands typed by anyone in an arena's world (players, spectators, admins in setup or just visiting) are shown under **Admin and commands**, e.g. `/bw game forcestart` or `/gamemode creative`, also when the command was refused (marked "no permission" when the player lacks the command's permission, "cancelled" when another plugin stopped it). Player commands from `GameLog.IgnoredCommands` (leaving, language, party, private messages, ...) are left out, and login and password commands (`/login`, `/register`, `/changepassword`, ...) are never logged.

## Shop editor

**Shop** (permission `shop.edit`) has three tabs:

- **Items**: every item of `plugins/NewBedwars/data/items.yml`. An item has a material, amount, price and currency, what buying it does (give the item, armor or a tool, buy a team upgrade, or nothing for decoration), its item logic (TNT, fireball, bridge egg, ... and addon logic), enchantments, item flags, an optional permission and "permanent". Under **Texts** you set the display name, the shop icon description and the lore of the bought item for each language, with `&` colour codes and a preview. The texts are stored in the language files (`CustomItemsNames`). A new item's name can't be changed later.
- **GUIs**: the shop and upgrade menus in `data/guis/`. The grid shows every slot of the inventory; header and footer rows (darker) are shown on every page, the rows between them show the home page or the open category. Drag an entry from the palette onto a slot, drag slots to swap them, or select a slot and choose what it shows on the right (arrow keys move, Delete empties). Categories are buttons that open a page with their items in the order you set; the category `home` is the start page.
- **Arenas**: which GUI the item shop and the upgrade shop of each arena open. Only arenas that are waiting can be changed.

Changes apply at once: players see them the next time they open a shop, no `/bw reload`. Items and GUIs that are still used (by a GUI, an arena or `config.yml`) can't be deleted; the page says where they are used. Every save keeps the previous file in `plugins/NewBedwars/data/backups/shop/` (`items/`, `guis/<id>/`, `language/<code>/`, as many as `Backups.Keep`).

References a GUI file already had but that can't be resolved on this server (for example an item that failed to load) are listed at the top and kept unchanged when you save; new references must be valid. Saving a GUI from the panel rewrites its file without the comments it had.

## Commands

Permission `newbedwars.panel.manage` (default: op). The console can always use them.

| Command | Does |
|---|---|
| `/nbwpanel status` | panel address, certificate (self-signed fingerprint, own keystore or plain HTTP), number of accounts, logged-in sessions |
| `/nbwpanel user create <name> <role>` | creates an account and prints its one-time password |
| `/nbwpanel user reset <name>` | new one-time password; the account is logged out |
| `/nbwpanel user delete <name>` | deletes the account and logs it out (not the last admin) |
| `/nbwpanel user list` | all accounts with their role; marks passwords that were not changed yet |

Account names have 3 to 32 characters: letters, digits, `_`, `.` and `-`. They are not case-sensitive.

The chat and console messages are in `plugins/NewBedwars-Panel/lang/en.yml` and `pl.yml`. You can edit them. Players get their game language and the console gets `DefaultLanguage`.

## Configuration

`plugins/NewBedwars-Panel/config.yml`:

| Key | Default | Meaning |
|---|---|---|
| `Bind` | `0.0.0.0` | Network address to listen on. `0.0.0.0` = all; `127.0.0.1` = only this machine (use it behind a reverse proxy on the same machine) |
| `Port` | `8150` | TCP port |
| `Https.Enabled` | `true` | `false` = plain HTTP. Only use it behind a reverse proxy that adds HTTPS, or on a trusted network. The console warns about it |
| `Https.Keystore` | `""` | Your own PKCS12 (`.p12`/`.pfx`) or JKS keystore, relative to the panel folder. Empty = self-signed certificate |
| `Https.KeystorePassword` | `""` | Its password |
| `Https.Hostnames` | `[]` | Extra domain names / IPs for the self-signed certificate (`localhost` and the `Bind` address are always included) |
| `TrustProxyHeaders` | `false` | `true` only behind your own reverse proxy: the client IP is taken from `X-Forwarded-For`, HTTPS from `X-Forwarded-Proto` and the host from `X-Forwarded-Host` |
| `Session.IdleMinutes` | `30` | Logged out after this long without activity |
| `Session.MaxHours` | `12` | Logged out after this long in any case |
| `Login.MaxAttempts` | `5` | Wrong passwords before the account **and** the address are locked |
| `Login.LockMinutes` | `10` | How long the lock lasts |
| `Upload.MaxMegabytes` | `512` | Largest arena zip that can be uploaded |
| `DefaultLanguage` | `en` | `en` or `pl`, used when the browser or the console has no other choice |
| `GameLog.IgnoredCommands` | player commands (`lobby`, `lang`, `party`, `msg`, `bw lobby`, ...) | Commands typed in an arena world that are **not** shown in the game log. A two-word entry like `bw lobby` leaves out only that subcommand |

## Using your own certificate

If you already have a certificate for a domain that points to your server (for example from Let's Encrypt), turn it into a PKCS12 file:

```bash
openssl pkcs12 -export -in fullchain.pem -inkey privkey.pem -out panel.p12 -name panel -passout pass:CHANGE_ME
```

Put `panel.p12` into `plugins/NewBedwars-Panel/` and set:

```yaml
Https:
  Enabled: true
  Keystore: "panel.p12"
  KeystorePassword: "CHANGE_ME"
```

Restart the server and open `https://your.domain:8150/`. Let's Encrypt certificates last 90 days: create the `.p12` again after each renewal and restart. If that is too much work, use a reverse proxy instead. With a real certificate the panel also sends `Strict-Transport-Security`.

## Behind a reverse proxy (nginx, Caddy)

A reverse proxy on the same machine can give the panel a normal `https://panel.example.com` address with an automatically renewed certificate. Let the panel listen only locally, over plain HTTP:

```yaml
Bind: 127.0.0.1
Port: 8150
Https:
  Enabled: false
TrustProxyHeaders: true
```

nginx:

```nginx
server {
    listen 443 ssl;
    server_name panel.example.com;
    # ssl_certificate / ssl_certificate_key as usual

    client_max_body_size 512m;   # at least Upload.MaxMegabytes

    location / {
        proxy_pass http://127.0.0.1:8150;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $host;
        proxy_read_timeout 600s;   # exports and imports of big maps can take a while
    }
}
```

Caddy:

```
panel.example.com {
    reverse_proxy 127.0.0.1:8150
}
```

Caddy sets the forwarding headers itself.

> [!WARNING]
> Turn on `TrustProxyHeaders` only when the panel port can't be reached directly (`Bind: 127.0.0.1`, or a firewall). Otherwise anyone can fake their IP address and get around the login lock.

Set `X-Forwarded-For` to the client address (`$remote_addr`), not by appending to an incoming header. The panel uses the last address in the header.

## Network mode

The panel works on any NewBedwars server, but it only controls the server it is installed on.

- **Game servers** (`Network.Mode: GAME`) and single servers: full arena list and all actions.
- **Lobby servers** (`Network.Mode: LOBBY`) have no arenas of their own. The dashboard lists the arenas reported by the game servers (read-only), and arena actions are refused.

To manage the arenas of a game server, install the panel there too. Each server has its own accounts. If several servers run on one machine, give each a different `Port`.

## Files and backups

Everything is in `plugins/NewBedwars-Panel/`:

| File | Contents |
|---|---|
| `config.yml` | settings |
| `accounts.dat` | accounts and roles, encrypted (AES-GCM). Passwords are stored only as PBKDF2 hashes |
| `panel.key` | the 32-byte key for `accounts.dat` (owner read/write only, where the system supports it) |
| `selfsigned.p12` | the self-signed certificate and its private key |
| `audit.log` | the activity log, one JSON line per action (moved to `audit.log.1` at server start once it is over 5 MB) |
| `lang/en.yml`, `lang/pl.yml` | command messages |

Back up `accounts.dat` and `panel.key` together. One without the other is useless. A lost `panel.key` means the accounts can't be read: delete `accounts.dat` and create the admin again with `nbwpanel user create`.

Instead of the file, the key can come from the environment variable `NBW_PANEL_KEY` (32 bytes, Base64, e.g. `openssl rand -base64 32`). The key then never lies next to the data. `panel.key` is not created or used while the variable is set.

Shop editor saves keep the previous version of `items.yml`, the GUI files and the language files in `plugins/NewBedwars/data/backups/shop/`.

Exports and uploaded imports are kept in `plugins/NewBedwars/data/exports/`, the same folder as `/bw arena export` and `/bw arena import`. Arena backups are in `plugins/NewBedwars/data/backups/`.

## Security notes

- Keep HTTPS on, or put a reverse proxy with HTTPS in front. Over plain HTTP, passwords and session cookies can be read on the network.
- If only you need the panel, bind it to `127.0.0.1` and reach it through an SSH tunnel: `ssh -L 8150:127.0.0.1:8150 you@server`, then open `https://localhost:8150/`.
- Give people the smallest role that does the job. `staff` can only look.
- Sessions use an `HttpOnly`, `SameSite=Strict` cookie (`__Host-nbw` over HTTPS). Every change needs a CSRF token, and requests from other websites are refused.
- The page itself loads nothing from other sites (no CDN, no fonts, no tracking) and forbids being embedded in frames.
- Check **Activity** (or `audit.log`) now and then. Failed and locked logins are recorded with the IP address.

## Troubleshooting

| Problem | Fix |
|---|---|
| `port 8150 on 0.0.0.0 is already in use` | Another program (or a second server) uses the port: change `Port` |
| `This Java runtime has no built-in web server` | Run the server with a full JDK/JRE (e.g. Temurin) |
| `accounts.dat can not be read with this key` | `panel.key` / `NBW_PANEL_KEY` is not the key the file was written with. Restore the matching key, or delete `accounts.dat` and create the accounts again |
| The browser can't connect | Open the port in the firewall / hosting panel; check `Bind`; use `https://`, not `http://` |
| "Too many wrong tries" | Wait `Login.LockMinutes`, or reset the password from the console |
| Upload fails with "larger than the upload limit" | Raise `Upload.MaxMegabytes` (and `client_max_body_size` in nginx) |
| Uploads fail behind a reverse proxy but work directly | The proxy's body size limit or timeout is too low (see the nginx example) |
