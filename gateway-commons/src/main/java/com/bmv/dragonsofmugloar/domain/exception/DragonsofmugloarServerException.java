package com.bmv.dragonsofmugloar.domain.exception;

import com.trimble.transportation.reference.data.management.domain.model.api.error.ErrorDetails;
import com.trimble.transportation.reference.data.management.domain.model.api.error.RdmError;
import com.trimble.transportation.reference.data.management.domain.model.api.exception.RdmExceptionBase;
import com.trimble.transportation.reference.data.management.domain.model.cluster.RdmService;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class DragonsofmugloarServerException extends RdmExceptionBase {

    private final String endpoint;

    private final int httpStatusCode;

    public DragonsofmugloarServerException(Throwable error, String endpoint, int httpStatusCode) {
        super(
                "Dragonsofmugloar server error: " + RdmExceptionBase.buildCauseMessage(error, RdmService.GATEWAY),
                RdmError.COMMANDMENT_EXECUTION_ERROR, true
        );
        this.endpoint = endpoint;
        this.httpStatusCode = httpStatusCode;
    }

    @Override
    public ErrorDetails errorDetails() {
        return super.details(
                Map.of(
                        "endpoint",
                        StringUtils.isNotEmpty(endpoint) ? endpoint : "unknown",
                        "status",
                        httpStatusCode
                )
        );
    }
}
