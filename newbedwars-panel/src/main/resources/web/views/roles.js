import { api, busy, confirmDialog, errorText, h, state, t, toast } from '../app.js';

export function render(root) {
  const list = h('div');
  const editor = h('div');

  function openEditor(role) {
    const name = h('input', { id: 'role-name', required: true, minlength: '2', maxlength: '32', pattern: '[a-z0-9_\\-]{2,32}', value: role ? role.name : '', readonly: !!role, autocomplete: 'off', spellcheck: 'false' });
    const boxes = state.me.allPermissions.map(permission => {
      const box = h('input', { type: 'checkbox', id: 'perm-' + permission, value: permission, checked: role ? role.permissions.includes(permission) : permission === 'dashboard.view' });
      if (!state.me.permissions.includes(permission)) box.disabled = true;
      return h('label', { class: 'check', for: 'perm-' + permission }, box, ' ', t('perm.' + permission), h('span', { class: 'muted mono' }, ' ' + permission));
    });
    const save = h('button', { class: 'btn btn-primary', type: 'submit' }, t('roles.save'));
    const cancel = h('button', { class: 'btn', type: 'button' }, t('common.cancel'));
    cancel.addEventListener('click', () => editor.replaceChildren());
    const form = h('form', { class: 'stack panel-section' },
      h('h2', {}, role ? role.name : t('roles.new')),
      h('label', { for: 'role-name' }, t('roles.name')), name,
      h('fieldset', { class: 'checks' }, h('legend', {}, t('roles.permissions')), boxes),
      h('div', { class: 'row-end' }, cancel, save));
    form.addEventListener('submit', e => {
      e.preventDefault();
      const permissions = [...form.querySelectorAll('input[type=checkbox]:checked')].map(b => b.value);
      busy(save, async () => {
        try {
          await api('POST', '/api/roles', { name: name.value.trim(), permissions });
          toast(t('roles.saved', { name: name.value.trim() }));
          editor.replaceChildren();
          load();
        } catch (err) { toast(errorText(err), 'error'); }
      });
    });
    editor.replaceChildren(form);
    (role ? form.querySelector('input[type=checkbox]') : name).focus();
  }

  async function load() {
    try {
      const data = await api('GET', '/api/roles');
      list.replaceChildren(h('ul', { class: 'roles' }, data.roles.map(role => {
        const buttons = [];
        if (!role.builtIn) {
          const edit = h('button', { class: 'btn btn-small', type: 'button' }, t('roles.edit'));
          edit.addEventListener('click', () => openEditor(role));
          const remove = h('button', { class: 'btn btn-small btn-danger', type: 'button' }, t('roles.delete'));
          remove.addEventListener('click', async () => {
            if (!await confirmDialog(t('roles.deleteConfirm', { name: role.name }), { danger: true, confirm: t('roles.delete') })) return;
            busy(remove, async () => {
              try {
                await api('DELETE', '/api/roles/' + encodeURIComponent(role.name));
                toast(t('roles.deleted', { name: role.name }));
                load();
              } catch (err) { toast(errorText(err), 'error'); }
            });
          });
          buttons.push(edit, remove);
        }
        return h('li', { class: 'role' },
          h('div', { class: 'role-head' },
            h('strong', {}, role.name),
            role.builtIn ? h('span', { class: 'badge badge-off' }, t('roles.builtIn')) : null,
            h('span', { class: 'muted' }, t('roles.users', { count: role.users })),
            h('span', { class: 'role-buttons' }, buttons)),
          h('ul', { class: 'perm-list' }, role.permissions.map(p => h('li', {}, t('perm.' + p)))));
      })));
    } catch (err) {
      list.replaceChildren(h('p', { class: 'form-error' }, errorText(err)));
    }
  }

  const add = h('button', { class: 'btn btn-primary', type: 'button' }, t('roles.new'));
  add.addEventListener('click', () => openEditor(null));
  root.append(h('h1', {}, t('roles.title')), h('p', { class: 'muted measure' }, t('roles.intro')), h('div', { class: 'row' }, add), editor, list);
  load();
}
