// Presentation timing only; dismissing feedback never changes the adventure history.
export function scheduleFeedbackDismissal(dismiss) {
  const timeout = setTimeout(dismiss, 10_000);
  return () => clearTimeout(timeout);
}
