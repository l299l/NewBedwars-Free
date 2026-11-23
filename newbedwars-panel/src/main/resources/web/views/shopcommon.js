import { api, busy, can, confirmDialog, errorText, h, isGateway, serverPicker, state, t, toast } from '../app.js';

const COLORS = {
  '0': '#000000', '1': '#0000aa', '2': '#00aa00', '3': '#00aaaa', '4': '#aa0000', '5': '#aa00aa', '6': '#ffaa00', '7': '#aaaaaa',
  '8': '#555555', '9': '#5555ff', 'a': '#55ff55', 'b': '#55ffff', 'c': '#ff5555', 'd': '#ff55ff', 'e': '#ffff55', 'f': '#ffffff',
};

const MATERIAL_COLORS = [
  ['LIGHT_BLUE', '#3ab3da'], ['LIGHT_GRAY', '#9d9d97'], ['WHITE', '#e9ecec'], ['ORANGE', '#f07613'], ['MAGENTA', '#bd44b3'],
  ['YELLOW', '#f8c627'], ['LIME', '#70b919'], ['PINK', '#ed8dac'], ['GRAY', '#3e4447'], ['CYAN', '#158991'], ['PURPLE', '#792aac'],
  ['BLUE', '#35399d'], ['BROWN', '#724728'], ['GREEN', '#546d1b'], ['RED', '#a12722'], ['BLACK', '#141519'],
  ['NETHERITE', '#4d494d'], ['DIAMOND', '#4aedd9'], ['EMERALD', '#17dd62'], ['GOLD', '#f6d846'], ['IRON', '#d8d8d8'],
  ['CHAINMAIL', '#8f8f8f'], ['LEATHER', '#a0522d'], ['STONE', '#7f7f7f'], ['WOOD', '#a3824e'], ['OAK', '#a3824e'],
  ['OBSIDIAN', '#1b1429'], ['END_STONE', '#dbde9e'], ['SANDSTONE', '#d8cb9b'], ['TERRACOTTA', '#985e43'], ['GLASS', '#c8e6f0'],
  ['TNT', '#db2f1f'], ['WATER', '#3f76e4'], ['LAVA', '#e2600e'], ['ENDER', '#258474'], ['BOW', '#8b6a3e'], ['ARROW', '#b5b5b5'],
  ['APPLE', '#e0c21f'], ['POTION', '#b04ad6'], ['EGG', '#e9dcb4'], ['FIRE', '#ff8c1a'], ['SHEARS', '#cfcfcf'], ['LADDER', '#9a7b4f'],
  ['BRICK', '#965a46'], ['SPIDER', '#8b2f39'], ['DRAGON', '#2b1d3d'], ['BELL', '#e2b33c'], ['FURNACE', '#6e6e6e'], ['BARRIER', '#d6271d'],
];

export function materialColor(material) {
  const name = String(material || '').toUpperCase();
  for (const [key, color] of MATERIAL_COLORS) if (name.includes(key)) return color;
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) | 0;
  return 'hsl(' + (Math.abs(hash) % 360) + ' 35% 55%)';
}

export function swatch(material) {
  const el = h('span', { class: 'swatch', 'aria-hidden': 'true' });
  el.style.setProperty('--team', materialColor(material));
  return el;
}

export function mcText(text, lineSplit) {
  const box = h('span', { class: 'mc' });
  const lines = lineSplit ? String(text || '').split(lineSplit) : [String(text || '')];
  lines.forEach((line, index) => {
    if (index > 0) box.append(h('br'));
    let color = null, bold = false, italic = false, underline = false, strike = false;
    let buffer = '';
    const flush = () => {
      if (!buffer) return;
      const span = h('span', {}, buffer);
      if (color) span.style.setProperty('color', color);
      if (bold) span.style.setProperty('font-weight', '700');
      if (italic) span.style.setProperty('font-style', 'italic');
      const deco = [underline ? 'underline' : '', strike ? 'line-through' : ''].filter(Boolean).join(' ');
      if (deco) span.style.setProperty('text-decoration', deco);
      box.append(span);
      buffer = '';
    };
    for (let i = 0; i < line.length; i++) {
      const ch = line[i];
      const code = (line[i + 1] || '').toLowerCase();
      if ((ch === '&' || ch === '§') && /[0-9a-fk-or]/.test(code)) {
        flush();
        if (COLORS[code]) { color = COLORS[code]; bold = italic = underline = strike = false; }
        else if (code === 'l') bold = true;
        else if (code === 'o') italic = true;
        else if (code === 'n') underline = true;
        else if (code === 'm') strike = true;
        else if (code === 'r') { color = null; bold = italic = underline = strike = false; }
        i++;
        continue;
      }
      buffer += ch;
    }
    flush();
  });
  return box;
}

let optionsCache = null;

let optionsServer = null;

export async function shopOptions() {
  if (!optionsCache || optionsServer !== state.server) {
    optionsServer = state.server;
    optionsCache = api('GET', '/api/shop/options').catch(err => { optionsCache = null; throw err; });
  }
  return optionsCache;
}

export function showResult(result, okKey) {
  if (result.ok) {
    toast(t(okKey));
    return true;
  }
  const text = t('shop.result.' + result.code);
  toast(result.detail ? text + ': ' + result.detail : text, 'error');
  return false;
}

export function usageText(usage) {
  const [kind, ...rest] = String(usage).split(':');
  const name = rest.join(':');
  return t('shop.usedBy.' + kind, { name });
}

export function shopTabs(active) {
  const tabs = [['items', 'shop.tab.items'], ['guis', 'shop.tab.guis'], ['upgrades', 'shop.tab.upgrades'], ['arenas', 'shop.tab.arenas']];
  if (can('modes.view')) tabs.push(['modes', 'shop.tab.modes']);
  return h('div', {}, serverPicker(), h('nav', { class: 'tabs', 'aria-label': t('nav.shop') },
    tabs.map(([route, key]) => h('a', {
      href: '#/shop/' + route, class: 'tab' + (route === active ? ' is-active' : ''),
      'aria-current': route === active ? 'page' : null,
    }, t(key)))));
}

export function select(id, values, selected, labels = {}) {
  const list = [...values];
  if (selected && !list.includes(selected)) list.unshift(selected);
  return h('select', { id }, list.map(v => h('option', { value: v, selected: v === selected }, labels[v] || v)));
}

export function datalist(id, values) {
  return h('datalist', { id }, values.map(v => h('option', { value: v })));
}

async function pushItems(source, target) {
  const list = (await api('GET', '/api/shop/items', undefined, { server: source })).items;
  let failed = 0;
  for (const item of list) {
    try {
      const detail = await api('GET', '/api/shop/items/' + encodeURIComponent(item.name), undefined, { server: source });
      const { name, usedBy, ...payload } = detail.item;
      const saved = await api('POST', '/api/shop/items/' + encodeURIComponent(item.name), payload, { server: target });
      if (!saved.ok) { failed++; continue; }
      if (Object.keys(detail.texts).length) {
        const texts = await api('POST', '/api/shop/items/' + encodeURIComponent(item.name) + '/texts', { texts: detail.texts }, { server: target });
        if (!texts.ok) failed++;
      }
    } catch (e) {
      failed++;
    }
  }
  return { total: list.length, failed };
}

export function tierEditor(prefix, tiers, maxLevel, currencies) {
  const rows = h('ol', { class: 'tier-rows' });
  const empty = h('p', { class: 'hint' }, t('shop.upgrades.noLevels'));
  const add = h('button', { class: 'btn btn-small', type: 'button' }, t('shop.upgrades.addLevel'));
  let next = 0;
  const refresh = () => {
    add.disabled = rows.children.length >= maxLevel;
    empty.hidden = rows.children.length > 0;
    [...rows.children].forEach((li, i) => { li.querySelector('.tier-level').textContent = t('shop.upgrades.level', { level: i + 1 }); });
  };
  function row(tier) {
    const n = next++;
    const price = h('input', { type: 'number', min: '0', max: '2304', step: '1', required: true, value: String(tier.price), id: prefix + '-price-' + n });
    const currency = select(prefix + '-cur-' + n, currencies, tier.currency);
    price.setAttribute('aria-label', t('shop.upgrades.price'));
    currency.setAttribute('aria-label', t('shop.upgrades.currency'));
    const remove = h('button', { class: 'btn btn-small btn-quiet', type: 'button' }, t('common.remove'));
    const li = h('li', { class: 'inline-form tier-row' }, h('span', { class: 'tier-level' }), price, currency, remove);
    remove.addEventListener('click', () => { li.remove(); refresh(); });
    rows.append(li);
  }
  tiers.forEach(row);
  add.addEventListener('click', () => {
    const last = rows.lastElementChild;
    row(last ? { price: Number(last.querySelector('input').value) || 0, currency: last.querySelector('select').value }
      : { price: 1, currency: currencies.includes('DIAMOND') ? 'DIAMOND' : currencies[0] });
    refresh();
  });
  refresh();
  return {
    el: h('div', { class: 'tier-editor' }, rows, empty, add),
    read: () => [...rows.children].map(li => ({ price: Number(li.querySelector('input').value), currency: li.querySelector('select').value })),
  };
}

async function pushUpgrades(source, target) {
  const list = (await api('GET', '/api/shop/upgrades', undefined, { server: source })).upgrades;
  let failed = 0;
  for (const u of list) {
    try {
      const saved = await api('POST', '/api/shop/upgrades/' + encodeURIComponent(u.name), { tiers: u.tiers }, { server: target });
      if (!saved.ok) failed++;
    } catch (e) {
      failed++;
    }
  }
  return { total: list.length, failed };
}

async function pushGuis(source, target) {
  const list = (await api('GET', '/api/shop/guis', undefined, { server: source })).guis;
  let failed = 0;
  for (const g of list) {
    try {
      const { problems, usedBy, ...payload } = g;
      let saved = await api('POST', '/api/shop/guis', payload, { server: target });
      if (!saved.ok && saved.code === 'NAME_TAKEN') saved = await api('POST', '/api/shop/guis/' + encodeURIComponent(g.id), payload, { server: target });
      if (!saved.ok) failed++;
    } catch (e) {
      failed++;
    }
  }
  return { total: list.length, failed };
}

export function syncButton(kind) {
  if (!isGateway()) return null;
  const button = h('button', { class: 'btn', type: 'button' }, t('shop.sync.' + kind));
  button.addEventListener('click', async () => {
    const source = state.server;
    let servers;
    try {
      servers = (await api('GET', '/api/network')).servers;
    } catch (err) {
      toast(errorText(err), 'error');
      return;
    }
    const sourceId = (servers.find(s => s.local) || {}).id;
    const from = source || sourceId;
    const targets = servers.filter(s => s.status === 'ONLINE' && (s.local ? null : s.id) !== source);
    if (!targets.length) { toast(t('shop.sync.none'), 'error'); return; }
    const names = targets.map(s => s.id).join(', ');
    if (!await confirmDialog(t('shop.sync.confirm.' + kind, { source: from, targets: names }), { confirm: t('shop.sync.' + kind) })) return;
    await busy(button, async () => {
      const results = await Promise.all(targets.map(async s => {
        try {
          const push = { items: pushItems, guis: pushGuis, upgrades: pushUpgrades }[kind];
          return { id: s.id, ...(await push(source, s.local ? null : s.id)) };
        } catch (err) {
          return { id: s.id, total: 0, failed: -1, error: errorText(err) };
        }
      }));
      for (const r of results) {
        if (r.failed === 0) toast(t('shop.sync.done', { server: r.id, count: r.total }));
        else if (r.failed < 0) toast(t('shop.sync.failedServer', { server: r.id, error: r.error }), 'error');
        else toast(t('shop.sync.partial', { server: r.id, failed: r.failed, count: r.total }), 'error');
      }
    });
  });
  return button;
}
