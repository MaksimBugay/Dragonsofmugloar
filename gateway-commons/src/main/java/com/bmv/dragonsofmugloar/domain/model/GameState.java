package com.bmv.dragonsofmugloar.domain.model;

import com.trimble.transportation.reference.data.management.util.RdmObjectUtils;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

public record GameState(
        @NotBlank String gameId,
        long gold,
        int lives,
        int level,
        long turn,
        long score,
        long highScore,
        @Nullable Reputation lastInvestigatedReputation,
        @Nullable Long lastReputationInvestigationTurn,
        long startedAt,
        @Nullable GameAction lastAction
) {
    public GameState(String gameId, long gold, int lives, int level, long turn, long score, long highScore) {
        this(
                gameId,
                gold,
                lives,
                level,
                turn,
                score,
                highScore,
                Reputation.NEUTRAL,
                turn,
                RdmObjectUtils.getCurrentTimeMillis(),
                null
        );
    }
}
