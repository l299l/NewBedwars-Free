import { api, isGateway, state } from './app.js';

const INTERVAL = 2000;
const listeners = new Set();
let local = null;
let network = null;
let rev = null;
let timer = null;
let failed = false;

function selected() {
  if (!network) return local && !state.server ? { data: local, status: 'ONLINE' } : null;
  const wanted = state.server;
  const found = network.servers.find(s => wanted ? s.id === wanted : s.local);
  return found ? { data: found.data || null, status: found.status } : { data: null, status: 'UNKNOWN' };
}

function notify(listener) {
  const current = selected();
  if (!current && !network && !failed) return;
  listener(current ? current.data : null, failed, network, current ? current.status : 'UNKNOWN');
}

async function tick() {
  timer = null;
  if (listeners.size === 0) return;
  if (!document.hidden) {
    try {
      if (isGateway()) {
        network = await api('GET', '/api/network', undefined, { server: null });
      } else {
        network = null;
        const data = await api('GET', '/api/dashboard' + (rev === null ? '' : '?rev=' + rev), undefined, { server: null });
        if (data) {
          rev = data.rev;
          local = data.data;
        }
      }
      failed = false;
    } catch (e) {
      failed = true;
    }
    for (const listener of listeners) notify(listener);
  }
  if (listeners.size > 0) timer = setTimeout(tick, INTERVAL);
}

export function watch(listener) {
  listeners.add(listener);
  notify(listener);
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
