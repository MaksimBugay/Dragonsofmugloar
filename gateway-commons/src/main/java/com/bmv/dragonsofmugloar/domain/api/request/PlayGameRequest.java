package com.bmv.dragonsofmugloar.domain.api.request;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.bmv.dragonsofmugloar.domain.model.AdProbability;
import com.bmv.dragonsofmugloar.domain.model.GameConfig;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Set;

import static java.util.Objects.requireNonNullElse;


// A missing type id falls back to this class, so an empty JSON object is a valid request for the default game config.
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type",
        defaultImpl = PlayGameRequest.class)
@JsonTypeName("PlayGameRequest")
public record PlayGameRequest(
        @PositiveOrZero Long goldReserve,
        Set<@NotNull AdProbability> easyAdProbabilities,
        GameConfig.UpgradeMode upgradeMode,
        Boolean reputationCheckEnabled,
        @PositiveOrZero Integer reputationMaxAge,
        @Positive Integer turnsLimit,
        @Positive Long timeLimitMillis,
        @Positive Long scoreGoal
) {
    /**
     * Builds the game configuration, taking each missing value from the default {@link GameConfig}.
     */
    public GameConfig toGameConfig() {
        GameConfig defaults = new GameConfig();
        return new GameConfig(
                requireNonNullElse(goldReserve, defaults.goldReserve()),
                requireNonNullElse(easyAdProbabilities, defaults.easyAdProbabilities()),
                requireNonNullElse(upgradeMode, defaults.upgradeMode()),
                requireNonNullElse(reputationCheckEnabled, defaults.reputationCheckEnabled()),
                requireNonNullElse(reputationMaxAge, defaults.reputationMaxAge()),
                requireNonNullElse(turnsLimit, defaults.turnsLimit()),
                requireNonNullElse(timeLimitMillis, defaults.timeLimitMillis()),
                requireNonNullElse(scoreGoal, defaults.scoreGoal())
        );
    }
}
