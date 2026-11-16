# Web panel (NewBedwars-Panel)

> [!NOTE]
> **New in 2.7-beta.** The panel needs NewBedwars 2.7-beta or newer on the same server.

`NewBedwars-Panel` is a separate addon jar that adds a browser admin panel to your server. With it you can:

- watch every arena live: its state, players, phase (with the time left and the next phase), game time, and each team's bed;
- control running games: start the countdown, start now, end as a draw, stop and reset, skip to the next phase, and send a message to everyone in the arena;
- read each arena's game log: joins and leaves, chat and team chat, kills and final kills, beds, phases, generator upgrades, eliminated teams and admin actions, updated live;
- look up any player's stats and the leaderboards (top 10, 25 or 100 by wins, kills, final kills, beds, deaths, losses or games);
- see online players, TPS and memory;
- enable, disable, back up, restore, reset, copy, export (download) and delete arenas;
- import arena zips you upload;
- on a network, manage the arenas, shops and stats of every game server from the lobby's panel with one login ([network mode](#network-mode)).

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
| `staff` | view the arenas and players and the player stats, also on the other servers of the network (`dashboard.view`, `stats.view`, `network.servers`) |

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
| `network.servers` | on a gateway panel: the server list and everything on the other servers of the network (what you may do there still depends on your other permissions). Never forwarded to other servers |
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

**Shop** (permission `shop.edit`) has four tabs:

- **Items**: every item of `plugins/NewBedwars/data/items.yml`. An item has a material, amount, price and currency, what buying it does (give the item, armor or a tool, buy a team upgrade, or nothing for decoration), its item logic (TNT, fireball, bridge egg, ... and addon logic), enchantments, item flags, an optional permission and "permanent". Under **Texts** you set the display name, the shop icon description and the lore of the bought item for each language, with `&` colour codes and a preview. The texts are stored in the language files (`CustomItemsNames`). A new item's name can't be changed later.
- **GUIs**: the shop and upgrade menus in `data/guis/`. The grid shows every slot of the inventory; header and footer rows (darker) are shown on every page, the rows between them show the home page or the open category. Drag an entry from the palette onto a slot, drag slots to swap them, or select a slot and choose what it shows on the right (arrow keys move, Delete empties). Categories are buttons that open a page with their items in the order you set; the category `home` is the start page. **Price overrides** give an upgrade GUI its own prices for some upgrades: arenas whose upgrade shop opens this GUI use them instead of the prices from the **Upgrades** tab (for example cheaper upgrades for solo arenas).
- **Upgrades**: the prices of every team upgrade and trap (`data/upgrades.yml`). Each row is one level the team can buy, in order, with its own price and currency (iron, gold, diamond or emerald). Fewer rows mean fewer levels, no rows means the upgrade can't be bought; each upgrade shows how many levels it supports at most. Upgrade icons in the **Items** tab have no price of their own: buying them always uses these prices.
- **Arenas**: which GUI the item shop and the upgrade shop of each arena open. Only arenas that are waiting can be changed.

Changes apply at once: players see them the next time they open a shop, no `/bw reload`. Items and GUIs that are still used (by a GUI, an arena or `config.yml`) can't be deleted; the page says where they are used. Every save keeps the previous file in `plugins/NewBedwars/data/backups/shop/` (`items/`, `guis/<id>/`, `upgrades/`, `language/<code>/`, as many as `Backups.Keep`).

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
| `/nbwpanel network status` | the panel's network role and, on a gateway, every server with its status and how its certificate is trusted |
| `/nbwpanel network secret` | prints a new random `Network.Secret` (it is not saved anywhere) |
| `/nbwpanel network forget <server>` | gateway: forgets the remembered certificate of a server, so the next connection trusts the certificate it sees |

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
| `Network.Role` | `STANDALONE` | `STANDALONE`, `GATEWAY` or `NODE`, see [Network mode](#network-mode) |
| `Network.Id` | `""` | Name of this server in the network (letters, digits, `_ -`). Empty = the NewBedwars server id. On a node it must match the node's key under `Network.Peers` on the gateway. Shown as `user@<id>` in the activity logs of the other servers |
| `Network.Secret` | `""` | Shared secret, at least 32 characters, the same on the gateway and every node |
| `Network.NodeLogin` | `false` | `NODE` only: `true` also allows logging in to this node directly with its own accounts |
| `Network.Peers` | `{}` | `GATEWAY` only: the other panels (`<server id>: {Url, Fingerprint}`) |
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

By default a panel only controls the server it is installed on. On a network (see [network.md](network.md)) you can instead log in **once**, on the lobby server's panel, and work with every game server from there.

- **Lobby server (the gateway)**: `Network.Role: GATEWAY`. Its dashboard shows every server (status, players, TPS, arenas) and one arena list for all of them. Arena pages, the game log, the shop editor, stats and arena import/export work for any server, picked by the server tag, the arena link or the **Server** selector.
- **Game servers (the nodes)**: `Network.Role: NODE`. A node answers only its gateway. It has no login of its own unless you set `Network.NodeLogin: true`.
- In the shop editor, the **Sync items**, **Sync GUIs** and **Sync upgrade prices** buttons (Items, GUIs and Upgrades tabs) copy the items, GUIs or upgrade prices of the selected server to every other online server. They only run when pressed; same-named entries are overwritten and entries that exist only on the other servers are kept. Sync the items before the GUIs.
- Servers that do not answer are shown as offline on the dashboard (their arenas get an offline badge and the player count shows `–`).
- Everything else (accounts, roles, the activity log) stays on the gateway. People can't use it to change the accounts of a node.
- On a lobby server (`Network.Mode: LOBBY`) the dashboard also lists arenas of servers without a panel link, read-only, as "Not connected".

Without a `Network.Role` (or with `STANDALONE`) nothing changes: one panel per server with its own accounts, as before.

### Setting it up

1. Install `NewBedwars-Panel` on the lobby server and on every game server. The panel is a Paper plugin only: nothing is installed on the Velocity proxy, and the panels talk to each other directly over HTTPS, not through the proxy. Each panel needs its own `Port` if they share a machine (for example 8150, 8151, 8152), and the gateway must be able to reach the nodes' ports. Keep node ports closed to everyone else (firewall, private network).
2. Make a secret: run `/nbwpanel network secret` on any server and copy the result.
3. On every game server, in `plugins/NewBedwars-Panel/config.yml`:

```yaml
Network:
  Role: NODE
  Secret: "<the secret>"
```

4. On the lobby server:

```yaml
Network:
  Role: GATEWAY
  Secret: "<the same secret>"
  Peers:
    game1:
      Url: "https://10.0.0.5:8151"
      Fingerprint: ""
    game2:
      Url: "https://10.0.0.6:8151"
      Fingerprint: ""
```

The keys under `Peers` are the server ids the proxy uses (the same names shown in `/bw join` on the lobby), and must be the same as each node's own id (its `Network.Id`, or its NewBedwars server id when `Network.Id` is empty). A node refuses requests meant for another server and says so in its console. The dashboard marks servers in the network list that have no `Peers` entry as "Not connected".

5. Restart the servers (panel changes need a restart), create the accounts on the **gateway** (`/nbwpanel user create <name> <role>`), and give roles that should reach other servers the `network.servers` permission. Check with `/nbwpanel network status`.

### How the servers trust each other

- **The secret.** Every request from the gateway to a node is signed with the secret (HMAC-SHA256) over the request, the time, a one-time number, the user, the user's permissions, the user's IP address, the target server and the body. A node refuses anything unsigned, changed, older or newer than a minute, or seen before. Without the secret nobody can talk to a node's panel, even if its port is open. Keep it as private as a database password, and use a different one per network.
- **Permissions.** The gateway checks what the user may do, and sends the permissions along; the node checks them again. `users.manage`, `roles.manage`, `audit.view` and `network.servers` are never accepted by a node.
- **Activity log.** The node records every action as `name@<gateway id>` with the user's IP address, in its own `audit.log`.
- **Certificates.** Panels use self-signed certificates by default, so the gateway does not use the normal certificate check for nodes. Instead it remembers the certificate it sees the **first time** (`peers.json`, shown by `/nbwpanel network status`) and refuses the server if the certificate ever changes (the server then shows "Certificate changed"). After a change you made on purpose run `/nbwpanel network forget <server>`. To skip trust-on-first-use, put the node's SHA-256 fingerprint (shown in its console at start, or by `/nbwpanel status`) in `Fingerprint`. A node with a normal CA-signed certificate (for example Let's Encrypt behind a reverse proxy) can use `Fingerprint: system`.
- **Plain HTTP.** `http://` URLs work but anything on the way can read the traffic (the secret itself is never sent), and uploaded arena files are not covered by the signature. The gateway warns about it at start. Use them only when both panels are on the same machine or a trusted private network.
- **Uploads.** Arena zips uploaded to another server are streamed straight through the gateway; their content is not covered by the signature (the user, the target and the time are).
- **Clocks.** The two servers' clocks must be within one minute of each other, or the node answers "rejected".

### Status values

| Status | Meaning |
|---|---|
| Online | answers |
| Offline | not reachable or no answer in time (the page shows the last known state in grey) |
| Rejected | the node refused the gateway: wrong `Network.Secret`, clocks too far apart, its `Peers` key differs from the node's id, or the node's panel is not a `NODE` |
| Certificate changed | the certificate differs from the remembered one, see above |
| Not connected | the server is in the network list but has no `Peers` entry |

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

Shop editor saves keep the previous version of `items.yml`, `upgrades.yml`, the GUI files and the language files in `plugins/NewBedwars/data/backups/shop/`.

Exports and uploaded imports are kept in `plugins/NewBedwars/data/exports/`, the same folder as `/bw arena export` and `/bw arena import`. Arena backups are in `plugins/NewBedwars/data/backups/`.

## Security notes

- Keep HTTPS on, or put a reverse proxy with HTTPS in front. Over plain HTTP, passwords and session cookies can be read on the network.
- If only you need the panel, bind it to `127.0.0.1` and reach it through an SSH tunnel: `ssh -L 8150:127.0.0.1:8150 you@server`, then open `https://localhost:8150/`.
- Give people the smallest role that does the job. `staff` can only look (it may look at every server of the network).
- Sessions use an `HttpOnly`, `SameSite=Strict` cookie (`__Host-nbw` over HTTPS). Every change needs a CSRF token, and requests from other websites are refused.
- The page itself loads nothing from other sites (no CDN, no fonts, no tracking) and forbids being embedded in frames.
- On a network, the secret is the key to every game server's panel. Keep `config.yml` private and don't expose node ports.
- Check **Activity** (or `audit.log`) now and then. Failed and locked logins are recorded with the IP address.
- `Bind: 0.0.0.0` makes the panel reachable from every network the server is on. If it doesn't have to be public, allow port 8150 only from your own IP in the firewall. The built-in web server has no protection against clients that keep many slow connections open, so a reverse proxy (nginx, Caddy) in front is the better choice for a panel that is open to the internet. The game itself is never slowed down by the panel.
- Enable `TrustProxyHeaders` only behind your own reverse proxy. Otherwise anyone can fake their IP address, and with it the login lock.
- Arena imports (upload and unzip) are limited to `Upload.MaxMegabytes`, 2 GB unpacked and 200 000 files, and files can only be written inside the server's export and map folders.

## Troubleshooting

| Problem | Fix |
|---|---|
| `port 8150 on 0.0.0.0 is already in use` | Another program (or a second server) uses the port: change `Port` |
| `This Java runtime has no built-in web server` | Run the server with a full JDK/JRE (e.g. Temurin) |
| `accounts.dat can not be read with this key` | `panel.key` / `NBW_PANEL_KEY` is not the key the file was written with. Restore the matching key, or delete `accounts.dat` and create the accounts again |
| The browser can't connect | Open the port in the firewall / hosting panel; check `Bind`; use `https://`, not `http://` |
| "Too many wrong tries" | Wait `Login.LockMinutes`, or reset the password from the console |
| Upload fails with "larger than the upload limit" | Raise `Upload.MaxMegabytes` (and `client_max_body_size` in nginx) |
| A server is "Rejected" | Different `Network.Secret`, clocks more than a minute apart, the `Peers` key on the gateway is not the node's id (the node's console names both), or `Network.Role` on that server is not `NODE` (check its console at start) |
| A server is "Certificate changed" | Its certificate was recreated (deleted `selfsigned.p12`, new IP in `Https.Hostnames`, ...). If that was you, run `/nbwpanel network forget <server>` on the gateway |
| A server is "Offline" | Wrong `Url`/port, the node's panel is not running, or a firewall blocks the gateway |
| `Network.Role is GATEWAY but Network.Secret is missing` | Set a `Network.Secret` of 32+ characters on every server; until then the panel runs as a standalone panel |
| Uploads fail behind a reverse proxy but work directly | The proxy's body size limit or timeout is too low (see the nginx example) |
