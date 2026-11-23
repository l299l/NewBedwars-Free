import { api, errorText, formatDuration, h, t } from '../app.js';
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

export function render(root) {
  const box = h('div');
  root.append(h('h1', {}, t('shop.title')), shopTabs('modes'), h('p', { class: 'hint measure' }, t('modes.note')), box);
  api('GET', '/api/modes').then(data => {
    if (!data.modes.length) {
      box.replaceChildren(h('p', { class: 'empty' }, t('modes.empty')));
      return;
    }
    box.replaceChildren(...data.modes.map(mode =>
      h('section', { class: 'panel-section' },
        h('h2', {}, mode.id, ' ', mode.builtIn ? h('span', { class: 'badge' }, t('modes.builtIn')) : null),
        facts(mode), timeline(mode))));
  }).catch(err => box.replaceChildren(h('p', { class: 'form-error' }, errorText(err))));
}
