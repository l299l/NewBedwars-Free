import { api, errorText, go, h, setMe, t } from '../app.js';

export function render(root) {
  const name = h('input', { id: 'login-name', name: 'username', autocomplete: 'username', required: true, maxlength: '64', autocapitalize: 'off', spellcheck: 'false' });
  const password = h('input', { id: 'login-password', name: 'password', type: 'password', autocomplete: 'current-password', required: true, maxlength: '128' });
  const error = h('p', { class: 'form-error', role: 'alert' });
  const submit = h('button', { class: 'btn btn-primary btn-wide', type: 'submit' }, t('login.submit'));
  const form = h('form', { class: 'stack', novalidate: false },
    h('label', { for: 'login-name' }, t('login.name')), name,
    h('label', { for: 'login-password' }, t('login.password')), password,
    error, submit);
  form.addEventListener('submit', async e => {
    e.preventDefault();
    error.textContent = '';
    submit.disabled = true;
    try {
      const me = await api('POST', '/api/login', { name: name.value.trim(), password: password.value }, { noRedirect: true });
      setMe(me);
      go(me.mustChange ? 'password' : '');
    } catch (err) {
      password.value = '';
      if (err.code === 'bad_login') error.textContent = t('login.failed');
      else if (err.code === 'locked') error.textContent = t('login.locked', { minutes: Math.ceil(((err.data && err.data.retryAfter) || 60) / 60) });
      else error.textContent = errorText(err);
      password.focus();
    } finally {
      submit.disabled = false;
    }
  });
  root.append(h('section', { class: 'login' },
    h('div', { class: 'login-beds', 'aria-hidden': 'true' },
      ['#FF5555', '#5555FF', '#55FF55', '#FFFF55'].map(color => {
        const bed = h('span', { class: 'bed' });
        bed.style.setProperty('--team', color);
        return bed;
      })),
    h('h1', {}, t('login.title')),
    h('p', { class: 'muted' }, t('login.intro')),
    form));
  name.focus();
}
