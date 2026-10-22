package com.bmv.dragonsofmugloar.application.service;

import static com.bmv.dragonsofmugloar.domain.model.AdProbability.GAMBLE;
import static com.bmv.dragonsofmugloar.domain.model.AdProbability.HMMM;
import static com.bmv.dragonsofmugloar.domain.model.AdProbability.IMPOSSIBLE;
import static com.bmv.dragonsofmugloar.domain.model.AdProbability.PLAYING_WITH_FIRE;
import static com.bmv.dragonsofmugloar.domain.model.AdProbability.QUITE_LIKELY;
import static com.bmv.dragonsofmugloar.domain.model.AdProbability.RISKY;
import static com.bmv.dragonsofmugloar.domain.model.AdProbability.SURE_THING;
import static com.bmv.dragonsofmugloar.domain.model.AdProbability.WALK_IN_THE_PARK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.bmv.dragonsofmugloar.domain.model.Ad;
import com.bmv.dragonsofmugloar.domain.model.AdProbability;
import com.bmv.dragonsofmugloar.domain.model.GameConfig;
import com.bmv.dragonsofmugloar.domain.model.GameConfig.UpgradeMode;
import com.bmv.dragonsofmugloar.domain.model.GameState;
import com.bmv.dragonsofmugloar.domain.model.ShopItem;
import com.bmv.dragonsofmugloar.domain.model.ShopItemId;
import com.bmv.dragonsofmugloar.domain.model.GameAction;
import com.bmv.dragonsofmugloar.domain.model.GameActionWithExecutionContext;
import org.junit.jupiter.api.Test;

class ChooseGameActionServiceTest {

  /** Default config: gold reserve 50, reputation max age 10, easy ads are 86-99 percent labels. */
  private static final GameConfig CONFIG = new GameConfig(100, 60_000);
  private static final List<ShopItem> SHOP = List.of(
      new ShopItem(ShopItemId.CLAW_SHARPENING, "Claw sharpening", 100),
      new ShopItem(ShopItemId.HEALING_POTION, "Healing potion", 50));
  private static final List<ShopItem> FULL_SHOP = Arrays.stream(ShopItemId.values())
      .sorted(Comparator.reverseOrder())
      .map(id -> new ShopItem(id, id.name(), -id.getFeatures().goldDiff()))
      .toList();
  /** With the default reserve, the cheapest +1-level item (100) is affordable from 150 gold. */
  private static final long UPGRADE_GOLD = 150;
  private static final long INVESTIGATED_AT = 100;
  private static final int LIVES = 3;

  private final ChooseGameActionService service = new ChooseGameActionService();

  // --- critical healing ---

  @Test
  void healsOnTheLastLifeWhenAHealingPotionIsAffordable() {
    assertTrue(service.chooseCriticalHealing(fresh(50, 1), SHOP));
    assertTrue(service.chooseCriticalHealing(fresh(1_000, 1), SHOP));
  }

  @Test
  void doesNotHealOnTheLastLifeWithoutGoldForAHealingPotion() {
    assertFalse(service.chooseCriticalHealing(fresh(49, 1), SHOP));
  }

  @Test
  void healsOnlyOnExactlyOneLife() {
    assertFalse(service.chooseCriticalHealing(fresh(1_000, 2), SHOP));
    assertFalse(service.chooseCriticalHealing(fresh(1_000, 0), SHOP));
  }

  @Test
  void healsAgainWhenThePotionIsStillAvailable() {
    assertTrue(service.chooseCriticalHealing(freshAfter(GameAction.BUY_HEALING_POTION, 1_000, 1), SHOP));
  }

  @Test
  void healsAgainAfterAnyOtherAction() {
    for (GameAction lastAction : GameAction.values()) {
      if (lastAction != GameAction.BUY_HEALING_POTION) {
        assertTrue(service.chooseCriticalHealing(freshAfter(lastAction, 1_000, 1), SHOP), lastAction.name());
      }
    }
  }

  // --- 1. REFRESH_REPUTATION ---

  @Test
  void refreshesReputationThatWasNeverInvestigated() {
    assertEquals(GameAction.REFRESH_REPUTATION,
        choose(neverInvestigated(), List.of(ordinary(10, 3, QUITE_LIKELY))));
  }

  @Test
  void refreshesReputationEvenWithoutAds() {
    assertEquals(GameAction.REFRESH_REPUTATION, choose(neverInvestigated(), List.of()));
  }

  @Test
  void reputationBecomesStaleOnlyAfterMaxAgeTurns() {
    var ads = List.of(ordinary(0, 5, HMMM));
    assertEquals(GameAction.SOLVE_BEST_REMAINING_AD, choose(investigatedTurnsAgo(10), ads));
    assertEquals(GameAction.REFRESH_REPUTATION, choose(investigatedTurnsAgo(11), ads));
  }

  @Test
  void usesTheConfiguredReputationMaxAge() {
    var config = config(CONFIG.goldReserve(), CONFIG.easyAdProbabilities(), UpgradeMode.GOLD_EFFICIENT, true, 0);
    assertEquals(GameAction.REFRESH_REPUTATION,
        service.chooseGameAction(config, investigatedTurnsAgo(1), List.of(ordinary(0, 5, HMMM)), SHOP).gameAction());
  }

  @Test
  void doesNotRefreshReputationWhenChecksAreDisabled() {
    var config = config(CONFIG.goldReserve(), CONFIG.easyAdProbabilities(), UpgradeMode.GOLD_EFFICIENT, false, 10);
    assertEquals(GameAction.SOLVE_BEST_REMAINING_AD,
        service.chooseGameAction(config, neverInvestigated(), List.of(ordinary(0, 5, HMMM)), SHOP).gameAction());
  }

  @Test
  void anOrdinaryAdExpiringWithinTwoTurnsIsUrgentAndDefersTheRefresh() {
    assertEquals(GameAction.SOLVE_BEST_REMAINING_AD,
        choose(neverInvestigated(), List.of(ordinary(0, 2, HMMM))));
    assertEquals(GameAction.REFRESH_REPUTATION,
        choose(neverInvestigated(), List.of(ordinary(0, 3, HMMM))));
  }

  @Test
  void riskyIsTheLowestProbabilityThatCanMakeAnAdUrgent() {
    assertEquals(GameAction.SOLVE_BEST_REMAINING_AD,
        choose(neverInvestigated(), List.of(ordinary(0, 1, RISKY))));
    assertEquals(GameAction.REFRESH_REPUTATION,
        choose(neverInvestigated(), List.of(ordinary(0, 1, PLAYING_WITH_FIRE))));
  }

  @Test
  void suspectedTrapsAndExpiredAdsAreNeverUrgent() {
    var ads = List.of(trap(100, 1, SURE_THING), ordinary(100, 0, SURE_THING), ordinary(100, -1, SURE_THING));
    assertEquals(GameAction.REFRESH_REPUTATION, choose(neverInvestigated(), ads));
  }

  @Test
  void reputationRefreshTakesPriorityOverTheLastLife() {
    assertEquals(GameAction.REFRESH_REPUTATION,
        choose(state(0, 1, 5, null), List.of(ordinary(100, 5, SURE_THING))));
  }

  // --- 2. SOLVE_SAFEST_AD_ON_LAST_LIFE ---

  @Test
  void solvesTheSafestAdOnTheLastLife() {
    var ads = List.of(ordinary(100, 5, SURE_THING), ordinary(10, 1, GAMBLE));
    assertEquals(GameAction.SOLVE_SAFEST_AD_ON_LAST_LIFE, choose(fresh(0, 1), ads));
  }

  @Test
  void considersSuspectedTrapsOnTheLastLife() {
    assertEquals(GameAction.SOLVE_SAFEST_AD_ON_LAST_LIFE, choose(fresh(0, 1), List.of(trap(50, 4, GAMBLE))));
  }

  @Test
  void healsBeforeConsideringExpiredAds() {
    assertEquals(GameAction.BUY_HEALING_POTION, choose(fresh(UPGRADE_GOLD, 1), List.of(ordinary(100, 0, SURE_THING))));
  }

  @Test
  void appliesTheLastLifeRuleOnlyToExactlyOneLife() {
    assertEquals(GameAction.SOLVE_PROFITABLE_AD, choose(fresh(0, 2), List.of(ordinary(100, 5, SURE_THING))));
  }

  // --- 3. SOLVE_PROFITABLE_AD ---

  @Test
  void solvesAnEasyAdWithPositiveExpectedValueBeforeUpgrading() {
    // 0.86 * 20 - 0.14 * 50 = 10.2
    assertEquals(GameAction.SOLVE_PROFITABLE_AD,
        choose(fresh(1_000, LIVES), List.of(ordinary(20, 4, WALK_IN_THE_PARK))));
  }

  @Test
  void anEasyAdWithNegativeExpectedValueIsNotProfitable() {
    // 0.86 * 5 - 0.14 * 50 = -2.7
    assertEquals(GameAction.SOLVE_BEST_REMAINING_AD,
        choose(fresh(0, LIVES), List.of(ordinary(5, 4, WALK_IN_THE_PARK))));
  }

  @Test
  void anExpectedValueOfExactlyZeroIsNotProfitable() {
    var config = config(CONFIG.goldReserve(), Set.of(HMMM), UpgradeMode.GOLD_EFFICIENT, true, 10);
    // 0.5 * 50 - 0.5 * 50 = 0
    assertEquals(GameAction.SOLVE_BEST_REMAINING_AD,
        service.chooseGameAction(config, fresh(0, LIVES), List.of(ordinary(50, 4, HMMM)), SHOP).gameAction());
    assertEquals(GameAction.SOLVE_PROFITABLE_AD,
        service.chooseGameAction(config, fresh(0, LIVES), List.of(ordinary(51, 4, HMMM)), SHOP).gameAction());
  }

  @Test
  void aHighValueAdOutsideTheEasyProbabilitiesIsNotProfitable() {
    assertEquals(GameAction.BUY_UPGRADE, choose(fresh(UPGRADE_GOLD, LIVES), List.of(ordinary(1_000, 4, HMMM))));
  }

  @Test
  void aSuspectedTrapIsNeverAProfitableAd() {
    assertEquals(GameAction.BUY_UPGRADE, choose(fresh(UPGRADE_GOLD, LIVES), List.of(trap(1_000, 4, SURE_THING))));
  }

  // --- 4. BUY_UPGRADE ---

  @Test
  void buysAnUpgradeOnlyWhenTheGoldReserveIsPreserved() {
    var ads = List.of(ordinary(0, 4, SURE_THING));
    assertEquals(GameAction.BUY_UPGRADE, choose(fresh(UPGRADE_GOLD, LIVES), ads));
    assertEquals(GameAction.SOLVE_BEST_REMAINING_AD, choose(fresh(UPGRADE_GOLD - 1, LIVES), ads));
  }

  @Test
  void usesTheConfiguredGoldReserve() {
    var config = config(0, CONFIG.easyAdProbabilities(), UpgradeMode.GOLD_EFFICIENT, true, 10);
    var ads = List.of(ordinary(0, 4, HMMM));
    assertEquals(GameAction.BUY_UPGRADE, service.chooseGameAction(config, fresh(100, LIVES), ads, SHOP).gameAction());
    assertEquals(GameAction.SOLVE_BEST_REMAINING_AD, service.chooseGameAction(config, fresh(99, LIVES), ads, SHOP).gameAction());
  }

  @Test
  void turnEfficientModeUsesTheSameAffordabilityThreshold() {
    var config = config(CONFIG.goldReserve(), CONFIG.easyAdProbabilities(), UpgradeMode.TURN_EFFICIENT, true, 10);
    var ads = List.of(ordinary(0, 4, HMMM));
    assertEquals(GameAction.BUY_UPGRADE, service.chooseGameAction(config, fresh(UPGRADE_GOLD, LIVES), ads, SHOP).gameAction());
    assertEquals(GameAction.SOLVE_BEST_REMAINING_AD,
        service.chooseGameAction(config, fresh(UPGRADE_GOLD - 1, LIVES), ads, SHOP).gameAction());
  }

  @Test
  void buysAnUpgradeEvenWithoutAds() {
    assertEquals(GameAction.BUY_UPGRADE, choose(fresh(UPGRADE_GOLD, LIVES), List.of()));
  }

  @Test
  void doesNotBuyAnUpgradeTwiceInARow() {
    var ads = List.of(ordinary(0, 4, HMMM));
    assertEquals(GameAction.SOLVE_BEST_REMAINING_AD,
        choose(freshAfter(GameAction.BUY_UPGRADE, 1_000, LIVES), ads));
    assertEquals(GameAction.ATTEMPT_SAFEST_TRAP,
        choose(freshAfter(GameAction.BUY_UPGRADE, 1_000, LIVES), List.of(trap(0, 4, HMMM))));
    assertEquals(GameAction.SKIP_TURN_NO_AVAILABLE_ADS,
        choose(freshAfter(GameAction.BUY_UPGRADE, 1_000, LIVES), List.of()));
  }

  @Test
  void buysAnUpgradeAgainAfterAnyOtherAction() {
    for (GameAction lastAction : GameAction.values()) {
      if (lastAction != GameAction.BUY_UPGRADE) {
        assertEquals(GameAction.BUY_UPGRADE, choose(freshAfter(lastAction, UPGRADE_GOLD, LIVES), List.of()),
            lastAction.name());
      }
    }
  }

  // --- 5. SOLVE_BEST_REMAINING_AD ---

  @Test
  void solvesAnUnprofitableOrdinaryAdBeforeATrap() {
    var ads = List.of(trap(1_000, 4, SURE_THING), ordinary(0, 4, IMPOSSIBLE));
    assertEquals(GameAction.SOLVE_BEST_REMAINING_AD, choose(fresh(0, LIVES), ads));
  }

  // --- 6. ATTEMPT_SAFEST_TRAP ---

  @Test
  void attemptsASuspectedTrapOnlyAsTheFinalFallback() {
    var ads = List.of(trap(80, 4, HMMM), ordinary(80, 0, SURE_THING));
    assertEquals(GameAction.ATTEMPT_SAFEST_TRAP, choose(fresh(0, LIVES), ads));
  }

  @Test
  void skipsWhenNoActionApplies() {
    assertEquals(GameAction.SKIP_TURN_NO_AVAILABLE_ADS, choose(fresh(0, LIVES), List.of()));
    assertEquals(new GameActionWithExecutionContext(GameAction.SKIP_TURN_NO_AVAILABLE_ADS, List.of(), null, CONFIG),
        service.chooseGameAction(CONFIG, fresh(0, LIVES), List.of(ordinary(80, 0, SURE_THING), trap(80, -1, SURE_THING)), SHOP));
  }

  @Test
  void executionContextContainsOnlyTheSelectedActionsEligibleUnexpiredAds() {
    Ad expired = ordinary(1_000, 0, SURE_THING);
    Ad expiredTrap = trap(1_000, -1, SURE_THING);
    Ad profitable = ordinary(100, 3, SURE_THING);
    Ad remaining = ordinary(0, 3, HMMM);
    Ad suspectedTrap = trap(1_000, 3, SURE_THING);
    List<Ad> ads = List.of(expired, expiredTrap, profitable, remaining, suspectedTrap);

    assertEquals(new GameActionWithExecutionContext(GameAction.SOLVE_PROFITABLE_AD, List.of(profitable), null, CONFIG),
        service.chooseGameAction(CONFIG, fresh(0, LIVES), ads, SHOP));
    assertEquals(new GameActionWithExecutionContext(GameAction.SOLVE_SAFEST_AD_ON_LAST_LIFE,
        List.of(profitable, remaining), null, CONFIG), service.chooseGameAction(CONFIG, fresh(0, 1), ads, SHOP));
    assertEquals(new GameActionWithExecutionContext(GameAction.SOLVE_SAFEST_AD_ON_LAST_LIFE, List.of(suspectedTrap), null, CONFIG),
        service.chooseGameAction(CONFIG, fresh(0, 1), List.of(expired, expiredTrap, suspectedTrap), SHOP));
    assertEquals(new GameActionWithExecutionContext(GameAction.SOLVE_BEST_REMAINING_AD, List.of(remaining), null, CONFIG),
        service.chooseGameAction(CONFIG, fresh(0, LIVES), List.of(expired, expiredTrap, remaining, suspectedTrap), SHOP));
    assertEquals(new GameActionWithExecutionContext(GameAction.ATTEMPT_SAFEST_TRAP, List.of(suspectedTrap), null, CONFIG),
        service.chooseGameAction(CONFIG, fresh(0, LIVES), List.of(expired, expiredTrap, suspectedTrap), SHOP));
  }

  @Test
  void actionsThatDoNotSolveAdsCarryNoAdCandidates() {
    List<Ad> ads = List.of(ordinary(0, 3, HMMM), trap(1_000, 3, SURE_THING));

    assertEquals(new GameActionWithExecutionContext(GameAction.REFRESH_REPUTATION, List.of(), null, CONFIG),
        service.chooseGameAction(CONFIG, neverInvestigated(), ads, SHOP));
    assertEquals(new GameActionWithExecutionContext(GameAction.BUY_UPGRADE, List.of(), SHOP.getFirst(), CONFIG),
        service.chooseGameAction(CONFIG, fresh(UPGRADE_GOLD, LIVES), ads, SHOP));
  }

  @Test
  void repeatedEmptyBoardTriesPurchasesBeforeRefreshingReputationAndIgnoresTheReserve() {
    var config = config(1_000, CONFIG.easyAdProbabilities(), UpgradeMode.GOLD_EFFICIENT, true, 10);
    var ads = List.of(ordinary(1_000, 0, SURE_THING), trap(1_000, -1, SURE_THING));
    var upgradeState = new GameState("game", 100, LIVES, 0, 100, 0, 0, null, null, 0,
        GameAction.SKIP_TURN_NO_AVAILABLE_ADS);
    var healingState = freshAfter(GameAction.SKIP_TURN_NO_AVAILABLE_ADS, 50, LIVES);

    assertEquals(new GameActionWithExecutionContext(GameAction.BUY_UPGRADE, List.of(), SHOP.get(0), config),
        service.chooseGameAction(config, upgradeState, ads, SHOP));
    assertEquals(new GameActionWithExecutionContext(GameAction.BUY_HEALING_POTION, List.of(), SHOP.get(1), config),
        service.chooseGameAction(config, healingState, ads, SHOP));
    assertEquals(GameAction.BUY_HEALING_POTION,
        choose(freshAfter(GameAction.SKIP_TURN_NO_AVAILABLE_ADS, 99, LIVES), ads));
    assertEquals(new GameActionWithExecutionContext(GameAction.REFRESH_REPUTATION, List.of(), null, config),
        service.chooseGameAction(config, freshAfter(GameAction.SKIP_TURN_NO_AVAILABLE_ADS, 49, LIVES), ads, SHOP));
  }

  @Test
  void aLiveAdAfterSkippingUsesTheNormalSelectionAndReserve() {
    var state = freshAfter(GameAction.SKIP_TURN_NO_AVAILABLE_ADS, 100, LIVES);
    var ad = ordinary(0, 3, HMMM);

    assertEquals(new GameActionWithExecutionContext(GameAction.SOLVE_BEST_REMAINING_AD, List.of(ad), null, CONFIG),
        service.chooseGameAction(CONFIG, state, List.of(ad), SHOP));
  }

  @Test
  void missingHealingPotionFallsThroughToSurvivalWithoutRecordingAPurchase() {
    var state = freshAfter(GameAction.BUY_HEALING_POTION, 1_000, 1);
    var ads = List.of(ordinary(10, 2, GAMBLE));
    var shopItems = List.of(SHOP.getFirst());

    assertFalse(service.chooseCriticalHealing(state, shopItems));
    assertEquals(new GameActionWithExecutionContext(GameAction.SOLVE_SAFEST_AD_ON_LAST_LIFE, ads, null, CONFIG),
        service.chooseGameAction(CONFIG, state, ads, shopItems));
    assertEquals(new GameActionWithExecutionContext(GameAction.BUY_HEALING_POTION, List.of(), SHOP.get(1), CONFIG),
        service.chooseGameAction(CONFIG, state, ads, SHOP));
  }

  @Test
  void criticalHealingUsesTheAvailablePotionPriceBeforeReputationOrAds() {
    var potion = new ShopItem(ShopItemId.HEALING_POTION, "Healing potion", 70);
    var ads = List.of(ordinary(1_000, 2, SURE_THING));
    var state = new GameState("game", 70, 1, 0, 100, 0, 0, null, null, 0, GameAction.BUY_HEALING_POTION);

    assertFalse(service.chooseCriticalHealing(fresh(69, 1), List.of(potion)));
    assertEquals(new GameActionWithExecutionContext(GameAction.BUY_HEALING_POTION, List.of(), potion, CONFIG),
        service.chooseGameAction(CONFIG, state, ads, List.of(potion)));
  }

  @Test
  void upgradeSelectionUsesTheModeAffordabilityAndDeterministicItemOrder() {
    var turnEfficient = config(CONFIG.goldReserve(), CONFIG.easyAdProbabilities(), UpgradeMode.TURN_EFFICIENT, true, 10);
    var ads = List.of(ordinary(0, 3, HMMM));

    var goldEfficientUpgrade = service.chooseGameAction(CONFIG, fresh(10_000, LIVES), ads, FULL_SHOP).shopItem();
    assertNotNull(goldEfficientUpgrade);
    assertEquals(ShopItemId.CLAW_SHARPENING, goldEfficientUpgrade.id());

    var turnEfficientUpgrade = service.chooseGameAction(turnEfficient, fresh(350, LIVES), ads, FULL_SHOP).shopItem();
    assertNotNull(turnEfficientUpgrade);
    assertEquals(ShopItemId.CLAW_HONING, turnEfficientUpgrade.id());

    var affordableUpgrade = service.chooseGameAction(turnEfficient, fresh(349, LIVES), ads, FULL_SHOP).shopItem();
    assertNotNull(affordableUpgrade);
    assertEquals(ShopItemId.CLAW_SHARPENING, affordableUpgrade.id());
  }

  @Test
  void missingOrUnaffordableUpgradesFallThroughToAnAd() {
    var ads = List.of(ordinary(0, 3, HMMM));
    var expected = new GameActionWithExecutionContext(GameAction.SOLVE_BEST_REMAINING_AD, ads, null, CONFIG);

    assertEquals(expected, service.chooseGameAction(CONFIG, fresh(1_000, LIVES), ads, List.of()));
    assertEquals(expected, service.chooseGameAction(CONFIG, fresh(1_000, LIVES), ads, List.of(SHOP.get(1))));
    assertEquals(expected, service.chooseGameAction(CONFIG, fresh(349, LIVES), ads,
        List.of(new ShopItem(ShopItemId.CLAW_HONING, "Claw honing", 300))));
  }

  @Test
  void goldEfficientModeCanChooseAnAvailableTwoLevelUpgrade() {
    var upgrade = new ShopItem(ShopItemId.CLAW_HONING, "Claw honing", 300);

    assertEquals(new GameActionWithExecutionContext(GameAction.BUY_UPGRADE, List.of(), upgrade, CONFIG),
        service.chooseGameAction(CONFIG, fresh(350, LIVES), List.of(ordinary(0, 3, HMMM)), List.of(upgrade)));
  }

  @Test
  void upgradeSelectionUsesTheFetchedPricesAndConfiguredReserve() {
    var config = config(0, CONFIG.easyAdProbabilities(), UpgradeMode.TURN_EFFICIENT, true, 10);
    var upgrade = new ShopItem(ShopItemId.ROCKET_FUEL, "Rocket fuel", 120);

    assertEquals(new GameActionWithExecutionContext(GameAction.BUY_UPGRADE, List.of(), upgrade, config),
        service.chooseGameAction(config, fresh(120, LIVES), List.of(ordinary(0, 3, HMMM)),
            List.of(SHOP.getFirst(), upgrade)));
  }

  @Test
  void repeatedEmptyBoardResolvesTheHealingFallbackBeforeExecution() {
    var ads = List.of(ordinary(1_000, 0, SURE_THING));
    var state = freshAfter(GameAction.SKIP_TURN_NO_AVAILABLE_ADS, 100, LIVES);
    var unaffordableUpgrade = new ShopItem(ShopItemId.CLAW_HONING, "Claw honing", 300);

    assertEquals(new GameActionWithExecutionContext(GameAction.BUY_HEALING_POTION, List.of(), SHOP.get(1), CONFIG),
        service.chooseGameAction(CONFIG, state, ads, List.of(unaffordableUpgrade, SHOP.get(1))));
    assertEquals(new GameActionWithExecutionContext(GameAction.REFRESH_REPUTATION, List.of(), null, CONFIG),
        service.chooseGameAction(CONFIG, state, ads, List.of(unaffordableUpgrade)));
  }

  // --- helpers ---

  @Test
  void detectsSuspectedTrapsByCaseInsensitivePrefix() {
    assertTrue(ChooseGameActionService.isSuspectedTrap(ad("Steal super awesome diamond rings", 0, 1, HMMM)));
    assertTrue(ChooseGameActionService.isSuspectedTrap(ad("STEAL SUPER AWESOME DIAMOND crown", 0, 1, HMMM)));
    assertFalse(ChooseGameActionService.isSuspectedTrap(ad("Steal super awesome diamond", 0, 1, HMMM)));
    assertFalse(ChooseGameActionService.isSuspectedTrap(ad("Please steal super awesome diamond rings", 0, 1, HMMM)));
    assertFalse(ChooseGameActionService.isSuspectedTrap(ad("Help a farmer", 0, 1, HMMM)));
    assertFalse(ChooseGameActionService.isSuspectedTrap(ad(null, 0, 1, HMMM)));
  }

  @Test
  void adsWithoutMessageAreOrdinary() {
    assertEquals(GameAction.SOLVE_PROFITABLE_AD, choose(fresh(0, LIVES), List.of(ad(null, 100, 4, SURE_THING))));
  }

  @Test
  void expectedValueWeighsTheRewardAgainstTheHealingCost() {
    assertEquals(98.5, ChooseGameActionService.expectedValue(ordinary(100, 1, SURE_THING)), 1e-9);
    assertEquals(0.0, ChooseGameActionService.expectedValue(ordinary(50, 1, HMMM)), 1e-9);
    assertEquals(-49.5, ChooseGameActionService.expectedValue(ordinary(0, 1, IMPOSSIBLE)), 1e-9);
  }

  private GameAction choose(GameState state, List<Ad> ads) {
    return service.chooseGameAction(CONFIG, state, ads, SHOP).gameAction();
  }

  private static GameConfig config(long goldReserve, Set<AdProbability> easyAdProbabilities, UpgradeMode upgradeMode,
      boolean reputationCheckEnabled, int reputationMaxAge) {
    return new GameConfig(goldReserve, easyAdProbabilities, upgradeMode, reputationCheckEnabled, reputationMaxAge,
        CONFIG.turnsLimit(), CONFIG.timeLimitMillis(), CONFIG.scoreGoal());
  }

  /** Reputation was investigated on the current turn, so it is not stale. */
  private static GameState fresh(long gold, int lives) {
    return state(gold, lives, INVESTIGATED_AT, INVESTIGATED_AT);
  }

  /** Like {@link #fresh}, right after {@code lastAction} was performed. */
  private static GameState freshAfter(GameAction lastAction, long gold, int lives) {
    return new GameState("game", gold, lives, 0, INVESTIGATED_AT, 0, 0, null, INVESTIGATED_AT, 0, lastAction);
  }

  private static GameState investigatedTurnsAgo(long turns) {
    return state(0, LIVES, INVESTIGATED_AT + turns, INVESTIGATED_AT);
  }

  private static GameState neverInvestigated() {
    return state(0, LIVES, 5, null);
  }

  private static GameState state(long gold, int lives, long turn, Long lastReputationInvestigationTurn) {
    return new GameState("game", gold, lives, 0, turn, 0, 0, null, lastReputationInvestigationTurn, 0, null);
  }

  private static Ad ordinary(long reward, int expiresIn, AdProbability probability) {
    return ad("Help defending the village", reward, expiresIn, probability);
  }

  private static Ad trap(long reward, int expiresIn, AdProbability probability) {
    return ad("Steal super awesome diamond rings", reward, expiresIn, probability);
  }

  private static Ad ad(String message, long reward, int expiresIn, AdProbability probability) {
    return new Ad("ad", message, reward, expiresIn, null, probability);
  }
}
