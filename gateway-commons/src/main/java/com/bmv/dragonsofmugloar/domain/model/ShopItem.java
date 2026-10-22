package com.bmv.dragonsofmugloar.domain.model;

/**
 * An item available for purchase, with its cost in gold.
 */
public record ShopItem(
    ShopItemId id,
    String name,
    long cost
) {
}
