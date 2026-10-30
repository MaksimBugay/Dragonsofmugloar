package com.bmv.gateway.infrastructure.adapter.in.web.basic.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.bmv.dragonsofmugloar.application.commandment.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;

@ExtendWith(MockitoExtension.class)
class ErrorResponseServiceTest {

  @Mock
  private ErrorUriBuilder errorUriBuilder;

  private ErrorResponseService errorResponseService;

  @BeforeEach
  void setUp() {
    errorResponseService = new ErrorResponseService(errorUriBuilder);
  }

  @Test
  void createValidationErrorResponse_ShouldReturnConsistentErrorFormat() {
    // Given
    when(errorUriBuilder.buildValidationErrorUri()).thenReturn("http://test.example.com/rdm/errors#validation-error");
    when(errorUriBuilder.buildFieldValidationUri()).thenReturn("http://test.example.com/rdm/errors#field-validation");

    FieldError fieldError1 = new FieldError("testObject", "name", "must not be blank");
    FieldError fieldError2 = new FieldError("testObject", "description", "must not be null");
    List<FieldError> fieldErrors = List.of(fieldError1, fieldError2);

    // When
    ErrorResponse response = errorResponseService.createValidationErrorResponse(fieldErrors);

    // Then
    assertValidErrorResponse(response, HttpStatus.BAD_REQUEST, "validation-error");
    assertEquals("Request validation failed", response.title());
    assertNotNull(response.errors());
    assertEquals(2, response.errors().size());

    // Verify field errors are properly formatted
    assertTrue(response.errors().stream()
        .anyMatch(err -> err.title().contains("Field 'name': must not be blank")));
    assertTrue(response.errors().stream()
        .anyMatch(err -> err.title().contains("Field 'description': must not be null")));
  }

  @Test
  void createConstraintViolationErrorResponse_ShouldReturnConsistentErrorFormat() {
    // Given
    when(errorUriBuilder.buildConstraintViolationUri()).thenReturn("http://test.example.com/rdm/errors#constraint-violation");
    when(errorUriBuilder.buildConstraintValidationUri()).thenReturn("http://test.example.com/rdm/errors#constraint-validation");

    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path propertyPath = mock(Path.class);
    when(violation.getPropertyPath()).thenReturn(propertyPath);
    when(propertyPath.toString()).thenReturn("columnsMetadata");
    when(violation.getMessage()).thenReturn("Duplicate column names are not allowed");

    List<ConstraintViolation<?>> violations = List.of(violation);

    // When
    ErrorResponse response = errorResponseService.createConstraintViolationErrorResponse(violations);

    // Then
    assertValidErrorResponse(response, HttpStatus.BAD_REQUEST, "constraint-violation");
    assertEquals("Request parameter validation failed", response.title());
    assertNotNull(response.errors());
    assertEquals(1, response.errors().size());
    assertTrue(response.errors().getFirst().title().contains("Parameter 'columnsMetadata': Duplicate column names are not allowed"));
  }

  @Test
  void createInputErrorResponse_ShouldReturnConsistentErrorFormat() {
    // Given
    when(errorUriBuilder.buildInputErrorUri()).thenReturn("http://test.example.com/rdm/errors#input-error");
    String reason = "Invalid JSON format";

    // When
    ErrorResponse response = errorResponseService.createInputErrorResponse(reason);

    // Then
    assertValidErrorResponse(response, HttpStatus.BAD_REQUEST, "input-error");
    assertEquals("Invalid request format: Invalid JSON format", response.title());
  }

  @Test
  void createHttpStatusErrorResponse_ShouldReturnConsistentErrorFormat() {
    // Given
    when(errorUriBuilder.buildHttpStatusUri(404)).thenReturn("http://test.example.com/rdm/errors#http-404");
    HttpStatus status = HttpStatus.NOT_FOUND;
    String reason = "Resource not found";

    // When
    ErrorResponse response = errorResponseService.createHttpStatusErrorResponse(status, reason);

    // Then
    assertValidErrorResponse(response, status, "http-404");
    assertEquals("Resource not found", response.title());
  }

  @Test
  void createCustomValidationErrorResponse_ShouldReturnConsistentErrorFormat() {
    // Given
    when(errorUriBuilder.buildInvalidArgumentUri()).thenReturn("http://test.example.com/rdm/errors#invalid-argument");
    String message = "Duplicate column names are not allowed";

    // When
    ErrorResponse response = errorResponseService.createCustomValidationErrorResponse(message);

    // Then
    assertValidErrorResponse(response, HttpStatus.BAD_REQUEST, "invalid-argument");
    assertEquals(message, response.title());
  }

  @Test
  void createRdmErrorResponse_ShouldReturnConsistentErrorFormat() {
    // Given
    when(errorUriBuilder.buildRdmErrorUri("REF_TABLE_NOT_FOUND")).thenReturn(
        "http://test.example.com/rdm/errors#rdm-ref_table_not_found");
    String errorCode = "REF_TABLE_NOT_FOUND";
    String message = "Reference table not found";
    HttpStatus status = HttpStatus.NOT_FOUND;

    // When
    ErrorResponse response = errorResponseService.createRdmErrorResponse(errorCode, message, status);

    // Then
    assertValidErrorResponse(response, status, "rdm-ref_table_not_found");
    assertEquals(message, response.title());
  }

  @Test
  void createInternalErrorResponse_ShouldReturnConsistentErrorFormat() {
    // Given
    when(errorUriBuilder.buildInternalErrorUri()).thenReturn("http://test.example.com/rdm/errors#internal-error");

    // When
    ErrorResponse response = errorResponseService.createInternalErrorResponse();

    // Then
    assertValidErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "internal-error");
    assertEquals("An unexpected error occurred", response.title());
  }

  @Test
  void errorResponseService_ShouldFollowSingleResponsibilityPrinciple() {
    when(errorUriBuilder.buildInvalidArgumentUri()).thenReturn("http://test.example.com/rdm/errors#invalid-argument");

    ErrorResponse response = errorResponseService.createCustomValidationErrorResponse("Test message");

    assertNotNull(response);
    assertTrue(response.type().startsWith("http://test.example.com/rdm/errors"));
    assertNotNull(response.title());
    assertNotNull(response.status());
  }

  /**
   * Helper method to validate ErrorResponse structure follows Trimble API standards
   */
  private void assertValidErrorResponse(ErrorResponse response, HttpStatus expectedStatus, String expectedUriSuffix) {
    assertNotNull(response, "ErrorResponse should not be null");
    assertNotNull(response.type(), "Type field is required");
    assertTrue(response.type().startsWith("http://test.example.com/rdm/errors"), "Type should be a URI reference");
    assertTrue(response.type().contains(expectedUriSuffix), "Type URI should contain expected suffix");
    assertNotNull(response.title(), "Title field is required");
    assertEquals(expectedStatus.value(), response.status(), "Status should match expected HTTP status");
  }
} 