import { api } from './app.js';

const INTERVAL = 2000;
const listeners = new Set();
let latest = null;
let rev = null;
let timer = null;
let failed = false;

async function tick() {
  timer = null;
  if (listeners.size === 0) return;
  if (!document.hidden) {
    try {
      const data = await api('GET', '/api/dashboard' + (rev === null ? '' : '?rev=' + rev));
      failed = false;
      if (data) {
        rev = data.rev;
        latest = data.data;
      }
    } catch (e) {
      failed = true;
    }
    for (const listener of listeners) listener(latest, failed);
  }
  if (listeners.size > 0) timer = setTimeout(tick, INTERVAL);
}

export function watch(listener) {
  listeners.add(listener);
  if (latest) listener(latest, failed);
  if (!timer) timer = setTimeout(tick, 0);
  return () => {
    listeners.delete(listener);
    if (listeners.size === 0 && timer) { clearTimeout(timer); timer = null; }
  };
}

export function refresh() {
  if (timer) clearTimeout(timer);
  timer = setTimeout(tick, 0);
}

document.addEventListener('visibilitychange', () => { if (!document.hidden && listeners.size > 0) refresh(); });
