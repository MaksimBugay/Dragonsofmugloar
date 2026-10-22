package com.bmv.dragonsofmugloar.domain.model;

/**
 * Turn strategy steps, declared in priority order: the first applicable action is taken.
 */
public enum GameAction {
  START_GAME,
  STOP_GAME,
  /** Handle critical healing*/
  BUY_HEALING_POTION,
  /** Investigate reputation when it is stale and no ordinary ad is about to expire. */
  REFRESH_REPUTATION,
  /** With one life left, solve the ad with the best survival ranking. */
  SOLVE_SAFEST_AD_ON_LAST_LIFE,
  /** Solve the best good-probability ad with a positive expected value. */
  SOLVE_PROFITABLE_AD,
  /** Buy a level upgrade while keeping the gold reserve. */
  BUY_UPGRADE,
  /** Solve the best remaining ordinary ad, even if its expected value is nonpositive. */
  SOLVE_BEST_REMAINING_AD,
  /** Final fallback: attempt the suspected trap with the best survival ranking. */
  ATTEMPT_SAFEST_TRAP,
  /** there are no unexpired ads. */
  SKIP_TURN_NO_AVAILABLE_ADS
}
