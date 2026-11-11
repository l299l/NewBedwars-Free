import { api, busy, confirmDialog, errorText, go, h, t } from '../app.js';
import { datalist, materialColor, mcText, select, shopOptions, shopTabs, showResult, swatch, usageText } from './shopcommon.js';

export function render(root, params) {
  const [page, arg] = params;
  if (page === 'item') { itemPage(root, arg); return null; }
  if (page === 'guis' || page === 'gui') {
    let stop = null;
    let gone = false;
    import('./shopgui.js').then(m => { if (!gone) stop = page === 'gui' ? m.guiPage(root, arg) : m.guiList(root); });
    return () => { gone = true; if (stop) stop(); };
  }
  if (page === 'arenas') return arenasPage(root);
  return itemsPage(root);
}

function head(root, active) {
  root.append(h('h1', {}, t('shop.title')), shopTabs(active));
}

function itemsPage(root) {
  head(root, 'items');
  const filter = h('input', { type: 'search', id: 'item-filter', autocomplete: 'off', spellcheck: 'false', placeholder: t('shop.items.filter') });
  const add = h('a', { class: 'btn btn-primary', href: '#/shop/item' }, t('shop.items.new'));
  const list = h('div', { class: 'table-wrap' }, h('p', { class: 'muted' }, t('common.working')));
  root.append(h('div', { class: 'inline-form shop-toolbar' }, h('label', { for: 'item-filter', class: 'sr-only' }, t('shop.items.filter')), filter, add), list);
  let items = [];

  function draw() {
    const q = filter.value.trim().toLowerCase();
    const shown = items.filter(i => !q || i.name.toLowerCase().includes(q) || i.material.toLowerCase().includes(q));
    if (!shown.length) {
      list.replaceChildren(h('p', { class: 'empty' }, t('shop.items.none')));
      return;
    }
    list.replaceChildren(h('table', { class: 'table' },
      h('thead', {}, h('tr', {},
        h('th', {}, t('shop.item.name')), h('th', {}, t('shop.item.material')), h('th', { class: 'num' }, t('shop.item.price')),
        h('th', { class: 'num' }, t('shop.item.amount')), h('th', { class: 'hide-narrow' }, t('shop.item.logic')),
        h('th', { class: 'hide-narrow' }, t('shop.items.used')))),
      h('tbody', {}, shown.map(i => h('tr', {},
        h('td', {}, h('a', { href: '#/shop/item/' + encodeURIComponent(i.name) }, i.name)),
        h('td', { class: 'mono' }, swatch(i.material), i.material),
        h('td', { class: 'num nowrap' }, i.price < 0 ? t('shop.item.configPrice') : i.price + ' ' + i.priceType),
        h('td', { class: 'num' }, String(i.amount)),
        h('td', { class: 'hide-narrow mono' }, i.logic),
        h('td', { class: 'hide-narrow' }, i.usedBy.length ? String(i.usedBy.length) : h('span', { class: 'muted' }, '-')))))));
  }

  filter.addEventListener('input', draw);
  api('GET', '/api/shop/items').then(data => { items = data.items; draw(); })
    .catch(err => list.replaceChildren(h('p', { class: 'form-error' }, errorText(err))));
}

function field(id, label, input, hint) {
  return [h('label', { for: id }, label), input, hint ? h('p', { class: 'hint' }, hint) : null];
}

async function itemPage(root, name) {
  const creating = !name;
  root.append(h('a', { class: 'back', href: '#/shop/items' }, t('shop.back')),
    h('h1', {}, creating ? t('shop.item.newTitle') : name));
  const body = h('div', {}, h('p', { class: 'muted' }, t('common.working')));
  root.append(body);
  let options, data;
  try {
    options = await shopOptions();
    data = creating ? null : await api('GET', '/api/shop/items/' + encodeURIComponent(name));
  } catch (err) {
    body.replaceChildren(h('p', { class: 'form-error' }, err.status === 404 ? t('shop.item.notFound') : errorText(err)));
    return;
  }
  const item = data ? data.item : {
    name: '', material: 'STONE', amount: 1, price: 1, priceType: 'IRON', onBuy: 'GIVE_ITEM', logic: 'NONE',
    permanent: false, permission: '', enchantments: {}, flags: [], usedBy: [],
  };
  const texts = data ? data.texts : {};

  const nameInput = h('input', { id: 'item-name', required: true, maxlength: '32', pattern: '[A-Za-z0-9_]{1,32}', autocomplete: 'off', spellcheck: 'false', value: item.name, readonly: !creating });
  const material = h('input', { id: 'item-material', list: 'materials', required: true, autocomplete: 'off', spellcheck: 'false', value: item.material });
  const materialSwatch = swatch(item.material);
  material.addEventListener('input', () => materialSwatch.style.setProperty('--team', materialColor(material.value)));
  const amount = h('input', { id: 'item-amount', type: 'number', min: '1', max: '64', required: true, value: String(item.amount) });
  const price = h('input', { id: 'item-price', type: 'number', min: '-1', max: '100000', required: true, value: String(item.price) });
  const priceType = select('item-price-type', options.priceTypes, item.priceType);
  const onBuy = select('item-onbuy', options.onBuyActions, item.onBuy, Object.fromEntries(options.onBuyActions.map(a => [a, t('shop.onBuy.' + a)])));
  const logic = select('item-logic', options.logicKeys, item.logic);
  const permanent = h('input', { id: 'item-permanent', type: 'checkbox', checked: item.permanent });
  const permission = h('input', { id: 'item-permission', maxlength: '128', autocomplete: 'off', spellcheck: 'false', value: item.permission });

  const enchRows = h('div', { class: 'ench-rows' });
  function enchRow(key, level) {
    const keySelect = select('', options.enchantments, key);
    keySelect.setAttribute('aria-label', t('shop.item.enchantment'));
    const levelInput = h('input', { type: 'number', min: '1', max: '255', value: String(level), 'aria-label': t('shop.item.level') });
    const remove = h('button', { class: 'btn btn-small btn-quiet', type: 'button' }, t('common.remove'));
    const row = h('div', { class: 'inline-form ench-row' }, keySelect, levelInput, remove);
    remove.addEventListener('click', () => row.remove());
    enchRows.append(row);
  }
  for (const [k, v] of Object.entries(item.enchantments)) enchRow(k, v);
  const addEnch = h('button', { class: 'btn btn-small', type: 'button' }, t('shop.item.addEnchantment'));
  addEnch.addEventListener('click', () => enchRow(options.enchantments[0], 1));

  const flags = h('fieldset', { class: 'checks' }, h('legend', {}, t('shop.item.flags')),
    options.flags.map(f => h('label', { class: 'check' }, h('input', { type: 'checkbox', value: f, checked: item.flags.includes(f) }), ' ', f)));

  const textBlocks = options.languages.map(lang => {
    const tx = texts[lang] || { name: '', iconDescription: '', lore: '' };
    const n = h('input', { id: 'tx-name-' + lang, maxlength: '128', autocomplete: 'off', value: tx.name });
    const icon = h('textarea', { id: 'tx-icon-' + lang, rows: '3', maxlength: '2048' });
    icon.value = tx.iconDescription;
    const lore = h('textarea', { id: 'tx-lore-' + lang, rows: '2', maxlength: '2048' });
    lore.value = tx.lore;
    const preview = h('div', { class: 'mc-preview', 'aria-label': t('shop.texts.preview') });
    const drawPreview = () => {
      const label = n.value || (lang === 'en' ? nameInput.value : (texts.en && texts.en.name) || nameInput.value);
      const desc = (icon.value || t('shop.texts.defaultIcon'))
        .split('/price/').join(price.value + ' ' + priceType.value)
        .split('/amount/').join(amount.value)
        .split('/name/').join(label);
      preview.replaceChildren(h('div', { class: 'mc-name' }, mcText(label)), h('div', { class: 'mc-lore' }, mcText(desc, /;|\n/)));
    };
    for (const el of [n, icon, nameInput, price, amount, priceType]) el.addEventListener('input', drawPreview);
    drawPreview();
    const block = h('details', { class: 'disclosure texts', open: lang === 'en' },
      h('summary', {}, t('shop.texts.lang', { lang: lang.toUpperCase() })),
      h('div', { class: 'texts-grid' },
        h('div', { class: 'stack' },
          ...field('tx-name-' + lang, t('shop.texts.name'), n, lang === 'en' ? null : t('shop.texts.fallback')),
          ...field('tx-icon-' + lang, t('shop.texts.icon'), icon, t('shop.texts.iconHint')),
          ...field('tx-lore-' + lang, t('shop.texts.lore'), lore)),
        preview));
    return { lang, block, initial: tx, read: () => ({ name: n.value, iconDescription: icon.value, lore: lore.value }) };
  });

  const error = h('p', { class: 'form-error', role: 'alert' });
  const save = h('button', { class: 'btn btn-primary', type: 'submit' }, creating ? t('shop.item.create') : t('common.save'));
  const del = creating ? null : h('button', { class: 'btn btn-danger', type: 'button', disabled: item.usedBy.length > 0 }, t('common.delete'));
  const usage = item.usedBy.length
    ? h('p', { class: 'hint' }, t('shop.item.usedBy', { list: item.usedBy.map(usageText).join(', ') }))
    : null;

  const form = h('form', { class: 'shop-form' },
    datalist('materials', options.materials),
    h('div', { class: 'form-grid' },
      h('div', { class: 'stack' },
        ...field('item-name', t('shop.item.name'), nameInput, creating ? t('shop.item.nameHint') : null),
        h('label', { for: 'item-material' }, t('shop.item.material')), h('div', { class: 'inline-form' }, materialSwatch, material),
        ...field('item-amount', t('shop.item.amount'), amount),
        ...field('item-price', t('shop.item.price'), price, t('shop.item.priceHint')),
        ...field('item-price-type', t('shop.item.priceType'), priceType)),
      h('div', { class: 'stack' },
        ...field('item-onbuy', t('shop.item.onBuy'), onBuy),
        ...field('item-logic', t('shop.item.logic'), logic),
        ...field('item-permission', t('shop.item.permission'), permission, t('shop.item.permissionHint')),
        h('label', { class: 'check' }, permanent, ' ', t('shop.item.permanent')))),
    h('section', { class: 'panel-section' }, h('h2', {}, t('shop.item.enchantments')), enchRows, addEnch),
    h('section', { class: 'panel-section' }, flags),
    h('section', { class: 'panel-section' }, h('h2', {}, t('shop.texts.title')), h('p', { class: 'hint' }, t('shop.texts.hint')),
      textBlocks.map(b => b.block)),
    usage, error,
    h('div', { class: 'row-end' }, del, save));
  body.replaceChildren(form);

  if (del) {
    del.addEventListener('click', async () => {
      if (!await confirmDialog(t('shop.item.deleteConfirm', { name: item.name }), { danger: true, confirm: t('common.delete') })) return;
      busy(del, async () => {
        try {
          const result = await api('POST', '/api/shop/items/' + encodeURIComponent(item.name) + '/delete', {});
          if (showResult(result, 'shop.item.deleted')) go('shop/items');
        } catch (err) { error.textContent = errorText(err); }
      });
    });
  }

  form.addEventListener('submit', e => {
    e.preventDefault();
    error.textContent = '';
    const itemName = nameInput.value.trim();
    const enchantments = {};
    for (const row of enchRows.querySelectorAll('.ench-row')) {
      const [k, lvl] = row.querySelectorAll('select, input');
      enchantments[k.value] = Number(lvl.value);
    }
    const payload = {
      material: material.value.trim().toUpperCase(), amount: Number(amount.value), price: Number(price.value),
      priceType: priceType.value, onBuy: onBuy.value, logic: logic.value, permanent: permanent.checked,
      permission: permission.value.trim(), enchantments,
      flags: [...flags.querySelectorAll('input:checked')].map(c => c.value),
    };
    busy(save, async () => {
      try {
        const result = await api('POST', '/api/shop/items/' + encodeURIComponent(itemName), payload);
        if (!result.ok) { showResult(result); return; }
        const changed = {};
        for (const b of textBlocks) {
          const now = b.read();
          if (creating || now.name !== b.initial.name || now.iconDescription !== b.initial.iconDescription || now.lore !== b.initial.lore) changed[b.lang] = now;
        }
        if (Object.keys(changed).length) {
          const textResult = await api('POST', '/api/shop/items/' + encodeURIComponent(itemName) + '/texts', { texts: changed });
          if (!textResult.ok) { showResult(textResult); return; }
        }
        showResult(result, 'shop.item.saved');
        if (creating) go('shop/item/' + encodeURIComponent(itemName));
        else for (const b of textBlocks) b.initial = b.read();
      } catch (err) { error.textContent = errorText(err); }
    });
  });
}

function arenasPage(root) {
  head(root, 'arenas');
  root.append(h('p', { class: 'hint measure' }, t('shop.arenas.hint')));
  const list = h('div', { class: 'table-wrap' }, h('p', { class: 'muted' }, t('common.working')));
  root.append(list);

  async function load() {
    try {
      const [arenas, guis] = await Promise.all([api('GET', '/api/shop/arenas'), api('GET', '/api/shop/guis')]);
      const ids = guis.guis.map(g => g.id);
      if (!arenas.arenas.length) {
        list.replaceChildren(h('p', { class: 'empty' }, t('shop.arenas.none')));
        return;
      }
      list.replaceChildren(h('table', { class: 'table' },
        h('thead', {}, h('tr', {}, h('th', {}, t('shop.arenas.arena')), h('th', {}, t('shop.arenas.shop')),
          h('th', {}, t('shop.arenas.upgrades')), h('th', {}))),
        h('tbody', {}, arenas.arenas.map(a => row(a, ids)))));
    } catch (err) {
      list.replaceChildren(h('p', { class: 'form-error' }, errorText(err)));
    }
  }

  function row(a, ids) {
    const shop = select('shop-' + a.arena, ids, a.shopGui);
    const upgrades = select('upg-' + a.arena, ids, a.upgradeGui);
    shop.setAttribute('aria-label', t('shop.arenas.shop') + ': ' + a.arena);
    upgrades.setAttribute('aria-label', t('shop.arenas.upgrades') + ': ' + a.arena);
    shop.disabled = upgrades.disabled = !a.waiting;
    const save = h('button', { class: 'btn btn-small', type: 'button', hidden: true }, t('common.save'));
    const changed = () => { save.hidden = shop.value === a.shopGui && upgrades.value === a.upgradeGui; };
    shop.addEventListener('change', changed);
    upgrades.addEventListener('change', changed);
    save.addEventListener('click', () => busy(save, async () => {
      try {
        const result = await api('POST', '/api/shop/arenas/' + encodeURIComponent(a.arena), { shopGui: shop.value, upgradeGui: upgrades.value });
        if (showResult(result, 'shop.arenas.saved')) load();
      } catch (err) { showResult({ ok: false, code: 'FAILED', detail: errorText(err) }); }
    }));
    return h('tr', {}, h('td', {}, a.arena, a.waiting ? null : h('span', { class: 'hint' }, ' ' + t('shop.arenas.running'))),
      h('td', {}, shop), h('td', {}, upgrades), h('td', { class: 'actions-cell' }, save));
  }

  load();
}
