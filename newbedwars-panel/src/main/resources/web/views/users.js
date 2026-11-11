import { api, busy, can, confirmDialog, errorText, formatTime, h, state, t, toast } from '../app.js';

function passwordBox(name, password) {
  const value = h('code', { class: 'secret' }, password);
  const copy = h('button', { class: 'btn btn-small', type: 'button' }, t('users.copy'));
  copy.addEventListener('click', async () => {
    try { await navigator.clipboard.writeText(password); toast(t('users.copied')); } catch (e) { }
  });
  const close = h('button', { class: 'btn btn-small btn-quiet', type: 'button' }, t('common.close'));
  const box = h('section', { class: 'notice notice-secret', role: 'alert' },
    h('h2', {}, t('users.oneTime', { name })), h('div', { class: 'secret-row' }, value, copy, close),
    h('p', { class: 'muted' }, t('users.oneTimeNote')));
  close.addEventListener('click', () => box.remove());
  return box;
}

export function render(root) {
  const list = h('div');
  const secret = h('div');
  const roleSelect = (selected, roles, id) => h('select', { id }, roles.map(r => h('option', { value: r.name, selected: r.name === selected }, r.name)));

  const name = h('input', { id: 'new-user', required: true, minlength: '3', maxlength: '32', pattern: '[A-Za-z0-9_.\\-]{3,32}', autocomplete: 'off', spellcheck: 'false' });
  const roleHolder = h('span');
  const create = h('button', { class: 'btn btn-primary', type: 'submit' }, t('users.create'));
  const form = h('form', { class: 'inline-form' },
    h('label', { for: 'new-user' }, t('users.name')), name,
    h('label', { for: 'new-user-role' }, t('users.role')), roleHolder, create);

  async function load() {
    try {
      const [users, roles] = await Promise.all([api('GET', '/api/users'), api('GET', '/api/roles')]);
      const current = roleHolder.querySelector('select');
      roleHolder.replaceChildren(roleSelect(current ? current.value : 'staff', roles.roles, 'new-user-role'));
      list.replaceChildren(h('table', { class: 'table' },
        h('thead', {}, h('tr', {}, h('th', {}, t('users.name')), h('th', {}, t('users.role')), h('th', {}, t('users.created')), h('th', {}))),
        h('tbody', {}, users.users.map(user => row(user, roles.roles)))));
    } catch (err) {
      list.replaceChildren(h('p', { class: 'form-error' }, errorText(err)));
    }
  }

  function row(user, roles) {
    const self = user.name.toLowerCase() === state.me.name.toLowerCase();
    const select = roleSelect(user.role, roles, 'role-' + user.name);
    select.setAttribute('aria-label', t('users.role') + ': ' + user.name);
    const save = h('button', { class: 'btn btn-small', type: 'button', hidden: true }, t('users.saveRole'));
    select.addEventListener('change', () => { save.hidden = select.value === user.role; });
    save.addEventListener('click', () => busy(save, async () => {
      try {
        await api('POST', '/api/users/' + encodeURIComponent(user.name) + '/role', { role: select.value });
        toast(t('users.roleChanged', { name: user.name, role: select.value }));
        load();
      } catch (err) { toast(errorText(err), 'error'); }
    }));
    const reset = h('button', { class: 'btn btn-small', type: 'button' }, t('users.reset'));
    reset.addEventListener('click', async () => {
      if (!await confirmDialog(t('users.resetConfirm', { name: user.name }))) return;
      busy(reset, async () => {
        try {
          const result = await api('POST', '/api/users/' + encodeURIComponent(user.name) + '/reset', {});
          secret.replaceChildren(passwordBox(result.name, result.password));
          load();
        } catch (err) { toast(errorText(err), 'error'); }
      });
    });
    const remove = h('button', { class: 'btn btn-small btn-danger', type: 'button' }, t('users.delete'));
    remove.addEventListener('click', async () => {
      if (!await confirmDialog(t('users.deleteConfirm', { name: user.name }), { danger: true, confirm: t('users.delete') })) return;
      busy(remove, async () => {
        try {
          await api('DELETE', '/api/users/' + encodeURIComponent(user.name));
          toast(t('users.deleted', { name: user.name }));
          load();
        } catch (err) { toast(errorText(err), 'error'); }
      });
    });
    return h('tr', {},
      h('td', {}, user.name, self ? h('span', { class: 'muted' }, ' (' + t('users.you') + ')') : null,
        user.mustChange ? h('div', { class: 'hint' }, t('users.mustChange')) : null),
      h('td', {}, select, ' ', save),
      h('td', { class: 'muted' }, user.created ? formatTime(user.created) : ''),
      h('td', { class: 'num actions-cell' }, reset, remove));
  }

  form.addEventListener('submit', async e => {
    e.preventDefault();
    const role = roleHolder.querySelector('select');
    busy(create, async () => {
      try {
        const result = await api('POST', '/api/users', { name: name.value.trim(), role: role ? role.value : 'staff' });
        secret.replaceChildren(passwordBox(result.name, result.password));
        name.value = '';
        load();
      } catch (err) { toast(errorText(err), 'error'); }
    });
  });

  root.append(h('h1', {}, t('users.title')), secret, h('section', { class: 'panel-section' }, form), list);
  load();
}
