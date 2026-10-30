package com.bmv.dragonsofmugloar.application.commandment.request;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("BuildHiddenMessageRequest")
public record BuildHiddenMessageRequest(
        @NotBlank String topic,
        @NotBlank String style,
        @NotNull List<String> history,
        String template
) {
}
