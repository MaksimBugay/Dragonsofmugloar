package com.bmv.dragonsofmugloar.domain.api.request;

import static com.bmv.dragonsofmugloar.domain.model.AdProbability.SURE_THING;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import com.bmv.dragonsofmugloar.domain.model.GameConfig;
import com.bmv.dragonsofmugloar.domain.model.GameConfig.UpgradeMode;
import com.trimble.transportation.reference.data.management.serialization.json.JsonUtility;
import org.junit.jupiter.api.Test;

class PlayGameRequestTest {

  @Test
  void usesTheDefaultConfigForEveryMissingValue() {
    var request = new PlayGameRequest(null, null, null, null, null, null, null, null);

    assertEquals(new GameConfig(), request.toGameConfig());
  }

  @Test
  void deserializesAnEmptyJsonObjectIntoTheFullyPopulatedDefaultConfig() {
    var request = JsonUtility.fromJson("{}", PlayGameRequest.class);

    assertEquals(new GameConfig(), request.toGameConfig());
  }

  @Test
  void keepsEveryProvidedValueIncludingZeroAndFalse() {
    var request = new PlayGameRequest(0L, Set.of(SURE_THING), UpgradeMode.TURN_EFFICIENT, false, 0, 7, 1_000L, 500L);

    assertEquals(new GameConfig(0, Set.of(SURE_THING), UpgradeMode.TURN_EFFICIENT, false, 0, 7, 1_000, 500),
        request.toGameConfig());
  }

  @Test
  void fillsOnlyTheMissingValues() {
    var request = new PlayGameRequest(200L, null, UpgradeMode.TURN_EFFICIENT, null, 3, null, 30_000L, null);
    var defaults = new GameConfig();

    assertEquals(new GameConfig(200, defaults.easyAdProbabilities(), UpgradeMode.TURN_EFFICIENT,
            defaults.reputationCheckEnabled(), 3, defaults.turnsLimit(), 30_000, defaults.scoreGoal()),
        request.toGameConfig());
  }
}
