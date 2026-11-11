import { api, errorText, go, h, setMe, state, t, toast } from '../app.js';

export function render(root) {
  const field = (id, key, autocomplete) => [
    h('label', { for: id }, t(key)),
    h('input', { id, type: 'password', autocomplete, required: true, maxlength: '128' })];
  const [currentLabel, current] = field('pw-current', 'password.current', 'current-password');
  const [nextLabel, next] = field('pw-new', 'password.new', 'new-password');
  const [repeatLabel, repeat] = field('pw-repeat', 'password.repeat', 'new-password');
  next.minLength = 10;
  const error = h('p', { class: 'form-error', role: 'alert' });
  const submit = h('button', { class: 'btn btn-primary', type: 'submit' }, t('password.submit'));
  const user = h('input', { type: 'text', autocomplete: 'username', value: state.me.name, hidden: true, readonly: true });
  const form = h('form', { class: 'stack narrow' }, user, currentLabel, current, nextLabel, next, repeatLabel, repeat,
    h('p', { class: 'hint' }, t('password.hint')), error, submit);
  form.addEventListener('submit', async e => {
    e.preventDefault();
    error.textContent = '';
    if (next.value !== repeat.value) { error.textContent = t('password.mismatch'); repeat.focus(); return; }
    submit.disabled = true;
    try {
      const me = await api('POST', '/api/password', { current: current.value, password: next.value });
      setMe(me);
      toast(t('password.changed'));
      go('');
    } catch (err) {
      error.textContent = errorText(err);
    } finally {
      submit.disabled = false;
    }
  });
  root.append(h('h1', {}, t('password.title')),
    state.me.mustChange ? h('p', { class: 'notice' }, t('password.required')) : null,
    form);
}
