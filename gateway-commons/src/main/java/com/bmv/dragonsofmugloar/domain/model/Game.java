package com.bmv.dragonsofmugloar.domain.model;

/**
 * Initial game state returned when a game is started.
 */
public record Game(
    String gameId,
    int lives,
    long gold,
    int level,
    long score,
    long highScore,
    long turn
) {
}
