package com.bmv.gateway.infrastructure.adapter.in.web.consumption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import com.bmv.flow.foundry.domain.model.api.error.ErrorDetails;
import com.bmv.dragonsofmugloar.application.commandment.response.ErrorResponse;
import com.bmv.gateway.infrastructure.adapter.in.web.basic.error.ErrorResponseService;
import com.bmv.gateway.infrastructure.adapter.in.web.consumption.exception.BmvExceptionWithDetails;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @Mock
  private ErrorResponseService errorResponseService;

  private GlobalExceptionHandler globalExceptionHandler;

  @BeforeEach
  void setUp() {
    globalExceptionHandler = new GlobalExceptionHandler(errorResponseService);
  }

  @Test
  void handleValidationException_ShouldReturnErrorResponseWithFieldErrors() {
    // Given
    Object target = new Object();
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "testObject");
    bindingResult.addError(new FieldError("testObject", "name", "must not be blank"));
    bindingResult.addError(new FieldError("testObject", "description", "must not be null"));

    @SuppressWarnings("DataFlowIssue") WebExchangeBindException exception = new WebExchangeBindException(null, bindingResult);

    ErrorResponse mockErrorResponse = new ErrorResponse(
        "http://test.example.com/bmv/errors#validation-error",
        "Request validation failed",
        HttpStatus.BAD_REQUEST.value(),
        null
    );

    when(errorResponseService.createValidationErrorResponse(anyList())).thenReturn(mockErrorResponse);

    // When
    Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleValidationException(exception);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
          ErrorResponse errorResponse = response.getBody();
          assertNotNull(errorResponse);
          assertEquals("http://test.example.com/bmv/errors#validation-error", errorResponse.type());
          assertEquals("Request validation failed", errorResponse.title());
          assertEquals(400, errorResponse.status());
        })
        .verifyComplete();
  }

  @Test
  void handleConstraintViolationException_ShouldReturnErrorResponseWithConstraintErrors() {
    // Given
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path propertyPath = mock(Path.class);
    when(violation.getPropertyPath()).thenReturn(propertyPath);
    when(propertyPath.toString()).thenReturn("columnName");
    when(violation.getMessage()).thenReturn("Duplicate column names are not allowed");

    ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

    ErrorResponse mockErrorResponse = new ErrorResponse(
        "http://test.example.com/bmv/errors#constraint-violation",
        "Request parameter validation failed",
        400,
        null
    );

    when(errorResponseService.createConstraintViolationErrorResponse(anyList())).thenReturn(mockErrorResponse);

    // When
    Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleConstraintViolationException(exception);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
          ErrorResponse errorResponse = response.getBody();
          assertNotNull(errorResponse);
          assertEquals("http://test.example.com/bmv/errors#constraint-violation", errorResponse.type());
          assertEquals("Request parameter validation failed", errorResponse.title());
          assertEquals(400, errorResponse.status());
        })
        .verifyComplete();
  }

  @Test
  void handleServerWebInputException_ShouldReturnErrorResponseForInputErrors() {
    // Given
    ServerWebInputException exception = new ServerWebInputException("Invalid JSON format");

    ErrorResponse mockErrorResponse = new ErrorResponse(
        "http://test.example.com/bmv/errors#input-error",
        "Invalid request format: Invalid JSON format",
        400,
        null
    );

    when(errorResponseService.createInputErrorResponse(anyString())).thenReturn(mockErrorResponse);

    // When
    Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleServerWebInputException(exception);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
          ErrorResponse errorResponse = response.getBody();
          assertNotNull(errorResponse);
          assertEquals("http://test.example.com/bmv/errors#input-error", errorResponse.type());
          assertEquals("Invalid request format: Invalid JSON format", errorResponse.title());
          assertEquals(400, errorResponse.status());
        })
        .verifyComplete();
  }

  @Test
  void handleResponseStatusException_ShouldReturnErrorResponseWithCorrectStatus() {
    // Given
    ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");

    ErrorResponse mockErrorResponse = new ErrorResponse(
        "http://test.example.com/bmv/errors#http-404",
        "Resource not found",
        404,
        null
    );

    when(errorResponseService.createHttpStatusErrorResponse(any(HttpStatus.class), anyString())).thenReturn(mockErrorResponse);

    // When
    Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleResponseStatusException(exception);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
          ErrorResponse errorResponse = response.getBody();
          assertNotNull(errorResponse);
          assertEquals("http://test.example.com/bmv/errors#http-404", errorResponse.type());
          assertEquals("Resource not found", errorResponse.title());
          assertEquals(404, errorResponse.status());
        })
        .verifyComplete();
  }

  @Test
  void handleIllegalArgumentException_ShouldReturnErrorResponseForCustomValidation() {
    // Given
    IllegalArgumentException exception = new IllegalArgumentException("Duplicate column names are not allowed");

    ErrorResponse mockErrorResponse = new ErrorResponse(
        "http://test.example.com/bmv/errors#invalid-argument",
        "Duplicate column names are not allowed",
        400,
        null
    );

    when(errorResponseService.createCustomValidationErrorResponse(anyString())).thenReturn(mockErrorResponse);

    // When
    Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleIllegalArgumentException(exception);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
          ErrorResponse errorResponse = response.getBody();
          assertNotNull(errorResponse);
          assertEquals("http://test.example.com/bmv/errors#invalid-argument", errorResponse.type());
          assertEquals("Duplicate column names are not allowed", errorResponse.title());
          assertEquals(400, errorResponse.status());
        })
        .verifyComplete();
  }

  @Test
  void handleBmvException_ShouldReturnErrorResponseForBmvErrors() {
    // Given
    ErrorDetails errorDetails = new ErrorDetails(1001, "Reference table not found", false, null);
    BmvExceptionWithDetails exception = new BmvExceptionWithDetails(errorDetails);

    ErrorResponse mockErrorResponse = new ErrorResponse(
        "http://test.example.com/bmv/errors#bmv-unknown",
        "Reference table not found",
        500,
        null
    );

    when(errorResponseService.createBmvErrorResponse(anyString(), anyString(), any(HttpStatus.class))).thenReturn(mockErrorResponse);

    // When
    Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleBmvException(exception);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          ErrorResponse errorResponse = response.getBody();
          assertNotNull(errorResponse);
          assertTrue(errorResponse.type().startsWith("http://test.example.com/bmv/errors#bmv-"));
          assertEquals("Reference table not found", errorResponse.title());
          assertTrue(errorResponse.status() >= 400);
        })
        .verifyComplete();
  }

  @Test
  void handleGenericException_ShouldReturnErrorResponseForUnexpectedErrors() {
    // Given
    RuntimeException exception = new RuntimeException("Unexpected error");

    ErrorResponse mockErrorResponse = new ErrorResponse(
        "http://test.example.com/bmv/errors#internal-error",
        "An unexpected error occurred",
        500,
        null
    );

    when(errorResponseService.createInternalErrorResponse()).thenReturn(mockErrorResponse);

    // When
    Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleGenericException(exception);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
          ErrorResponse errorResponse = response.getBody();
          assertNotNull(errorResponse);
          assertEquals("http://test.example.com/bmv/errors#internal-error", errorResponse.type());
          assertEquals("An unexpected error occurred", errorResponse.title());
          assertEquals(500, errorResponse.status());
        })
        .verifyComplete();
  }

  @Test
  void errorResponseFormat_ShouldMatchApiStandards() {
    // Given
    IllegalArgumentException exception = new IllegalArgumentException("Test validation error");

    ErrorResponse mockErrorResponse = new ErrorResponse(
        "http://test.example.com/bmv/errors#invalid-argument",
        "Test validation error",
        400,
        null
    );

    when(errorResponseService.createCustomValidationErrorResponse(anyString())).thenReturn(mockErrorResponse);

    // When
    Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleIllegalArgumentException(exception);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          ErrorResponse errorResponse = response.getBody();
          assertNotNull(errorResponse);

          assertNotNull(errorResponse.type(), "type field is required");
          assertTrue(errorResponse.type().startsWith("http://test.example.com/bmv/errors"), "type should be a URI reference");
          assertNotNull(errorResponse.title(), "title field is required");
          assertNotNull(errorResponse.status(), "status field is required");
          // errors field is optional and can be null
        })
        .verifyComplete();
  }
} 