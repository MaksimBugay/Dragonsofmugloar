package com.bmv.dragonsofmugloar.domain.model;

/**
 * Shop item identifiers and their observed purchase effects.
 */
public enum ShopItemId {
    HEALING_POTION("hpot", new ShopItemFeatures(-50, 1, 0)),
    CLAW_SHARPENING("cs", new ShopItemFeatures(-100, 0, 1)),
    GASOLINE("gas", new ShopItemFeatures(-100, 0, 1)),
    COPPER_PLATING("wax", new ShopItemFeatures(-100, 0, 1)),
    BOOK_OF_TRICKS("tricks", new ShopItemFeatures(-100, 0, 1)),
    POTION_OF_STRONGER_WINGS("wingpot", new ShopItemFeatures(-100, 0, 1)),
    CLAW_HONING("ch", new ShopItemFeatures(-300, 0, 2)),
    ROCKET_FUEL("rf", new ShopItemFeatures(-300, 0, 2)),
    IRON_PLATING("iron", new ShopItemFeatures(-300, 0, 2)),
    BOOK_OF_MEGATRICKS("mtrix", new ShopItemFeatures(-300, 0, 2)),
    POTION_OF_AWESOME_WINGS("wingpotmax", new ShopItemFeatures(-300, 0, 2));

    private final String id;
    private final ShopItemFeatures features;

    ShopItemId(String id, ShopItemFeatures features) {
        this.id = id;
        this.features = features;
    }

    public String getId() {
        return id;
    }

    public ShopItemFeatures getFeatures() {
        return features;
    }

    public static final long HEALING_COST = -HEALING_POTION.getFeatures().goldDiff();

}
