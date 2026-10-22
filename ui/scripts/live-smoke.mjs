import assert from 'node:assert/strict';
import { setTimeout as delay } from 'node:timers/promises';
import { createGameApi } from '../src/services/gameApi.js';
import { decodeAd, rankAds } from '../src/domain/ads.js';
import { initialGame, applyAction, adventureEvent } from '../src/domain/game.js';
import { shopItem } from '../src/domain/shop.js';

// Opt-in: this creates a separate real game and performs real gameplay actions.
const api = createGameApi();
let state = initialGame(await api.startGame());
const journal = [];
console.log(`Started isolated live game ${state.gameId}.`);
const act = async (type, subject) => {
  await delay(700);
  const result = type === 'solve'
    ? await api.solveAd(state.gameId, subject.adId)
    : await api.buyItem(state.gameId, subject.id);
  const after = applyAction(state, type, result);
  const event = adventureEvent(type, subject, result, state, after);
  journal.push(event);
  state = after;
  console.log(JSON.stringify({ type, success: event.success, turn: state.turn, differences: event.differences }));
  return event;
};
await delay(700);
const items = (await api.getShop(state.gameId)).map(shopItem);
assert.equal(items.length, 11);
const rejected = await act('purchase', items.find((item) => item.id === 'rf'));
assert.equal(rejected.success, false);
assert.equal(rejected.differences.turn, 1);
for (let attempts = 0; attempts < 8 && state.gold < 150 && state.lives > 0; attempts++) {
  await delay(700);
  const ads = rankAds((await api.getAds(state.gameId)).map(decodeAd));
  assert.ok(ads.every((ad) => ad.adId && ad.message && ad.probability));
  const selected = ads.find((ad) => ad.expiresIn > 0 && !ad.message.toLowerCase().startsWith('steal super awesome diamond '));
  assert.ok(selected, 'A non-expired quest is available');
  const event = await act('solve', selected);
  assert.equal(event.differences.gold, event.success ? selected.reward : 0);
  assert.equal(event.differences.score, event.success ? selected.reward : 0);
}
assert.ok(state.gold >= 150 && state.lives > 0, 'Live run earned enough gold for healing and an upgrade; rerun if random outcomes prevented this.');
const healing = await act('purchase', items.find((item) => item.id === 'hpot'));
assert.equal(healing.success, true); assert.equal(healing.differences.lives, 1); assert.equal(healing.differences.gold, -50);
const upgrade = await act('purchase', items.find((item) => item.id === 'cs'));
assert.equal(upgrade.success, true); assert.equal(upgrade.differences.level, 1);
assert.equal(upgrade.differences.score, 0);
await delay(700);
assert.ok((await api.getAds(state.gameId)).map(decodeAd).length);
console.log(`Live API smoke passed: ${journal.length} recorded actions, final turn ${state.turn}.`);
