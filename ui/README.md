# Dragons of Mugloar UI

A complete Vue 3 single-page game using the live [Dragons of Mugloar API](https://dragonsofmugloar.com/doc/). No Java server, account, API key, mock backend, or runtime asset CDN is required.

## Run

Requires Node.js **22.12+** (validated with Node 26.8.2) and npm.

```sh
cd /Users/mbugai/work/Dragonsofmugloar/ui
npm install
npm run dev
```

Open **http://127.0.0.1:5173/**. The browser calls `https://dragonsofmugloar.com/api/v2` directly; the API currently permits cross-origin requests. Internet access is required for gameplay. All game illustrations, textures, fonts, story content, and application code are local. The portable pages additionally load the asynchronous tracking script from `https://cdn.ywxi.net/js/1.js`.

For a production build:

```sh
npm run build
npm run preview
```

Preview runs at **http://127.0.0.1:4173/**. Deploy the generated `dist/` directory to a static host at its root. Vite preview is for local verification, not a production server.

## Play

Read the two original introductory paragraphs on the animated scroll, then choose **Start Game**. Choose **Solve Ad** on a notice to attempt that quest. The API decides its outcome; favourable labels are estimates rather than guarantees.

Open **Dragon Slayer’s Code** beside the board’s Refresh button (a book icon on mobile) for a blood-stained parchment guide written for the dragon slayer in clear language with a light epic tone. Its seven steps cover:

1. Reserve gold for +1 life and heal on the last life.
2. Solve Easy quests from the top of the board.
3. Upgrade when Easy quests run out, then check again.
4. Attempt Uncertain quests and check for Easy quests after each attempt.
5. Upgrade again when Uncertain quests run out, then recheck both groups.
6. When broke with one life and only Risky/Trap quests left, investigate reputation for two or three turns while checking for better quests.
7. Pray and attempt a Risky quest if no better option appears.

The guide provides advice for manual play. Its body scrolls while the heading and close button stay visible.

The turn appears at the top center. Lives, gold, dragon level, score, and the server's high score appear along the bottom. The central board occupies 60% of a desktop viewport and presents its notices in one column, with rune-inspired lettering for ad IDs. On smaller screens, it expands for readability and the panels become overlays. During mobile gameplay, the header, turn and reputation banners, panel buttons, and bottom state bar stay fixed while only the ads list scrolls. Compact controls keep the list usable on shorter phone screens.

Open **Potion Shop** on the left to load its current inventory. Refresh fetches it again without taking a turn. Click an affordable item's image to purchase it. The image tooltip uses the server's name; its badge shows the effect from the Java item model. Insufficient-gold items are disabled.

Open **Adventure Journal** on the right to see quest attempts, purchases, and reputation investigations in one chronology, including failures, source quest/item, result, and changes in lives, gold, score, and level. It begins closed. Each new outcome appears in a centered info popup that closes automatically after 3 seconds and can also be dismissed immediately. The popup has no journal link; its entry remains available in Adventure Journal. Each entry retains independent before/after snapshots. All game state and history are **in memory**: reloading the page or choosing **New adventure** starts fresh. A game-over summary allows another adventure while keeping the completed journal readable until then.

During an adventure, five minutes since the last mouse, keyboard, scrolling, or touch interaction reloads the page and clears the in-memory adventure. Returning to a suspended tab checks the elapsed time; it does not restart the idle period. A new adventure gets a fresh timer. The intro page has no inactivity timer, and expiry makes no stop-game API request.

The Reputation banner uses a person-and-star icon and shows the last investigated People/State values as `0.3/4.5`, on one line within a fixed-width banner. All reputation scores are rounded for display to exactly one decimal place everywhere: banner, popup, tooltips, accessible labels, action feedback, and journal. The full popup includes a leading `+` on positive scores; zero appears as `0.0`. The original API values remain unchanged in game state and event snapshots. The banner sits beside Turn on mobile and at the right of the header on desktop. Select it to open the full Reputation popup, including People, State, and Underworld. Opening the popup is free; its refresh button investigates all three factions, and its close button or Escape returns to the board. Each investigation uses one turn, refreshes the bounty board, and records a journal entry. Values remain the last investigated snapshot until refreshed; no investigation runs automatically.

Panels support Escape and focus restoration. Overlay panels contain keyboard focus. CSS animations respect `prefers-reduced-motion`. Buttons and status changes have accessible labels, and quest risk is communicated with text as well as colour.

## Architecture and existing Java behavior

- `src/services/gameApi.js`: six API operations, including reputation investigation, escaped identifiers, timeouts, and HTTP/network error handling. POSTs are never automatically retried.
- `src/stores/gameStore.js`: Vue refs/computed values coordinate initial loading, serialized actions, state replacement, and history. Stale listing responses cannot replace a new game or newer turn. A separate state library is unnecessary for this one-page application.
- `src/domain/ads.js`: decoding, ranking, classification, and turn-based expiry.
- `src/domain/game.js`: Java-shaped `GameState`, partial-response application, state differences, and immutable event snapshots.
- `src/domain/shop.js`: live item names/prices with observed Java item effects.
- `src/domain/java-models.js`: generated definitions from `AdProbability`, `ShopItemId`, and the default easy set in `GameConfig`. Run `npm run sync:models` after changing the Java models.
- `src/components/`: separate TurnIndicator, CurrentGameState, AdsList/AdNotice, AdventureJournal, ShopPanel, ReputationBanner/ReputationWidget, and DragonSlayersCode components. GameDialog shares modal behavior between the guide and reputation report.
- `src/styles/`: theme, layout, component styling, and CSS-only animation. `src/presentation/` contains standalone keyboard/focus helpers and the shared one-decimal reputation formatter; no visual effects run inside game logic.

### Exact domain rules

Base64 marker `1` decodes **adId, message, and probability** using UTF-8 bytes; marker `2` applies ASCII-only ROT13. Null/missing/zero markers preserve plaintext. The decoded ID is used in solve URLs. Unsupported encryption fails the listing explicitly, matching the Java adapter. Unknown labels map to `UNKNOWN`.

Ranking ports `ChooseGameActionService.expectedValue`:

```text
p = AdProbability.successPercentage / 100
expectedValue = p * reward - (1 - p) * ShopItemId.HEALING_COST
```

The current healing cost is 50. Display order is Easy, Uncertain, Risky, Expired, then Trap. Trap ads always stay at the end, including expired traps. Within each category, active ads precede expired ads and higher expected value comes first; ties follow `PerformGameActionService.VALUE_RANKING`: higher probability, then sooner expiry. Expired ads (`expiresIn <= 0`) have disabled buttons.

Quest reputation badges use the opening word of the decoded message: helping is positive, stealing negative, and other wording neutral. These are hints, not exact faction-score changes.

The Java enum itself has no colour field. Green uses the existing `GameConfig` default easy set (Sure thing, Piece of cake, Walk in the park, Quite likely). Red uses `RISKY` and the enum's lower success estimates; remaining non-expired labels are blue. Expired notices are gray. The estimated percentages are not presented as server-guaranteed odds.

Each action is compared with the state immediately before it. Solve responses preserve the current level; purchase responses preserve score/high score. Both use the **returned turn**, including failed purchases. The board loads when an adventure starts and refreshes only when the returned turn changes or the user presses Refresh. Actions that leave the turn unchanged and rejected requests preserve the current board. Refresh failure cannot erase a committed action: the aged cached board remains visible and solving is disabled until a successful refresh.

### API contracts

| Method | Path relative to `/api/v2` | Purpose |
| --- | --- | --- |
| POST | `/game/start` | Start game |
| GET | `/{gameId}/messages` | Fetch quests |
| POST | `/{gameId}/solve/{decodedAdId}` | Solve quest |
| GET | `/{gameId}/shop` | Fetch items |
| POST | `/{gameId}/shop/buy/{itemId}` | Purchase item |
| POST | `/{gameId}/investigate/reputation` | Investigate People, State, and Underworld reputation; uses one turn |

As recorded in the repository findings and verified live, listings are arrays, rewards are numeric, and `shoppingSuccess` is boolean. These observations correct outdated details in the API documentation.

## Verification

```sh
npm run check
```

This checks generated definitions against the sibling Java sources, runs 51 Node regression tests, and builds the production bundle. The Java source parity check needs this repository's sibling `gateway-commons` directory; `npm test` and `npm run build` also work on an isolated UI checkout.

Tests cover the findings' actual Base64/Unicode and ROT13 examples; unknown encodings/labels; expected value, ranking ties and expired ads; all 11 item effects; partial result fields; action snapshots/deltas; failed purchases/quests; duplicate clicks; stale reads across turns/games; malformed responses; timeouts; network uncertainty; reputation validation, turn advancement, failures, and one-decimal feedback/journal formatting; game-over/restart behavior; and the five-minute inactivity deadline, input resets, suspended tabs, and timer cleanup. Test fixtures are used only by tests, never by the application.

An opt-in check creates a new real game and spends its game gold:

```sh
npm run test:live
```

It verifies the five original live endpoints (reputation investigation is not included in this smoke script), rejected purchase turn advancement, quests, healing, and an upgrade. Quest outcomes are random; an unlucky live run can fail its funding precondition without indicating an application defect.

Initial validation on 22 October 2026 included the production build; the then-current 26 regression tests; npm dependency audit; a live API smoke game with seven recorded actions; and Chrome desktop/mobile UI gameplay. Browser checks verified original intro text, default-closed journal, ten live notices, 11 shop images, actual solve and healing responses, journal deltas, keyboard navigation, reduced motion, no console warnings/errors, and no horizontal overflow at 320, 390, 768, 1024, and 1440 pixels.

Latest verification on 23 October 2026: all 51 regression tests and production builds passed. Browser checks covered the guide scroll and close controls on desktop/mobile, and reputation rounding in the banner, popup, tooltips, feedback, and journal for npm and both portable pages. Inactivity checks confirmed the intro stays open, user activity extends the deadline, and five idle minutes trigger one reload back to the intro without a stop-game request. Deployment dry runs confirmed the current bundle pair and guide/shop assets. These checks did not publish the site.

## Content and assets

`npm run sync:story` imports the first two original paragraphs and copyright notice directly from the [official homepage](https://dragonsofmugloar.com/). It preserves wording, punctuation, and paragraph breaks; HTML indentation is normalized as rendered whitespace. The source notice is available under **Original story & credits**. The checked-in import makes the intro independent of source-site availability at runtime.

`public/assets/kingdom.jpg` is an original generated illustration. Shop illustrations, crest, parchment texture, and `public/assets/dragon-code-blood.svg` are original local SVG assets. The guide combines the paper texture and blood-stain artwork with CSS scroll rolls. See [ASSETS.md](ASSETS.md) for the artwork prompt and provenance.

## Remaining platform limitations

The public API exposes no read-only game-state recovery endpoint or action idempotency key. If a mutating request loses its response, the client cannot know whether the server advanced the game. It keeps the last confirmed state/history, explains the uncertainty, disables further actions, and offers a new adventure rather than retrying a potentially completed purchase or quest. A new adventure is also available if the server expires a session.

Availability, rate limits, quest outcomes, and future undocumented encryption/item changes are controlled by the external API. There is no offline game mode or cross-reload persistence, consistent with the in-memory requirement.

## Portable build and deployment

The checked-in `portable/index.html` and `portable/dragonsofmugloar.html` share the same JS/CSS bundle pair. Their asset paths are relative so the game works under `/game/` as well as at a host root. Shop images use Vite’s base path rather than root-relative `/assets/` URLs.

Both portable pages include the asynchronous `cdn.ywxi.net/js/1.js` tracking script and a separate Secure FileShare `WebApplication` JSON-LD block for `https://secure.fileshare.ovh/`, alongside the existing game metadata. Deployment checks that tracking and Secure FileShare metadata are present.

After UI changes:

1. Run `npm run check`, then `npm run build -- --base=./` for the portable build.
2. Copy `dist/assets/` into `portable/assets/`, including new images and fonts.
3. Update the JS/CSS references in **both** portable HTML pages to the bundle names in `dist/index.html`. Preserve the existing canonical URL, social metadata, both JSON-LD blocks, tracking script, and page title; do not replace the portable pages with Vite’s minimal HTML.
4. Keep `deploy-game.sh` aware of any new asset types and run `bash deploy-game.sh --dry-run` before finishing. It discovers current bundle names from the two pages automatically and skips obsolete hashed JS/CSS files.

To publish the prepared portable build, run `bash deploy-game.sh`. It uses SSH/SCP, defaults to `root@148.251.49.213:/data/nginx-pusher/data/game`, and supports `SSH_KEY` and `DEPLOY_HOST` overrides. It may prompt for the SSH key passphrase. The public game URL is [dragonsofmugloar.html](https://secure.fileshare.ovh/game/dragonsofmugloar.html).

`bash deploy-game.sh --dry-run` lists the current files and the normal public URL. Deployment uploads assets first, publishes the social preview and both HTML pages by rename, then downloads both public HTML pages, their JS/CSS bundles, the blood-stain artwork, and all shop images and compares their contents with the local portable files. A mismatch makes deployment fail verification. Run `bash deploy-game.sh --verify` to repeat these public checks without uploading or connecting over SSH.

### Cache behavior

`Cache-Control`, `Pragma`, and `Expires` are not supported HTML `http-equiv` values. Configure caching through HTTP response headers: the host must send `Cache-Control: no-cache` for both portable HTML pages to require a freshness check on each new opening.

Earlier deployment checks found the game host serving HTML with `Cache-Control: public, max-age=3600`, which lets a phone keep the previous page and its old bundle names for one hour after deployment. Reload or clear the browser cache if the previous version appears after deployment. To always check for a new release at the normal public URL, configure the host to serve game HTML with `Cache-Control: no-cache`; hashed assets can remain cached.
