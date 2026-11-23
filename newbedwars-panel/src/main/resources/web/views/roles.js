import { api, busy, confirmDialog, errorText, h, state, t, toast } from '../app.js';

const GROUPS = ['overview', 'games', 'arenas', 'modes', 'shop', 'admin', 'other'];
const GROUP_OF = { dashboard: 'overview', stats: 'overview', audit: 'overview', game: 'games', arena: 'arenas', modes: 'modes', shop: 'shop', users: 'admin', roles: 'admin', network: 'admin' };
const SENSITIVE = new Set(['arena.delete', 'arena.restore', 'arena.import', 'shop.commands', 'users.manage', 'roles.manage']);

function groupOf(permission) {
  return GROUP_OF[permission.split('.')[0]] || 'other';
}

function grouped(permissions) {
  const map = new Map(GROUPS.map(g => [g, []]));
  for (const p of permissions) map.get(groupOf(p)).push(p);
  return [...map].filter(([, list]) => list.length);
}

export function render(root) {
  const list = h('div');
  const editor = h('div');

  function openEditor(role) {
    const name = h('input', { id: 'role-name', required: true, minlength: '2', maxlength: '32', pattern: '[a-z0-9_\\-]{2,32}', value: role ? role.name : '', readonly: !!role, autocomplete: 'off', spellcheck: 'false' });
    const all = state.me.allPermissions;
    const counter = h('span', { class: 'muted' });
    const groupSyncs = [];

    function refresh() {
      groupSyncs.forEach(sync => sync());
      const checked = form.querySelectorAll('.perm-option input:checked').length;
      counter.textContent = t('roles.selected', { count: checked, total: all.length });
    }

    const sections = grouped(all).map(([group, perms]) => {
      const boxes = perms.map(permission => {
        const box = h('input', { type: 'checkbox', id: 'perm-' + permission, value: permission, checked: role ? role.permissions.includes(permission) : permission === 'dashboard.view' });
        const locked = !state.me.permissions.includes(permission);
        if (locked) box.disabled = true;
        box.addEventListener('change', refresh);
        return h('label', { class: 'perm-option' + (locked ? ' perm-locked' : ''), for: 'perm-' + permission },
          box,
          h('span', { class: 'perm-text' },
            h('span', {}, t('perm.' + permission),
              SENSITIVE.has(permission) ? h('span', { class: 'perm-flag' }, t('roles.sensitive')) : null),
            h('span', { class: 'muted mono perm-key' }, permission),
            locked ? h('span', { class: 'muted perm-key' }, t('roles.notYours')) : null));
      });
      const toggle = h('input', { type: 'checkbox', 'aria-label': t('roles.toggleGroup', { group: t('permGroup.' + group) }) });
      const count = h('span', { class: 'muted perm-count' });
      const editable = () => boxes.map(b => b.querySelector('input')).filter(b => !b.disabled);
      toggle.addEventListener('change', () => {
        editable().forEach(b => { b.checked = toggle.checked; });
        refresh();
      });
      groupSyncs.push(() => {
        const inputs = boxes.map(b => b.querySelector('input'));
        const on = inputs.filter(b => b.checked).length;
        const free = editable();
        const freeOn = free.filter(b => b.checked).length;
        toggle.disabled = free.length === 0;
        toggle.checked = free.length > 0 && freeOn === free.length;
        toggle.indeterminate = freeOn > 0 && freeOn < free.length;
        count.textContent = on + '/' + inputs.length;
      });
      return h('fieldset', { class: 'perm-group' },
        h('legend', { class: 'perm-group-head' },
          h('label', { class: 'perm-group-toggle' }, toggle, ' ', h('strong', {}, t('permGroup.' + group))),
          count),
        h('div', { class: 'perm-options' }, boxes));
    });

    const selectAll = h('button', { class: 'btn btn-small', type: 'button' }, t('roles.selectAll'));
    const clearAll = h('button', { class: 'btn btn-small', type: 'button' }, t('roles.clearAll'));
    const setAll = value => {
      form.querySelectorAll('.perm-option input:not(:disabled)').forEach(b => { b.checked = value; });
      refresh();
    };
    selectAll.addEventListener('click', () => setAll(true));
    clearAll.addEventListener('click', () => setAll(false));

    const save = h('button', { class: 'btn btn-primary', type: 'submit' }, t('roles.save'));
    const cancel = h('button', { class: 'btn', type: 'button' }, t('common.cancel'));
    cancel.addEventListener('click', () => editor.replaceChildren());
    const form = h('form', { class: 'stack panel-section' },
      h('h2', {}, role ? role.name : t('roles.new')),
      h('label', { for: 'role-name' }, t('roles.name')), name,
      h('div', { class: 'perm-toolbar' }, h('strong', {}, t('roles.permissions')), counter, h('span', { class: 'role-buttons' }, selectAll, clearAll)),
      h('div', { class: 'perm-groups' }, sections),
      h('div', { class: 'row-end' }, cancel, save));
    form.addEventListener('submit', e => {
      e.preventDefault();
      const permissions = [...form.querySelectorAll('.perm-option input:checked')].map(b => b.value);
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
    refresh();
    editor.scrollIntoView({ block: 'start', behavior: 'smooth' });
    (role ? form.querySelector('.perm-option input:not(:disabled)') || name : name).focus({ preventScroll: true });
  }

  function summary(role) {
    const total = state.me.allPermissions.length;
    if (role.permissions.length >= total) return h('p', { class: 'muted role-all' }, t('roles.allPermissions'));
    if (!role.permissions.length) return h('p', { class: 'muted role-all' }, t('roles.noPermissions'));
    return h('dl', { class: 'role-perms' }, grouped(role.permissions).flatMap(([group, perms]) => [
      h('dt', {}, t('permGroup.' + group)),
      h('dd', {}, h('ul', { class: 'perm-chips' }, perms.map(p => h('li', { class: 'perm-chip' + (SENSITIVE.has(p) ? ' perm-chip-sensitive' : ''), title: p }, t('perm.' + p)))))
    ]));
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
            h('strong', { class: 'role-name' }, role.name),
            role.builtIn ? h('span', { class: 'badge badge-off' }, t('roles.builtIn')) : null,
            h('span', { class: 'muted' }, t('roles.users', { count: role.users })),
            h('span', { class: 'muted' }, t('roles.permCount', { count: role.permissions.length, total: state.me.allPermissions.length })),
            h('span', { class: 'role-buttons' }, buttons)),
          summary(role));
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
