import { api, errorText, formatTime, h, t } from '../app.js';

export function render(root) {
  const list = h('div');
  root.append(h('h1', {}, t('audit.title')), h('p', { class: 'muted measure' }, t('audit.intro')), list);
  api('GET', '/api/audit?limit=300').then(data => {
    if (!data.entries.length) { list.replaceChildren(h('p', { class: 'empty' }, t('audit.empty'))); return; }
    list.replaceChildren(h('div', { class: 'table-wrap' }, h('table', { class: 'table audit' },
      h('thead', {}, h('tr', {}, ['audit.time', 'audit.user', 'audit.action', 'audit.target', 'audit.result'].map(k => h('th', {}, t(k))))),
      h('tbody', {}, data.entries.map(e => {
        const result = String(e.result || '');
        const good = ['ok', 'created', 'reset', 'deleted', 'role_changed'].includes(result.toLowerCase());
        return h('tr', {},
          h('td', { class: 'muted nowrap' }, formatTime(e.time)),
          h('td', {}, e.user || '-', h('div', { class: 'hint mono' }, e.ip || '')),
          h('td', { class: 'mono' }, e.action),
          h('td', {}, e.target || ''),
          h('td', {}, h('span', { class: 'result' + (good ? ' is-good' : ' is-bad') }, result)));
      })))));
  }).catch(err => list.replaceChildren(h('p', { class: 'form-error' }, errorText(err))));
}
