package com.bmv.dragonsofmugloar.domain.model;

import java.time.Duration;
import java.util.Comparator;
import java.util.Set;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import static com.bmv.dragonsofmugloar.domain.model.AdProbability.*;
import static com.bmv.dragonsofmugloar.domain.model.ShopItemId.HEALING_COST;

public record GameConfig(
        @PositiveOrZero long goldReserve,
        @NotNull Set<@NotNull AdProbability> easyAdProbabilities,
        @NotNull UpgradeMode upgradeMode,
        boolean reputationCheckEnabled,
        @PositiveOrZero int reputationMaxAge,
        @Positive int turnsLimit,
        @Positive long timeLimitMillis,
        @Positive long scoreGoal
) {

    private static final int DEFAULT_SCORE_GOAL = 1001;
    private static final int DEFAULT_REPUTATION_MAX_AGE = 10;
    private static final long DEFAULT_PLAY_TIME_LIMIT = Duration.ofMinutes(5).toMillis();
    private static final int DEFAULT_TURNS_LIMIT = Integer.MAX_VALUE; //unlimited
    private static final Set<@NotNull AdProbability> DEFAULT_EASY_AD_PROBABILITIES = Set.of(SURE_THING, PIECE_OF_CAKE, WALK_IN_THE_PARK, QUITE_LIKELY);

    public GameConfig(int turnsLimit, long timeLimitMillis) {
        this(
                HEALING_COST,
                DEFAULT_EASY_AD_PROBABILITIES,
                UpgradeMode.GOLD_EFFICIENT,
                true,
                DEFAULT_REPUTATION_MAX_AGE,
                turnsLimit,
                timeLimitMillis,
                DEFAULT_SCORE_GOAL
        );
    }

    public GameConfig() {
        this(
                DEFAULT_TURNS_LIMIT,
                DEFAULT_PLAY_TIME_LIMIT
        );
    }

    /**
     * Which of the affordable level upgrades to buy.
     */
    public enum UpgradeMode {
        /**
         * The cheapest upgrade.
         */
        GOLD_EFFICIENT(Comparator.comparingLong(ShopItem::cost)),
        /**
         * The upgrade with the largest level gain.
         */
        TURN_EFFICIENT(Comparator.comparingInt((ShopItem item) -> item.id().getFeatures().levelDiff()).reversed());

        private final Comparator<ShopItem> preference;

        UpgradeMode(Comparator<ShopItem> preference) {
            this.preference = preference;
        }

        /**
         * Orders upgrades from most to least preferred.
         */
        public Comparator<ShopItem> getPreference() {
            return preference;
        }
    }
}
