const activityEvents = ['pointerdown', 'pointermove', 'click', 'keydown', 'wheel', 'touchstart', 'touchmove'];
const listenerOptions = { capture: true, passive: true };

// User activity extends the deadline; returning to a suspended tab only checks it.
export function manageInactivityReload(reload, {
  windowTarget = window,
  documentTarget = document,
  timeoutMs = 5 * 60 * 1000,
} = {}) {
  let lastActivity = Date.now();
  let timer;
  let stopped = false;

  function stop() {
    stopped = true;
    clearTimeout(timer);
    for (const type of activityEvents) windowTarget.removeEventListener(type, onActivity, listenerOptions);
    windowTarget.removeEventListener('focus', checkDeadline);
    windowTarget.removeEventListener('pageshow', checkDeadline);
    documentTarget.removeEventListener('visibilitychange', checkDeadline);
  }

  function checkDeadline() {
    if (stopped) return;
    clearTimeout(timer);
    const remaining = timeoutMs - (Date.now() - lastActivity);
    if (remaining <= 0) {
      stop();
      reload();
    } else {
      timer = setTimeout(checkDeadline, remaining);
    }
  }

  function onActivity() {
    if (stopped) return;
    // A delayed browser timer must not let the first late interaction revive an expired game.
    if (Date.now() - lastActivity >= timeoutMs) {
      checkDeadline();
      return;
    }
    lastActivity = Date.now();
  }

  for (const type of activityEvents) windowTarget.addEventListener(type, onActivity, listenerOptions);
  windowTarget.addEventListener('focus', checkDeadline);
  windowTarget.addEventListener('pageshow', checkDeadline);
  documentTarget.addEventListener('visibilitychange', checkDeadline);
  checkDeadline();
  return stop;
}
