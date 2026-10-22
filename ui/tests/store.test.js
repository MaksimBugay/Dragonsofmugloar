import test from 'node:test';
import assert from 'node:assert/strict';
import { createGameStore } from '../src/stores/gameStore.js';
import { ApiError } from '../src/services/gameApi.js';

const base = { gameId: 'game', lives: 3, gold: 100, level: 0, score: 20, highScore: 80, turn: 0 };
const rawAd = { adId: 'quest', message: 'Help the village', reward: 70, expiresIn: 4, encrypted: null, probability: 'Sure thing' };
const potion = { id: 'hpot', name: 'Healing potion', cost: 50 };
const result = { success: true, lives: 3, gold: 170, score: 90, highScore: 90, turn: 1, message: 'Success' };
const deferred = () => { let resolve; let reject; const promise = new Promise((a, b) => { resolve = a; reject = b; }); return { promise, resolve, reject }; };
const fixture = (overrides = {}) => {
  const api = {
    startGame: async () => ({ ...base }), getAds: async () => [{ ...rawAd }],
    getShop: async () => [{ ...potion }], solveAd: async () => ({ ...result }),
    buyItem: async () => ({ shoppingSuccess: true, lives: 4, gold: 120, level: 0, turn: 2 }),
    ...overrides,
  };
  return { store: createGameStore(api), api };
};

test('start hides intro immediately, suppresses duplicate starts, and initializes real returned state', async () => {
  const start = deferred(); let calls = 0;
  const { store } = fixture({ startGame: () => { calls++; return start.promise; } });
  const first = store.startGame();
  assert.equal(store.phase.value, 'starting');
  await store.startGame();
  start.resolve({ ...base }); await first;
  assert.equal(calls, 1);
  assert.equal(store.phase.value, 'active');
  assert.equal(store.game.value.gameId, 'game');
  assert.equal(store.ads.value.length, 1);
  assert.equal(store.history.value.length, 0);
});

test('failed starts return to the intro and remain retryable', async () => {
  const { store } = fixture({ startGame: async () => { throw new Error('Offline'); } });
  await store.startGame();
  assert.equal(store.phase.value, 'intro');
  assert.equal(store.game.value, null);
  assert.equal(store.busy.value, false);
  assert.equal(store.actionError.value, 'Offline');
});

test('solve and purchase share one history with accurate immediate-state snapshots', async () => {
  const { store } = fixture();
  await store.startGame(); await store.refreshShop();
  await store.solveAd(store.ads.value[0]);
  await store.buyItem(store.items.value[0]);
  assert.equal(store.history.value.length, 2);
  assert.equal(store.history.value[0].differences.gold, 70);
  assert.equal(store.history.value[1].differences.gold, -50);
  assert.equal(store.history.value[1].differences.lives, 1);
  assert.equal(store.history.value[1].differences.score, 0);
  assert.equal(store.game.value.score, 90);
  assert.equal(store.game.value.highScore, 90);
  assert.equal(store.game.value.turn, 2);
});

test('failure outcomes still advance the turn and are saved as failures', async () => {
  const { store } = fixture({
    solveAd: async () => ({ ...result, success: false, lives: 2, gold: 100, score: 20, highScore: 80 }),
    buyItem: async () => ({ shoppingSuccess: false, lives: 2, gold: 100, level: 0, turn: 2 }),
  });
  await store.startGame(); await store.refreshShop(); await store.solveAd(store.ads.value[0]); await store.buyItem(store.items.value[0]);
  assert.equal(store.game.value.turn, 2);
  assert.equal(store.history.value[0].success, false);
  assert.equal(store.history.value[0].differences.lives, -1);
  assert.equal(store.history.value[1].success, false);
  assert.equal(store.history.value[1].differences.gold, 0);
});

test('double clicks and cross-action clicks cannot submit simultaneous mutations', async () => {
  const action = deferred(); let solves = 0; let purchases = 0;
  const { store } = fixture({ solveAd: () => { solves++; return action.promise; }, buyItem: () => { purchases++; } });
  await store.startGame(); await store.refreshShop();
  const first = store.solveAd(store.ads.value[0]);
  await store.solveAd(store.ads.value[0]); await store.buyItem(store.items.value[0]);
  action.resolve({ ...result }); await first;
  assert.equal(solves, 1); assert.equal(purchases, 0); assert.equal(store.history.value.length, 1);
});

test('failed refresh after a successful action preserves committed state and journal and ages cached ads', async () => {
  let reads = 0;
  const { store } = fixture({ getAds: async () => { if (++reads > 1) throw new Error('Read failed'); return [{ ...rawAd }, { ...rawAd, adId: 'remaining', expiresIn: 1 }]; } });
  await store.startGame(); await store.solveAd(store.ads.value.find((ad) => ad.adId === 'quest'));
  assert.equal(store.game.value.gold, 170);
  assert.equal(store.history.value.length, 1);
  assert.equal(store.actionError.value, '');
  assert.equal(store.adsError.value, 'Read failed');
  assert.equal(store.adsStale.value, true);
  assert.equal(store.ads.value[0].expiresIn, 0);
  assert.equal(store.ads.value.length, 1);
});

test('uncertain mutation outcomes freeze actions without fabricating a result', async () => {
  const { store } = fixture({ solveAd: async () => { throw new ApiError('Network loss', { uncertain: true }); } });
  await store.startGame(); await store.solveAd(store.ads.value[0]);
  assert.equal(store.history.value.length, 0);
  assert.equal(store.game.value.turn, 0);
  assert.equal(store.canAct.value, false);
  assert.equal(store.uncertain.value, true);
});

test('malformed successful HTTP results are treated as uncertain instead of corrupting state', async () => {
  const { store } = fixture({ solveAd: async () => ({ success: true }) });
  await store.startGame(); await store.solveAd(store.ads.value[0]);
  assert.equal(store.uncertain.value, true);
  assert.equal(store.game.value.lives, 3);
  assert.equal(store.history.value.length, 0);
});

test('expired ads, unavailable items, and unaffordable items never issue mutation requests', async () => {
  let calls = 0;
  const { store } = fixture({ getAds: async () => [{ ...rawAd, expiresIn: 0 }], getShop: async () => [{ ...potion, cost: 300 }], solveAd: async () => { calls++; }, buyItem: async () => { calls++; } });
  await store.startGame(); await store.refreshShop(); await store.solveAd(store.ads.value[0]); await store.buyItem(store.items.value[0]); await store.buyItem({ id: 'missing' });
  assert.equal(calls, 0);
});

test('late old-game ad and shop responses cannot replace a newer adventure', async () => {
  const oldAds = deferred(); const oldShop = deferred(); let starts = 0; let reads = 0; let shops = 0;
  const { store } = fixture({
    startGame: async () => ({ ...base, gameId: `game-${++starts}` }),
    getAds: async () => ++reads === 2 ? oldAds.promise : [{ ...rawAd, adId: `quest-${starts}` }],
    getShop: async () => ++shops === 1 ? oldShop.promise : [{ ...potion }],
  });
  await store.startGame();
  const firstRead = store.refreshAds(); const firstShop = store.refreshShop();
  await store.startGame();
  oldAds.resolve([{ ...rawAd, adId: 'old' }]); oldShop.resolve([{ ...potion, name: 'old' }]);
  await Promise.all([firstRead, firstShop]);
  assert.equal(store.game.value.gameId, 'game-2');
  assert.equal(store.ads.value[0].adId, 'quest-2');
  assert.equal(store.items.value.length, 0);
});

test('an earlier board response cannot overwrite the board after a purchase advances the turn', async () => {
  const old = deferred(); let reads = 0;
  const { store } = fixture({ getAds: async () => ++reads === 2 ? old.promise : [{ ...rawAd, adId: `quest-${reads}` }] });
  await store.startGame(); await store.refreshShop();
  const first = store.refreshAds();
  await store.buyItem(store.items.value[0]);
  old.resolve([{ ...rawAd, adId: 'stale' }]); await first;
  assert.equal(store.ads.value[0].adId, 'quest-3');
});

test('last-life failure ends the game and a new adventure resets its in-memory history', async () => {
  const { store } = fixture({ solveAd: async () => ({ ...result, success: false, lives: 0, gold: 100, score: 20, highScore: 80 }) });
  await store.startGame(); await store.solveAd(store.ads.value[0]);
  assert.equal(store.gameOver.value, true); assert.equal(store.canAct.value, false);
  assert.equal(store.history.value.length, 1);
  await store.startGame();
  assert.equal(store.history.value.length, 0); assert.equal(store.gameOver.value, false);
});

for (const type of ['solve', 'purchase']) {
  for (const success of [true, false]) {
    for (const turn of [0, 1]) {
      test(`${type} with success=${success} and turn=${turn} refreshes the board only when the turn changes`, async () => {
        let reads = 0;
        const { store } = fixture({
          getAds: async () => { reads++; return [{ ...rawAd }]; },
          solveAd: async () => ({ ...result, success, turn }),
          buyItem: async () => ({ shoppingSuccess: success, lives: 3, gold: 100, level: 0, turn }),
        });
        await store.startGame();
        await store.refreshShop();
        const beforeAds = JSON.parse(JSON.stringify(store.ads.value));
        if (type === 'solve') await store.solveAd(store.ads.value[0]);
        else await store.buyItem(store.items.value[0]);
        assert.equal(reads, turn === 0 ? 1 : 2);
        assert.equal(store.game.value.turn, turn);
        assert.equal(store.history.value.length, 1);
        assert.equal(store.uncertain.value, false);
        assert.equal(store.adsStale.value, false);
        if (turn === 0) assert.deepEqual(store.ads.value, beforeAds);
        await store.refreshAds();
        assert.equal(reads, turn === 0 ? 2 : 3);
      });
    }
  }
}

test('a rejected solve preserves the board until the user refreshes it', async () => {
  let reads = 0;
  const { store } = fixture({
    getAds: async () => [{ ...rawAd, adId: `quest-${++reads}` }],
    solveAd: async () => { throw new ApiError('Quest unavailable', { status: 404 }); },
  });
  await store.startGame();
  await store.solveAd(store.ads.value[0]);
  assert.equal(reads, 1);
  assert.equal(store.game.value.turn, 0);
  assert.equal(store.history.value.length, 0);
  assert.equal(store.actionError.value, 'Quest unavailable');
  assert.equal(store.ads.value[0].adId, 'quest-1');
  await store.refreshAds();
  assert.equal(reads, 2);
  assert.equal(store.ads.value[0].adId, 'quest-2');
});
