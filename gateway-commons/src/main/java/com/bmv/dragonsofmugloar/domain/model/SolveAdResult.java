package com.bmv.dragonsofmugloar.domain.model;

/**
 * Ad outcome and game state after the attempt.
 */
public record SolveAdResult(
    boolean success,
    int lives,
    long gold,
    long score,
    long highScore,
    long turn,
    String message
) {
}
