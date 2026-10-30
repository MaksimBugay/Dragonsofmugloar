package com.bmv.dragonsofmugloar.domain.exception;

import com.bmv.flow.foundry.domain.model.api.error.ErrorDetails;
import com.bmv.flow.foundry.domain.model.api.error.BmvError;
import com.bmv.flow.foundry.domain.model.api.exception.BmvExceptionBase;
import com.bmv.flow.foundry.domain.model.cluster.BmvService;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class DragonsofmugloarServerException extends BmvExceptionBase {

    private final String endpoint;

    private final int httpStatusCode;

    public DragonsofmugloarServerException(Throwable error, String endpoint, int httpStatusCode) {
        super(
                "Dragonsofmugloar server error: " + BmvExceptionBase.buildCauseMessage(error, BmvService.GATEWAY),
                BmvError.COMMANDMENT_EXECUTION_ERROR, true
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
