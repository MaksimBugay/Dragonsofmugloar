import { SHOP_ITEM_FEATURES } from './java-models.js';

export function shopItem(raw) {
  const features = SHOP_ITEM_FEATURES[raw.id];
  if (!features) throw new Error(`Unknown shop item ID: ${raw.id}`);
  if (typeof raw.name !== 'string' || !Number.isSafeInteger(raw.cost) || raw.cost < 0) {
    throw new Error('The apothecary returned an invalid item.');
  }
  return { id: raw.id, name: raw.name, cost: raw.cost, features: { ...features } };
}
