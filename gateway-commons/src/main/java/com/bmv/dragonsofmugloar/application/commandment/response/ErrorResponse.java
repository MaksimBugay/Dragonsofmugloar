package com.bmv.dragonsofmugloar.application.commandment.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response structure")
public record ErrorResponse(
    @Schema(
        description = "URI reference that identifies the problem type. Should be a well-known identifier for the class of problem",
        example = "https://api.example.com/problems/validation-error",
        format = "uri",
        nullable = true
    )
    String type,

    @Schema(
        description = "Short, human-readable summary of the problem type.",
        example = "Validation Error",
        nullable = true
    )
    String title,

    @Schema(
        description = "HTTP status code for this occurrence of the problem",
        example = "400",
        minimum = "100",
        maximum = "599",
        nullable = true
    )
    Integer status,

    @Schema(
        description = "List of nested error details providing specific information about validation failures or sub-errors",
        example = "[{\"type\": \"field-validation\", \"title\": \"Invalid field value\", \"status\": 400}]",
        nullable = true
    )
    List<ErrorResponse> errors
) {
}