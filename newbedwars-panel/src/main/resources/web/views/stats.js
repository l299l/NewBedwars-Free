import { api, errorText, h, t } from '../app.js';

const STATS = ['wins', 'kills', 'final_kills', 'beds_broken', 'deaths', 'losses', 'games_played'];
const LIMITS = [10, 25, 100];

function ratio(a, b) {
  return b === 0 ? String(a) : (a / b).toFixed(2);
}

function card(player) {
  const item = (label, value) => h('div', { class: 'stat' }, h('dt', {}, label), h('dd', {}, String(value)));
  return h('section', { class: 'panel-section player-card' },
    h('h2', {}, player.name),
    h('dl', { class: 'server' },
      STATS.map(s => item(t('stat.' + s), player[s])),
      item(t('stat.kd'), ratio(player.kills, player.deaths)),
      item(t('stat.fkd'), ratio(player.final_kills, player.deaths)),
      item(t('stat.winRate'), player.games_played ? Math.round(100 * player.wins / player.games_played) + '%' : '-')));
}

export function render(root) {
  let stat = 'wins';
  let limit = 25;
  let searchTimer = null;
  const tabs = h('div', { class: 'tabs', role: 'tablist' });
  const limits = h('div', { class: 'chips' });
  const table = h('div', { class: 'table-wrap' });
  const playerBox = h('div');
  const suggestions = h('datalist', { id: 'player-names' });
  const search = h('input', { type: 'search', id: 'player-search', list: 'player-names', autocomplete: 'off', spellcheck: 'false', maxlength: '32', placeholder: t('stats.searchHint') });

  async function openPlayer(name) {
    playerBox.replaceChildren(h('p', { class: 'muted' }, t('common.working')));
    try {
      playerBox.replaceChildren(card(await api('GET', '/api/players/' + encodeURIComponent(name))));
      playerBox.scrollIntoView({ block: 'nearest' });
    } catch (err) {
      playerBox.replaceChildren(h('p', { class: 'form-error' }, err.code === 'player_not_found' ? t('stats.notFound', { name }) : errorText(err)));
    }
  }

  function drawTabs() {
    tabs.replaceChildren(...STATS.map(s => {
      const b = h('button', { class: 'tab' + (s === stat ? ' is-active' : ''), type: 'button', role: 'tab', 'aria-selected': s === stat ? 'true' : 'false' }, t('stat.' + s));
      b.addEventListener('click', () => { stat = s; drawTabs(); load(); });
      return b;
    }));
    limits.replaceChildren(...LIMITS.map(n => {
      const b = h('button', { class: 'chip' + (n === limit ? ' is-active' : ''), type: 'button', 'aria-pressed': n === limit ? 'true' : 'false' }, t('stats.top', { n }));
      b.addEventListener('click', () => { limit = n; drawTabs(); load(); });
      return b;
    }));
  }

  async function load() {
    table.replaceChildren(h('p', { class: 'muted' }, t('common.working')));
    try {
      const data = await api('GET', '/api/leaderboard?stat=' + stat.toUpperCase() + '&limit=' + limit);
      if (!data.entries.length) {
        table.replaceChildren(h('p', { class: 'empty' }, t('stats.empty')));
        return;
      }
      table.replaceChildren(h('table', { class: 'table leaderboard' },
        h('thead', {}, h('tr', {}, h('th', { class: 'num' }, '#'), h('th', {}, t('stats.player')),
          h('th', { class: 'num is-sorted' }, t('stat.' + stat)),
          ...STATS.filter(s => s !== stat).slice(0, 4).map(s => h('th', { class: 'num hide-narrow' }, t('stat.' + s))))),
        h('tbody', {}, data.entries.map(e => {
          const link = h('button', { class: 'link-button', type: 'button' }, e.name);
          link.addEventListener('click', () => openPlayer(e.name));
          return h('tr', {}, h('td', { class: 'num muted' }, String(e.rank)), h('td', {}, link),
            h('td', { class: 'num strong' }, String(e[stat])),
            ...STATS.filter(s => s !== stat).slice(0, 4).map(s => h('td', { class: 'num hide-narrow' }, String(e[s]))));
        }))));
    } catch (err) {
      table.replaceChildren(h('p', { class: 'form-error' }, errorText(err)));
    }
  }

  search.addEventListener('input', () => {
    clearTimeout(searchTimer);
    const q = search.value.trim();
    if (!q) { suggestions.replaceChildren(); return; }
    searchTimer = setTimeout(async () => {
      try {
        const data = await api('GET', '/api/players?q=' + encodeURIComponent(q));
        suggestions.replaceChildren(...data.players.map(n => h('option', { value: n })));
      } catch (e) { }
    }, 250);
  });
  const form = h('form', { class: 'inline-form', role: 'search' },
    h('label', { for: 'player-search' }, t('stats.search')), search, suggestions,
    h('button', { class: 'btn', type: 'submit' }, t('stats.show')));
  form.addEventListener('submit', e => {
    e.preventDefault();
    if (search.value.trim()) openPlayer(search.value.trim());
  });

  root.append(h('h1', {}, t('stats.title')),
    h('section', { class: 'panel-section' }, form, playerBox),
    h('section', { class: 'panel-section' }, h('h2', {}, t('stats.leaderboard')), tabs, limits, table));
  drawTabs();
  load();
  return () => clearTimeout(searchTimer);
}
