import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { decodeText, decodeAd, expectedValue, rankAds, adStatus, ageAds, isTrapAd, adReputation } from '../src/domain/ads.js';
import { AD_PROBABILITIES as P, SHOP_ITEM_FEATURES } from '../src/domain/java-models.js';
import { initialGame, applyAction, adventureEvent } from '../src/domain/game.js';
import { shopItem } from '../src/domain/shop.js';

const ad = (overrides = {}) => ({ adId: 'quest', message: 'Help the kingdom.', reward: 100, expiresIn: 3, probability: P.PIECE_OF_CAKE, ...overrides });
const initial = () => initialGame({ gameId: 'game', lives: 3, gold: 100, level: 2, score: 150, highScore: 200, turn: 8 });

test('decodes all three Base64 fields as UTF-8 using the findings example', () => {
  const result = decodeAd({ adId: 'WFVMSnFHSzA=', message: 'SW52ZXN0aWdhdGUgTmV6acyHaGUgT3JhbmdlIGFuZCBmaW5kIG91dCB0aGVpciByZWxhdGlvbiB0byB0aGUgbWFnaWMgcGFuLg==', reward: 194, expiresIn: 3, encrypted: 1, probability: 'U3VpY2lkZSBtaXNzaW9u' });
  assert.equal(result.adId, 'XULJqGK0');
  assert.equal(result.message, 'Investigate Nezi̇he Orange and find out their relation to the magic pan.');
  assert.equal(result.probability.key, 'SUICIDE_MISSION');
  assert.equal(result.reward, 194);
  assert.equal(result.encrypted, 1);
});

test('ROT13 decodes IDs and labels while preserving Unicode, punctuation, and digits', () => {
  const result = decodeAd({ adId: 'QScWIzOx', message: 'Xvyy Nqqvr Fcbbare jvgu png', reward: 166, expiresIn: 2, encrypted: 2, probability: 'Fhvpvqr zvffvba' });
  assert.equal(result.adId, 'DFpJVmBk');
  assert.equal(result.message, 'Kill Addie Spooner with cat');
  assert.equal(result.probability.key, 'SUICIDE_MISSION');
  assert.equal(decodeText('Nn Mm ðí! 42', 2), 'Aa Zz ðí! 42');
});

test('plain, unknown probability, invalid Base64, and unsupported encryption follow the adapter', () => {
  for (const marker of [null, undefined, 0]) assert.equal(decodeText('Readable', marker), 'Readable');
  assert.equal(decodeText(null, 1), null);
  assert.throws(() => decodeText('anything', 3), /Unsupported ad encryption/);
  assert.throws(() => decodeText('!!bad!!', 1));
  assert.equal(decodeAd({ adId: 'x', message: 'quest', reward: '42', expiresIn: 1, probability: 'New label' }).probability.key, 'UNKNOWN');
  assert.throws(() => decodeAd({ adId: 'x', reward: null, expiresIn: 1 }));
});

test('expected value uses calibrated Java probability and healing cost', () => {
  assert.equal(expectedValue(ad()), 88);
  assert.equal(expectedValue(ad({ probability: P.RISKY })), 10);
  assert.equal(expectedValue(ad({ probability: P.IMPOSSIBLE, reward: 50 })), -49);
});

test('value ranking breaks ties by probability and expiry, always putting expired ads last', () => {
  const candidates = [
    ad({ adId: 'expired', reward: 10000, expiresIn: 0 }),
    ad({ adId: 'late', expiresIn: 5 }),
    ad({ adId: 'early', expiresIn: 2 }),
    ad({ adId: 'risky', probability: P.RISKY, reward: 100 }),
    ad({ adId: 'negative-expiry', reward: 20000, expiresIn: -1 }),
  ];
  assert.deepEqual(rankAds(candidates).map((a) => a.adId), ['early', 'late', 'risky', 'negative-expiry', 'expired']);
  assert.equal(candidates[0].adId, 'expired');
  const equalValue = [
    ad({ adId: 'lower', probability: P.RISKY, reward: 200 }),
    ad({ adId: 'higher', probability: P.HMMM, reward: 150 }),
  ];
  assert.deepEqual(rankAds(equalValue).map((a) => a.adId), ['higher', 'lower']);
});

test('visual bands reuse GameConfig easy labels and the AdProbability risky threshold', () => {
  for (const probability of [P.SURE_THING, P.PIECE_OF_CAKE, P.WALK_IN_THE_PARK, P.QUITE_LIKELY]) assert.equal(adStatus(ad({ probability })), 'easy');
  for (const probability of [P.RISKY, P.PLAYING_WITH_FIRE, P.RATHER_DETRIMENTAL, P.SUICIDE_MISSION, P.IMPOSSIBLE]) assert.equal(adStatus(ad({ probability })), 'risky');
  for (const probability of [P.HMMM, P.GAMBLE, P.UNKNOWN]) assert.equal(adStatus(ad({ probability })), 'normal');
  assert.equal(adStatus(ad({ expiresIn: 0 })), 'expired');
});

test('local expiry ages by actual turn delta and removes only the attempted ad', () => {
  const before = [ad({ adId: 'attempted' }), ad({ adId: 'retained', expiresIn: 1 })];
  assert.deepEqual(ageAds(before, 2, 'attempted').map(({ adId, expiresIn }) => ({ adId, expiresIn })), [{ adId: 'retained', expiresIn: 0 }]);
  assert.equal(before[1].expiresIn, 1);
});

test('solve and purchase responses preserve omitted state and use immediately previous deltas', () => {
  const before = initial();
  const solve = { success: true, lives: 3, gold: 170, score: 220, highScore: 220, turn: 9, message: 'Success' };
  const solved = applyAction(before, 'solve', solve);
  assert.equal(solved.level, 2);
  const purchase = { shoppingSuccess: true, gold: 120, lives: 4, level: 2, turn: 10 };
  const after = applyAction(solved, 'purchase', purchase);
  assert.equal(after.score, 220);
  assert.equal(after.highScore, 220);
  const event = adventureEvent('purchase', { id: 'hpot' }, purchase, solved, after);
  assert.deepEqual(event.differences, { lives: 1, gold: -50, level: 0, score: 0, highScore: 0, turn: 1 });
  assert.equal(event.before.gold, 170);
  after.gold = 0;
  assert.equal(event.after.gold, 120);
  assert.throws(() => applyAction(before, 'solve', { ...solve, success: 'false' }));
  assert.throws(() => applyAction(before, 'solve', { ...solve, turn: 7 }));
});

test('all eleven shop items retain live names/costs and observed effects', () => {
  assert.equal(Object.keys(SHOP_ITEM_FEATURES).length, 11);
  for (const [id, features] of Object.entries(SHOP_ITEM_FEATURES)) {
    const item = shopItem({ id, name: `Live name ${id}`, cost: 123 });
    assert.equal(item.name, `Live name ${id}`);
    assert.equal(item.cost, 123);
    assert.equal(features.livesDiff, id === 'hpot' ? 1 : 0);
    assert.equal(features.levelDiff, id === 'hpot' ? 0 : ['ch', 'rf', 'iron', 'mtrix', 'wingpotmax'].includes(id) ? 2 : 1);
  }
  assert.throws(() => shopItem({ id: 'unknown', name: 'Unknown', cost: 1 }), /Unknown shop item/);
});

test('the imported story retains two paragraphs and official attribution', async () => {
  const story = JSON.parse(await readFile(new URL('../src/content/story.json', import.meta.url), 'utf8'));
  assert.equal(story.source, 'https://dragonsofmugloar.com/');
  assert.equal(story.paragraphs.length, 2);
  assert.ok(story.paragraphs.every((paragraph) => paragraph.length > 400));
  assert.match(story.attribution, /Copyright © 2018 Bigbank AS/);
});

test('trap detection matches the decoded message prefix regardless of case', () => {
  const message = 'Steal super awesome diamond from the royal vault';
  for (const encrypted of [0, 1, 2]) {
    const encode = (text) => encrypted === 1 ? btoa(text) : encrypted === 2 ? decodeText(text, 2) : text;
    const decoded = decodeAd({ adId: encode('trap'), message: encode(message), probability: encode('Sure thing'), reward: 100, expiresIn: 3, encrypted });
    assert.equal(isTrapAd(decoded), true);
  }
  assert.equal(isTrapAd(ad({ message: message.toUpperCase() })), true);
  assert.equal(isTrapAd(ad({ message: 'Help recover the Steal super awesome diamond notice' })), false);
  assert.equal(isTrapAd(ad()), false);
});

test('active diamond traps have their own category regardless of the advertised probability', () => {
  const message = 'Steal super awesome diamond from the royal vault';
  for (const probability of Object.values(P)) {
    const trap = ad({ message, probability });
    assert.equal(adStatus(trap), 'trap');
    for (const other of ['easy', 'normal', 'risky']) assert.notEqual(adStatus(trap), other);
  }
  assert.equal(adStatus(ad({ message, expiresIn: 0 })), 'expired');
  assert.equal(adStatus(ad({ message, expiresIn: -1 })), 'expired');
});

test('reputation badges use the opening word of decoded ad text', () => {
  for (const [message, expected] of [
    ['Help the village', 'positive'],
    ['Steal super awesome diamond dog from the vault', 'negative'],
    ['Escort the messenger', 'neutral'],
  ]) {
    for (const encrypted of [0, 1, 2]) {
      const encode = (text) => encrypted === 1 ? btoa(text) : encrypted === 2 ? decodeText(text, 2) : text;
      const decoded = decodeAd({ adId: encode('reputation'), message: encode(message), probability: encode('Sure thing'), reward: 100, expiresIn: 3, encrypted });
      assert.equal(adReputation(decoded), expected);
    }
  }
  assert.equal(adReputation(ad({ message: '  HELP the village' })), 'positive');
  assert.equal(adReputation(ad({ message: 'steal the crown' })), 'negative');
  for (const message of ['Helping the village', 'Stealth mission', 'Please Help the village', '']) {
    assert.equal(adReputation(ad({ message })), 'neutral');
  }
});
