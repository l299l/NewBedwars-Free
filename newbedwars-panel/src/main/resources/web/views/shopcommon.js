import { api, h, t, toast } from '../app.js';

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

export async function shopOptions() {
  if (!optionsCache) optionsCache = api('GET', '/api/shop/options').catch(err => { optionsCache = null; throw err; });
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
  const tabs = [['items', 'shop.tab.items'], ['guis', 'shop.tab.guis'], ['arenas', 'shop.tab.arenas']];
  return h('nav', { class: 'tabs', 'aria-label': t('nav.shop') },
    tabs.map(([route, key]) => h('a', {
      href: '#/shop/' + route, class: 'tab' + (route === active ? ' is-active' : ''),
      'aria-current': route === active ? 'page' : null,
    }, t(key))));
}

export function select(id, values, selected, labels = {}) {
  const list = [...values];
  if (selected && !list.includes(selected)) list.unshift(selected);
  return h('select', { id }, list.map(v => h('option', { value: v, selected: v === selected }, labels[v] || v)));
}

export function datalist(id, values) {
  return h('datalist', { id }, values.map(v => h('option', { value: v })));
}
