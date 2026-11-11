const LANGUAGES = ['en', 'pl'];

export const state = { me: null, lang: 'en', strings: {}, fallback: {} };

export function t(key, vars = {}) {
  let text = state.strings[key] ?? state.fallback[key] ?? key;
  for (const [name, value] of Object.entries(vars)) text = text.split('{' + name + '}').join(String(value));
  return text;
}

export function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [key, value] of Object.entries(attrs || {})) {
    if (value === null || value === undefined || value === false) continue;
    if (key === 'class') el.className = value;
    else if (key === 'text') el.textContent = value;
    else if (key.startsWith('on') && typeof value === 'function') el.addEventListener(key.slice(2), value);
    else if (value === true) el.setAttribute(key, '');
    else el.setAttribute(key, value);
  }
  for (const child of children.flat()) {
    if (child === null || child === undefined || child === false) continue;
    el.append(child instanceof Node ? child : document.createTextNode(String(child)));
  }
  return el;
}

export function can(permission) {
  return !!state.me && state.me.permissions.includes(permission);
}

export class ApiError extends Error {
  constructor(status, code, data) {
    super(code);
    this.status = status;
    this.code = code;
    this.data = data;
  }
}

export async function api(method, path, body, options = {}) {
  const headers = {};
  if (state.me) headers['X-CSRF-Token'] = state.me.csrf;
  let payload;
  if (body instanceof Blob) {
    payload = body;
    headers['Content-Type'] = 'application/octet-stream';
  } else if (body !== undefined) {
    payload = JSON.stringify(body);
    headers['Content-Type'] = 'application/json';
  }
  let response;
  try {
    response = await fetch(path, { method, headers, body: payload, credentials: 'same-origin', cache: 'no-store', signal: options.signal });
  } catch (e) {
    if (e.name === 'AbortError') throw e;
    throw new ApiError(0, 'network');
  }
  if (response.status === 204) return null;
  let data = null;
  try { data = await response.json(); } catch (e) { data = null; }
  if (!response.ok) {
    const code = data && data.error ? data.error : 'http_' + response.status;
    if (response.status === 401 && !options.noRedirect) {
      state.me = null;
      go('login');
    }
    if (code === 'password_change_required' && state.me) {
      state.me.mustChange = true;
      go('password');
    }
    throw new ApiError(response.status, code, data);
  }
  return data;
}

export function errorText(error) {
  if (!(error instanceof ApiError)) return t('error.generic', { code: error && error.message ? error.message : '?' });
  const key = 'error.' + error.code;
  return key in state.fallback ? t(key) : t('error.generic', { code: error.code });
}

export function toast(text, kind = 'ok') {
  const box = document.getElementById('toasts');
  const item = h('div', { class: 'toast toast-' + kind, role: kind === 'error' ? 'alert' : 'status' }, text);
  box.append(item);
  setTimeout(() => item.remove(), kind === 'error' ? 8000 : 4000);
}

export function confirmDialog(message, options = {}) {
  return new Promise(resolve => {
    const input = options.typed ? h('input', { type: 'text', autocomplete: 'off', spellcheck: 'false', 'aria-label': options.typed }) : null;
    const ok = h('button', { class: 'btn ' + (options.danger ? 'btn-danger' : 'btn-primary'), type: 'submit' }, options.confirm || t('common.confirm'));
    const cancel = h('button', { class: 'btn', type: 'button' }, t('common.cancel'));
    const form = h('form', { method: 'dialog', class: 'dialog-body' },
      h('p', {}, message), input,
      h('div', { class: 'row-end' }, cancel, ok));
    const dialog = h('dialog', { class: 'dialog' }, form);
    if (input) {
      ok.disabled = true;
      input.addEventListener('input', () => { ok.disabled = input.value !== options.typed; });
    }
    let answer = false;
    form.addEventListener('submit', () => { answer = input ? input.value === options.typed : true; });
    cancel.addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => { dialog.remove(); resolve(answer); });
    document.body.append(dialog);
    dialog.showModal();
    (input || ok).focus();
  });
}

export async function busy(button, task) {
  const label = button.textContent;
  button.disabled = true;
  button.textContent = t('common.working');
  try {
    return await task();
  } finally {
    button.disabled = false;
    button.textContent = label;
  }
}

export function formatTime(millis) {
  return new Date(millis).toLocaleString(state.lang === 'pl' ? 'pl-PL' : 'en-GB', { dateStyle: 'short', timeStyle: 'medium' });
}

export function formatDuration(seconds) {
  seconds = Math.max(0, Math.floor(seconds));
  const d = Math.floor(seconds / 86400), hr = Math.floor(seconds % 86400 / 3600), m = Math.floor(seconds % 3600 / 60), s = seconds % 60;
  if (d > 0) return d + 'd ' + hr + 'h';
  if (hr > 0) return hr + 'h ' + String(m).padStart(2, '0') + 'm';
  return m + ':' + String(s).padStart(2, '0');
}

export function formatUptime(seconds) {
  seconds = Math.max(0, Math.floor(seconds));
  const d = Math.floor(seconds / 86400), hr = Math.floor(seconds % 86400 / 3600), m = Math.floor(seconds % 3600 / 60);
  if (d > 0) return d + ' d ' + hr + ' h';
  if (hr > 0) return hr + ' h ' + m + ' min';
  return Math.max(1, m) + ' min';
}

export function go(route) {
  const target = '#/' + route;
  if (location.hash === target) render();
  else location.hash = target;
}

async function loadStrings(lang) {
  const response = await fetch('/i18n/' + lang + '.json', { cache: 'no-cache' });
  return response.ok ? response.json() : {};
}

function pickLanguage(serverDefault) {
  let saved = null;
  try { saved = localStorage.getItem('nbw-lang'); } catch (e) { saved = null; }
  if (LANGUAGES.includes(saved)) return saved;
  for (const wanted of navigator.languages || [navigator.language]) {
    const code = String(wanted || '').toLowerCase().split('-')[0];
    if (LANGUAGES.includes(code)) return code;
  }
  return LANGUAGES.includes(serverDefault) ? serverDefault : 'en';
}

export async function setLanguage(lang) {
  state.lang = lang;
  try { localStorage.setItem('nbw-lang', lang); } catch (e) { }
  state.strings = lang === 'en' ? state.fallback : await loadStrings(lang);
  document.documentElement.lang = lang;
  document.title = t('app.name');
  render();
}

const ROUTES = {
  login: { module: './views/login.js', public: true },
  password: { module: './views/password.js', shell: true },
  dashboard: { module: './views/dashboard.js', shell: true, permission: 'dashboard.view' },
  arena: { module: './views/arena.js', shell: true, permission: 'dashboard.view' },
  stats: { module: './views/stats.js', shell: true, permission: 'stats.view' },
  shop: { module: './views/shop.js', shell: true, permission: 'shop.edit' },
  users: { module: './views/users.js', shell: true, permission: 'users.manage' },
  roles: { module: './views/roles.js', shell: true, permission: 'roles.manage' },
  audit: { module: './views/audit.js', shell: true, permission: 'audit.view' },
};

const NAV = [
  ['dashboard', 'nav.dashboard', 'dashboard.view'],
  ['stats', 'nav.stats', 'stats.view'],
  ['shop', 'nav.shop', 'shop.edit'],
  ['users', 'nav.users', 'users.manage'],
  ['roles', 'nav.roles', 'roles.manage'],
  ['audit', 'nav.audit', 'audit.view'],
];

let cleanup = null;
let renderId = 0;

function homeRoute() {
  if (!state.me) return 'login';
  if (state.me.mustChange) return 'password';
  const first = NAV.find(([, , permission]) => can(permission));
  return first ? first[0] : 'password';
}

function shell(active, content) {
  const links = NAV.filter(([, , permission]) => can(permission)).map(([route, key]) =>
    h('a', { href: '#/' + route, class: 'nav-link' + (active === route ? ' is-active' : ''), 'aria-current': active === route ? 'page' : null }, t(key)));
  const language = h('select', { class: 'lang', 'aria-label': 'Language / Język', onchange: e => setLanguage(e.target.value) },
    LANGUAGES.map(code => h('option', { value: code, selected: code === state.lang }, code.toUpperCase())));
  const logout = h('button', { class: 'btn btn-quiet', type: 'button', onclick: async () => {
    try { await api('POST', '/api/logout', {}, { noRedirect: true }); } catch (e) { }
    state.me = null;
    go('login');
  } }, t('nav.logout'));
  return h('div', { class: 'shell' },
    h('header', { class: 'topbar' },
      h('a', { href: '#/', class: 'brand' }, h('span', { class: 'brand-mark', 'aria-hidden': 'true' }), t('app.name')),
      h('nav', { class: 'nav', 'aria-label': 'Main' }, state.me && !state.me.mustChange ? links : []),
      h('div', { class: 'account' },
        h('span', { class: 'account-name' }, t('nav.signedIn', { name: state.me.name, role: state.me.role })),
        h('a', { href: '#/password', class: 'nav-link' + (active === 'password' ? ' is-active' : '') }, t('nav.password')),
        language, logout)),
    h('main', { class: 'content', id: 'main' }, content));
}

export async function render() {
  const id = ++renderId;
  if (cleanup) { try { cleanup(); } catch (e) { } cleanup = null; }
  const [name, ...rest] = location.hash.replace(/^#\/?/, '').split('/');
  let routeName = name || homeRoute();
  let route = ROUTES[routeName];
  if (!route) { routeName = homeRoute(); route = ROUTES[routeName]; }
  if (!route.public && !state.me) { routeName = 'login'; route = ROUTES.login; }
  else if (state.me && state.me.mustChange && routeName !== 'password') { routeName = 'password'; route = ROUTES.password; }
  else if (route.permission && !can(route.permission)) { routeName = homeRoute(); route = ROUTES[routeName]; }
  if (routeName === 'login' && state.me) { routeName = homeRoute(); route = ROUTES[routeName]; }

  const view = await import(route.module);
  if (id !== renderId) return;
  const root = h('div', { class: 'view view-' + routeName });
  const app = document.getElementById('app');
  app.replaceChildren(route.shell ? shell(routeName, root) : root);
  const params = rest.map(decodeURIComponent);
  cleanup = view.render(root, params) || null;
  const heading = root.querySelector('h1');
  if (heading) { heading.tabIndex = -1; heading.focus({ preventScroll: true }); }
}

export function setMe(me) {
  state.me = me;
}

async function start() {
  state.fallback = await loadStrings('en');
  let info = { language: 'en' };
  try { info = await api('GET', '/api/info'); } catch (e) { }
  try { state.me = await api('GET', '/api/me', undefined, { noRedirect: true }); } catch (e) { state.me = null; }
  window.addEventListener('hashchange', render);
  await setLanguage(pickLanguage(info.language));
}

start();
