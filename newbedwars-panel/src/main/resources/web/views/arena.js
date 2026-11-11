import { api, busy, can, confirmDialog, errorText, formatDuration, go, h, t, toast } from '../app.js';
import { refresh, watch } from '../live.js';
import { bedStrip, stateBadge } from './dashboard.js';
import { createLog } from './gamelog.js';

const path = (name, action) => '/api/arenas/' + encodeURIComponent(name) + '/' + action;

function report(result) {
  toast(result.message || (result.ok ? t('common.done') : result.code), result.ok ? 'ok' : 'error');
  return result;
}

async function run(button, name, action, body) {
  return busy(button, async () => {
    try {
      const result = report(await api('POST', path(name, action), body || {}));
      refresh();
      return result;
    } catch (err) {
      toast(errorText(err), 'error');
      return null;
    }
  });
}

function facts(arena) {
  const row = (label, value) => value === null || value === '' ? null : h('div', { class: 'stat' }, h('dt', {}, label), h('dd', {}, value));
  return h('dl', { class: 'server' },
    row(t('arena.players'), arena.players + ' / ' + arena.maxPlayers),
    row(t('arena.spectators'), String(arena.spectators)),
    row(t('arena.phase'), arena.state === 'PLAYING' ? (arena.phase || '') + (arena.phaseTimeLeft >= 0 ? ' (' + formatDuration(arena.phaseTimeLeft) + ')' : '') : null),
    row(t('arena.nextPhase'), arena.state === 'PLAYING' && arena.nextPhase ? arena.nextPhase : null),
    row(t('arena.time'), arena.state === 'PLAYING' ? formatDuration(arena.gameTime) : null),
    row(t('arena.group'), arena.group || null));
}

function teams(arena) {
  return h('table', { class: 'table teams' },
    h('thead', {}, h('tr', {}, h('th', {}, t('arena.teams')), h('th', {}, t('arena.bed')), h('th', { class: 'num' }, t('arena.players')))),
    h('tbody', {}, arena.teams.map(team => {
      const swatch = h('span', { class: 'swatch', 'aria-hidden': 'true' });
      swatch.style.setProperty('--team', team.color);
      const status = !team.alive ? t('arena.out') : team.bed ? t('arena.bedAlive') : t('arena.bedGone');
      return h('tr', { class: team.alive ? '' : 'is-out' },
        h('td', {}, swatch, team.name), h('td', {}, status), h('td', { class: 'num' }, String(team.players)));
    })));
}

function actions(arena) {
  const name = arena.name;
  const buttons = [];
  const add = (permission, key, handler, cls = 'btn') => {
    if (!can(permission)) return;
    const button = h('button', { class: cls, type: 'button' }, t(key));
    button.addEventListener('click', () => handler(button));
    buttons.push(button);
  };
  if (arena.enabled) {
    add('arena.disable', 'arena.disable', async b => {
      if (await confirmDialog(t('arena.disableConfirm', { arena: name }))) run(b, name, 'disable');
    });
  } else {
    add('arena.enable', 'arena.enable', b => run(b, name, 'enable'), 'btn btn-primary');
  }
  add('arena.backup', 'arena.backup', async b => { await run(b, name, 'backup'); loadBackups(name); });
  add('arena.resetmap', 'arena.resetmap', async b => {
    if (await confirmDialog(t('arena.resetConfirm', { arena: name }))) run(b, name, 'resetmap');
  });
  add('arena.export', 'arena.export', async b => {
    const result = await run(b, name, 'export');
    if (result && result.ok && result.detail) showDownload(result.detail);
  });
  add('arena.delete', 'arena.delete', async b => {
    if (await confirmDialog(t('arena.deleteConfirm', { arena: name }), { typed: name, danger: true, confirm: t('arena.delete') })) {
      const result = await run(b, name, 'delete');
      if (result && (result.ok || result.code === 'PARTIAL')) go('dashboard');
    }
  }, 'btn btn-danger');
  return buttons;
}

const GAME_ACTIONS = {
  WAITING: ['start', 'forcestart'],
  STARTING: ['forcestart', 'forcestop'],
  PLAYING: ['nextphase', 'stop', 'forcestop'],
  ENDING: ['forcestop'],
};
const CONFIRM = { stop: 'game.stopConfirm', forcestop: 'game.forcestopConfirm' };

function gameSection(arena) {
  if (!can('game.manage') || !arena.enabled) return null;
  const name = arena.name;
  const buttons = (GAME_ACTIONS[arena.state] || []).map(action => {
    const danger = action === 'stop' || action === 'forcestop';
    const button = h('button', { class: 'btn' + (danger ? ' btn-danger' : action === 'start' || action === 'forcestart' ? ' btn-primary' : ''), type: 'button' }, t('game.' + action));
    button.addEventListener('click', async () => {
      if (CONFIRM[action] && !await confirmDialog(t(CONFIRM[action], { arena: name }), { danger: true, confirm: t('game.' + action) })) return;
      run(button, name, 'game/' + action);
    });
    return button;
  });
  const message = h('input', { id: 'broadcast-text', maxlength: '256', autocomplete: 'off', placeholder: t('game.broadcastHint') });
  const send = h('button', { class: 'btn', type: 'submit' }, t('game.broadcast'));
  const form = h('form', { class: 'inline-form broadcast' }, h('label', { for: 'broadcast-text' }, t('game.broadcastLabel')), message, send);
  form.addEventListener('submit', async e => {
    e.preventDefault();
    if (!message.value.trim()) return;
    const result = await run(send, name, 'broadcast', { message: message.value });
    if (result && result.ok) message.value = '';
  });
  return h('section', { class: 'panel-section' }, h('h2', {}, t('game.title')),
    buttons.length ? h('div', { class: 'actions' }, buttons) : h('p', { class: 'muted' }, t('game.noActions')),
    form);
}

let downloadBox = null;
let backupBox = null;

function showDownload(file) {
  if (!downloadBox) return;
  downloadBox.replaceChildren(h('a', { class: 'btn btn-primary', href: '/api/exports/' + encodeURIComponent(file), download: file }, t('arena.download', { file })));
}

function copyForm(name) {
  if (!can('arena.copy')) return null;
  const input = h('input', { id: 'copy-name', required: true, maxlength: '64', autocomplete: 'off', spellcheck: 'false' });
  const submit = h('button', { class: 'btn', type: 'submit' }, t('arena.copy'));
  const form = h('form', { class: 'inline-form' }, h('label', { for: 'copy-name' }, t('arena.copyName')), input, submit);
  form.addEventListener('submit', async e => {
    e.preventDefault();
    const result = await run(submit, name, 'copy', { newName: input.value.trim() });
    if (result && result.ok) form.reset();
  });
  return form;
}

async function loadBackups(name) {
  if (!backupBox || !(can('arena.restore') || can('arena.backup'))) return;
  try {
    const data = await api('GET', path(name, 'backups'));
    if (!data.backups.length) {
      backupBox.replaceChildren(h('p', { class: 'muted' }, t('arena.noBackups')));
      return;
    }
    backupBox.replaceChildren(h('table', { class: 'table' }, h('tbody', {}, data.backups.map(backup => {
      const contents = [backup.arenaFile ? t('arena.contents.file') : null, backup.map ? t('arena.contents.map') : null].filter(Boolean).join(', ');
      let cell = null;
      if (can('arena.restore')) {
        const button = h('button', { class: 'btn btn-small', type: 'button' }, t('arena.restore'));
        button.addEventListener('click', async () => {
          if (await confirmDialog(t('arena.restoreConfirm', { arena: name, id: backup.id }))) run(button, name, 'restore', { backup: backup.id });
        });
        cell = button;
      }
      return h('tr', {}, h('td', { class: 'mono' }, backup.id), h('td', { class: 'muted' }, contents), h('td', { class: 'num' }, cell));
    }))));
  } catch (err) {
    backupBox.replaceChildren(h('p', { class: 'form-error' }, errorText(err)));
  }
}

export function render(root, [name]) {
  const header = h('div', { class: 'arena-head' });
  const body = h('div');
  downloadBox = h('div', { class: 'download' });
  backupBox = h('div');
  let shownKey = null;
  const log = can('game.log') ? createLog(name) : null;
  root.append(h('a', { href: '#/dashboard', class: 'back' }, t('arena.back')), header, body);

  const draw = arena => {
    header.replaceChildren(h('h1', {}, arena.name), stateBadge(arena.state, arena.enabled), bedStrip(arena.teams));
    const actionKey = arena.enabled + '|' + arena.state;
    const live = [facts(arena),
      arena.problems.length ? h('section', { class: 'notice notice-warn' }, h('h2', {}, t('arena.problems')), h('ul', {}, arena.problems.map(p => h('li', {}, p)))) : null,
      teams(arena)];
    if (shownKey !== actionKey) {
      shownKey = actionKey;
      const buttons = actions(arena);
      const copy = copyForm(arena.name);
      body.replaceChildren(h('div', { class: 'live' }, live),
        gameSection(arena),
        log ? log.element : null,
        buttons.length || copy ? h('section', { class: 'panel-section' }, h('h2', {}, t('arena.actions')), h('div', { class: 'actions' }, buttons), downloadBox, copy) : null,
        can('arena.restore') || can('arena.backup') ? h('section', { class: 'panel-section' }, h('h2', {}, t('arena.backups')), backupBox) : null);
    } else {
      body.querySelector('.live').replaceChildren(...live.filter(Boolean));
    }
  };

  const stop = watch(data => {
    if (!data) return;
    const arena = data.arenas.find(a => a.name.toLowerCase() === String(name).toLowerCase());
    if (!arena) {
      shownKey = null;
      header.replaceChildren(h('h1', {}, name));
      body.replaceChildren(h('p', { class: 'empty' }, t('arena.notFound')));
      return;
    }
    const first = shownKey === null;
    draw(arena);
    if (first) loadBackups(arena.name);
  });
  return () => { stop(); if (log) log.stop(); downloadBox = null; backupBox = null; };
}
