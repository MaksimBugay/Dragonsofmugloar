package com.bmv.gateway.infrastructure.adapter.in.web.consumption.exception;

import com.trimble.transportation.reference.data.management.domain.model.api.error.ErrorDetails;
import com.trimble.transportation.reference.data.management.serialization.json.JsonUtility;

public class RdmExceptionWithDetails extends RuntimeException {

  private final ErrorDetails errorDetails;

  public RdmExceptionWithDetails(ErrorDetails errorDetails) {
    super(JsonUtility.toJson(errorDetails));
    this.errorDetails = errorDetails;
  }

  public ErrorDetails getErrorDetails() {
    return errorDetails;
  }
}
