import { api, h, state, t } from '../app.js';

const INTERVAL = 2000;
const MAX_LINES = 2000;
const GROUPS = {
  chat: ['CHAT', 'TEAM_CHAT'],
  combat: ['KILL', 'FINAL_KILL', 'DEATH', 'BED', 'BEDS_DESTROYED', 'TEAM_OUT'],
  game: ['START', 'END', 'PHASE', 'GENERATOR', 'UPGRADE'],
  players: ['JOIN', 'REJOIN', 'LEAVE'],
  admin: ['ADMIN', 'COMMAND'],
};

function groupOf(type) {
  for (const [group, types] of Object.entries(GROUPS)) if (types.includes(type)) return group;
  return 'game';
}

function colored(text, color) {
  const span = h('span', { class: 'log-name' }, text);
  if (color) span.style.setProperty('--team', color);
  else span.classList.add('no-team');
  return span;
}

function fill(template, values) {
  const nodes = [];
  let rest = template;
  const pattern = /\{(\w+)\}/;
  let match;
  while ((match = pattern.exec(rest))) {
    if (match.index > 0) nodes.push(rest.slice(0, match.index));
    const value = values[match[1]];
    nodes.push(value instanceof Node ? value : String(value ?? ''));
    rest = rest.slice(match.index + match[0].length);
  }
  if (rest) nodes.push(rest);
  return nodes;
}

function label(prefix, value) {
  if (!value) return '';
  const key = prefix + value;
  return key in state.fallback ? t(key) : value.toLowerCase().replace(/_/g, ' ');
}

function lineText(e) {
  const values = {
    player: e.player ? colored(e.player, e.teamColor) : '',
    target: e.target ? colored(e.target, e.targetColor) : '',
    team: e.team ? colored(e.team, e.teamColor) : '',
    detail: e.detail || '',
  };
  let key = 'log.' + e.type;
  if (e.type === 'END' && !e.target) key = 'log.END_DRAW';
  if (e.type === 'PHASE' && e.detail === 'forced') key = 'log.PHASE_FORCED';
  if (e.type === 'DEATH') {
    const final = (e.detail || '').startsWith('FINAL ');
    values.detail = label('cause.', (e.detail || '').replace('FINAL ', ''));
    if (final) key = 'log.FINAL_DEATH';
  }
  if (e.type === 'LEAVE') values.detail = label('leave.', e.detail);
  if (e.type === 'ADMIN') {
    values.target = label('game.', e.target);
    if (e.target === 'broadcast') key = 'log.ADMIN_BROADCAST';
  }
  if (e.type === 'GENERATOR') values.target = label('kind.', e.target);
  if (e.type === 'KILL' || e.type === 'FINAL_KILL') values.detail = label('cause.', e.detail);
  return fill(t(key), values);
}

function clock(millis) {
  return new Date(millis).toLocaleTimeString(state.lang === 'pl' ? 'pl-PL' : 'en-GB', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

export function createLog(arenaName) {
  const shown = new Set(Object.keys(GROUPS));
  const list = h('ol', { class: 'log-list', 'aria-live': 'off' });
  const box = h('div', { class: 'log-box', tabindex: '0', 'aria-label': t('log.title') }, list);
  const empty = h('p', { class: 'muted log-empty' }, t('log.empty'));
  const note = h('p', { class: 'hint', hidden: true }, t('log.truncated'));
  const chips = h('div', { class: 'chips' }, Object.keys(GROUPS).map(group => {
    const chip = h('button', { class: 'chip is-active', type: 'button', 'aria-pressed': 'true' }, t('log.group.' + group));
    chip.addEventListener('click', () => {
      if (shown.has(group)) shown.delete(group); else shown.add(group);
      chip.classList.toggle('is-active', shown.has(group));
      chip.setAttribute('aria-pressed', shown.has(group) ? 'true' : 'false');
      for (const li of list.children) li.hidden = !shown.has(li.dataset.group);
    });
    return chip;
  }));
  const element = h('section', { class: 'panel-section' },
    h('h2', {}, t('log.title')), h('p', { class: 'hint' }, t('log.privacy')), chips, note, empty, box);

  let after = 0;
  let timer = null;
  let stopped = false;

  function append(entries) {
    const atBottom = box.scrollHeight - box.scrollTop - box.clientHeight < 40;
    for (const e of entries) {
      const group = groupOf(e.type);
      const li = h('li', { class: 'log-line log-' + e.type.toLowerCase(), 'data-group': group },
        h('time', { class: 'log-time', datetime: new Date(e.time).toISOString() }, clock(e.time)),
        h('span', { class: 'log-text' }, lineText(e)));
      li.hidden = !shown.has(group);
      list.append(li);
    }
    while (list.children.length > MAX_LINES) list.firstElementChild.remove();
    empty.hidden = list.children.length > 0;
    if (atBottom) box.scrollTop = box.scrollHeight;
  }

  async function tick() {
    timer = null;
    if (stopped) return;
    if (!document.hidden) {
      try {
        const page = await api('GET', '/api/arenas/' + encodeURIComponent(arenaName) + '/log?after=' + after);
        if (page.seq < after) {
          list.replaceChildren();
          after = 0;
        }
        if (page.truncated) note.hidden = false;
        append(page.entries);
        if (page.entries.length) after = page.entries[page.entries.length - 1].seq;
      } catch (e) { }
    }
    if (!stopped) timer = setTimeout(tick, INTERVAL);
  }

  tick();
  return {
    element,
    stop() {
      stopped = true;
      if (timer) clearTimeout(timer);
    },
  };
}
