package com.bmv.dragonsofmugloar.application.commandment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;

import com.bmv.dragonsofmugloar.application.commandment.request.BuildHiddenMessageRequest;
import com.bmv.dragonsofmugloar.application.commandment.response.HiddenMessageResponse;
import com.bmv.flow.foundry.application.core.CommandmentWithDefinition;
import com.bmv.flow.foundry.application.core.GlobalCommandmentDefinitionRegistry;
import com.bmv.flow.foundry.domain.model.api.error.ErrorDetails;
import com.bmv.flow.foundry.domain.model.auth.BmvServiceAuthorization;
import com.bmv.flow.foundry.serialization.json.JsonUtility;
import com.bmv.flow.foundry.util.BmvObjectUtils;
import org.junit.jupiter.api.Test;

class BuildHiddenMessageAiToolTest {

  private static final BuildHiddenMessageRequest REQUEST =
      new BuildHiddenMessageRequest("Работа", "Нейтральный", List.of("Обсудим планы", "Когда?"), List.of("Привет", "Как дела?"));

  @Test
  void roundTripsWithSuccessfulResponse() {
    assertRoundTrips(new HiddenMessageResponse("Завтра мы обсудим планы.", null));
  }

  @Test
  void roundTripsWithErrorResponse() {
    assertRoundTrips(new HiddenMessageResponse(null,
        new ErrorDetails(500, "Hidden-message generation failed", true, Map.of("attempt", "1"))));
  }

  @Test
  void roundTripsWithoutResponse() {
    var tool = new BuildHiddenMessageAiTool(REQUEST);

    var restored = deserializeAsRegisteredCommandment(tool);

    assertEquals(tool.getId(), restored.getId());
    assertEquals(REQUEST, restored.getRequestObject());
    assertNull(restored.getResponseObject());
  }

  @Test
  void restoresRequestWithoutProhibitedMessagesAndWithEmptyHistory() {
    var request = new BuildHiddenMessageRequest("Работа", "Нейтральный", List.of(), null);

    var restored = deserializeAsRegisteredCommandment(new BuildHiddenMessageAiTool(request));

    assertEquals(request, restored.getRequestObject());
  }

  private static void assertRoundTrips(HiddenMessageResponse response) {
    var tool = new BuildHiddenMessageAiTool(
        BmvObjectUtils.generateUUID(), new BmvServiceAuthorization(), REQUEST, response, null);

    var restored = deserializeAsRegisteredCommandment(tool);

    assertEquals(tool.getId(), restored.getId());
    assertEquals(REQUEST, restored.getRequestObject());
    assertEquals(response, restored.getResponseObject());
    assertInstanceOf(BmvServiceAuthorization.class, restored.getAuthorization());
    assertEquals(tool.getAuthorization().getRoles(), restored.getAuthorization().getRoles());
    assertEquals(BuildHiddenMessageAiTool.NAME, restored.getName());
    assertSame(BuildHiddenMessageAiTool.TYPE_DEFINITION, restored.getCommandmentDefinition());
    assertSame(BuildHiddenMessageAiTool.TYPE_DEFINITION,
        GlobalCommandmentDefinitionRegistry.getCommandmentDefinitionByName(BuildHiddenMessageAiTool.NAME));
  }

  // Resolves the concrete class from the serialized commandment name.
  private static BuildHiddenMessageAiTool deserializeAsRegisteredCommandment(BuildHiddenMessageAiTool tool) {
    return assertInstanceOf(BuildHiddenMessageAiTool.class,
        JsonUtility.fromJson(JsonUtility.toJson(tool), CommandmentWithDefinition.class));
  }
}
