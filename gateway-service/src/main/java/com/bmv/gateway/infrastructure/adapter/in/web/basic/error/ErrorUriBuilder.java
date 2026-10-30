package com.bmv.gateway.infrastructure.adapter.in.web.basic.error;

import com.bmv.gateway.config.BmvConfig;
import org.springframework.stereotype.Component;

/**
 * Utility class for building consistent error URIs.
 */
@Component
public class ErrorUriBuilder {

  private final String baseUri;

  public ErrorUriBuilder(BmvConfig bmvConfig) {
    this.baseUri = bmvConfig.getErrorResponseBaseUri();
  }

  /**
   * Builds URI for validation errors
   */
  public String buildValidationErrorUri() {
    return baseUri + "#validation-error";
  }

  /**
   * Builds URI for constraint violation errors
   */
  public String buildConstraintViolationUri() {
    return baseUri + "#constraint-violation";
  }

  /**
   * Builds URI for input format errors
   */
  public String buildInputErrorUri() {
    return baseUri + "#input-error";
  }

  /**
   * Builds URI for HTTP status errors
   */
  public String buildHttpStatusUri(int statusCode) {
    return baseUri + "#http-" + statusCode;
  }

  /**
   * Builds URI for invalid argument errors
   */
  public String buildInvalidArgumentUri() {
    return baseUri + "#invalid-argument";
  }

  /**
   * Builds URI for BMV errors
   */
  public String buildBmvErrorUri(String errorCode) {
    return baseUri + "#rdm-" + errorCode.toLowerCase();
  }

  /**
   * Builds URI for internal system errors
   */
  public String buildInternalErrorUri() {
    return baseUri + "#internal-error";
  }

  /**
   * Builds URI for field validation errors
   */
  public String buildFieldValidationUri() {
    return baseUri + "#field-validation";
  }

  /**
   * Builds URI for constraint validation errors
   */
  public String buildConstraintValidationUri() {
    return baseUri + "#constraint-validation";
  }

} 