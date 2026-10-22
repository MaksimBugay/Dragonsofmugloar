package com.bmv.dragonsofmugloar.domain.model;

/**
 * State changes observed for successful purchases in shopping.txt.
 * Each difference is the returned value minus the value before the purchase.
 */
public record ShopItemFeatures(
    long goldDiff,
    int livesDiff,
    int levelDiff
) {
}
