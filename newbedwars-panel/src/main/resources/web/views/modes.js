import { api, busy, can, confirmDialog, errorText, formatDuration, h, t, toast } from '../app.js';
import { shopTabs } from './shopcommon.js';

const dash = value => value === null || value === undefined || value === '' ? t('modes.unchanged') : String(value);
const positive = value => value > 0 ? String(value) : t('modes.unchanged');

function facts(mode) {
  const row = (label, value) => h('div', { class: 'stat' }, h('dt', {}, label), h('dd', {}, value));
  return h('dl', { class: 'server' },
    row(t('modes.generators'), dash(mode.generators)),
    row(t('modes.gamePhases'), dash(mode.gamePhases)),
    row(t('modes.shopGui'), dash(mode.shopGui)),
    row(t('modes.upgradeGui'), dash(mode.upgradeGui)),
    row(t('modes.maxInTeam'), positive(mode.maxInTeam)),
    row(t('modes.teams'), positive(mode.teams)));
}

function timeline(mode) {
  if (!mode.phases) return h('p', { class: 'muted' }, t('modes.noPhases'));
  return h('div', { class: 'table-wrap' }, h('table', { class: 'table' },
    h('thead', {}, h('tr', {}, h('th', {}, t('modes.phaseStart')), h('th', {}, t('modes.phaseName')), h('th', {}, t('modes.phaseActions')))),
    h('tbody', {}, mode.phases.map(phase =>
      h('tr', {}, h('td', { class: 'mono' }, formatDuration(phase.start)), h('td', {}, phase.name), h('td', { class: 'muted' }, phase.actions.join(', ')))))));
}

const ACTIONS = [
  { id: 'DIAMOND_GENERATOR', level: true }, { id: 'EMERALD_GENERATOR', level: true }, { id: 'BED_DESTRUCTION' },
  { id: 'DISABLE_REGENERATION' }, { id: 'DISABLE_TEAMS_FORGE' }, { id: 'ENDER_DRAGON_START' }, { id: 'ENDER_DRAGON_END' },
  { id: 'GAME_END:DRAW' }, { id: 'GAME_END:MOST_KILLS' },
];
const MAX_YAML_BYTES = 48 * 1024;

function clock(seconds) {
  return Math.floor(seconds / 60) + ':' + String(seconds % 60).padStart(2, '0');
}

function parseClock(text) {
  const m = /^\s*(\d{1,4}):(\d{1,2})\s*$/.exec(text);
  if (!m || Number(m[2]) > 59) return null;
  const seconds = Number(m[1]) * 60 + Number(m[2]);
  return seconds <= 86400 ? seconds : null;
}

function withId(text, id) {
  const line = 'ConfigurationId: ' + id;
  return /^[ \t]*ConfigurationId[ \t]*:.*$/m.test(text) ? text.replace(/^([ \t]*)ConfigurationId[ \t]*:.*$/m, (all, indent) => indent + line) : line + '\n' + text;
}

function showError(box, err) {
  const data = err && err.data ? err.data : {};
  const list = (title, items) => Array.isArray(items) && items.length ? [h('p', { class: 'hint' }, title), h('ul', {}, items.map(item => h('li', {}, String(item))))] : [];
  box.replaceChildren(h('div', { class: 'notice notice-warn', role: 'alert' },
    h('p', { class: 'form-error' }, errorText(err)),
    list(t('modes.problems'), data.problems), list(t('modes.usedBy'), data.usedBy)));
}

function field(id, label, control, hint) {
  return h('div', { class: 'field' }, h('label', { for: id }, label), control, hint ? h('p', { class: 'hint' }, hint) : null);
}

function optionSelect(id, values, current) {
  const list = [...values];
  if (current && !list.includes(current)) list.unshift(current);
  return h('select', { id }, h('option', { value: '' }, t('modes.unchanged')), list.map(v => h('option', { value: v, selected: v === current }, v)));
}

function actionsEditor(actions, onChange) {
  const chips = h('ul', { class: 'chips' });
  const pick = h('select', { 'aria-label': t('modes.addAction') }, ACTIONS.map(a => h('option', { value: a.id }, t('modes.action.' + a.id))));
  const level = h('input', { type: 'number', min: '1', max: '99', value: '2', 'aria-label': t('modes.actionLevel') });
  const add = h('button', { class: 'btn btn-small', type: 'button' }, t('modes.addAction'));
  const sync = () => { level.hidden = !ACTIONS.find(a => a.id === pick.value).level; };
  function draw() {
    chips.replaceChildren(...(actions.length ? actions.map((action, index) => {
      const remove = h('button', { class: 'btn btn-small btn-quiet', type: 'button', 'aria-label': t('modes.removeRow') + ' ' + action }, '×');
      remove.addEventListener('click', () => { actions.splice(index, 1); draw(); onChange(); });
      return h('li', { class: 'chip' }, h('span', { class: 'mono' }, action), remove);
    }) : [h('li', { class: 'muted' }, t('modes.noActions'))]));
  }
  pick.addEventListener('change', sync);
  add.addEventListener('click', () => {
    const def = ACTIONS.find(a => a.id === pick.value);
    const n = Math.max(1, Math.min(99, parseInt(level.value, 10) || 1));
    const value = def.level ? def.id + ':' + n : def.id;
    if (!actions.includes(value)) actions.push(value);
    draw();
    onChange();
  });
  sync();
  draw();
  return h('div', { class: 'actions-edit' }, chips, h('div', { class: 'inline-form' }, pick, level, add));
}

export function render(root) {
  const editing = can('modes.edit');
  const box = h('div');
  const notice = h('div');
  const editor = h('div');
  root.append(h('h1', {}, t('shop.title')), shopTabs('modes'), h('p', { class: 'hint measure' }, t('modes.note')), notice, editor, box);
  let data = null;
  let generators = [];
  let guis = [];

  function closeEditor() { editor.replaceChildren(); }

  function openEditor(form) {
    notice.replaceChildren();
    editor.replaceChildren(form);
    form.scrollIntoView({ block: 'nearest' });
  }

  function finish(button, errorBox, task, okText) {
    busy(button, async () => {
      try {
        await task();
        toast(okText);
        closeEditor();
        notice.replaceChildren();
        load();
      } catch (err) { showError(errorBox, err); }
    });
  }

  function remove(path, id) {
    const button = h('button', { class: 'btn btn-small btn-danger', type: 'button' }, t('modes.delete'));
    button.addEventListener('click', async () => {
      if (!await confirmDialog(t('modes.deleteConfirm', { name: id }), { danger: true, confirm: t('modes.delete') })) return;
      busy(button, async () => {
        try {
          await api('DELETE', path + encodeURIComponent(id));
          toast(t('modes.deleted', { name: id }));
          closeEditor();
          notice.replaceChildren();
          load();
        } catch (err) { showError(notice, err); }
      });
    });
    return button;
  }

  function editButton(open) {
    const button = h('button', { class: 'btn btn-small', type: 'button' }, t('modes.edit'));
    button.addEventListener('click', open);
    return button;
  }

  function modeForm(mode) {
    const errors = h('div');
    const id = h('input', { id: 'mode-id', type: 'text', maxlength: '32', pattern: '[A-Za-z0-9_\\-]{1,32}', autocomplete: 'off', spellcheck: 'false', required: true, readonly: !!mode, value: mode ? mode.id : '' });
    const gen = optionSelect('mode-gen', data.generatorConfigs, mode && mode.generators);
    const phases = optionSelect('mode-phases', data.phaseLists.map(l => l.id), mode && mode.gamePhases);
    const shop = optionSelect('mode-shop', guis, mode && mode.shopGui);
    const upgrade = optionSelect('mode-upgrade', guis, mode && mode.upgradeGui);
    const num = (key, value) => h('input', { id: 'mode-' + key, type: 'number', min: '0', max: '64', value: String(value > 0 ? value : 0) });
    const perTeam = num('max', mode ? mode.maxInTeam : 0);
    const teams = num('teams', mode ? mode.teams : 0);
    const save = h('button', { class: 'btn btn-primary', type: 'submit' }, t('common.save'));
    const cancel = h('button', { class: 'btn', type: 'button' }, t('common.cancel'));
    cancel.addEventListener('click', closeEditor);
    const form = h('form', { class: 'panel-section mode-form stack', novalidate: true },
      h('h2', {}, mode ? t('modes.formEdit', { name: mode.id }) : t('modes.formNew')),
      h('div', { class: 'form-grid' },
        field('mode-id', t('modes.id'), id, mode ? null : t('modes.idHint')),
        field('mode-gen', t('modes.generators'), gen), field('mode-phases', t('modes.gamePhases'), phases),
        field('mode-shop', t('modes.shopGui'), shop), field('mode-upgrade', t('modes.upgradeGui'), upgrade),
        field('mode-max', t('modes.maxInTeam'), perTeam, t('modes.countHint')), field('mode-teams', t('modes.teams'), teams, t('modes.countHint'))),
      errors, h('div', { class: 'row-end' }, cancel, save));
    form.addEventListener('submit', e => {
      e.preventDefault();
      const name = id.value.trim();
      finish(save, errors, () => api('PUT', '/api/modes/' + encodeURIComponent(name), {
        generators: gen.value || null, gamePhases: phases.value || null, shopGui: shop.value || null, upgradeGui: upgrade.value || null,
        maxInTeam: parseInt(perTeam.value, 10) || 0, teams: parseInt(teams.value, 10) || 0,
      }), t('modes.saved', { name }));
    });
    return form;
  }

  function phaseForm(list) {
    const errors = h('div');
    const rows = (list ? list.phases : [{ name: 'Start', duration: 360, actions: [] }]).map(p => ({ name: p.name, text: clock(p.duration), actions: [...p.actions] }));
    const id = h('input', { id: 'phase-id', type: 'text', maxlength: '32', autocomplete: 'off', spellcheck: 'false', required: true, readonly: !!list, value: list ? list.id : '' });
    const holder = h('div', { class: 'phase-rows' });
    const add = h('button', { class: 'btn', type: 'button' }, t('modes.addPhase'));
    function draw() {
      holder.replaceChildren(...rows.map((row, index) => {
        const name = h('input', { type: 'text', maxlength: '64', value: row.name, 'aria-label': t('modes.phaseName') });
        const time = h('input', { type: 'text', inputmode: 'numeric', class: 'time-input', value: row.text, placeholder: '6:00', 'aria-label': t('modes.phaseDuration') });
        name.addEventListener('input', () => { row.name = name.value; });
        time.addEventListener('input', () => { row.text = time.value; });
        const tool = (label, text, task, disabled) => {
          const b = h('button', { class: 'btn btn-small btn-quiet', type: 'button', 'aria-label': label, title: label, disabled }, text);
          b.addEventListener('click', task);
          return b;
        };
        return h('div', { class: 'phase-row' },
          h('div', { class: 'phase-head' }, h('span', { class: 'tier-level' }, '#' + (index + 1)), name, time,
            tool(t('modes.moveUp'), '↑', () => { rows.splice(index - 1, 0, rows.splice(index, 1)[0]); draw(); }, index === 0),
            tool(t('modes.moveDown'), '↓', () => { rows.splice(index + 1, 0, rows.splice(index, 1)[0]); draw(); }, index === rows.length - 1),
            tool(t('modes.removeRow'), '×', () => { rows.splice(index, 1); draw(); })),
          actionsEditor(row.actions, () => {}));
      }));
    }
    add.addEventListener('click', () => { rows.push({ name: '', text: '5:00', actions: [] }); draw(); });
    const save = h('button', { class: 'btn btn-primary', type: 'submit' }, t('common.save'));
    const cancel = h('button', { class: 'btn', type: 'button' }, t('common.cancel'));
    cancel.addEventListener('click', closeEditor);
    const form = h('form', { class: 'panel-section mode-form stack', novalidate: true },
      h('h2', {}, list ? t('modes.phaseFormEdit', { name: list.id }) : t('modes.phaseFormNew')),
      field('phase-id', t('modes.id'), id, list ? null : t('modes.idHint')), holder, h('div', {}, add), errors,
      h('div', { class: 'row-end' }, cancel, save));
    form.addEventListener('submit', e => {
      e.preventDefault();
      const phases = [];
      for (const row of rows) {
        const duration = parseClock(row.text);
        if (!row.name.trim()) { errors.replaceChildren(h('p', { class: 'form-error' }, t('modes.needName'))); return; }
        if (duration === null) { errors.replaceChildren(h('p', { class: 'form-error' }, t('modes.badDuration'))); return; }
        phases.push({ name: row.name.trim(), duration, actions: row.actions });
      }
      const name = id.value.trim();
      finish(save, errors, () => api('PUT', '/api/phases/' + encodeURIComponent(name), { phases }), t('modes.saved', { name }));
    });
    draw();
    return form;
  }

  async function generatorForm(info) {
    const errors = h('div');
    const id = h('input', { id: 'gen-id', type: 'text', maxlength: '32', autocomplete: 'off', spellcheck: 'false', required: true, readonly: !!info, value: info ? info.id : '' });
    const area = h('textarea', { id: 'gen-yaml', class: 'yaml-edit', rows: '22', spellcheck: 'false', autocomplete: 'off', wrap: 'off' });
    let copy = null;
    const loadText = async source => {
      try {
        const text = (await api('GET', '/api/generators/' + encodeURIComponent(source))).yaml;
        area.value = info ? text : withId(text, id.value.trim() || 'NewConfig');
      } catch (err) { showError(errors, err); }
    };
    if (!info) {
      copy = h('select', { id: 'gen-copy' }, generators.map(g => h('option', { value: g.id }, g.id)));
      copy.addEventListener('change', () => loadText(copy.value));
      id.addEventListener('input', () => { if (id.value.trim()) area.value = withId(area.value, id.value.trim()); });
    }
    const save = h('button', { class: 'btn btn-primary', type: 'submit' }, t('common.save'));
    const cancel = h('button', { class: 'btn', type: 'button' }, t('common.cancel'));
    cancel.addEventListener('click', closeEditor);
    const form = h('form', { class: 'panel-section mode-form stack', novalidate: true },
      h('h2', {}, info ? t('modes.generatorFormEdit', { name: info.id }) : t('modes.generatorFormNew')),
      h('div', { class: 'form-grid' }, field('gen-id', t('modes.id'), id, info ? null : t('modes.idHint')),
        copy ? field('gen-copy', t('modes.copyFrom'), copy) : null),
      field('gen-yaml', t('modes.generatorYaml'), area), errors, h('div', { class: 'row-end' }, cancel, save));
    form.addEventListener('submit', e => {
      e.preventDefault();
      const name = id.value.trim();
      if (new Blob([area.value]).size > MAX_YAML_BYTES) { errors.replaceChildren(h('p', { class: 'form-error' }, t('error.yaml_too_large'))); return; }
      finish(save, errors, () => api('PUT', '/api/generators/' + encodeURIComponent(name), { yaml: area.value }), t('modes.saved', { name }));
    });
    if (info) await loadText(info.id);
    else if (generators.length) await loadText(generators[0].id);
    return form;
  }

  function section(title, note, newLabel, onNew, body) {
    const add = h('button', { class: 'btn btn-primary', type: 'button' }, newLabel);
    add.addEventListener('click', onNew);
    return h('section', { class: 'panel-section' }, h('div', { class: 'row-between' }, h('h2', {}, title), add), note ? h('p', { class: 'hint measure' }, note) : null, body);
  }

  function phaseTable() {
    if (!data.phaseLists.length) return h('p', { class: 'empty' }, t('modes.noPhaseLists'));
    return h('div', { class: 'table-wrap' }, h('table', { class: 'table' },
      h('thead', {}, h('tr', {}, h('th', {}, t('modes.id')), h('th', {}, t('modes.phaseName')), h('th', {}))),
      h('tbody', {}, data.phaseLists.map(list => h('tr', {},
        h('td', {}, list.id), h('td', { class: 'muted' }, t('modes.phaseCount', { count: list.phases.length }) + ' · ' + formatDuration(list.phases.reduce((sum, p) => sum + p.duration, 0))),
        h('td', { class: 'actions-cell' }, editButton(() => openEditor(phaseForm(list))), remove('/api/phases/', list.id)))))));
  }

  function generatorTable() {
    if (!generators.length) return h('p', { class: 'empty' }, t('modes.noGenerators'));
    return h('div', { class: 'table-wrap' }, h('table', { class: 'table' },
      h('thead', {}, h('tr', {}, h('th', {}, t('modes.id')), h('th', {}, t('modes.generatorFile')), h('th', {}))),
      h('tbody', {}, generators.map(info => h('tr', {},
        h('td', {}, info.id, ' ', info.builtIn ? h('span', { class: 'badge' }, t('modes.builtIn')) : null),
        h('td', { class: 'mono' }, info.fileName),
        h('td', { class: 'actions-cell' },
          editButton(async () => openEditor(await generatorForm(info))),
          info.builtIn ? null : remove('/api/generators/', info.id)))))));
  }

  function load() {
    const requests = [api('GET', '/api/modes')];
    if (editing) {
      requests.push(api('GET', '/api/generators'));
      requests.push(api('GET', '/api/shop/guis').catch(() => ({ guis: [] })));
    }
    Promise.all(requests).then(([modes, gens, guiData]) => {
      data = modes;
      generators = gens ? gens.generators : [];
      guis = guiData ? guiData.guis.map(g => g.id) : [];
      const out = [];
      if (editing) {
        const add = h('button', { class: 'btn btn-primary', type: 'button' }, t('modes.new'));
        add.addEventListener('click', () => openEditor(modeForm(null)));
        out.push(h('div', { class: 'actions' }, add));
      }
      if (!data.modes.length) out.push(h('p', { class: 'empty' }, t('modes.empty')));
      for (const mode of data.modes) {
        out.push(h('section', { class: 'panel-section' },
          h('div', { class: 'row-between' },
            h('h2', {}, mode.id, ' ', mode.builtIn ? h('span', { class: 'badge' }, t('modes.builtIn')) : null),
            editing ? h('div', { class: 'actions-cell' }, editButton(() => openEditor(modeForm(mode))), mode.builtIn ? null : remove('/api/modes/', mode.id)) : null),
          facts(mode), timeline(mode)));
      }
      if (editing) {
        out.push(section(t('modes.phasesTitle'), t('modes.phasesNote'), t('modes.phaseNew'), () => openEditor(phaseForm(null)), phaseTable()));
        out.push(section(t('modes.generatorsTitle'), t('modes.generatorsNote'), t('modes.generatorNew'), async () => openEditor(await generatorForm(null)), generatorTable()));
      }
      box.replaceChildren(...out);
    }).catch(err => box.replaceChildren(h('p', { class: 'form-error' }, errorText(err))));
  }

  load();
}
