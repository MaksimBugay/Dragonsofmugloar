package com.bmv.dragonsofmugloar.application.commandment.response;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.bmv.flow.foundry.domain.model.api.error.ErrorDetails;
import com.bmv.flow.foundry.domain.model.api.error.WithErrorDetails;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("HiddenMessageResponse")
public record HiddenMessageResponse(
        String text,
        ErrorDetails errorDetails) implements WithErrorDetails {
}
