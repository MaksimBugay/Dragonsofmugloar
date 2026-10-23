import test from 'node:test';
import assert from 'node:assert/strict';
import { manageInactivityReload } from '../src/presentation/inactivity.js';

const minute = 60 * 1000;
function fixture() {
  const windowTarget = new EventTarget();
  const documentTarget = new EventTarget();
  let reloads = 0;
  const stop = manageInactivityReload(() => reloads++, { windowTarget, documentTarget });
  return { windowTarget, documentTarget, stop, reloads: () => reloads };
}

test('inactivity reloads once after exactly five minutes', (t) => {
  t.mock.timers.enable({ apis: ['Date', 'setTimeout'], now: 0 });
  const idle = fixture();
  t.mock.timers.tick(5 * minute - 1);
  assert.equal(idle.reloads(), 0);
  t.mock.timers.tick(1);
  assert.equal(idle.reloads(), 1);
  t.mock.timers.tick(5 * minute);
  idle.windowTarget.dispatchEvent(new Event('click'));
  assert.equal(idle.reloads(), 1);
});

test('mouse, keyboard, scrolling, and touch activity each extend the idle deadline', (t) => {
  t.mock.timers.enable({ apis: ['Date', 'setTimeout'], now: 0 });
  for (const type of ['pointerdown', 'pointermove', 'click', 'keydown', 'wheel', 'touchstart', 'touchmove']) {
    const idle = fixture();
    t.mock.timers.tick(4 * minute);
    assert.equal(idle.reloads(), 0);
    idle.windowTarget.dispatchEvent(new Event(type));
    t.mock.timers.tick(5 * minute - 1);
    assert.equal(idle.reloads(), 0);
    t.mock.timers.tick(1);
    assert.equal(idle.reloads(), 1);
  }
});

test('returning to a suspended tab checks elapsed time without extending the deadline', (t) => {
  t.mock.timers.enable({ apis: ['Date', 'setTimeout'], now: 0 });
  for (const type of ['visibilitychange', 'focus', 'pageshow']) {
    const idle = fixture();
    t.mock.timers.setTime(Date.now() + 6 * minute);
    (type === 'visibilitychange' ? idle.documentTarget : idle.windowTarget).dispatchEvent(new Event(type));
    assert.equal(idle.reloads(), 1);
  }
});

test('late input cannot revive an expired game when the browser delayed its timer', (t) => {
  t.mock.timers.enable({ apis: ['Date', 'setTimeout'], now: 0 });
  const idle = fixture();
  t.mock.timers.setTime(6 * minute);
  idle.windowTarget.dispatchEvent(new Event('pointerdown'));
  assert.equal(idle.reloads(), 1);
});

test('cleanup cancels the idle timer and removes event listeners', (t) => {
  t.mock.timers.enable({ apis: ['Date', 'setTimeout'], now: 0 });
  const idle = fixture();
  idle.stop();
  t.mock.timers.setTime(6 * minute);
  for (const type of ['pointerdown', 'focus', 'pageshow']) idle.windowTarget.dispatchEvent(new Event(type));
  idle.documentTarget.dispatchEvent(new Event('visibilitychange'));
  t.mock.timers.tick(5 * minute);
  assert.equal(idle.reloads(), 0);
});
