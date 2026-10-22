package com.bmv.dragonsofmugloar.application.service;

import com.bmv.dragonsofmugloar.domain.model.Ad;
import com.bmv.dragonsofmugloar.domain.model.AdProbability;
import com.bmv.dragonsofmugloar.domain.model.GameConfig;
import com.bmv.dragonsofmugloar.domain.model.GameState;
import com.bmv.dragonsofmugloar.domain.model.ShopItem;
import com.bmv.dragonsofmugloar.domain.model.GameAction;
import com.bmv.dragonsofmugloar.domain.model.GameActionWithExecutionContext;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.bmv.dragonsofmugloar.domain.model.Ad.SUSPECTED_TRAP_PREFIX;
import static com.bmv.dragonsofmugloar.domain.model.ShopItemId.HEALING_COST;

public class ChooseGameActionService {

    static final int URGENT_EXPIRES_IN = 3;

    /** Returns whether an affordable healing potion is available when only one life remains. */
    public boolean chooseCriticalHealing(GameState gameState, List<ShopItem> shopItems) {
        return gameState.lives() == 1 && selectHealingPotion(gameState, shopItems) != null;
    }

    public GameActionWithExecutionContext chooseCriticalHealingAction(GameConfig gameConfig, GameState gameState,
                                                                      List<ShopItem> shopItems) {
        if (gameState.lives() != 1) {
            return null;
        }
        ShopItem healingPotion = selectHealingPotion(gameState, shopItems);
        return healingPotion == null ? null
                : new GameActionWithExecutionContext(GameAction.BUY_HEALING_POTION, List.of(), healingPotion, gameConfig);
    }

    /**
     * Returns the first applicable action with its eligible, unexpired ad candidates or one selected shop item.
     * All shop availability, affordability, and item preferences are resolved here before execution.
     * If no action applies, skips once or returns IN_STUCK after a previous skip.
     */
    public GameActionWithExecutionContext chooseGameAction(GameConfig gameConfig, GameState gameState,
                                                          @NotEmpty List<@NotNull Ad> ads, List<ShopItem> shopItems) {
        GameActionWithExecutionContext criticalHealing = chooseCriticalHealingAction(gameConfig, gameState, shopItems);
        if (criticalHealing != null) {
            return criticalHealing;
        }

        List<Ad> unexpiredAds = ads.stream().filter(ad -> ad.expiresIn() > 0).toList();
        List<Ad> ordinaryAds = unexpiredAds.stream().filter(ad -> !isSuspectedTrap(ad)).toList();

        if (gameState.lastAction() == GameAction.SKIP_TURN_NO_AVAILABLE_ADS && unexpiredAds.isEmpty()) {
            ShopItem upgrade = selectUpgrade(gameConfig, gameState, shopItems, 0);
            if (upgrade != null) {
                return new GameActionWithExecutionContext(GameAction.BUY_UPGRADE, List.of(), upgrade, gameConfig);
            }
            ShopItem healingPotion = selectHealingPotion(gameState, shopItems);
            if (healingPotion != null) {
                return new GameActionWithExecutionContext(GameAction.BUY_HEALING_POTION, List.of(), healingPotion, gameConfig);
            }
        }

        if (gameConfig.reputationCheckEnabled()
                && isReputationStale(gameConfig, gameState)
                && ordinaryAds.stream().noneMatch(ChooseGameActionService::isUrgent)) {
            return new GameActionWithExecutionContext(GameAction.REFRESH_REPUTATION, List.of(), null, gameConfig);
        }

        if (gameState.lives() == 1 && !unexpiredAds.isEmpty()) {
            return new GameActionWithExecutionContext(GameAction.SOLVE_SAFEST_AD_ON_LAST_LIFE,
                    ordinaryAds.isEmpty() ? unexpiredAds : ordinaryAds, null, gameConfig);
        }

        List<Ad> profitableAds = ordinaryAds.stream().filter(ad -> isProfitable(gameConfig, ad)).toList();
        if (!profitableAds.isEmpty()) {
            return new GameActionWithExecutionContext(GameAction.SOLVE_PROFITABLE_AD, profitableAds, null, gameConfig);
        }

        if (gameState.lastAction() != GameAction.BUY_UPGRADE) {
            ShopItem upgrade = selectUpgrade(gameConfig, gameState, shopItems, gameConfig.goldReserve());
            if (upgrade != null) {
                return new GameActionWithExecutionContext(GameAction.BUY_UPGRADE, List.of(), upgrade, gameConfig);
            }
        }

        if (!ordinaryAds.isEmpty()) {
            return new GameActionWithExecutionContext(GameAction.SOLVE_BEST_REMAINING_AD, ordinaryAds, null, gameConfig);
        }

        if (!unexpiredAds.isEmpty()) {
            return new GameActionWithExecutionContext(GameAction.ATTEMPT_SAFEST_TRAP, unexpiredAds, null, gameConfig);
        }

        if (gameState.lastAction() == GameAction.SKIP_TURN_NO_AVAILABLE_ADS) {
            return new GameActionWithExecutionContext(GameAction.REFRESH_REPUTATION, List.of(), null, gameConfig);
        }

        return new GameActionWithExecutionContext(GameAction.SKIP_TURN_NO_AVAILABLE_ADS, List.of(), null, gameConfig);
    }

    private static ShopItem selectHealingPotion(GameState gameState, List<ShopItem> shopItems) {
        return shopItems.stream()
                .filter(item -> item.id().getFeatures().livesDiff() > 0)
                .filter(item -> item.cost() <= gameState.gold())
                .min(Comparator.comparingLong(ShopItem::cost).thenComparing(ShopItem::id))
                .orElse(null);
    }

    private static ShopItem selectUpgrade(GameConfig gameConfig, GameState gameState, List<ShopItem> shopItems,
                                          long goldReserve) {
        return shopItems.stream()
                .filter(item -> item.id().getFeatures().levelDiff() > 0)
                .filter(item -> gameState.gold() - item.cost() >= goldReserve)
                .min(gameConfig.upgradeMode().getPreference().thenComparing(ShopItem::id))
                .orElse(null);
    }

    static boolean isProfitable(GameConfig gameConfig, Ad ad) {
        return gameConfig.easyAdProbabilities().contains(ad.probability()) && expectedValue(ad) > 0;
    }

    static boolean isSuspectedTrap(Ad ad) {
        return ad.message() != null
                && ad.message().toLowerCase(Locale.ROOT).startsWith(SUSPECTED_TRAP_PREFIX);
    }

    static double expectedValue(Ad ad) {
        double p = ad.probability().getSuccessPercentage() / 100.0;
        return p * ad.reward() - (1 - p) * HEALING_COST;
    }

    private static boolean isReputationStale(GameConfig gameConfig, GameState gameState) {
        Long lastInvestigationTurn = gameState.lastReputationInvestigationTurn();
        return lastInvestigationTurn == null
                || gameState.turn() - lastInvestigationTurn > gameConfig.reputationMaxAge();
    }

    private static boolean isUrgent(Ad ad) {
        return ad.probability().getSuccessPercentage() >= AdProbability.RISKY.getSuccessPercentage()
                && ad.expiresIn() < URGENT_EXPIRES_IN;
    }
}
