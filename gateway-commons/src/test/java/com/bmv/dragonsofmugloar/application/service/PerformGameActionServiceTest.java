package com.bmv.dragonsofmugloar.application.service;

import static com.bmv.dragonsofmugloar.domain.model.AdProbability.GAMBLE;
import static com.bmv.dragonsofmugloar.domain.model.AdProbability.HMMM;
import static com.bmv.dragonsofmugloar.domain.model.AdProbability.IMPOSSIBLE;
import static com.bmv.dragonsofmugloar.domain.model.AdProbability.PIECE_OF_CAKE;
import static com.bmv.dragonsofmugloar.domain.model.AdProbability.QUITE_LIKELY;
import static com.bmv.dragonsofmugloar.domain.model.AdProbability.SURE_THING;
import static com.bmv.dragonsofmugloar.domain.model.AdProbability.SUICIDE_MISSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.bmv.dragonsofmugloar.domain.model.Ad;
import com.bmv.dragonsofmugloar.domain.model.AdProbability;
import com.bmv.dragonsofmugloar.domain.model.Game;
import com.bmv.dragonsofmugloar.domain.model.GameConfig;
import com.bmv.dragonsofmugloar.domain.model.GameConfig.UpgradeMode;
import com.bmv.dragonsofmugloar.domain.model.GameState;
import com.bmv.dragonsofmugloar.domain.model.PurchaseResult;
import com.bmv.dragonsofmugloar.domain.model.Reputation;
import com.bmv.dragonsofmugloar.domain.model.ShopItem;
import com.bmv.dragonsofmugloar.domain.model.ShopItemId;
import com.bmv.dragonsofmugloar.domain.model.SolveAdResult;
import com.bmv.dragonsofmugloar.domain.model.GameAction;
import com.bmv.dragonsofmugloar.domain.model.GameActionWithExecutionContext;
import com.bmv.dragonsofmugloar.port.out.DragonsofmugloarPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class PerformGameActionServiceTest {

  private static final String GAME_ID = "game";
  private static final String TRAP = "Steal super awesome diamond rings";
  private static final int TRAP_EXPIRES_IN = 3;
  private static final int EMPTY_BOARD_RUNS = 20;
  private static final EnumSet<GameAction> LIFECYCLE_ACTIONS = EnumSet.of(GameAction.START_GAME, GameAction.STOP_GAME);
  /** Default config: gold reserve 50, GOLD_EFFICIENT, easy ads are 86-99 percent labels. */
  private static final GameConfig GOLD_EFFICIENT = new GameConfig(100, 60_000);
  private static final GameConfig TURN_EFFICIENT = new GameConfig(GOLD_EFFICIENT.goldReserve(),
      GOLD_EFFICIENT.easyAdProbabilities(), UpgradeMode.TURN_EFFICIENT, GOLD_EFFICIENT.reputationCheckEnabled(),
      GOLD_EFFICIENT.reputationMaxAge(), GOLD_EFFICIENT.turnsLimit(), GOLD_EFFICIENT.timeLimitMillis(),
      GOLD_EFFICIENT.scoreGoal());
  private static final Reputation REPUTATION = new Reputation(1.5, -2.0, 0.5);
  /** Action recorded before the one under test; every performed action must replace it. */
  private static final GameAction PREVIOUS_ACTION = GameAction.ATTEMPT_SAFEST_TRAP;
  private static final GameState STATE =
      new GameState(GAME_ID, 120, 3, 4, 20, 500, 900, REPUTATION, 15L, 1_000, PREVIOUS_ACTION);
  private static final SolveAdResult SOLVE_RESULT = new SolveAdResult(true, 2, 180, 560, 950, 21, "done");
  private static final PurchaseResult PURCHASE_RESULT = new PurchaseResult(true, 20, 3, 5, 21);
  /** Every shop item at its known cost, listed in reverse declaration order. */
  private static final List<ShopItem> SHOP = Arrays.stream(ShopItemId.values())
      .sorted(Comparator.reverseOrder())
      .map(id -> new ShopItem(id, id.name(), -id.getFeatures().goldDiff()))
      .toList();

  private static final ShopItem HEALING = new ShopItem(ShopItemId.HEALING_POTION, "Healing potion", 50);
  private static final ShopItem UPGRADE = new ShopItem(ShopItemId.CLAW_SHARPENING, "Claw sharpening", 100);

  private final DragonsofmugloarPort port = mock(DragonsofmugloarPort.class);
  private final PerformGameActionService service = new PerformGameActionService(port);

  // --- START_GAME ---

  @Test
  void startingAGameTakesTheInitialStateFromTheApiWithNeutralReputationAndRecordsTheStart() {
    when(port.startGame()).thenReturn(Mono.just(new Game(GAME_ID, 3, 0, 0, 0, 4_000, 0)));
    long before = System.currentTimeMillis();

    GameState started = service.startGame().block();

    long after = System.currentTimeMillis();
    assertNotNull(started);
    assertEquals(new GameState(GAME_ID, 0, 3, 0, 0, 0, 4_000, Reputation.NEUTRAL, 0L, started.startedAt(),
        GameAction.START_GAME), started);
    assertTrue(started.startedAt() >= before && started.startedAt() <= after);
    verify(port).startGame();
    verifyNoMoreInteractions(port);
  }

  // --- BUY_HEALING_POTION ---

  @Test
  void buyingAHealingPotionTakesGoldLivesLevelAndTurnFromTheResultAndKeepsTheRest() {
    var healed = new PurchaseResult(true, 70, 2, 4, 21);
    when(port.buyItem(GAME_ID, ShopItemId.HEALING_POTION)).thenReturn(Mono.just(healed));

    assertEquals(new GameState(GAME_ID, 70, 2, 4, 21, 500, 900, REPUTATION, 15L, 1_000, GameAction.BUY_HEALING_POTION),
        service.buyHealingPotion(STATE, HEALING).block());
  }

  // --- REFRESH_REPUTATION ---

  @Test
  void refreshReputationStoresTheReputationAndSpendsATurn() {
    var reputation = new Reputation(3, 2, 1);
    when(port.investigateReputation(GAME_ID)).thenReturn(Mono.just(reputation));

    assertEquals(new GameState(GAME_ID, 120, 3, 4, 21, 500, 900, reputation, 21L, 1_000, GameAction.REFRESH_REPUTATION),
        service.refreshReputation(STATE).block());
  }

  @Test
  void refreshReputationStartsTrackingWhenNeverInvestigated() {
    var state = new GameState(GAME_ID, 0, 3, 0, 0, 0, 0, null, null, 0, null);
    when(port.investigateReputation(GAME_ID)).thenReturn(Mono.just(REPUTATION));

    assertEquals(new GameState(GAME_ID, 0, 3, 0, 1, 0, 0, REPUTATION, 1L, 0, GameAction.REFRESH_REPUTATION),
        service.refreshReputation(state).block());
  }

  // --- solving ads: state update ---

  @Test
  void solvingAnAdTakesGoldLivesTurnAndScoresFromTheResultAndKeepsTheRest() {
    stubSolve();

    assertEquals(new GameState(GAME_ID, 180, 2, 4, 21, 560, 950, REPUTATION, 15L, 1_000, GameAction.SOLVE_BEST_REMAINING_AD),
        service.solveBestRemainingAd(STATE, List.of(ad("a", 10, 3, HMMM))).block());
  }

  // --- SOLVE_SAFEST_AD_ON_LAST_LIFE ---

  @Test
  void lastLifePrefersAnOrdinaryAdOverASaferTrap() {
    assertSolved("ordinary", () -> service.solveSafestAdOnLastLife(STATE, List.of(
        trap("trap", 100, SURE_THING),
        ad("ordinary", 10, 3, GAMBLE))));
  }

  @Test
  void lastLifeFallsBackToTheSafestTrap() {
    assertSolved("safer-trap", () -> service.solveSafestAdOnLastLife(STATE, List.of(
        trap("risky-trap", 500, GAMBLE),
        trap("safer-trap", 10, PIECE_OF_CAKE))));
  }

  @Test
  void survivalRankingPrefersProbabilityThenRewardThenSoonerExpiry() {
    assertSolved("safest", () -> service.solveSafestAdOnLastLife(STATE, List.of(
        ad("richer", 500, 1, PIECE_OF_CAKE),
        ad("safest", 1, 5, SURE_THING))));
    assertSolved("richer", () -> service.solveSafestAdOnLastLife(STATE, List.of(
        ad("poorer", 10, 1, SURE_THING),
        ad("richer", 20, 5, SURE_THING))));
    assertSolved("sooner", () -> service.solveSafestAdOnLastLife(STATE, List.of(
        ad("later", 10, 5, SURE_THING),
        ad("sooner", 10, 2, SURE_THING))));
  }

  // --- SOLVE_PROFITABLE_AD ---

  @Test
  void profitableAdIsTheHighestValueEasyOrdinaryAdWithPositiveExpectedValue() {
    assertSolved("best", () -> service.solveProfitableAd(STATE, List.of(
        ad("unprofitable", 0, 3, SURE_THING),
        ad("good", 20, 3, QUITE_LIKELY),
        ad("best", 60, 3, QUITE_LIKELY),
        ad("not-easy", 1_000, 3, HMMM),
        trap("trap", 1_000, SURE_THING)), GOLD_EFFICIENT));
  }

  @Test
  void valueRankingPrefersExpectedValueThenProbabilityThenSoonerExpiry() {
    // 0.99 * 100 - 0.01 * 50 = 98.5 beats 0.92 * 105 - 0.08 * 50 = 92.6
    assertSolved("higher-value", () -> service.solveBestRemainingAd(STATE, List.of(
        ad("higher-reward", 105, 3, PIECE_OF_CAKE),
        ad("higher-value", 100, 3, SURE_THING))));
    // Equal expected values: 0.01 * 4950 - 0.99 * 50 = 0 and 0.5 * 50 - 0.5 * 50 = 0
    assertSolved("more-likely", () -> service.solveBestRemainingAd(STATE, List.of(
        ad("less-likely", 4_950, 3, IMPOSSIBLE),
        ad("more-likely", 50, 3, HMMM))));
    assertSolved("sooner", () -> service.solveBestRemainingAd(STATE, List.of(
        ad("later", 10, 5, HMMM),
        ad("sooner", 10, 1, HMMM))));
  }

  // --- SOLVE_BEST_REMAINING_AD ---

  @Test
  void bestRemainingAdIsSolvedEvenWithNegativeExpectedValueAndNeverATrap() {
    assertSolved("least-bad", () -> service.solveBestRemainingAd(STATE, List.of(
        trap("trap", 1_000, SURE_THING),
        ad("worse", 0, 3, SUICIDE_MISSION),
        ad("least-bad", 0, 3, GAMBLE))));
  }

  // --- ATTEMPT_SAFEST_TRAP ---

  @Test
  void safestTrapIsTheTrapWithTheBestSurvivalRanking() {
    assertSolved("safer-trap", () -> service.attemptSafestTrap(STATE, List.of(
        ad("ordinary", 100, 3, SURE_THING),
        trap("risky-trap", 1_000, GAMBLE),
        trap("safer-trap", 10, HMMM))));
  }

  @Test
  void trapsAreDetectedIgnoringCase() {
    assertSolved("shouting-trap", () -> service.attemptSafestTrap(STATE, List.of(
        ad("shouting-trap", 10, 3, HMMM, "STEAL SUPER AWESOME DIAMOND crown"))));
  }

  // --- BUY_UPGRADE ---

  @Test
  void buyingTakesGoldLivesLevelAndTurnFromTheResultAndKeepsTheRest() {
    stubPurchase();

    assertEquals(new GameState(GAME_ID, 20, 3, 5, 21, 500, 900, REPUTATION, 15L, 1_000, GameAction.BUY_UPGRADE),
        service.buyUpgrade(gold(150), UPGRADE).block());
  }

  // --- no target ---

  @Test
  void failsWithoutCallingThePortWhenTheActionHasNoTarget() {
    var trapsOnly = List.of(trap("trap", 10, SURE_THING));
    assertThrows(NoSuchElementException.class, () -> service.solveBestRemainingAd(STATE, trapsOnly).block());
    assertThrows(NoSuchElementException.class,
        () -> service.solveProfitableAd(STATE, trapsOnly, GOLD_EFFICIENT).block());
    assertThrows(NoSuchElementException.class,
        () -> service.attemptSafestTrap(STATE, List.of(ad("a", 10, 3, HMMM))).block());
    assertThrows(NoSuchElementException.class, () -> service.solveSafestAdOnLastLife(STATE, List.of()).block());
    verify(port, never()).solveAd(any(), any());
  }

  // --- performGameAction dispatch ---

  @Test
  void performGameActionBuysAHealingPotion() {
    stubPurchase();
    service.performGameAction(STATE,
        new GameActionWithExecutionContext(GameAction.BUY_HEALING_POTION, List.of(), HEALING, GOLD_EFFICIENT)).block();
    verify(port).buyItem(GAME_ID, ShopItemId.HEALING_POTION);
    verify(port, never()).getShopItems(anyString());
  }

  @Test
  void performGameActionRefreshesReputation() {
    when(port.investigateReputation(GAME_ID)).thenReturn(Mono.just(REPUTATION));
    service.performGameAction(STATE,
        new GameActionWithExecutionContext(GameAction.REFRESH_REPUTATION, List.of(), null, GOLD_EFFICIENT)).block();
    verify(port).investigateReputation(GAME_ID);
  }

  @Test
  void performGameActionBuysUpgrade() {
    stubPurchase();
    service.performGameAction(gold(150),
        new GameActionWithExecutionContext(GameAction.BUY_UPGRADE, List.of(), UPGRADE, GOLD_EFFICIENT)).block();
    verify(port).buyItem(GAME_ID, ShopItemId.CLAW_SHARPENING);
    verify(port, never()).getShopItems(anyString());
  }

  @Test
  void performGameActionSolvesTheTargetOfEachAdAction() {
    var ads = List.of(
        ad("profitable", 60, 3, QUITE_LIKELY),
        ad("safest", 1, 3, SURE_THING),
        ad("not-easy", 1_000, 3, HMMM),
        trap("trap", 10, PIECE_OF_CAKE));

    assertSolved("safest", () -> service.performGameAction(STATE,
        new GameActionWithExecutionContext(GameAction.SOLVE_SAFEST_AD_ON_LAST_LIFE, ads, null, GOLD_EFFICIENT)));
    assertSolved("profitable", () -> service.performGameAction(STATE,
        new GameActionWithExecutionContext(GameAction.SOLVE_PROFITABLE_AD, ads, null, GOLD_EFFICIENT)));
    assertSolved("not-easy", () -> service.performGameAction(STATE,
        new GameActionWithExecutionContext(GameAction.SOLVE_BEST_REMAINING_AD, ads, null, GOLD_EFFICIENT)));
    assertSolved("trap", () -> service.performGameAction(STATE,
        new GameActionWithExecutionContext(GameAction.ATTEMPT_SAFEST_TRAP, ads, null, GOLD_EFFICIENT)));
  }

  @Test
  void everyPerformedActionIsRecordedAsTheLastAction() {
    when(port.investigateReputation(GAME_ID)).thenReturn(Mono.just(REPUTATION));
    stubSolve();
    stubPurchase();
    var ads = List.of(ad("profitable", 60, 3, QUITE_LIKELY), trap("trap", 10, PIECE_OF_CAKE));

    for (GameAction action : EnumSet.complementOf(LIFECYCLE_ACTIONS)) {
      ShopItem shopItem = action == GameAction.BUY_HEALING_POTION ? HEALING
          : action == GameAction.BUY_UPGRADE ? UPGRADE : null;
      assertEquals(action, service.performGameAction(gold(1_000),
          new GameActionWithExecutionContext(action, ads, shopItem, GOLD_EFFICIENT))
          .mapNotNull(GameState::lastAction).block(), action.name());
    }
  }

  @Test
  void lifecycleActionsAreNotPerformedAsTurns() {
    for (GameAction action : LIFECYCLE_ACTIONS) {
      var context = new GameActionWithExecutionContext(action, List.of(), null, GOLD_EFFICIENT);

      assertThrows(IllegalArgumentException.class, () -> service.performGameAction(STATE, context), action.name());
    }
    verifyNoInteractions(port);
  }

  @Test
  void selectedExecutionContextsNeverSolveExpiredAds() {
    var chooser = new ChooseGameActionService();
    var lastLife = new GameState(GAME_ID, 0, 1, STATE.level(), STATE.turn(), STATE.score(), STATE.highScore(),
        REPUTATION, STATE.lastReputationInvestigationTurn(), STATE.startedAt(), PREVIOUS_ACTION);
    var expired = ad("expired", 10_000, 0, SURE_THING);
    var expiredTrap = ad("expired-trap", 10_000, -1, SURE_THING, TRAP);

    assertSolved("last-life", () -> service.performGameAction(lastLife,
        chooser.chooseGameAction(GOLD_EFFICIENT, lastLife, List.of(expired, expiredTrap,
            ad("last-life", 10, 3, GAMBLE), trap("safer-trap", 1_000, SURE_THING)), SHOP)));
    assertSolved("profitable-live", () -> service.performGameAction(STATE,
        chooser.chooseGameAction(GOLD_EFFICIENT, STATE, List.of(expired, expiredTrap,
            ad("profitable-live", 60, 3, QUITE_LIKELY), trap("valuable-trap", 10_000, SURE_THING)), SHOP)));
    assertSolved("remaining-live", () -> service.performGameAction(STATE,
        chooser.chooseGameAction(GOLD_EFFICIENT, STATE, List.of(expired, expiredTrap,
            ad("remaining-live", 0, 3, HMMM), trap("tempting-trap", 10_000, SURE_THING)), SHOP)));
    assertSolved("trap-live", () -> service.performGameAction(STATE,
        chooser.chooseGameAction(GOLD_EFFICIENT, STATE, List.of(expired, expiredTrap,
            trap("trap-live", 10, HMMM)), SHOP)));
    verify(port, never()).solveAd(GAME_ID, "expired");
    verify(port, never()).solveAd(GAME_ID, "expired-trap");
  }

  @Test
  void repeatedEmptyBoardBuysAnUpgradeWithTheReservedGold() {
    var state = gold(100, GameAction.SKIP_TURN_NO_AVAILABLE_ADS);
    stubPurchase();

    assertEquals(goldAfterPurchase(GameAction.BUY_UPGRADE), performEmptyBoardRecovery(state, GOLD_EFFICIENT, SHOP));
    verify(port).buyItem(GAME_ID, ShopItemId.CLAW_SHARPENING);
    verify(port, never()).getShopItems(anyString());
  }

  @Test
  void repeatedEmptyBoardFallsBackToHealingWhenOnlyUnaffordableUpgradesAreAvailable() {
    var state = gold(100, GameAction.SKIP_TURN_NO_AVAILABLE_ADS);
    var shopItems = List.of(
        new ShopItem(ShopItemId.CLAW_HONING, "Claw honing", 300),
        new ShopItem(ShopItemId.HEALING_POTION, "Healing potion", 50));
    when(port.buyItem(GAME_ID, ShopItemId.HEALING_POTION)).thenReturn(Mono.just(PURCHASE_RESULT));

    assertEquals(goldAfterPurchase(GameAction.BUY_HEALING_POTION), performEmptyBoardRecovery(state, GOLD_EFFICIENT, shopItems));
    verify(port).buyItem(GAME_ID, ShopItemId.HEALING_POTION);
    verify(port, never()).buyItem(GAME_ID, ShopItemId.CLAW_HONING);
    verify(port, never()).getShopItems(anyString());
  }

  @Test
  void repeatedEmptyBoardCanSpendAllGoldOnHealingWithMoreThanOneLife() {
    var state = gold(50, GameAction.SKIP_TURN_NO_AVAILABLE_ADS);
    stubPurchase();

    assertEquals(goldAfterPurchase(GameAction.BUY_HEALING_POTION), performEmptyBoardRecovery(state, GOLD_EFFICIENT, SHOP));
    verify(port).buyItem(GAME_ID, ShopItemId.HEALING_POTION);
  }

  @Test
  void repeatedEmptyBoardKeepsTheUpgradeModeWhenSpendingTheReserve() {
    var state = gold(300, GameAction.SKIP_TURN_NO_AVAILABLE_ADS);
    stubPurchase();

    assertEquals(goldAfterPurchase(GameAction.BUY_UPGRADE), performEmptyBoardRecovery(state, TURN_EFFICIENT, SHOP));
    verify(port).buyItem(GAME_ID, ShopItemId.CLAW_HONING);
  }

  @Test
  void repeatedEmptyBoardRefreshesReputationWhenNeitherPurchaseIsAvailable() {
    var state = gold(100, GameAction.SKIP_TURN_NO_AVAILABLE_ADS);
    var shopItems = List.of(new ShopItem(ShopItemId.CLAW_HONING, "Claw honing", 300));
    when(port.investigateReputation(GAME_ID)).thenReturn(Mono.just(REPUTATION));

    assertEquals(reputationRefreshed(100), performEmptyBoardRecovery(state, GOLD_EFFICIENT, shopItems));
    verify(port, never()).buyItem(any(), any());
  }

  @Test
  void repeatedEmptyBoardWithoutEnoughGoldOnlyRefreshesReputation() {
    var state = gold(49, GameAction.SKIP_TURN_NO_AVAILABLE_ADS);
    when(port.investigateReputation(GAME_ID)).thenReturn(Mono.just(REPUTATION));

    assertEquals(reputationRefreshed(49), performEmptyBoardRecovery(state, GOLD_EFFICIENT, SHOP));
    verify(port).investigateReputation(GAME_ID);
    verifyNoMoreInteractions(port);
  }

  @Test
  void anEmptyBoardAlternatesBetweenSkippingAndRefreshingReputationOnEveryRun() {
    var chooser = new ChooseGameActionService();
    var state = gold(0);
    var actions = new ArrayList<GameAction>();
    when(port.investigateReputation(GAME_ID)).thenReturn(Mono.just(REPUTATION));

    for (int run = 0; run < EMPTY_BOARD_RUNS; run++) {
      var context = chooser.chooseGameAction(GOLD_EFFICIENT, state, List.of(), SHOP);
      state = service.performGameAction(state, context).block();
      assertNotNull(state);
      actions.add(state.lastAction());
    }

    var expectedActions = IntStream.range(0, EMPTY_BOARD_RUNS)
        .mapToObj(run -> run % 2 == 0 ? GameAction.SKIP_TURN_NO_AVAILABLE_ADS : GameAction.REFRESH_REPUTATION)
        .toList();
    assertEquals(expectedActions, actions);
    // Only the refreshes spend a turn; nothing else changes and the loop never ends by itself.
    long refreshes = EMPTY_BOARD_RUNS / 2;
    assertEquals(new GameState(GAME_ID, 0, 3, 4, 20 + refreshes, 500, 900, REPUTATION, 20 + refreshes, 1_000,
        GameAction.REFRESH_REPUTATION), state);
    verify(port, times((int) refreshes)).investigateReputation(GAME_ID);
    verifyNoMoreInteractions(port);
  }

  @Test
  void performerBuysExactlyTheChosenItemWithoutRecheckingTheReserve() {
    var selected = new ShopItem(ShopItemId.GASOLINE, "Gasoline", 100);
    var state = gold(100);
    stubPurchase();

    assertEquals(goldAfterPurchase(GameAction.BUY_UPGRADE), service.performGameAction(state,
        new GameActionWithExecutionContext(GameAction.BUY_UPGRADE, List.of(), selected, GOLD_EFFICIENT)).block());
    verify(port).buyItem(GAME_ID, ShopItemId.GASOLINE);
    verify(port, never()).getShopItems(anyString());
  }

  private GameState performEmptyBoardRecovery(GameState state, GameConfig config, List<ShopItem> shopItems) {
    var context = new ChooseGameActionService().chooseGameAction(config, state, List.of(ad("expired", 1_000, 0, SURE_THING)), shopItems);
    return service.performGameAction(state, context).block();
  }

  private static GameState goldAfterPurchase(GameAction action) {
    return new GameState(GAME_ID, PURCHASE_RESULT.gold(), PURCHASE_RESULT.lives(), PURCHASE_RESULT.level(),
        PURCHASE_RESULT.turn(), STATE.score(), STATE.highScore(), REPUTATION,
        STATE.lastReputationInvestigationTurn(), STATE.startedAt(), action);
  }

  private void assertSolved(String adId, Supplier<Mono<GameState>> action) {
    stubSolve();
    assertEquals(SOLVE_RESULT.turn(), action.get().map(GameState::turn).block());
    verify(port).solveAd(GAME_ID, adId);
  }

  private void stubSolve() {
    when(port.solveAd(eq(GAME_ID), anyString())).thenReturn(Mono.just(SOLVE_RESULT));
  }

  private void stubPurchase() {
    when(port.buyItem(eq(GAME_ID), any())).thenReturn(Mono.just(PURCHASE_RESULT));
  }

  private static GameState gold(long gold) {
    return gold(gold, PREVIOUS_ACTION);
  }

  private static GameState gold(long gold, GameAction lastAction) {
    return new GameState(GAME_ID, gold, 3, 4, 20, 500, 900, REPUTATION, 15L, 1_000, lastAction);
  }

  private static GameState reputationRefreshed(long gold) {
    return new GameState(GAME_ID, gold, 3, 4, 21, 500, 900, REPUTATION, 21L, 1_000, GameAction.REFRESH_REPUTATION);
  }

  private static Ad ad(String adId, long reward, int expiresIn, AdProbability probability) {
    return ad(adId, reward, expiresIn, probability, "Help defending the village");
  }

  private static Ad trap(String adId, long reward, AdProbability probability) {
    return ad(adId, reward, TRAP_EXPIRES_IN, probability, TRAP);
  }

  private static Ad ad(String adId, long reward, int expiresIn, AdProbability probability, String message) {
    return new Ad(adId, message, reward, expiresIn, null, probability);
  }
}
