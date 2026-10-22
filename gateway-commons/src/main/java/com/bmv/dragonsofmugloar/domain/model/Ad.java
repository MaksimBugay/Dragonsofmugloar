package com.bmv.dragonsofmugloar.domain.model;

import jakarta.annotation.Nullable;

/**
 * An ad with decoded text, reward and probability.
 *
 * @param expiresIn number of game turns before the ad expires
 * @param encrypted raw API encryption marker, or null for an unencrypted ad
 */
public record Ad(
    String adId,
    String message,
    long reward,
    int expiresIn,
    @Nullable Integer encrypted,
    AdProbability probability
) {
    public static final String SUSPECTED_TRAP_PREFIX = "steal super awesome diamond ";
}
