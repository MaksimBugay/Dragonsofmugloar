package com.bmv.dragonsofmugloar.domain.api.request;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.bmv.dragonsofmugloar.domain.model.GameConfig;
import com.bmv.dragonsofmugloar.domain.model.GameState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("PlayGameTurnRequest")
public record PlayGameTurnRequest(
        @NotNull @Valid GameState gameState,
        @NotNull @Valid GameConfig gameConfig
) {
}
