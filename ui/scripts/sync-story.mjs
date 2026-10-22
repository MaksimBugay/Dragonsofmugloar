import { writeFile } from 'node:fs/promises';

// Read the source paragraphs directly, preserving punctuation and paragraph boundaries.
// Only HTML indentation and line wrapping are collapsed to rendered spaces.
const source = 'https://dragonsofmugloar.com/';
const response = await fetch(source, { signal: AbortSignal.timeout(20000) });
if (!response.ok) throw new Error(`Story source returned HTTP ${response.status}`);
const html = await response.text();
const normalize = (text) => text.replace(/\s+/g, ' ').trim();
const content = html.split('<div class="content">')[1];
const paragraphs = [...(content ?? '').matchAll(/<p>\s*([^<]+)\s*<\/p>/g)]
  .slice(0, 2).map((match) => normalize(match[1]));
const footer = html.match(/<div class="copyright">([\s\S]*?)<\/div>/)?.[1];
const attribution = footer ? normalize(footer.replace(/<[^>]+>/g, ' ')) : '';
if (paragraphs.length !== 2 || paragraphs.some((text) => text.length < 200) || !attribution.includes('Bigbank')) {
  throw new Error('Official homepage structure changed; inspect it before updating the story.');
}
await writeFile(new URL('../src/content/story.json', import.meta.url),
  JSON.stringify({ source, paragraphs, attribution }, null, 2) + '\n');
console.log('Saved the two original homepage paragraphs and source attribution.');
