# Dragons of Mugloar: investigation findings

Investigations conducted on 20–21 October 2026 through the public API, using the Java adapter and controlled live probes.

This report records what was observed in this chat. A verified observation establishes behavior in the tested cases; it does not prove an undocumented rule holds for every game. Derived conclusions and remaining unknowns are identified explicitly.

## 1. Evidence and scope

| Evidence | Contents |
|---|---|
| [ads.txt](ads.txt) | 785 distinct ads, saved when first discovered; later changes to the same ad are not recorded here. |
| [shopping.txt](shopping.txt) | 126 successful purchase records, each with item ID and gold/lives/level deltas against the immediately preceding state. |
| [solve-ad-result.txt](solve-ad-result.txt) | A representative detailed solve result and notes about generic-message counts. This file currently includes explanatory text as well as JSON. |
| [reputation-investigation.jsonl](reputation-investigation.jsonl) | Controlled investigation: 71 mission attempts across three games at level zero, including reputation and matched-ad comparisons. |
| [skipped-turn-ads.jsonl](skipped-turn-ads.jsonl) | Complete boards for three games, each advanced through 28 reputation checks without solving or purchasing. |
| [skipped-turn-ads-summary.json](skipped-turn-ads-summary.json) | Definitions, controls, checkpoints and aggregate results for the skipped-turn experiment. |

The 150-attempt message investigation also produced a temporary full trace at `/tmp/solve-ad-message-observations.jsonl`. Its counts were rechecked when preparing this report. That temporary trace is outside the repository and should not be treated as a durable repository artifact.

## 2. Live API contract

The API was accessible without credentials at `https://dragonsofmugloar.com/api/v2`.

| Method | Relative path | Operation |
|---|---|---|
| POST | `/game/start` | Start a game. |
| GET | `/{gameId}/messages` | Fetch ads. |
| POST | `/{gameId}/solve/{adId}` | Attempt an ad. |
| GET | `/{gameId}/shop` | List shop items. |
| POST | `/{gameId}/shop/buy/{itemId}` | Attempt a purchase. |
| POST | `/{gameId}/investigate/reputation` | Investigate reputation. |

Observed response details that extend or correct the documentation:

- Ad and shop listings are JSON arrays.
- Ad `reward` is numeric in the sampled live responses.
- Purchase `shoppingSuccess` is a JSON boolean.
- Ads include `probability` and `encrypted`, which were absent from the inspected endpoint field descriptions.
- Reputation contains three numeric fields: `people`, `state` and `underworld`. Fractional and negative values occur.

Reference: [official API documentation](https://dragonsofmugloar.com/doc/).

### Ad encoding

| `encrypted` | Observed interpretation | Distinct ads in ads.txt |
|---|---|---:|
| `null` | Plain text | 609 |
| `1` | Base64 | 159 |
| `2` | ROT13 | 17 |

The adapter decoded ad IDs, descriptions and probability labels. Numeric rewards remained numeric in these observations. The message investigation attempted 27 Base64 ads and one ROT13 ad through the adapter.

A separate Base64 probe confirmed that the decoded ID was accepted by the solve endpoint. Retaining the raw encryption marker in the model preserves how an ad was received.

No claim is made about other encryption markers or formats.

## 3. Turns, state transitions and expiration

| Action | Observed turn effect |
|---|---|
| Fetch ads | No turn advance in the repeated-read controls. |
| Solve an ad | Advances one turn. |
| Successful purchase | Advances one turn. |
| Unsuccessful purchase | Also advances one turn; confirmed by the live integration test. |
| Investigate reputation | Advances one turn. |

Ads expire according to game turns. In the reputation controls, each investigation reduced the remaining expiry of surviving ads by one; all 60 such expiry comparisons agreed.

The 71-attempt reputation trace was checked against the preceding game state, accounting for intervening healing purchases:

- All 64 successful solves added the ad reward to both gold and score and left lives unchanged.
- All seven failed solves added no gold or score and removed one life.
- Repeating ad reads without an intervening action left the complete board unchanged in all three skipped-turn controls.

No additional penalty merely for leaving an ad unsolved was observed during play. However, losing favorable opportunities through expiration is a demonstrated cost; see section 7.

## 4. Shop effects

Every logged purchase of a given item produced the same three deltas. All 126 entries in shopping.txt represent successful purchases.

| ShopItemId | API ID | Gold delta | Lives delta | Level delta | Observations |
|---|---|---:|---:|---:|---:|
| HEALING_POTION | hpot | -50 | +1 | 0 | 58 |
| CLAW_SHARPENING | cs | -100 | 0 | +1 | 12 |
| GASOLINE | gas | -100 | 0 | +1 | 12 |
| COPPER_PLATING | wax | -100 | 0 | +1 | 11 |
| BOOK_OF_TRICKS | tricks | -100 | 0 | +1 | 11 |
| POTION_OF_STRONGER_WINGS | wingpot | -100 | 0 | +1 | 10 |
| CLAW_HONING | ch | -300 | 0 | +2 | 3 |
| ROCKET_FUEL | rf | -300 | 0 | +2 | 3 |
| IRON_PLATING | iron | -300 | 0 | +2 | 2 |
| BOOK_OF_MEGATRICKS | mtrix | -300 | 0 | +2 | 2 |
| POTION_OF_AWESOME_WINGS | wingpotmax | -300 | 0 | +2 | 2 |

Verified observations:

- Healing potion was the only one of the 11 tested items that increased lives.
- All ten other items increased level without changing lives.
- Every item was successfully purchased more than once; purchases were not restricted to one use in the sampled games.
- Items in the same price tier had identical effects on the returned gold, lives and level fields.

Derived tradeoff:

- Two 100-gold upgrades give +2 levels for 200 gold, but consume two turns.
- One 300-gold upgrade gives +2 levels for 300 gold and consumes one turn.
- The cheaper tier is more efficient in gold per observed level increase. That does not establish that it is always the better choice.

Hidden item-specific effects were not tested. Healing at full lives, maximum lives, and a maximum dragon level were not established.

## 5. Probability labels depend on state

A controlled live probe compared identical ad IDs immediately before and after a level-zero-to-level-one upgrade:

| Same ad ID | Before upgrade | After upgrade |
|---|---|---|
| 2XNivg4Q | Risky | Gamble |
| qPKa9NHX | Piece of cake | Sure thing |

This confirms that the advertised probability is not an immutable property of an ad. A level upgrade can improve its label. Fetching the board again after an upgrade is therefore useful.

The integer percentages in `AdProbability` were manually guessed from the labels. They were not returned by the API or calibrated by these experiments.

A favorable label is not a guarantee of success. In particular, eight trap outcomes in the message investigation came from ads labeled `SURE_THING`.

## 6. Reputation investigation

### Reputation changes caused by sampled missions

The controlled experiment used three games with dragon level fixed at zero. Its 71 attempts produced 64 successes and seven failures.

The following deltas were measured after successful missions. Values are rounded to remove floating-point representation noise.

| Task wording/category | People delta | State delta | Underworld delta | Observations |
|---|---:|---:|---:|---:|
| Reach an agreement | +0.2 | 0 | 0 | 5 |
| Clean something | +0.1 | 0 | 0 | 1 |
| Sell an unordinary item | +0.1 | 0 | 0 | 5 |
| Transport a magic item | +0.3 | 0 | 0 | 3 |
| Escort someone | +0.1 | 0 | 0 | 3 |
| Create an advertisement campaign | +1 | 0 | 0 | 4 |
| Steal a delivery and share profits with the people | +1 | -2 | 0 | 6 |
| Investigate someone and their relation to a magic item | -0.1 | +1 | 0 | 2 |
| Help defend a location from intruders | 0 | 0 | 0 | 35 |

The seven failures in this experiment left reputation unchanged. These observations do not establish rules for every task template or every failure.

### Effects on advertised probabilities

During the experiment:

- People reputation ranged from the initial zero to 6.3.
- State reputation ranged from zero to -6.
- Underworld reputation remained zero.
- Reputation changed after 29 mission attempts.
- There were 421 comparisons of surviving ads with identical IDs.
- Of those, 179 followed a reputation change.
- No compared ad changed its probability label.

**Supported conclusion:** no effect of the sampled reputation changes on existing ads' advertised labels was detected at level zero.

**Not established:** reputation has no effect on actual success, traps, newly generated ads, or other game behavior. Underworld reputation was not varied, and no traps occurred in this controlled experiment.

The separate control also established that reputation investigation has a cost: it consumes a turn even though the repeated checks did not change reputation.

## 7. Skipping turns can reduce the supply of easy ads

Design:

- Three fresh games.
- No missions solved and no items purchased.
- Advance 28 turns per game using reputation checks.
- Fetch and save the board after every skipped turn.
- Level stayed zero; every returned reputation value stayed zero.
- All 90 recorded boards contained 10 ads.
- Repeated reads before advancing turns were identical in all three games.

Two definitions were used, based on labels rather than unverified numeric odds:

- **Strict easy:** Sure thing or Piece of cake.
- **Broad easy:** the above plus Walk in the park.

| Game | Strict easy, turn 0 | Strict easy, turn 28 | Broad easy, turn 0 | Broad easy, turn 28 |
|---|---:|---:|---:|---:|
| 1 | 6 | 1 | 8 | 3 |
| 2 | 3 | 1 | 5 | 1 |
| 3 | 2 | 2 | 4 | 2 |
| **Total** | **11** | **4** | **17** | **6** |

At these endpoints, strict easy counts fell approximately 64% overall, and broad easy counts fell approximately 65%.

Existing ads retained their labels across 708 comparisons. Changes in the board's difficulty mix appeared as ads expired and replacements arrived.

The decline was not monotonic or universal: game 2 initially rose from three strict-easy ads to four, and game 3 ended with the same strict-easy count it started with.

**Supported conclusion:** replenishment preserves the number of offers, but does not preserve the supply of favorable offers. Skipping turns lost easy opportunities overall in this sample.

This does not prove a fixed difficulty-growth formula or that every future skipped turn makes the board worse. No actual mission outcomes were measured in this experiment.

## 8. Solve-result messages and traps

The message investigation attempted 150 missions across six games, varying task difficulty, upgrades and task selection.

| Exact message | Occurrences |
|---|---:|
| You successfully solved the mission! | 100 |
| You failed on the mission! | 39 |
| You fell into a trap set up, by people who did not appreciate your dealings. | 11 |

Only the trap response qualified as a distinct, more detailed explanation. One representative full `SolveAdResult` was saved rather than repeating the same message.

Additional observations from the full temporary trace:

- All 11 trap ads had descriptions beginning `Steal super awesome diamond ...`.
- Eight were labeled Sure thing and three Piece of cake.
- All returned `success: false`.

The wording is a useful observed warning pattern, not proof that every similarly worded ad must be a trap. The reputation conditions, thresholds and mechanism behind traps remain unverified.

Only three message variants were observed in these 150 attempts. This is not an exhaustive inventory of every message the API can return.

## 9. Gameplay results

Two collecting runs recorded in the chat illustrate achievable outcomes:

| Run | Successful solves | Failed solves | Successful purchases | Score | Final level | Lives when stopped |
|---|---:|---:|---:|---:|---:|---:|
| Initial collection of 50 distinct ads | 12 | 3 | 7 | 664 | 5 | 2 |
| Shopping-focused game, ending when lives ran out | 44 | 16 | 59 | 7,660 | 58 | 0 |

The first run stopped when its collection target was met. The second spent 7,650 gold, restored 13 lives and purchased every shop item type.

One game exceeded the task's 1,000-point threshold. These runs do not establish that the strategy reliably reaches that threshold or is optimal.

Live requests were separated by 600–800 ms pauses in these experiments. No rate-limit rejection was observed in the successful runs at that pace; this does not establish an official rate limit.

## 10. Practical implications and limits

The evidence supports these implementation choices:

1. Refresh ads after a level upgrade; the same ad can acquire a better probability label.
2. Account for turns spent on both purchases and reputation investigations when evaluating expiry.
3. Use easy-ad availability and reward/risk, rather than board size alone, to assess whether skipping a turn is costly.
4. Treat labels as estimates. Trap outcomes demonstrate that a favorable label alone is insufficient.
5. Use observed item effects for planning, while accounting for the gold-versus-turn tradeoff between upgrade tiers.
6. Avoid treating repeated reputation checks as free observations.

The investigations did **not** establish exact success percentages, an optimal strategy, the full set of result messages, reputation-to-success or reputation-to-trap formulas, all faction effects, hidden item differences, life/level caps, or a universal rule that easy-ad counts must decline every turn.

## 11. Final implemented algorithm

This section describes the current Java implementation, including the full-game
command, turn command, action chooser, performer, adapter and result contract.
The strategy uses empirical estimates and the trap heuristic from the investigations;
its decisions do not establish additional rules of the external game.

### Request and configuration

`PlayGameRequest.toGameConfig()` fills each missing or null request field from a
new default `GameConfig`. An empty JSON object is a valid default request; the
request's Jackson type ID is optional.

| Request/configuration field | Default | Meaning |
|---|---|---|
| `goldReserve` | `50` | Gold retained when selecting a normal upgrade. |
| `easyAdProbabilities` | `SURE_THING`, `PIECE_OF_CAKE`, `WALK_IN_THE_PARK`, `QUITE_LIKELY` | Labels eligible for the profitable-ad action. Membership is configurable independently of estimated percentage. |
| `upgradeMode` | `GOLD_EFFICIENT` | Cheapest eligible upgrade; alternative `TURN_EFFICIENT` prefers the largest level gain. |
| `reputationCheckEnabled` | `true` | Enables the optional stale-reputation branch. Empty-board recovery can investigate even when this is false. |
| `reputationMaxAge` | `10` turns | Reputation becomes stale when its age is strictly greater than this value. |
| `turnsLimit` | `Integer.MAX_VALUE` (`2_147_483_647`) | Maximum absolute game turn reported in state. |
| `timeLimitMillis` | `300_000` (five minutes) | Elapsed wall time measured from initialization of `GameState` after the start response. |
| `scoreGoal` | `1001` | Stop when the current score reaches or exceeds this value. |

Gold reserve and reputation age must be nonnegative. Turn, time and score limits
must be positive. An explicitly empty easy-label set is preserved and disables
the profitable-ad branch; the remaining solving fallbacks still apply.

`ShopItemId.HEALING_COST` is `50` and is used for expected-value calculations.
Purchase affordability uses the actual fetched `ShopItem.cost()`, including for
healing. The modeled potion grants one life; the modeled upgrade tiers grant
one level for 100 gold or two levels for 300 gold.

### Ad evaluation and ranking

The current `AdProbability` estimates are:

| Label/enum | Estimated success percentage |
|---|---:|
| `SURE_THING` | 99 |
| `PIECE_OF_CAKE` | 92 |
| `WALK_IN_THE_PARK` | 86 |
| `QUITE_LIKELY` | 86 |
| `HMMM` | 50 |
| `GAMBLE` | 46 |
| `RISKY` | 40 |
| `PLAYING_WITH_FIRE` | 36 |
| `RATHER_DETRIMENTAL` | 21 |
| `SUICIDE_MISSION` | 11 |
| `IMPOSSIBLE` | 1 |
| `UNKNOWN` | 50 |

These are fitted label-level estimates, including trap failures, rather than
API-supplied odds. See [AD_PROBABILITY_CALIBRATION.md](AD_PROBABILITY_CALIBRATION.md).
Unknown advertised labels map to `UNKNOWN`.

For each ad, `p = successPercentage / 100.0` and
`expectedValue = p * reward - (1 - p) * HEALING_COST`.

| Ranking | Preference, in order |
|---|---|
| Value | Higher expected value; higher estimated success percentage; smaller `expiresIn`. |
| Survival | Higher estimated success percentage; higher reward; smaller `expiresIn`. |
| Last-life solving | Ordinary ads before suspected traps, then survival ranking. |

Ads with `expiresIn <= 0` are excluded. A suspected trap has a decoded message
beginning with `Steal super awesome diamond `, ignoring case using
`Locale.ROOT`. A null message is not classified as a trap. The prefix remains
an observed heuristic.

### Full-game orchestration

`PlayDragonsofmugloarGameCommand<PlayGameRequest, PlayGameResponse>` is named
`PLAY_DRAGONSOFMUGLOAR_GAME`. It uses `AnonymousAuthorization`, accepts the
`ANONYMOUS_USER` role, and orchestrates its two stages on `RdmService.GATEWAY`.
Its buffer is `Buffer(GameState gameState, GameConfig gameConfig)`.

| Stage | Execution and buffer behavior |
|---|---|
| `1: START_GAME` | A linked-service rule calls `PlayDragonsofmugloarGameService.startGame`, starts the game through the port, creates state from the returned game, derives configuration from the request, stores both in the buffer, and returns an initial summary. |
| `2: PLAY_TURNS` | `ExecutionRuleWithLinkedServiceAndConditionalLoop` wraps `ExecutionRuleWithSubCommandmentAndStaticMappers` for `PlayGameTurnCommand`, with `propagateErrors = false`, `RdmService.CURRENT`, a ceiling of `Integer.MAX_VALUE` iterations, and no configured delay. |

Initial state preserves the returned game ID, gold, lives, level, turn, score and
high score. It starts with neutral reputation, the returned turn as the last
reputation-investigation turn, the current timestamp as `startedAt`, and a null
`lastAction`. The outer start stage calls the port directly; the separate
`PerformGameActionService.startGame()` helper additionally records `START_GAME`
but is not used by this stage.

Each iteration maps the buffered state and config into `PlayGameTurnRequest`.
A non-null `PlayGameTurnResponse.gameState()` replaces the buffered state. If
the turn response contains no state, the previous state is retained. The
response mapper produces a `PlayGameResponse` from that latest state and the
turn's `errorDetails`.

The loop checks its exit condition before the first turn and after each mapped
iteration. It exits when any of these conditions holds:

- The response has an error whose message is null or does not contain the exact
  substring `Dragonsofmugloar server error: `.
- `gameState.turn() >= gameConfig.turnsLimit()`.
- `currentTimeMillis - gameState.startedAt() >= gameConfig.timeLimitMillis()`.
- `gameState.score() >= gameConfig.scoreGoal()`.

The iteration ceiling is an additional framework bound. Turns count game turns,
including purchases and investigations, rather than loop attempts. A local skip
or a failed API call can leave the game turn unchanged.

A response with the server-error marker allows another iteration while the
limits remain unmet. This decision uses the serialized error message, not an
exception-class check or the generic `recoverable` flag. Response merging takes
the latest non-null iteration response, so a successful turn clears an earlier
server error. If a limit is reached while the latest result is a server error,
that error remains in the final response.

The time limit is checked between iterations. An in-progress turn is not
interrupted by this game-specific check and can finish after the limit. Router,
HTTP and blocking-call timeouts are separate bounds. The loop has no explicit
`lives <= 0` condition and no terminal `IN_STUCK` action; it stops according to
the limits, response errors or iteration ceiling above.

Both commands use the framework's mutable-buffer support with immutable record
snapshots. Their flow states replace the current buffer and assemble commands
with that latest snapshot, preserving it across stage transitions and JSON
serialization.

### One-turn stages

`PlayGameTurnCommand` is named `PLAY_GAME_TURN`. It uses
`RdmServiceAuthorization`, requires `RDM_CLUSTER_SERVICE`, and runs on the
gateway. Its request requires non-null, validated game state and configuration.
Its buffer carries copied shop/ad lists and the selected execution context.

| Stage | Behavior |
|---|---|
| `1: FETCH_SHOP_ITEMS` | Fetch the current shop and save it in the buffer. |
| `2: CHOOSE_CRITICAL_HEALING` | If one life remains and healing is affordable, buffer the selected healing context and jump to `EXECUTE_ACTION`. This stage neither purchases nor returns a turn result. |
| `3: FETCH_ADS` | Fetch ads, filter out expired entries, and buffer the remaining ads. This stage is bypassed by critical healing. |
| `4: CHOOSE_ACTION` | Run the chooser and buffer its action/execution context. |
| `5: EXECUTE_ACTION` | Perform the buffered action and return `PlayGameTurnResponse(updatedState, null)`. |

The critical-healing jump uses `ExecutionRuleWithLinkedServiceAndConditionalStage`;
the other stages use linked-service rules. A new iteration fetches shop items
again, and fetches fresh ads whenever it does not take the healing jump.

### Action selection, in exact priority order

`ChooseGameActionService` resolves shop availability, affordability and upgrade
preferences. It returns `GameActionWithExecutionContext(gameAction, ads, shopItem,
gameConfig)`. Purchase contexts require one non-null selected item. Solving
contexts carry only the eligible candidate ads for the chosen action; non-solving
contexts carry an empty ad list.

1. **Critical healing.** If `lives == 1`, choose an available item with positive
   `livesDiff` and `cost <= gold`, preferring lower cost and then
   `ShopItemId` declaration order. Return `BUY_HEALING_POTION`. Ignore the gold
   reserve and the previous action. This check also exists in the standalone
   chooser, in addition to the command's early healing stage.

2. **Recover from a previous empty-board skip.** If
   `lastAction == SKIP_TURN_NO_AVAILABLE_ADS` and no unexpired ads remain,
   first select an affordable available upgrade using a reserve of zero. Apply
   the configured upgrade preference and then item declaration order. If no
   upgrade qualifies, select affordable healing, even with more than one life.
   If neither purchase qualifies, continue.

3. **Optionally refresh stale reputation.** Reputation is stale when
   `lastReputationInvestigationTurn == null` or
   `turn - lastReputationInvestigationTurn > reputationMaxAge`. An urgent ad is
   ordinary and unexpired, has estimated probability at least `RISKY` (40%),
   and has `expiresIn < 3` (one or two turns left). If checks are enabled,
   reputation is stale and no urgent ordinary ad exists, return
   `REFRESH_REPUTATION`.

4. **Prioritize survival on the last life.** If `lives == 1` and ads remain,
   return `SOLVE_SAFEST_AD_ON_LAST_LIFE` with ordinary candidates if any exist,
   otherwise suspected traps. The performer selects the best by survival ranking.

5. **Solve a profitable easy ad.** Filter ordinary ads to configured
   `easyAdProbabilities` with strictly positive expected value. If any remain,
   return `SOLVE_PROFITABLE_AD`; the performer uses value ranking.

6. **Buy a normal upgrade.** If `lastAction != BUY_UPGRADE`, consider available
   items with positive `levelDiff` and `gold - cost >= goldReserve`.
   `GOLD_EFFICIENT` prefers the cheapest item regardless of level gain;
   `TURN_EFFICIENT` prefers the largest level gain. Both break equal preferences
   by `ShopItemId` declaration order. Return `BUY_UPGRADE` with the chosen
   item. If none qualifies, continue. Empty-board recovery in step 2 can still
   select an upgrade after a previous upgrade.

7. **Solve a remaining ordinary ad.** If ordinary ads remain, return
   `SOLVE_BEST_REMAINING_AD` and use value ranking, including when the best
   expected value is nonpositive.

8. **Attempt a suspected trap.** If only suspected traps remain, return
   `ATTEMPT_SAFEST_TRAP` and use survival ranking.

9. **Advance after a repeated empty-board skip.** If no earlier action applies
   and `lastAction == SKIP_TURN_NO_AVAILABLE_ADS`, return
   `REFRESH_REPUTATION`. This recovery action applies even if optional
   reputation checking is disabled or reputation is fresh, and advances the
   game so the board can change.

10. **Record the first empty-board skip.** Otherwise return
    `SKIP_TURN_NO_AVAILABLE_ADS`. Performing it only records `lastAction`;
    it makes no further API call and does not advance the turn. The next
    iteration fetches the shop and ads again.

The chooser declares `@NotEmpty List<@NotNull Ad>` on its ad parameter, but the
staged service filters before calling it and supports an empty resulting list.
The empty-board recovery paths are implemented explicitly.

### Action execution and state updates

`PerformGameActionService` ranks the supplied candidates or buys the single
item selected by the chooser. It does not fetch the shop or repeat affordability,
reserve or item-preference selection. Each iteration executes at most one
turn-changing operation.

| Action/result | State update |
|---|---|
| Solve an ad | Use returned `gold`, `lives`, `turn`, `score` and `highScore`; preserve level. Both successful and failed missions apply the returned state. |
| Buy healing or an upgrade | Use returned `gold`, `lives`, `level` and `turn`; preserve score and high score. `shoppingSuccess` is available in `PurchaseResult` but is not a separate branch or state field; the returned values apply for either outcome. |
| Investigate reputation | Replace reputation, increment the local turn by one, and set the last investigation turn to that updated turn. Preserve gold, lives, level, score and high score. |
| Skip with no available ads | Change only `lastAction`. |

Every successful execution preserves `gameId` and `startedAt` and records the
performed action. Solves and purchases preserve the last investigated reputation
and investigation turn. The current strategy does not use reputation values
directly in its ad rankings or trap classification.

`START_GAME` and `STOP_GAME` are lifecycle enum values and are rejected by
`performGameAction` as turn actions. The full-game loop does not call a remote
stop endpoint or overwrite the final state with `STOP_GAME`.

### Adapter and error handling

`DragonsofmugloarAdapter` implements the six port operations listed in section 2
against `https://dragonsofmugloar.com/api/v2`. It uses the shared HTTP/1 client
pool and its configured default read timeout.

It decodes ad ID, message and probability using the encryption marker: null or
zero is plain text, one is UTF-8 Base64, and two is ROT13. It retains the marker.
Unsupported markers and unknown shop item IDs raise errors. Shop IDs are mapped
to `ShopItemId`, and prices are taken from each live shop response.

HTTP 400 and 401 errors propagate unchanged. Other HTTP errors, transport errors,
timeouts and blank/empty bodies are wrapped in
`DragonsofmugloarServerException`, with the marker
`Dragonsofmugloar server error: `, `COMMANDMENT_EXECUTION_ERROR`,
`recoverable = true`, and endpoint/status error parameters. A failure without
an HTTP response uses status 503. JSON parsing and ad/shop conversion happen
after this wrapping step; their errors do not automatically receive the
server-error marker.

There is no explicit API pacing, loop delay or backoff in the current algorithm.
There is also no per-request adapter retry. After a mapped server error, the
outer loop retries the turn from the latest buffered state, fetching the shop
and ads again as applicable. A POST that succeeded remotely but lost its response
can therefore leave local state stale; the implementation has no reconciliation
or idempotency mechanism for that case. Failure of the initial start stage
returns an error rather than entering the turn loop.

### Response and gateway entry point

`PlayGameResponse` contains `gameId`, `gold`, `lives`, `level`, `turn`,
`score`, `highScore`, `reputation`, `durationMs` and `errorDetails`.
The summary uses the latest buffered state; `reputation` is the latest stored
investigation result, and `durationMs` is the current wall time minus
`startedAt`. A mapped turn error retains the known game ID and state summary.
The definition's generic error response has null game ID/reputation, zero
numeric fields and the supplied error details, including when starting fails.
There is no separate stop-reason field.

The current `DragonsofmugloarGameController` accepts JSON through
`POST /bmv/dragonsofmugloar/api/v1/autoplay`, validates `PlayGameRequest`, executes
`PlayDragonsofmugloarGameCommand` through `GatewayCommandmentRouter`, returns
the response and logs `Game results`. Sending `{}` selects defaults; sending
`{"timeLimitMillis":60000}` changes only the game time limit to one minute.

### Verification and implementation references

`PlayDragonsofmugloarGameCommandTest` covers turn/score/time limits, pre-loop
termination, recovery from server errors with preserved state, terminal errors,
start failure, command buffer/stage serialization, and response game ID/JSON
round-trip behavior. Turn-command, chooser, performer, adapter and request tests
cover their respective branches and mappings.

`PlayGameIT.playsGameWithDefaultsAndOneMinuteLimitAndPrintsResults` uses the
real router and live adapter, overrides only `timeLimitMillis` to `60_000`,
and logs the final JSON response. It asserts a non-null, error-free response and
at least one executed turn. Its current router timeout and blocking timeout are
also one minute, and its JUnit timeout is five minutes. A score goal or terminal
error can end the game earlier; reaching the time limit is not guaranteed.
`PlayGameIT` also retains the separate five-sequential-turn test. The `IT`
suffix excludes these live tests from default Surefire selection.

Run only the full-game integration test from the repository root:

```shell
mvn -pl gateway-service -am \
  '-Dtest=PlayGameIT#playsGameWithDefaultsAndOneMinuteLimitAndPrintsResults' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Implementation sources:

- [Full-game command definition](gateway-commons/src/main/java/com/bmv/dragonsofmugloar/domain/api/commandment/definition/PlayDragonsofmugloarGameCommandDefinition.java) and [service](gateway-commons/src/main/java/com/bmv/dragonsofmugloar/application/service/PlayDragonsofmugloarGameService.java).
- [Turn command definition](gateway-commons/src/main/java/com/bmv/dragonsofmugloar/domain/api/commandment/definition/PlayGameTurnCommandDefinition.java) and [service](gateway-commons/src/main/java/com/bmv/dragonsofmugloar/application/service/PlayGameTurnService.java).
- [Action chooser](gateway-commons/src/main/java/com/bmv/dragonsofmugloar/application/service/ChooseGameActionService.java) and [performer](gateway-commons/src/main/java/com/bmv/dragonsofmugloar/application/service/PerformGameActionService.java).
- [Game configuration](gateway-commons/src/main/java/com/bmv/dragonsofmugloar/domain/model/GameConfig.java), [request](gateway-commons/src/main/java/com/bmv/dragonsofmugloar/domain/api/request/PlayGameRequest.java) and [response](gateway-commons/src/main/java/com/bmv/dragonsofmugloar/domain/api/response/PlayGameResponse.java).
- [Adapter](gateway-commons/src/main/java/com/bmv/dragonsofmugloar/infrastructure/adapter/DragonsofmugloarAdapter.java), [gateway controller](gateway-service/src/main/java/com/bmv/gateway/infrastructure/adapter/in/web/consumption/DragonsofmugloarGameController.java) and [live integration test](gateway-service/src/test/java/com/bmv/gateway/infrastructure/adapter/in/PlayGameIT.java).
