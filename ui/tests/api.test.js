import test from 'node:test';
import assert from 'node:assert/strict';
import { API_BASE, ApiError, createGameApi } from '../src/services/gameApi.js';

test('all documented endpoints use the real origin, correct verbs, and escaped path segments', async () => {
  const calls = [];
  const api = createGameApi({ fetcher: async (url, options) => { calls.push({ url, ...options }); return Response.json({ ok: true }); } });
  await api.startGame();
  await api.getAds('game/a');
  await api.solveAd('game/a', 'decoded+id');
  await api.getShop('game/a');
  await api.buyItem('game/a', 'hpot');
  await api.investigateReputation('game/a');
  assert.deepEqual(calls.map(({ url, method }) => [url, method]), [
    [`${API_BASE}/game/start`, 'POST'], [`${API_BASE}/game%2Fa/messages`, 'GET'],
    [`${API_BASE}/game%2Fa/solve/decoded%2Bid`, 'POST'], [`${API_BASE}/game%2Fa/shop`, 'GET'],
    [`${API_BASE}/game%2Fa/shop/buy/hpot`, 'POST'],
    [`${API_BASE}/game%2Fa/investigate/reputation`, 'POST'],
  ]);
  assert.ok(calls.every((call) => call.credentials === 'omit' && call.cache === 'no-store'));
});

test('server errors, invalid JSON, and network loss never automatically retry mutations', async () => {
  for (const fetcher of [async () => new Response('', { status: 503 }), async () => new Response('invalid json'), async () => { throw new TypeError('Offline'); }]) {
    let calls = 0;
    const api = createGameApi({ fetcher: (...args) => { calls++; return fetcher(...args); } });
    await assert.rejects(api.solveAd('game', 'quest'), (error) => error instanceof ApiError && error.uncertain);
    assert.equal(calls, 1);
  }
});

test('read failures and rejected client requests do not claim an uncertain action outcome', async () => {
  const offline = createGameApi({ fetcher: async () => { throw new Error('offline'); } });
  await assert.rejects(offline.getAds('game'), (error) => !error.uncertain);
  const rejected = createGameApi({ fetcher: async () => new Response('', { status: 400 }) });
  await assert.rejects(rejected.buyItem('game', 'hpot'), (error) => error.status === 400 && !error.uncertain);
});

test('timeouts abort outstanding requests and preserve mutation uncertainty', async () => {
  const api = createGameApi({ timeout: 10, fetcher: (_url, { signal }) => new Promise((_resolve, reject) => signal.addEventListener('abort', () => reject(new Error('aborted')))) });
  await assert.rejects(api.buyItem('game', 'hpot'), (error) => error.uncertain && /timed out/.test(error.message));
});
