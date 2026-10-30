package com.bmv.dragonsofmugloar.domain.api.response;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.bmv.flow.foundry.domain.model.api.error.ErrorDetails;
import com.bmv.flow.foundry.domain.model.api.error.WithErrorDetails;
import com.bmv.dragonsofmugloar.domain.model.GameState;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("PlayGameTurnResponse")
public record PlayGameTurnResponse(
        GameState gameState,
        ErrorDetails errorDetails) implements WithErrorDetails {
}
