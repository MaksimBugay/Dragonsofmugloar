const options = { minimumFractionDigits: 1, maximumFractionDigits: 1, useGrouping: false };
const number = new Intl.NumberFormat('en-US', options);
const signedNumber = new Intl.NumberFormat('en-US', { ...options, signDisplay: 'exceptZero' });

export function formatReputation(value, { signed = false } = {}) {
  return (signed ? signedNumber : number).format(value);
}
