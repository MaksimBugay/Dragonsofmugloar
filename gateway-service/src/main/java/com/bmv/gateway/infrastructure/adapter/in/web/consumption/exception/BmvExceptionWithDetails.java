package com.bmv.gateway.infrastructure.adapter.in.web.consumption.exception;

import com.bmv.flow.foundry.domain.model.api.error.ErrorDetails;
import com.bmv.flow.foundry.serialization.json.JsonUtility;

public class BmvExceptionWithDetails extends RuntimeException {

  private final ErrorDetails errorDetails;

  public BmvExceptionWithDetails(ErrorDetails errorDetails) {
    super(JsonUtility.toJson(errorDetails));
    this.errorDetails = errorDetails;
  }

  public ErrorDetails getErrorDetails() {
    return errorDetails;
  }
}
