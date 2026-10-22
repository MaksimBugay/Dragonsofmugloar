package com.bmv.dragonsofmugloar.domain.model;

/**
 * Reputation with the three factions returned by a reputation investigation.
 */
public record Reputation(
        double people,
        double state,
        double underworld
) {
    public static final Reputation NEUTRAL = new Reputation(0.0, 0.0, 0.0);
}
