package com.bmv.gateway.infrastructure.adapter.in.web.basic.error;

import java.util.List;
import java.util.stream.Collectors;

import com.bmv.dragonsofmugloar.application.commandment.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.FieldError;

/**
 * Service responsible for creating standardized ErrorResponse objects.
 */
@Service
public class ErrorResponseService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ErrorResponseService.class);

  private final ErrorUriBuilder errorUriBuilder;

  public ErrorResponseService(ErrorUriBuilder errorUriBuilder) {
    this.errorUriBuilder = errorUriBuilder;
  }

  /**
   * Creates an ErrorResponse for validation errors from field errors
   */
  public ErrorResponse createValidationErrorResponse(List<FieldError> fieldErrors) {
    LOGGER.debug("Creating validation error response for {} field errors", fieldErrors.size());

    List<ErrorResponse> errors = fieldErrors.stream()
        .map(this::createFieldValidationError)
        .collect(Collectors.toList());

    return new ErrorResponse(
        errorUriBuilder.buildValidationErrorUri(),
        "Request validation failed",
        HttpStatus.BAD_REQUEST.value(),
        errors
    );
  }

  /**
   * Creates an ErrorResponse for constraint violations
   */
  public ErrorResponse createConstraintViolationErrorResponse(List<ConstraintViolation<?>> violations) {
    LOGGER.debug("Creating constraint violation error response for {} violations", violations.size());

    List<ErrorResponse> errors = violations.stream()
        .map(this::createConstraintViolationError)
        .collect(Collectors.toList());

    return new ErrorResponse(
        errorUriBuilder.buildConstraintViolationUri(),
        "Request parameter validation failed",
        HttpStatus.BAD_REQUEST.value(),
        errors
    );
  }

  /**
   * Creates an ErrorResponse for input parsing errors
   */
  public ErrorResponse createInputErrorResponse(String reason) {
    LOGGER.debug("Creating input error response: {}", reason);

    return new ErrorResponse(
        errorUriBuilder.buildInputErrorUri(),
        "Invalid request format: " + reason,
        HttpStatus.BAD_REQUEST.value(),
        null
    );
  }

  /**
   * Creates an ErrorResponse for HTTP status exceptions
   */
  public ErrorResponse createHttpStatusErrorResponse(HttpStatus status, String reason) {
    LOGGER.debug("Creating HTTP status error response: {} - {}", status.value(), reason);

    return new ErrorResponse(
        errorUriBuilder.buildHttpStatusUri(status.value()),
        reason != null ? reason : "Request failed",
        status.value(),
        null
    );
  }

  /**
   * Creates an ErrorResponse for custom validation errors
   */
  public ErrorResponse createCustomValidationErrorResponse(String message) {
    LOGGER.debug("Creating custom validation error response: {}", message);

    return new ErrorResponse(
        errorUriBuilder.buildInvalidArgumentUri(),
        message,
        HttpStatus.BAD_REQUEST.value(),
        null
    );
  }

  /**
   * Creates an ErrorResponse for BMV errors
   */
  public ErrorResponse createBmvErrorResponse(String errorCode, String message, HttpStatus status) {
    LOGGER.debug("Creating BMV error response: {} - {}", errorCode, message);

    return new ErrorResponse(
        errorUriBuilder.buildBmvErrorUri(errorCode),
        message,
        status.value(),
        null
    );
  }

  /**
   * Creates an ErrorResponse for unexpected system errors
   */
  public ErrorResponse createInternalErrorResponse() {
    LOGGER.error("Creating internal error response for unexpected system error");

    return new ErrorResponse(
        errorUriBuilder.buildInternalErrorUri(),
        "An unexpected error occurred",
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        null
    );
  }

  /**
   * Creates a field validation error for individual field failures
   */
  private ErrorResponse createFieldValidationError(FieldError fieldError) {
    return new ErrorResponse(
        errorUriBuilder.buildFieldValidationUri(),
        String.format("Field '%s': %s", fieldError.getField(), fieldError.getDefaultMessage()),
        HttpStatus.BAD_REQUEST.value(),
        null
    );
  }

  /**
   * Creates a constraint violation error for individual constraint failures
   */
  private ErrorResponse createConstraintViolationError(ConstraintViolation<?> violation) {
    return new ErrorResponse(
        errorUriBuilder.buildConstraintValidationUri(),
        String.format("Parameter '%s': %s", violation.getPropertyPath(), violation.getMessage()),
        HttpStatus.BAD_REQUEST.value(),
        null
    );
  }
} 