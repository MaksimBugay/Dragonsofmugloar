package com.bmv.dragonsofmugloar.domain.api.response;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.bmv.flow.foundry.domain.model.api.error.ErrorDetails;
import com.bmv.flow.foundry.domain.model.api.error.WithErrorDetails;
import com.bmv.dragonsofmugloar.domain.model.Reputation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("PlayGameResponse")
public record PlayGameResponse(
        String gameId,
        long gold,
        int lives,
        int level,
        long turn,
        long score,
        long highScore,
        Reputation reputation,
        long durationMs,
        ErrorDetails errorDetails) implements WithErrorDetails {
}
