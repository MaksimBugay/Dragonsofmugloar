package com.bmv.gateway.infrastructure.adapter.in.web.error;

import java.util.ArrayList;

import com.bmv.flow.foundry.domain.model.api.error.BmvError;
import com.bmv.dragonsofmugloar.application.commandment.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final ErrorResponseService errorResponseService;

  public GlobalExceptionHandler(ErrorResponseService errorResponseService) {
    this.errorResponseService = errorResponseService;
  }

  /**
   * Resolve HTTP status for BMV errors (existing logic preserved)
   */
  public static HttpStatus resolveHttpStatus(BmvError error) {
    return switch (error) {
      case COMMANDMENT_NOT_FOUND, REF_RECORD_NOT_EXISTS, REF_TABLE_NOT_FOUND, USER_NOT_FOUND, DOMAIN_ENTITY_NOT_FOUND ->
          HttpStatus.NOT_FOUND;

      case COMMANDMENT_EXECUTION_ERROR, NON_RECOVERABLE_PERSISTENCE_ERROR, RECOVERABLE_PERSISTENCE_ERROR,
           FAILED_ITEMS_PROCESSING_WITH_ERROR_DICTIONARY, NON_RECOVERABLE_EXTERNAL_API_ERROR, RECOVERABLE_EXTERNAL_API_ERROR ->
          HttpStatus.INTERNAL_SERVER_ERROR;

      case REF_RECORD_COMPOSED_VALIDATION_ERROR, REF_RECORD_VALIDATION_ERROR, REF_RECORD_COLUMN_VALIDATION_ERROR, AMBIGUOUS_REQUEST,
           BAD_REQUEST, REF_TABLE_ALREADY_EXISTS, REF_TABLE_VALIDATION_ERROR, DOMAIN_ENTITY_ALREADY_EXISTS,
           REF_TABLE_REVISION_IN_ILLEGAL_SRA_STATE, REF_RECORD_DELETION_OR_DEPRECATION_VALIDATION_ERROR,
           NON_UNIQUE_REF_RECORD_ID_VALIDATION_ERROR, NON_UNIQUE_REF_RECORD_PRIMARY_KEY_VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;

      case BMV_SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;

      case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;

      case FORBIDDEN -> HttpStatus.FORBIDDEN;

      case SERVER_OVERLOADED -> HttpStatus.TOO_MANY_REQUESTS;

      case COMMANDMENT_EXECUTION_TIMEOUT -> HttpStatus.REQUEST_TIMEOUT;

      case FUTURE_RESPONSE_IS_NOT_READY -> HttpStatus.TOO_EARLY;
    };
  }

  /**
   * Handle Bean Validation errors (from @Valid annotations on request bodies)
   */
  @ExceptionHandler(WebExchangeBindException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleValidationException(WebExchangeBindException ex) {
    LOGGER.warn("Validation error: {} field errors", ex.getBindingResult().getFieldErrorCount());

    ErrorResponse errorResponse = errorResponseService.createValidationErrorResponse(
        ex.getBindingResult().getFieldErrors()
    );

    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
  }

  /**
   * Handle method parameter validation errors (from @Valid on method parameters)
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleConstraintViolationException(ConstraintViolationException ex) {
    LOGGER.warn("Constraint violation error: {} violations", ex.getConstraintViolations().size());

    ErrorResponse errorResponse = errorResponseService.createConstraintViolationErrorResponse(
        new ArrayList<>(ex.getConstraintViolations())
    );

    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
  }

  /**
   * Handle request parsing errors (malformed JSON, missing request body, etc.)
   */
  @ExceptionHandler(ServerWebInputException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleServerWebInputException(ServerWebInputException ex) {
    LOGGER.warn("Server input error: {}", ex.getReason());

    ErrorResponse errorResponse = errorResponseService.createInputErrorResponse(ex.getReason());

    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
  }

  /**
   * Handle ResponseStatusException (generic HTTP status exceptions)
   */
  @ExceptionHandler(ResponseStatusException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleResponseStatusException(ResponseStatusException ex) {
    LOGGER.warn("HTTP status exception: {} - {}", ex.getStatusCode(), ex.getReason());

    ErrorResponse errorResponse = errorResponseService.createHttpStatusErrorResponse(
        HttpStatus.valueOf(ex.getStatusCode().value()),
        ex.getReason()
    );

    return Mono.just(ResponseEntity.status(ex.getStatusCode()).body(errorResponse));
  }

  /**
   * Handle custom IllegalArgumentException (validation errors)
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException ex) {
    LOGGER.warn("Invalid argument: {}", ex.getMessage());

    ErrorResponse errorResponse = errorResponseService.createCustomValidationErrorResponse(ex.getMessage());

    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
  }

  /**
   * Handle BMV exceptions
   */
  @ExceptionHandler(BmvExceptionWithDetails.class)
  public Mono<ResponseEntity<ErrorResponse>> handleBmvException(BmvExceptionWithDetails ex) {
    BmvError error = BmvError.fromCode(ex.getErrorDetails().code());
    HttpStatus status = error != null ? resolveHttpStatus(error) : HttpStatus.INTERNAL_SERVER_ERROR;

    LOGGER.warn("BMV error: {} - {}", error != null ? error.name() : "UNKNOWN", ex.getErrorDetails().message());

    ErrorResponse errorResponse = errorResponseService.createBmvErrorResponse(
        error != null ? error.name() : "unknown",
        ex.getErrorDetails().message(),
        status
    );

    return Mono.just(ResponseEntity.status(status).body(errorResponse));
  }

  /**
   * Handle all other unexpected exceptions
   */
  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
    LOGGER.error("Unexpected system error", ex);

    ErrorResponse errorResponse = errorResponseService.createInternalErrorResponse();

    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
  }
}
