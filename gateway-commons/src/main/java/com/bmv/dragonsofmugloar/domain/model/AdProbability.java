package com.bmv.dragonsofmugloar.domain.model;

/**
 * Qualitative success estimates supplied by the API, after decoding.
 * Percentages are fitted live-sample estimates, including trap failures, in the range 1-99.
 * Estimates preserve label ordering through isotonic pooling; they are not API-provided odds.
 * Unobserved labels retain their previous fallback. See AD_PROBABILITY_CALIBRATION.md.
 * UNKNOWN uses a neutral estimate of 50 percent.
 */
public enum AdProbability {
  SURE_THING("Sure thing", 99),
  PIECE_OF_CAKE("Piece of cake", 92),
  WALK_IN_THE_PARK("Walk in the park", 86),
  QUITE_LIKELY("Quite likely", 86),
  HMMM("Hmmm....", 50),
  GAMBLE("Gamble", 46),
  RISKY("Risky", 40),
  PLAYING_WITH_FIRE("Playing with fire", 36),
  RATHER_DETRIMENTAL("Rather detrimental", 21),
  SUICIDE_MISSION("Suicide mission", 11),
  IMPOSSIBLE("Impossible", 1),
  UNKNOWN("Unknown", 50);

  private final String label;
  private final int successPercentage;

  AdProbability(String label, int successPercentage) {
    this.label = label;
    this.successPercentage = successPercentage;
  }

  public String getLabel() {
    return label;
  }

  /**
   * Returns a calibrated label-level success estimate; individual ad outcomes may differ.
   */
  public int getSuccessPercentage() {
    return successPercentage;
  }
}
