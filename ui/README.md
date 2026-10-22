# Dragons of Mugloar UI

A complete Vue 3 single-page game using the live [Dragons of Mugloar API](https://dragonsofmugloar.com/doc/). No Java server, account, API key, mock backend, or runtime asset CDN is required.

## Run

Requires Node.js **22.12+** (validated with Node 26.8.2) and npm.

```sh
cd /Users/mbugai/work/Dragonsofmugloar/ui
npm install
npm run dev
```

Open **http://127.0.0.1:5173/**. The browser calls `https://dragonsofmugloar.com/api/v2` directly; the API currently permits cross-origin requests. Internet access is required for gameplay. All illustrations, textures, fonts, story content, and application code are local.

For a production build:

```sh
npm run build
npm run preview
```

Preview runs at **http://127.0.0.1:4173/**. Deploy the generated `dist/` directory to a static host at its root. Vite preview is for local verification, not a production server.

## Play

Read the two original introductory paragraphs on the animated scroll, then choose **Start Game**. Choose **Solve Ad** on a notice to attempt that quest. The API decides its outcome; favourable labels are estimates rather than guarantees.

The turn appears at the top center. Lives, gold, dragon level, score, and the server's high score appear along the bottom. The central board occupies 60% of a desktop viewport and presents its notices in one column, with rune-inspired lettering for ad IDs. On smaller screens, it expands for readability and the panels become overlays.

Open **Potion Shop** on the left to load its current inventory. Refresh fetches it again without taking a turn. Click an affordable item's image to purchase it. The image tooltip uses the server's name; its badge shows the effect from the Java item model. Insufficient-gold items are disabled.

Open **Adventure Journal** on the right to see quest attempts and purchases in one chronology, including failures, source quest/item, result, and changes in lives, gold, score, and level. It begins closed. The latest outcome is also shown above the board for 10 seconds. Each new result restarts that timer; journal entries remain available. Each entry retains independent before/after snapshots. All game state and history are **in memory**: reloading the page or choosing **New adventure** starts fresh. A game-over summary allows another adventure while keeping the completed journal readable until then.

Panels support Escape and focus restoration. Overlay panels contain keyboard focus. CSS animations respect `prefers-reduced-motion`. Buttons and status changes have accessible labels, and quest risk is communicated with text as well as colour.

## Architecture and existing Java behavior

- `src/services/gameApi.js`: the five documented endpoints, escaped identifiers, timeouts, and HTTP/network error handling. POSTs are never automatically retried.
- `src/stores/gameStore.js`: Vue refs/computed values coordinate initial loading, serialized actions, state replacement, and history. Stale listing responses cannot replace a new game or newer turn. A separate state library is unnecessary for this one-page application.
- `src/domain/ads.js`: decoding, ranking, classification, and turn-based expiry.
- `src/domain/game.js`: Java-shaped `GameState`, partial-response application, state differences, and immutable event snapshots.
- `src/domain/shop.js`: live item names/prices with observed Java item effects.
- `src/domain/java-models.js`: generated definitions from `AdProbability`, `ShopItemId`, and the default easy set in `GameConfig`. Run `npm run sync:models` after changing the Java models.
- `src/components/`: separate TurnIndicator, CurrentGameState, AdsList/AdNotice, AdventureJournal, and ShopPanel components, plus small shared presentation components.
- `src/styles/`: theme, layout, component styling, and CSS-only animation. `src/presentation/` contains standalone keyboard/focus and feedback-dismissal helpers; no visual effects run inside game logic.

### Exact domain rules

Base64 marker `1` decodes **adId, message, and probability** using UTF-8 bytes; marker `2` applies ASCII-only ROT13. Null/missing/zero markers preserve plaintext. The decoded ID is used in solve URLs. Unsupported encryption fails the listing explicitly, matching the Java adapter. Unknown labels map to `UNKNOWN`.

Ranking ports `ChooseGameActionService.expectedValue`:

```text
p = AdProbability.successPercentage / 100
expectedValue = p * reward - (1 - p) * ShopItemId.HEALING_COST
```

The current healing cost is 50. Ties follow `PerformGameActionService.VALUE_RANKING`: higher probability, then sooner expiry. Expired ads (`expiresIn <= 0`) always follow non-expired ads, and their buttons are disabled.

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

As recorded in the repository findings and verified live, listings are arrays, rewards are numeric, and `shoppingSuccess` is boolean. These observations correct outdated details in the documentation. No paid reputation investigation is performed implicitly.

## Verification

```sh
npm run check
```

This checks generated definitions against the sibling Java sources, runs 26 Node regression tests, and builds the production bundle. The Java source parity check needs this repository's sibling `gateway-commons` directory; `npm test` and `npm run build` also work on an isolated UI checkout.

Tests cover the findings' actual Base64/Unicode and ROT13 examples; unknown encodings/labels; expected value, ranking ties and expired ads; all 11 item effects; partial result fields; action snapshots/deltas; failed purchases/quests; duplicate clicks; stale reads across turns/games; malformed responses; timeouts; network uncertainty; and game-over/restart behavior. Test fixtures are used only by tests, never by the application.

An opt-in check creates a new real game and spends its game gold:

```sh
npm run test:live
```

It verifies the five live endpoints, rejected purchase turn advancement, quests, healing, and an upgrade. Quest outcomes are random; an unlucky live run can fail its funding precondition without indicating an application defect.

Validated on 22 October 2026: production build; 26 regression tests; npm dependency audit; a live API smoke game with seven recorded actions; and Chrome desktop/mobile UI gameplay. Browser checks verified original intro text, default-closed journal, ten live notices, 11 shop images, actual solve and healing responses, journal deltas, keyboard navigation, reduced motion, no console warnings/errors, and no horizontal overflow at 320, 390, 768, 1024, and 1440 pixels.

## Content and assets

`npm run sync:story` imports the first two original paragraphs and copyright notice directly from the [official homepage](https://dragonsofmugloar.com/). It preserves wording, punctuation, and paragraph breaks; HTML indentation is normalized as rendered whitespace. The source notice is available under **Original story & credits**. The checked-in import makes the intro independent of source-site availability at runtime.

`public/assets/kingdom.jpg` is an original generated illustration. Shop illustrations, crest, and parchment texture are original local SVG assets. See [ASSETS.md](ASSETS.md) for the artwork prompt and provenance.

## Remaining platform limitations

The public API exposes no read-only game-state recovery endpoint or action idempotency key. If a mutating request loses its response, the client cannot know whether the server advanced the game. It keeps the last confirmed state/history, explains the uncertainty, disables further actions, and offers a new adventure rather than retrying a potentially completed purchase or quest. A new adventure is also available if the server expires a session.

Availability, rate limits, quest outcomes, and future undocumented encryption/item changes are controlled by the external API. There is no offline game mode or cross-reload persistence, consistent with the in-memory requirement.
