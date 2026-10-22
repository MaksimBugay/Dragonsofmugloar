package com.bmv.dragonsofmugloar.domain.model;

/**
 * Purchase outcome and game state after the transaction.
 */
public record PurchaseResult(
    boolean shoppingSuccess,
    long gold,
    int lives,
    int level,
    long turn
) {
}
