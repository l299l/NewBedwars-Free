import { api, busy, can, errorText, formatDuration, formatUptime, h, isGateway, t, toast } from '../app.js';
import { refresh, watch } from '../live.js';

export function bedStrip(teams) {
  return h('ul', { class: 'beds', 'aria-label': t('arena.teams') },
    teams.map(team => {
      const status = !team.alive ? t('arena.out') : team.bed ? t('arena.bedAlive') : t('arena.bedGone');
      const bed = h('li', { class: 'bed' + (team.bed ? '' : ' is-broken') + (team.alive ? '' : ' is-out'), title: team.name + ': ' + status },
        h('span', { class: 'sr-only' }, team.name + ': ' + status));
      bed.style.setProperty('--team', team.color);
      return bed;
    }));
}

export function stateBadge(state, enabled = true) {
  if (!enabled) return h('span', { class: 'badge badge-off' }, t('arena.disabled'));
  return h('span', { class: 'badge badge-' + state.toLowerCase() }, t('state.' + state));
}

function serverStrip(server) {
  const tps = server.tps && server.tps.length ? server.tps.join('  ') : 'n/a';
  const low = server.tps && server.tps.length && server.tps[0] < 18;
  const item = (label, value, extra) => h('div', { class: 'stat' + (extra || '') }, h('dt', {}, label), h('dd', {}, value));
  return h('dl', { class: 'server' },
    item(t('dash.players'), server.online + ' / ' + server.maxPlayers),
    item(t('dash.tps'), tps, low ? ' is-warn' : ''),
    item(t('dash.memory'), server.memoryUsedMb + ' / ' + server.memoryMaxMb + ' MB'),
    item(t('dash.uptime'), formatUptime((Date.now() - server.startedAt) / 1000)),
    item(t('dash.versions'), 'NewBedwars ' + server.plugin + (server.serverId ? ' (' + server.serverId + ')' : '')));
}

export function phaseText(arena) {
  const parts = [];
  if (arena.phase) parts.push(arena.phase + (arena.phaseTimeLeft >= 0 ? ' ' + formatDuration(arena.phaseTimeLeft) : ''));
  parts.push(t('arena.timeShort', { time: formatDuration(arena.gameTime) }));
  return parts.join(', ');
}

function arenaRow(arena, serverId, awayStatus) {
  const away = !!awayStatus;
  const players = away ? '– / ' + arena.maxPlayers : arena.players + ' / ' + arena.maxPlayers + (arena.spectators ? ' +' + arena.spectators : '');
  return h('li', { class: 'arena' + (arena.enabled && !away ? '' : ' is-disabled') },
    h('a', { class: 'arena-link', href: '#/arena/' + (serverId ? encodeURIComponent(serverId) + '/' : '') + encodeURIComponent(arena.name) },
      h('span', { class: 'arena-name' }, arena.name,
        serverId ? h('span', { class: 'arena-group' }, serverId) : null,
        arena.group ? h('span', { class: 'arena-group' }, arena.group) : null),
      away ? netBadge(awayStatus) : stateBadge(arena.state, arena.enabled),
      h('span', { class: 'arena-players' }, players),
      h('span', { class: 'arena-phase' }, !away && arena.state === 'PLAYING' ? phaseText(arena) : ''),
      bedStrip(arena.teams)));
}

function networkTable(list) {
  if (!list.length) return null;
  return h('section', { class: 'panel-section' },
    h('h2', {}, t('dash.network')),
    h('p', { class: 'muted' }, t('dash.networkNote')),
    h('table', { class: 'table' },
      h('tbody', {}, list.map(a => h('tr', {},
        h('td', {}, a.server), h('td', {}, a.name), h('td', {}, a.group || ''),
        h('td', {}, stateBadge(a.state)), h('td', { class: 'num' }, a.players + ' / ' + a.maxPlayers))))));
}

function playersList(players) {
  return h('section', { class: 'panel-section' },
    h('h2', {}, t('dash.online')),
    players.length
      ? h('ul', { class: 'players' }, players.map(p => h('li', {}, p.name,
        p.arena ? h('span', { class: 'muted' }, ' ' + t('dash.inArena', { arena: p.arena })) : null,
        p.server ? h('span', { class: 'muted' }, ' @ ' + p.server) : null)))
      : h('p', { class: 'muted' }, t('dash.nobody')));
}

function netBadge(status) {
  return h('span', { class: 'badge badge-net-' + status.toLowerCase() }, t('net.status.' + status));
}

function serversTable(net) {
  const rows = net.servers.map(s => {
    const d = s.data;
    const live = d && s.status === 'ONLINE';
    const tps = live && d.server.tps && d.server.tps.length ? String(d.server.tps[0]) : '';
    const low = live && d.server.tps && d.server.tps.length && d.server.tps[0] < 18;
    return h('tr', {},
      h('td', {}, s.id, s.local ? h('span', { class: 'muted' }, ' (' + t('net.thisServer') + ')') : null,
        s.url ? h('div', { class: 'muted mono small' }, s.url) : null),
      h('td', {}, netBadge(s.status)),
      h('td', { class: 'num' }, d ? (s.status === 'ONLINE' ? d.server.online : '–') + ' / ' + d.server.maxPlayers : ''),
      h('td', { class: 'num' + (low ? ' is-warn' : '') }, tps),
      h('td', { class: 'num' }, d ? String(d.arenas.length) : s.networkArenas ? String(s.networkArenas.length) : ''),
      h('td', { class: 'hide-narrow muted' }, d ? 'NewBedwars ' + d.server.plugin : ''));
  });
  return h('section', { class: 'panel-section' },
    h('h2', {}, t('net.servers')),
    h('div', { class: 'table-wrap' }, h('table', { class: 'table' },
      h('thead', {}, h('tr', {}, h('th', {}, t('net.server')), h('th', {}, t('net.state')), h('th', { class: 'num' }, t('dash.players')),
        h('th', { class: 'num' }, 'TPS'), h('th', { class: 'num' }, t('net.arenas')), h('th', { class: 'hide-narrow' }, t('dash.versions')))),
      h('tbody', {}, rows))));
}

function importForm() {
  const target = h('select', { id: 'import-server' });
  const file = h('input', { type: 'file', accept: '.zip,application/zip', id: 'import-file', required: true });
  const submit = h('button', { class: 'btn', type: 'submit' }, t('dash.importButton'));
  const form = h('form', { class: 'import' },
    h('label', { for: 'import-file' }, t('dash.importFile')), file,
    isGateway() ? [h('label', { for: 'import-server' }, t('net.importTo')), target] : null, submit);
  if (isGateway()) {
    api('GET', '/api/network').then(data => {
      target.replaceChildren(...data.servers.filter(s => s.local || s.status === 'ONLINE').map(s =>
        h('option', { value: s.local ? '' : s.id }, s.id + (s.local ? ' (' + t('net.thisServer') + ')' : ''))));
    }).catch(() => {});
  }
  form.addEventListener('submit', async e => {
    e.preventDefault();
    const chosen = file.files[0];
    if (!chosen) return;
    await busy(submit, async () => {
      try {
        const result = await api('POST', '/api/import?name=' + encodeURIComponent(chosen.name), chosen, { server: isGateway() && target.value ? target.value : null });
        if (result.ok) toast(t('dash.imported', { arena: result.arena || chosen.name }));
        else toast(result.message || result.code, 'error');
        form.reset();
        refresh();
      } catch (err) {
        toast(errorText(err), 'error');
      }
    });
  });
  return h('details', { class: 'panel-section disclosure' },
    h('summary', {}, t('dash.import')),
    h('p', { class: 'muted' }, t('dash.importHint')), form);
}

export function render(root) {
  const serverBox = h('div', { class: 'server-box' }, h('p', { class: 'muted' }, t('dash.loading')));
  const notice = h('p', { class: 'notice notice-warn', hidden: true }, t('dash.stale'));
  const arenas = h('div');
  const network = h('div');
  const players = h('div');
  root.append(...[h('h1', {}, t('dash.title')), notice, serverBox, arenas, can('arena.import') ? importForm() : null, network, players].filter(Boolean));

  return watch((data, failed, net) => {
    notice.hidden = !failed;
    if (net) {
      serverBox.replaceChildren(serversTable(net));
      const groups = net.servers.filter(s => s.data);
      arenas.replaceChildren(groups.some(s => s.data.arenas.length)
        ? h('ul', { class: 'arenas' }, groups.flatMap(s => s.data.arenas.map(a => arenaRow(a, s.local ? null : s.id, s.status !== 'ONLINE' ? s.status : null))))
        : h('p', { class: 'empty' }, t('dash.noArenas')));
      network.replaceChildren(networkTable(net.servers.flatMap(s => s.networkArenas || [])) || '');
      players.replaceChildren(playersList(groups.filter(s => s.status === 'ONLINE').flatMap(s => s.data.players.map(p => s.local ? p : { ...p, server: s.id }))));
      return;
    }
    if (!data) return;
    serverBox.replaceChildren(serverStrip(data.server));
    arenas.replaceChildren(data.arenas.length
      ? h('ul', { class: 'arenas' }, data.arenas.map(a => arenaRow(a, null, false)))
      : h('p', { class: 'empty' }, t('dash.noArenas')));
    network.replaceChildren(networkTable(data.network) || '');
    players.replaceChildren(playersList(data.players));
  });
}
