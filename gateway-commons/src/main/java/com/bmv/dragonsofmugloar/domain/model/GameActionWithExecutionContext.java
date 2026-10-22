package com.bmv.dragonsofmugloar.domain.model;

import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Selected action, its eligible unexpired ad candidates, selected shop item, and selection configuration.
 * Purchase actions require one non-null item already chosen for availability and affordability.
 * Other actions carry no shop item, and actions that do not solve ads carry an empty ad list.
 */
public record GameActionWithExecutionContext(GameAction gameAction, List<Ad> ads, @Nullable ShopItem shopItem,
                                           GameConfig gameConfig) {

    public GameActionWithExecutionContext {
        if (gameAction == GameAction.BUY_UPGRADE || gameAction == GameAction.BUY_HEALING_POTION) {
            Objects.requireNonNull(shopItem, "Purchase actions require a selected shop item");
        }
    }
}
