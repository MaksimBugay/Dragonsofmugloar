# Local artwork and source content

- `public/assets/kingdom.jpg`: original fantasy background generated with the built-in imagegen tool on 22 October 2026; optimized to JPEG locally. The original generated PNG was preserved outside the repository.
- `public/assets/items/*.svg`: eleven original vector illustrations matching the API's potion, claw, fuel, plating, and book items. The two observed upgrade tiers are represented through their model-derived effect badges.
- `public/assets/crest.svg`: original dragon heraldry.
- `public/assets/fonts/caesar-dressing-latin.woff2`: locally hosted Caesar Dressing by Open Window, used for rune-inspired ad IDs. Downloaded unmodified from [Google Fonts](https://fonts.googleapis.com/css2?family=Caesar+Dressing&display=swap); [upstream source](https://github.com/google/fonts/tree/main/ofl/caesardressing). Its SIL Open Font License and copyright notice are included in `public/assets/fonts/OFL-Caesar-Dressing.txt`.
- `public/assets/parchment.svg`: original procedural paper texture.
- `public/assets/dragon-code-blood.svg`: original procedural SVG blood stains for the Dragon Slayer’s Code scroll; paired with the parchment texture and CSS scroll rolls.
- `src/content/story.json`: imported source content and attribution from https://dragonsofmugloar.com/ via `npm run sync:story`. The official site's illustrations and background photo are not reused by this UI. The full original attribution remains preserved in the story credits.

No remote fonts or runtime image services are required.

## Final artwork prompt

Use case: stylized-concept. Asset type: wide landscape background artwork for a polished medieval fantasy browser game called Dragons of Mugloar. Create an original beautifully painted high fantasy kingdom evocative of classic late-1990s fantasy strategy game box art, King's Bounty and Heroes of Might and Magic atmosphere. Panoramic 1536x1024 composition: deep blue-green alpine valley, layered misty mountains, imposing weathered stone castle with warm amber windows on a cliff at the far left, broad sweeping dragon with bronze-green scales perched on a crag at the far right, rugged fir trees in the foreground and winding river below, golden sunrise breaking through dramatic clouds. Keep the middle 60 percent fairly tranquil and low-contrast with distant atmospheric mist because parchment interface overlays it. Detailed painterly oil textures, sophisticated muted teal and weathered gold, heroic, magical, inviting rather than horror. The foreground at the bottom fades into very dark green. No text, no lettering, no UI, no logos, no watermarks. Full bleed landscape illustration.
