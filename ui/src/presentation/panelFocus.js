// Presentation-only keyboard behavior, independent of Vue and game state.
export function managePanelFocus(element, close, { trapFocus = false } = {}) {
  const previous = document.activeElement;
  const mobile = window.matchMedia('(max-width: 1279px)');
  const controls = () => [...element.querySelectorAll('button:not(:disabled), a[href], summary, [tabindex="0"]')]
    .filter((control) => control.getClientRects().length > 0);
  const onKey = (event) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      close();
    }
    if (event.key !== 'Tab' || (!trapFocus && !mobile.matches)) return;
    const focusable = controls();
    const first = focusable[0];
    const last = focusable.at(-1);
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last?.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first?.focus();
    }
  };
  element.addEventListener('keydown', onKey);
  controls()[0]?.focus({ preventScroll: true });
  return () => {
    element.removeEventListener('keydown', onKey);
    if (element.contains(document.activeElement)) previous?.focus({ preventScroll: true });
  };
}
