# Dragons of Mugloar — Single-Page UI Implementation Specification

## 1. Objective

Build a **single-page web application** for the **Dragons of Mugloar** game, with a polished fantasy UI inspired by **King's Bounty** and **Heroes of Might and Magic**.

The application must integrate with the existing Dragons of Mugloar server API and support starting a game, browsing and solving ads (quests), buying potions, viewing the current game state, and inspecting the adventure history.

### Primary references

- **Official game website and intro story source:** https://dragonsofmugloar.com/
- **Server API documentation:** https://dragonsofmugloar.com/doc/
- **Ad encryption/decryption findings:** `/Users/mbugai/work/Dragonsofmugloar/DRAGONS_OF_MUGLOAR_FINDINGS.md`
- **Existing Java domain models:** `/Users/mbugai/work/Dragonsofmugloar/gateway-commons/src/main/java/com/bmv/dragonsofmugloar/domain/model`
- **Ad probability model:** `/Users/mbugai/work/Dragonsofmugloar/gateway-commons/src/main/java/com/bmv/dragonsofmugloar/domain/model/AdProbability.java`
- **Ad ranking calculation:** `com.bmv.dragonsofmugloar.application.service.ChooseGameActionService.expectedValue`
- **Shop item name model:** `com.bmv.dragonsofmugloar.domain.model.ShopItem.name`

**Before implementing**, inspect these references and reuse their existing definitions and logic where applicable. Treat the API documentation and existing project code as the sources of truth for request/response structures, field semantics, encryption, item effects, and ranking behavior.

## 2. Technology and Code Quality Requirements

1. Use **Vue.js** for application state management, business logic orchestration, API interactions, and UI components.
2. For visual effects, animations, and other presentation behavior, **prefer HTML and CSS**. Use JavaScript only when the effect cannot reasonably be implemented with HTML/CSS alone.
3. When JavaScript is required for presentation effects, use **plain/vanilla JavaScript**, kept **separate from Vue business logic**.
4. Apply **SOLID principles** to business logic. In particular, ensure each Vue component has a **single, well-defined responsibility**.
5. **Reuse existing code and behavior** whenever possible. Avoid copy/paste duplication and unnecessary reimplementation.
6. Write **simple, well-formatted, easy-to-read code** that is understandable to a human developer.
7. Pay particular attention to the encryption technique described in `DRAGONS_OF_MUGLOAR_FINDINGS.md`. Implement the required **JavaScript decryption** so ads display **proper readable text**.
8. Create a **gorgeous, cohesive, medieval-fantasy design**, including the required assets and imagery, inspired by the atmosphere of King's Bounty and Heroes of Might and Magic, while remaining compatible with the Dragons of Mugloar game and API.

## 3. Page Layout and Visual Design

Use a single page with these main areas:

- **Top center:** the current turn number, displayed separately from other game-state information.
- **Center:** the main bounty/quest board, occupying **approximately 60% of the screen** and showing available ads.
- **Bottom:** the current game state, presented with clear, self-explanatory icons and labels.
- **Right side:** a **collapsible Adventure Journal** panel, **closed by default**.
- **Left side:** a **collapsible Potion Shop** panel, styled like a medieval **tavern or apothecary**.

Use a consistent medieval-fantasy visual language: parchment, old wooden surfaces, bounty notices, fantasy iconography, appropriate illustrations, and tasteful animation/effects. Provide the assets and images needed for this design.

### Intro / game-start view

Before a game has started:

1. Take the **original game story directly from https://dragonsofmugloar.com/** — specifically, the **two introductory story paragraphs** at the beginning of the official homepage. **Preserve the original text verbatim**, including its wording, punctuation, and paragraph breaks; do not invent, paraphrase, shorten, or rewrite the story.
2. Present that story as **animated, rolling text on ancient parchment**, against a **heroic fantasy background**. The story text must remain readable throughout the animation.
3. Place a **Start Game** button at the bottom of the story.
4. Once the button is pressed, remove/hide both the story and the Start Game button, and transition to the active game view.

Use the official site as the content reference, rather than generating replacement fantasy lore. Preserve any required copyright/attribution notices when reusing website content.

## 4. Game State and Data Handling

- Keep the **current game state in memory**.
- Model this state using JavaScript data structures **similar to the existing Java domain models** in:
  `/Users/mbugai/work/Dragonsofmugloar/gateway-commons/src/main/java/com/bmv/dragonsofmugloar/domain/model`
- Use Vue to coordinate API calls and propagate state changes to the UI.
- Maintain a history of adventure events, including **ad-solving attempts** and **potion purchases**.
- For every action result, calculate changes against the **state immediately before that action**.
- Reflect updated state in the game-state display and turn indicator wherever applicable.

## 5. Game Flows

### 5.1. Start a new game

**Trigger:** user clicks **Start Game**.

**Required behavior:**

1. Hide the intro story and Start Game button.
2. Invoke the **start game API** through Vue.
3. Save the initial game state in memory, using structures consistent with the Java domain model.
4. Display the current game state at the bottom with meaningful icons and labels.
5. Display the **turn number** independently at the **top center**.
6. Load the list of available ads from the API.
7. Decrypt the ads using the findings referenced above.
8. Display the decrypted ads on the central bounty board.

### 5.2. Display and rank ads

Present ads as individual **medieval bounty notices** on the central board.

For each ad:

- Show the **ad ID as the title**.
- Show the decrypted **message as the body**.
- Show **reward**, **expiresIn**, and **probability** together as attributes in the **bottom-right area** of the ad element.
- Provide a **Solve Ad** button alongside the ad, with an appropriate attractive icon and label.
- Apply these visual statuses:

  | Ad status | Visual treatment |
  | --- | --- |
  | Expired | Gray |
  | Easy | Green |
  | Risky | Red |
  | All other non-expired ads | Blue |

  Use the existing `AdProbability.java` model to determine probability classifications, rather than inventing a different interpretation.

**Ordering:**

1. Rank non-expired ads according to the calculation implemented in:
   `com.bmv.dragonsofmugloar.application.service.ChooseGameActionService.expectedValue`
2. Place **all expired ads last**, regardless of their expected value.

### 5.3. Solve an ad

**Trigger:** user clicks **Solve Ad** on a particular ad.

**Required behavior:**

1. Invoke the corresponding **solve message/ad API** through Vue.
2. Determine whether the attempt **succeeded** from the API response.
3. Calculate the differences in **lives**, **gold**, and **score** between the API response and the game state immediately before the attempt.
4. Save a history event containing the **ad** and its **solving result**, including success/failure and calculated differences.
5. Update the in-memory current game state from the API response.
6. Update the visible game state and any affected turn information.
7. Show this event in the **Adventure Journal**.

### 5.4. Adventure Journal (right panel)

- Provide a **collapsible panel on the right side** of the page.
- The panel must be **closed by default**.
- Style it as a **medieval adventure journal**.
- Display the history of game actions, including:
  - **Solve Ad** events, with the related ad, success/failure, and state differences.
  - **Potion purchase** events, with the purchased item, result, and state differences.
- Display both kinds of events together in the same adventure history.

### 5.5. Potion Shop (left panel)

- Provide a **collapsible panel on the left side** of the page.
- Style it like a **medieval tavern or apothecary**.
- Include a **Refresh** button.

**Triggers for loading shop items:**

1. The user **opens/expands the Potion Shop panel**.
2. The user clicks **Refresh**.

**Required behavior:**

1. Call the **Get the listing of items available in shop** API endpoint.
2. Display each potion/item as a **small image**.
3. Expose `com.bmv.dragonsofmugloar.domain.model.ShopItem.name` as a **hint/tooltip** for each image.
4. Overlay a **small icon at the bottom-right corner** of each potion image showing its effect, such as **+1 life** or **+N level**, according to the actual item data/model.

### 5.6. Purchase a potion

**Trigger:** user clicks a potion image in the shop.

**Required behavior:**

1. Invoke the **Purchase an item** API endpoint through Vue.
2. Calculate the state differences by comparing the response with the current state before purchase.
3. Update the in-memory game state using the purchase response, **including the turn number**.
4. Save a **purchase event** containing the item, purchase result, and calculated differences in the adventure history.
5. Show the event in the **Adventure Journal**, alongside ad-solving events.
6. Refresh the visible game-state indicators and turn number to reflect the new state.

## 6. Vue Component Requirements

Create **separate Vue components**, at minimum, for the following responsibilities:

| Component responsibility | Purpose |
| --- | --- |
| **Turn** | Display the current turn at the top center of the page. |
| **Current Game State** | Display the game-state attributes with clear icons and wording at the bottom. |
| **Ads List** | Render decrypted, ranked, color-coded bounty notices with Solve Ad actions. |
| **Adventure Journal** | Display the collapsible right-side history panel with solve and purchase events. |
| **Shop** | Display the collapsible left-side potion shop with refresh and purchase actions. |

Follow **single responsibility** strictly. Keep API access, game rules, shared computations, decryption, and presentation-only animations separated as appropriate, to avoid oversized components and duplicated behavior.

## 7. Implementation Guidelines for Codex

1. **Inspect the existing repository and reference files first.** Identify reusable models, calculations, decryption logic, and any existing frontend code before creating new implementations.
2. Obtain the **intro story from the official homepage** at https://dragonsofmugloar.com/ and use its two opening story paragraphs verbatim; do not generate substitute text.
3. Use the **official API documentation** to map each flow to the correct endpoint and response fields; do not guess endpoint formats.
4. Implement the in-memory game state and adventure-event history, using the established domain models as a guide.
5. Implement the required Vue components and the start, ad-solving, shop-listing, and purchasing flows.
6. Use CSS/HTML for visual effects first; isolate any unavoidable vanilla-JS effects from the Vue business layer.
7. Deliver the complete fantasy-style single-page UI, including necessary visual assets.
8. Keep the code readable, concise, maintainable, and free of duplicated business logic.

## 8. Acceptance Checklist

- [ ] Single-page Dragons of Mugloar application integrated with the documented server API.
- [ ] Fantasy design inspired by King's Bounty and Heroes of Might and Magic, with all required assets/images.
- [ ] Intro contains the **two original story paragraphs from https://dragonsofmugloar.com/** verbatim, with original wording, punctuation, and paragraph breaks.
- [ ] Intro story is displayed on animated parchment with a heroic backdrop and bottom-positioned Start Game button.
- [ ] Start Game hides the intro, calls the API, and initializes in-memory game state.
- [ ] Turn appears separately at the top center; game state appears at the bottom with clear icons and labels.
- [ ] Ads load, decrypt correctly, and appear on the central medieval bounty board (approximately 60% of screen).
- [ ] Each ad shows its ID, decrypted message, reward, expiresIn, probability, and icon-bearing Solve Ad button.
- [ ] Ad colors match expired/easy/risky/other classifications; ranking matches `expectedValue`; expired ads are last.
- [ ] Solving an ad calls the API, determines success, calculates lives/gold/score differences, updates state, and records the event.
- [ ] Right-side Adventure Journal is collapsible, initially closed, and contains both solve and purchase events.
- [ ] Left-side Potion Shop is collapsible, has a Refresh button, and loads items when opened or refreshed.
- [ ] Shop items are images with a `ShopItem.name` hint and a bottom-right effect icon (+1 life / +N level, as applicable).
- [ ] Clicking a potion purchases it, updates game state **including turn**, computes differences, and records a journal entry.
- [ ] Vue components exist separately for Turn, Current Game State, Ads List, Adventure Journal, and Shop.
- [ ] Business logic follows SOLID/single-responsibility and reuses existing implementations without copy/paste.
- [ ] Presentation uses HTML/CSS wherever possible; any required vanilla JavaScript presentation logic is separate from Vue business logic.
- [ ] Code is simple, properly formatted, and easy for a human to read.
