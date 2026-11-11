import { api, busy, confirmDialog, errorText, go, h, t } from '../app.js';
import { mcText, select, shopOptions, shopTabs, showResult, swatch, usageText } from './shopcommon.js';

const ID_PATTERN = '[A-Za-z0-9_\\-]{1,32}';

export function parseToken(token) {
  const raw = String(token == null ? '' : token).trim();
  const lower = raw.toLowerCase();
  if (lower === 'fill') return { kind: 'fill', target: '' };
  if (!raw || lower === 'empty') return { kind: 'empty', target: '' };
  const colon = raw.indexOf(':');
  if (colon < 0) return { kind: 'item', target: raw, plain: true };
  const type = lower.slice(0, colon);
  const target = raw.slice(colon + 1).trim();
  if (type === 'ci' || type === 'customitem') return { kind: 'item', target };
  if (type === 'cat' || type === 'category') return { kind: 'category', target };
  if (type === 'upg' || type === 'upgrade') return { kind: 'upgrade', target };
  return { kind: 'invalid', target: raw };
}

export function makeToken(kind, target) {
  if (kind === 'fill' || kind === 'empty') return kind;
  if (kind === 'item') return 'ci:' + target;
  if (kind === 'category') return 'cat:' + target;
  if (kind === 'upgrade') return 'upg:' + target;
  return target;
}

export function guiList(root) {
  root.append(h('h1', {}, t('shop.title')), shopTabs('guis'));
  const add = h('a', { class: 'btn btn-primary', href: '#/shop/gui' }, t('shop.guis.new'));
  const list = h('div', { class: 'table-wrap' }, h('p', { class: 'muted' }, t('common.working')));
  root.append(h('div', { class: 'inline-form shop-toolbar' }, add), list);
  api('GET', '/api/shop/guis').then(data => {
    if (!data.guis.length) { list.replaceChildren(h('p', { class: 'empty' }, t('shop.guis.none'))); return; }
    list.replaceChildren(h('table', { class: 'table' },
      h('thead', {}, h('tr', {}, h('th', {}, t('shop.gui.id')), h('th', {}, t('shop.gui.title')), h('th', { class: 'num' }, t('shop.gui.rows')),
        h('th', { class: 'num hide-narrow' }, t('shop.gui.categories')), h('th', { class: 'hide-narrow' }, t('shop.items.used')), h('th', {}))),
      h('tbody', {}, data.guis.map(g => h('tr', {},
        h('td', {}, h('a', { href: '#/shop/gui/' + encodeURIComponent(g.id) }, g.id)),
        h('td', {}, mcText(g.title)),
        h('td', { class: 'num' }, String(g.size / 9)),
        h('td', { class: 'num hide-narrow' }, String(g.categories.length)),
        h('td', { class: 'hide-narrow' }, g.usedBy.length ? g.usedBy.map(usageText).join(', ') : h('span', { class: 'muted' }, '-')),
        h('td', {}, g.problems.length ? h('span', { class: 'badge badge-off' }, t('shop.gui.problemCount', { n: g.problems.length })) : null))))));
  }).catch(err => list.replaceChildren(h('p', { class: 'form-error' }, errorText(err))));
  return null;
}

export function guiPage(root, id) {
  const creating = !id;
  root.append(h('a', { class: 'back', href: '#/shop/guis' }, t('shop.back')), h('h1', {}, creating ? t('shop.gui.newTitle') : id));
  const body = h('div', {}, h('p', { class: 'muted' }, t('common.working')));
  root.append(body);
  Promise.all([shopOptions(), api('GET', '/api/shop/items'), creating ? null : api('GET', '/api/shop/guis/' + encodeURIComponent(id))])
    .then(([options, items, data]) => editor(body, options, items.items, data ? data.gui : null))
    .catch(err => body.replaceChildren(h('p', { class: 'form-error' }, err.status === 404 ? t('shop.gui.notFound') : errorText(err))));
  return null;
}

function editor(body, options, items, loaded) {
  const creating = !loaded;
  const gui = loaded ? structuredClone(loaded) : {
    id: '', title: '&8Shop', size: 54, headerSize: 18, footerSize: 0, closeOnTransaction: false, fill: 'empty', empty: 'empty',
    slots: Array(54).fill('empty'), categories: [], problems: [], usedBy: [],
  };
  const itemNames = items.map(i => i.name);
  const materials = Object.fromEntries(items.map(i => [i.name.toLowerCase(), i.material]));
  const canonical = name => itemNames.find(n => n.toLowerCase() === String(name || '').toLowerCase()) || name;
  let selected = 0;

  const idInput = h('input', { id: 'gui-id', required: true, maxlength: '32', pattern: ID_PATTERN, autocomplete: 'off', spellcheck: 'false', value: gui.id, readonly: !creating });
  const title = h('input', { id: 'gui-title', required: true, maxlength: '64', autocomplete: 'off', value: gui.title });
  const titlePreview = h('span', { class: 'mc-inline' }, mcText(gui.title));
  title.addEventListener('input', () => { gui.title = title.value; titlePreview.replaceChildren(mcText(title.value)); });
  const rows = h('select', { id: 'gui-rows' }, [1, 2, 3, 4, 5, 6].map(n => h('option', { value: String(n), selected: n * 9 === gui.size }, String(n))));
  const header = h('select', { id: 'gui-header' });
  const footer = h('select', { id: 'gui-footer' });
  const close = h('input', { id: 'gui-close', type: 'checkbox', checked: gui.closeOnTransaction });
  close.addEventListener('change', () => { gui.closeOnTransaction = close.checked; });
  const itemOrEmpty = (sel, value) => {
    const ref = parseToken(value);
    const s = select(sel, ['empty', ...itemNames.map(n => 'ci:' + n)], ref.kind === 'item' ? 'ci:' + canonical(ref.target) : value,
      Object.assign({ empty: t('shop.gui.nothing') }, Object.fromEntries(itemNames.map(n => ['ci:' + n, n]))));
    return s;
  };
  const fill = itemOrEmpty('gui-fill', gui.fill);
  const empty = itemOrEmpty('gui-empty', gui.empty);
  fill.addEventListener('change', () => { gui.fill = fill.value; drawGrid(); });
  empty.addEventListener('change', () => { gui.empty = empty.value; drawGrid(); });

  function fillHeaderFooter() {
    const total = gui.size / 9;
    gui.headerSize = Math.min(gui.headerSize, gui.size);
    gui.footerSize = Math.min(gui.footerSize, gui.size - gui.headerSize);
    header.replaceChildren(...Array.from({ length: total + 1 }, (_, n) => h('option', { value: String(n * 9), selected: n * 9 === gui.headerSize }, String(n))));
    footer.replaceChildren(...Array.from({ length: total - gui.headerSize / 9 + 1 }, (_, n) => h('option', { value: String(n * 9), selected: n * 9 === gui.footerSize }, String(n))));
  }
  rows.addEventListener('change', () => {
    const size = Number(rows.value) * 9;
    while (gui.slots.length < size) gui.slots.push('empty');
    gui.slots.length = size;
    gui.size = size;
    selected = Math.min(selected, size - 1);
    fillHeaderFooter();
    drawGrid();
  });
  header.addEventListener('change', () => { gui.headerSize = Number(header.value); fillHeaderFooter(); drawGrid(); });
  footer.addEventListener('change', () => { gui.footerSize = Number(footer.value); drawGrid(); });
  fillHeaderFooter();

  const grid = h('div', { class: 'slot-grid', role: 'group', 'aria-label': t('shop.gui.grid') });
  const inspector = h('div', { class: 'inspector' });
  const palette = h('div', { class: 'palette' });
  const categoriesBox = h('div', { class: 'categories' });

  function label(token) {
    const ref = parseToken(token);
    if (ref.kind === 'empty') {
      const e = parseToken(gui.empty);
      return { text: '', material: e.kind === 'item' ? materials[e.target.toLowerCase()] : null, kind: 'empty' };
    }
    if (ref.kind === 'fill') {
      const f = parseToken(gui.fill);
      return { text: t('shop.gui.fill'), material: f.kind === 'item' ? materials[f.target.toLowerCase()] : null, kind: 'fill' };
    }
    if (ref.kind === 'item') return { text: ref.target, material: materials[ref.target.toLowerCase()], kind: 'item', broken: !materials[ref.target.toLowerCase()] };
    if (ref.kind === 'upgrade') return { text: ref.target, material: materials[ref.target.toLowerCase()], kind: 'upgrade', broken: !options.upgrades.some(u => u.toLowerCase() === ref.target.toLowerCase()) };
    if (ref.kind === 'category') {
      const cat = gui.categories.find(c => c.id === ref.target);
      return { text: cat ? cat.id : ref.target, material: cat ? materials[String(cat.icon).toLowerCase()] : null, kind: 'category', broken: !cat };
    }
    return { text: String(token), material: null, kind: 'invalid', broken: true };
  }

  function setSlot(index, token) {
    gui.slots[index] = token;
    drawGrid();
  }

  function drawGrid() {
    const cells = gui.slots.map((token, index) => {
      const info = label(token);
      const zone = index < gui.headerSize ? ' is-header' : index >= gui.size - gui.footerSize ? ' is-footer' : '';
      const cell = h('button', {
        type: 'button', draggable: 'true',
        class: 'slot slot-' + info.kind + zone + (index === selected ? ' is-selected' : '') + (info.broken ? ' is-broken' : ''),
        'aria-label': t('shop.gui.slotLabel', { n: index + 1, what: info.text || t('shop.gui.nothing') }),
        'aria-pressed': index === selected ? 'true' : 'false',
      }, info.material ? swatch(info.material) : null, h('span', { class: 'slot-text' }, info.kind === 'category' ? '▤ ' + info.text : info.kind === 'upgrade' ? '⬆ ' + info.text : info.text));
      cell.addEventListener('click', () => { selected = index; drawGrid(); });
      cell.addEventListener('dragstart', e => { e.dataTransfer.setData('text/plain', 'cell:' + index); e.dataTransfer.effectAllowed = 'move'; });
      cell.addEventListener('dragover', e => { e.preventDefault(); cell.classList.add('is-drop'); });
      cell.addEventListener('dragleave', () => cell.classList.remove('is-drop'));
      cell.addEventListener('drop', e => {
        e.preventDefault();
        const data = e.dataTransfer.getData('text/plain');
        if (data.startsWith('cell:')) {
          const from = Number(data.slice(5));
          if (from !== index && from >= 0 && from < gui.slots.length) {
            [gui.slots[from], gui.slots[index]] = [gui.slots[index], gui.slots[from]];
          }
        } else if (data.startsWith('tok:')) {
          gui.slots[index] = data.slice(4);
        }
        selected = index;
        drawGrid();
      });
      cell.addEventListener('keydown', e => {
        const moves = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -9, ArrowDown: 9 };
        if (moves[e.key] !== undefined) {
          const next = index + moves[e.key];
          if (next >= 0 && next < gui.slots.length) {
            e.preventDefault();
            selected = next;
            drawGrid();
            grid.children[next].focus();
          }
        } else if (e.key === 'Delete' || e.key === 'Backspace') {
          e.preventDefault();
          setSlot(index, 'empty');
          grid.children[index].focus();
        }
      });
      return cell;
    });
    grid.replaceChildren(...cells);
    drawInspector();
  }

  function drawInspector() {
    const ref = parseToken(gui.slots[selected]);
    const kinds = ['empty', 'fill', 'item', 'category', 'upgrade'];
    const kind = select('slot-kind', kinds, kinds.includes(ref.kind) ? ref.kind : 'empty', Object.fromEntries(kinds.map(k => [k, t('shop.gui.kind.' + k)])));
    const targets = { item: itemNames, category: gui.categories.map(c => c.id), upgrade: options.upgrades };
    let target = null;
    if (targets[kind.value]) {
      const current = ref.kind === kind.value ? (kind.value === 'item' ? canonical(ref.target) : ref.target) : (targets[kind.value][0] || '');
      target = select('slot-target', targets[kind.value], current);
      target.addEventListener('change', () => setSlot(selected, makeToken(kind.value, target.value)));
    }
    kind.addEventListener('change', () => {
      const list = targets[kind.value];
      setSlot(selected, makeToken(kind.value, list ? (list[0] || '') : ''));
    });
    inspector.replaceChildren(
      h('h2', {}, t('shop.gui.slot', { n: selected + 1 })),
      h('p', { class: 'hint' }, selected < gui.headerSize ? t('shop.gui.zoneHeader') : selected >= gui.size - gui.footerSize ? t('shop.gui.zoneFooter') : t('shop.gui.zoneContent')),
      h('div', { class: 'stack' },
        h('label', { for: 'slot-kind' }, t('shop.gui.what')), kind,
        target ? h('label', { for: 'slot-target' }, t('shop.gui.kind.' + kind.value)) : null, target,
        ref.kind === 'invalid' ? h('p', { class: 'form-error' }, t('shop.gui.invalidToken', { value: gui.slots[selected] })) : null),
      h('p', { class: 'hint' }, t('shop.gui.keys')));
  }

  function chip(token, text, material) {
    const c = h('button', { type: 'button', class: 'chip palette-chip', draggable: 'true', title: token }, material ? swatch(material) : null, text);
    c.addEventListener('dragstart', e => { e.dataTransfer.setData('text/plain', 'tok:' + token); e.dataTransfer.effectAllowed = 'copy'; });
    c.addEventListener('click', () => setSlot(selected, token));
    return c;
  }

  const paletteFilter = h('input', { type: 'search', id: 'palette-filter', autocomplete: 'off', spellcheck: 'false', placeholder: t('shop.items.filter') });
  const paletteItems = h('div', { class: 'chips palette-items' });
  function drawPalette() {
    const q = paletteFilter.value.trim().toLowerCase();
    paletteItems.replaceChildren(...items.filter(i => !q || i.name.toLowerCase().includes(q) || i.material.toLowerCase().includes(q))
      .map(i => chip('ci:' + i.name, i.name, i.material)));
  }
  paletteFilter.addEventListener('input', drawPalette);
  function drawPaletteAll() {
    palette.replaceChildren(
      h('h2', {}, t('shop.gui.palette')),
      h('p', { class: 'hint' }, t('shop.gui.paletteHint')),
      h('div', { class: 'chips' }, chip('empty', t('shop.gui.kind.empty')), chip('fill', t('shop.gui.kind.fill'))),
      h('h3', {}, t('shop.gui.categories')),
      h('div', { class: 'chips' }, gui.categories.length ? gui.categories.map(c => chip('cat:' + c.id, '▤ ' + c.id, materials[String(c.icon).toLowerCase()])) : h('span', { class: 'muted' }, t('shop.gui.noCategories'))),
      h('h3', {}, t('shop.gui.upgrades')),
      h('div', { class: 'chips' }, options.upgrades.map(u => chip('upg:' + u, '⬆ ' + u, materials[u.toLowerCase()]))),
      h('h3', {}, t('shop.tab.items')),
      h('label', { for: 'palette-filter', class: 'sr-only' }, t('shop.items.filter')), paletteFilter, paletteItems);
    drawPalette();
  }

  function renameCategory(oldId, newId) {
    gui.slots = gui.slots.map(s => { const r = parseToken(s); return r.kind === 'category' && r.target === oldId ? 'cat:' + newId : s; });
  }

  function drawCategories() {
    const list = gui.categories.map((cat, index) => {
      const p = 'cat-' + index + '-';
      const idField = h('input', { id: p + 'id', required: true, maxlength: '32', pattern: ID_PATTERN, autocomplete: 'off', spellcheck: 'false', value: cat.id });
      idField.addEventListener('change', () => {
        const next = idField.value.trim();
        if (!next || gui.categories.some((c, i) => i !== index && c.id === next)) { idField.value = cat.id; return; }
        renameCategory(cat.id, next);
        cat.id = next;
        drawAll();
      });
      const name = h('input', { id: p + 'name', required: true, maxlength: '64', autocomplete: 'off', value: cat.name });
      const namePreview = h('span', { class: 'mc-inline' }, mcText(cat.name));
      name.addEventListener('input', () => { cat.name = name.value; namePreview.replaceChildren(mcText(name.value)); });
      const icon = select(p + 'icon', itemNames, canonical(cat.icon));
      icon.addEventListener('change', () => { cat.icon = icon.value; drawGrid(); drawPaletteAll(); });
      const desc = h('input', { id: p + 'desc', maxlength: '256', autocomplete: 'off', value: cat.description });
      desc.addEventListener('input', () => { cat.description = desc.value; });

      const entries = h('ol', { class: 'cat-items' });
      const drawEntries = () => {
        entries.replaceChildren(...cat.items.map((token, i) => {
          const ref = parseToken(token);
          const text = ref.kind === 'upgrade' ? '⬆ ' + ref.target : ref.target;
          const up = h('button', { type: 'button', class: 'btn btn-small btn-quiet', 'aria-label': t('shop.gui.moveUp') + ': ' + text, disabled: i === 0 }, '↑');
          const down = h('button', { type: 'button', class: 'btn btn-small btn-quiet', 'aria-label': t('shop.gui.moveDown') + ': ' + text, disabled: i === cat.items.length - 1 }, '↓');
          const remove = h('button', { type: 'button', class: 'btn btn-small btn-quiet', 'aria-label': t('common.remove') + ': ' + text }, '×');
          up.addEventListener('click', () => { [cat.items[i - 1], cat.items[i]] = [cat.items[i], cat.items[i - 1]]; drawEntries(); });
          down.addEventListener('click', () => { [cat.items[i + 1], cat.items[i]] = [cat.items[i], cat.items[i + 1]]; drawEntries(); });
          remove.addEventListener('click', () => { cat.items.splice(i, 1); drawEntries(); });
          return h('li', { class: 'cat-item' }, materials[ref.target.toLowerCase()] ? swatch(materials[ref.target.toLowerCase()]) : null, h('span', {}, text), up, down, remove);
        }));
      };
      drawEntries();
      entries.addEventListener('dragover', e => e.preventDefault());
      entries.addEventListener('drop', e => {
        e.preventDefault();
        const data = e.dataTransfer.getData('text/plain');
        if (!data.startsWith('tok:')) return;
        const ref = parseToken(data.slice(4));
        if (ref.kind === 'item') cat.items.push(ref.target);
        else if (ref.kind === 'upgrade') cat.items.push('upg:' + ref.target);
        drawEntries();
      });
      const addSelect = select(p + 'add', [...itemNames, ...options.upgrades.map(u => 'upg:' + u)], itemNames[0] || '',
        Object.fromEntries(options.upgrades.map(u => ['upg:' + u, '⬆ ' + u])));
      addSelect.setAttribute('aria-label', t('shop.gui.addToCategory'));
      const add = h('button', { class: 'btn btn-small', type: 'button' }, t('shop.gui.addToCategory'));
      add.addEventListener('click', () => { if (addSelect.value) { cat.items.push(addSelect.value); drawEntries(); } });
      const removeCat = h('button', { class: 'btn btn-small btn-danger', type: 'button' }, t('shop.gui.removeCategory'));
      removeCat.addEventListener('click', async () => {
        if (!await confirmDialog(t('shop.gui.removeCategoryConfirm', { id: cat.id }), { danger: true, confirm: t('common.remove') })) return;
        gui.slots = gui.slots.map(s => { const r = parseToken(s); return r.kind === 'category' && r.target === cat.id ? 'empty' : s; });
        gui.categories.splice(index, 1);
        drawAll();
      });
      return h('details', { class: 'disclosure category', open: creating && index === gui.categories.length - 1 },
        h('summary', {}, mcText(cat.name), ' ', h('span', { class: 'muted mono' }, '(' + cat.id + ', ' + t('shop.gui.itemCount', { n: cat.items.length }) + ')')),
        h('div', { class: 'form-grid' },
          h('div', { class: 'stack' },
            h('label', { for: p + 'id' }, t('shop.gui.categoryId')), idField,
            h('label', { for: p + 'name' }, t('shop.gui.categoryName')), name, namePreview,
            h('label', { for: p + 'icon' }, t('shop.gui.categoryIcon')), icon,
            h('label', { for: p + 'desc' }, t('shop.gui.categoryDescription')), desc),
          h('div', { class: 'stack' },
            h('span', { class: 'strong' }, t('shop.gui.categoryItems')),
            h('p', { class: 'hint' }, t('shop.gui.categoryItemsHint')),
            entries, h('div', { class: 'inline-form' }, addSelect, add))),
        h('div', { class: 'row-end' }, removeCat));
    });
    const addCat = h('button', { class: 'btn', type: 'button' }, t('shop.gui.addCategory'));
    addCat.addEventListener('click', () => {
      let n = gui.categories.length + 1;
      while (gui.categories.some(c => c.id === 'category' + n)) n++;
      gui.categories.push({ id: 'category' + n, name: '&f' + t('shop.gui.categoryDefault', { n }), icon: itemNames[0] || '', description: '', items: [] });
      drawAll();
    });
    categoriesBox.replaceChildren(h('h2', {}, t('shop.gui.categories')),
      h('p', { class: 'hint' }, t('shop.gui.categoriesHint')), ...list, addCat);
  }

  function drawAll() {
    drawGrid();
    drawPaletteAll();
    drawCategories();
  }

  const error = h('p', { class: 'form-error', role: 'alert' });
  const save = h('button', { class: 'btn btn-primary', type: 'submit' }, creating ? t('shop.gui.create') : t('common.save'));
  const del = creating ? null : h('button', { class: 'btn btn-danger', type: 'button', disabled: gui.usedBy.length > 0 }, t('common.delete'));
  if (del) del.addEventListener('click', async () => {
    if (!await confirmDialog(t('shop.gui.deleteConfirm', { id: gui.id }), { danger: true, confirm: t('common.delete') })) return;
    busy(del, async () => {
      try {
        const result = await api('POST', '/api/shop/guis/' + encodeURIComponent(gui.id) + '/delete', {});
        if (showResult(result, 'shop.gui.deleted')) go('shop/guis');
      } catch (err) { error.textContent = errorText(err); }
    });
  });

  const problems = gui.problems.length ? h('section', { class: 'notice notice-warn' },
    h('strong', {}, t('shop.gui.problems')), h('ul', {}, gui.problems.map(p => h('li', { class: 'mono' }, p))),
    h('p', { class: 'hint' }, t('shop.gui.problemsHint'))) : null;

  const form = h('form', { class: 'shop-form' },
    problems,
    h('div', { class: 'gui-settings' },
      h('div', { class: 'stack' }, h('label', { for: 'gui-id' }, t('shop.gui.id')), idInput),
      h('div', { class: 'stack gui-title' }, h('label', { for: 'gui-title' }, t('shop.gui.title')), title, titlePreview),
      h('div', { class: 'stack' }, h('label', { for: 'gui-rows' }, t('shop.gui.rows')), rows),
      h('div', { class: 'stack' }, h('label', { for: 'gui-header' }, t('shop.gui.header')), header),
      h('div', { class: 'stack' }, h('label', { for: 'gui-footer' }, t('shop.gui.footer')), footer),
      h('div', { class: 'stack' }, h('label', { for: 'gui-fill' }, t('shop.gui.fillItem')), fill),
      h('div', { class: 'stack' }, h('label', { for: 'gui-empty' }, t('shop.gui.emptyItem')), empty),
      h('label', { class: 'check' }, close, ' ', t('shop.gui.close'))),
    h('p', { class: 'hint measure' }, t('shop.gui.layoutHint')),
    h('div', { class: 'gui-editor' }, h('div', { class: 'grid-wrap' }, grid), inspector),
    h('section', { class: 'panel-section' }, palette),
    h('section', { class: 'panel-section' }, categoriesBox),
    gui.usedBy.length ? h('p', { class: 'hint' }, t('shop.gui.usedBy', { list: gui.usedBy.map(usageText).join(', ') })) : null,
    error,
    h('div', { class: 'row-end' }, del, save));
  body.replaceChildren(form);
  drawAll();

  form.addEventListener('submit', e => {
    e.preventDefault();
    error.textContent = '';
    const guiId = idInput.value.trim();
    const payload = {
      id: guiId, title: title.value, size: gui.size, headerSize: gui.headerSize, footerSize: gui.footerSize,
      closeOnTransaction: close.checked, fill: fill.value, empty: empty.value, slots: gui.slots,
      categories: gui.categories.map(c => ({ id: c.id, name: c.name, icon: c.icon, description: c.description, items: c.items })),
    };
    busy(save, async () => {
      try {
        const result = creating
          ? await api('POST', '/api/shop/guis', payload)
          : await api('POST', '/api/shop/guis/' + encodeURIComponent(guiId), payload);
        if (showResult(result, 'shop.gui.saved') && creating) go('shop/gui/' + encodeURIComponent(guiId));
      } catch (err) { error.textContent = errorText(err); }
    });
  });
}
