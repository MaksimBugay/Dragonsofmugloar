import { AD_PROBABILITIES, EASY_PROBABILITIES, SHOP_ITEM_FEATURES } from './java-models.js';

export function decodeText(value, encrypted) {
  if (value == null) return value;
  switch (encrypted) {
    case undefined:
    case null:
    case 0:
      return value;
    case 1: {
      // atob returns bytes, not Unicode. Names can contain combining characters.
      const bytes = Uint8Array.from(atob(value), (character) => character.charCodeAt(0));
      return new TextDecoder('utf-8', { fatal: true }).decode(bytes);
    }
    case 2:
      return value.replace(/[a-zA-Z]/g, (character) => {
        const base = character <= 'Z' ? 65 : 97;
        return String.fromCharCode(base + (character.charCodeAt(0) - base + 13) % 26);
      });
    default:
      throw new Error(`Unsupported ad encryption: ${encrypted}`);
  }
}

export function decodeAd(raw) {
  const adId = decodeText(raw.adId, raw.encrypted);
  const message = decodeText(raw.message, raw.encrypted);
  const label = decodeText(raw.probability, raw.encrypted);
  const reward = Number(raw.reward);
  if (typeof adId !== 'string' || !adId || typeof message !== 'string'
      || raw.reward == null || !Number.isFinite(reward) || !Number.isFinite(raw.expiresIn)) {
    throw new Error('The noticeboard returned an invalid quest. Refresh to try again.');
  }
  const probability = Object.values(AD_PROBABILITIES).find((value) => value.label === label)
    ?? AD_PROBABILITIES.UNKNOWN;
  return { adId, message, reward, expiresIn: raw.expiresIn, encrypted: raw.encrypted ?? null, probability };
}

// ChooseGameActionService.expectedValue: the cost of a lost life is a healing potion.
export function expectedValue(ad) {
  const p = ad.probability.successPercentage / 100;
  return p * ad.reward - (1 - p) * -SHOP_ITEM_FEATURES.hpot.goldDiff;
}

export function adReputation(ad) {
  const message = ad.message.trimStart();
  if (/^help\b/i.test(message)) return 'positive';
  if (/^steal\b/i.test(message)) return 'negative';
  return 'neutral';
}

export function isTrapAd(ad) {
  return ad.message.toLowerCase().startsWith('steal super awesome diamond');
}

export function adStatus(ad) {
  if (ad.expiresIn <= 0) return 'expired';
  if (isTrapAd(ad)) return 'trap';
  if (EASY_PROBABILITIES.includes(ad.probability.key)) return 'easy';
  if (ad.probability.successPercentage <= AD_PROBABILITIES.RISKY.successPercentage) return 'risky';
  return 'normal';
}

// PerformGameActionService.VALUE_RANKING, with expired notices placed last for display.
export function rankAds(ads) {
  return [...ads].sort((a, b) => Number(a.expiresIn <= 0) - Number(b.expiresIn <= 0)
    || expectedValue(b) - expectedValue(a)
    || b.probability.successPercentage - a.probability.successPercentage
    || a.expiresIn - b.expiresIn);
}

export function ageAds(ads, turns, attemptedId) {
  return ads.filter((ad) => ad.adId !== attemptedId)
    .map((ad) => ({ ...ad, expiresIn: Math.max(0, ad.expiresIn - turns) }));
}
