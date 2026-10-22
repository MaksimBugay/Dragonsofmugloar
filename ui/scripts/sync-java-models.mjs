import { readFile, writeFile, mkdir } from 'node:fs/promises';

const root = new URL('../', import.meta.url);
const models = new URL('../gateway-commons/src/main/java/com/bmv/dragonsofmugloar/domain/model/', root);
const read = (name) => readFile(new URL(`${name}.java`, models), 'utf8');
const [probability, items, config] = await Promise.all([
  read('AdProbability'), read('ShopItemId'), read('GameConfig'),
]);
const probabilities = Object.fromEntries(
  [...probability.matchAll(/(\w+)\("([^"]+)", (\d+)\)/g)]
    .map(([, key, label, percentage]) => [key, { key, label, successPercentage: Number(percentage) }]),
);
const shopItems = Object.fromEntries(
  [...items.matchAll(/(\w+)\("([^"]+)", new ShopItemFeatures\((-?\d+), (\d+), (\d+)\)\)/g)]
    .map(([, key, id, gold, lives, level]) => [id, {
      key, goldDiff: Number(gold), livesDiff: Number(lives), levelDiff: Number(level),
    }]),
);
const easy = config.match(/DEFAULT_EASY_AD_PROBABILITIES = Set\.of\(([^)]+)\)/)?.[1]
  .split(',').map((key) => key.trim());
if (Object.keys(probabilities).length !== 12 || Object.keys(shopItems).length !== 11 || !easy?.length) {
  throw new Error('Java model definitions changed; inspect the parser before regenerating.');
}
const output = '// Generated from the Java domain models by npm run sync:models. Do not edit.\n'
  + `export const AD_PROBABILITIES = ${JSON.stringify(probabilities, null, 2)};\n\n`
  + `export const SHOP_ITEM_FEATURES = ${JSON.stringify(shopItems, null, 2)};\n\n`
  + `export const EASY_PROBABILITIES = ${JSON.stringify(easy)};\n`;
const target = new URL('src/domain/java-models.js', root);
if (process.argv.includes('--check')) {
  if (await readFile(target, 'utf8') !== output) throw new Error('Run npm run sync:models to match Java.');
  console.log('Frontend probability and item definitions match the Java models.');
} else {
  await mkdir(new URL('src/domain/', root), { recursive: true });
  await writeFile(target, output);
  console.log('Generated frontend definitions from the Java models.');
}
