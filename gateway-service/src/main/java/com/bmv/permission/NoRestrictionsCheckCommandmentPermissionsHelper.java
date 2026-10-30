package com.bmv.permission;

import com.bmv.flow.foundry.application.commandment.response.YesNoResponse;
import com.bmv.flow.foundry.application.port.in.InternalApiInboundPort;
import com.bmv.flow.foundry.application.service.permission.CheckCommandmentPermissionsHelper;
import com.bmv.flow.foundry.domain.model.rdm.RefTableRevision;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class NoRestrictionsCheckCommandmentPermissionsHelper implements CheckCommandmentPermissionsHelper {
    @Override
    public Mono<YesNoResponse> isApplicationSubscribedOnTable(UUID sequenceId, UUID applicationId, UUID tableId, String tableName, InternalApiInboundPort internalApiInboundPort) {
        return Mono.just(YesNoResponse.yes());
    }

    @Override
    public Mono<YesNoResponse> isUserOwnerOfTable(UUID sequenceId, String userId, UUID tableId, InternalApiInboundPort internalApiInboundPort) {
        return Mono.just(YesNoResponse.yes());
    }

    @Override
    public Mono<YesNoResponse> isUserCreatorOfTableRevision(UUID sequenceId, String userId, RefTableRevision revision, InternalApiInboundPort internalApiInboundPort) {
        return Mono.just(YesNoResponse.yes());
    }

    @Override
    public Mono<YesNoResponse> isUserOwnerOfApplication(UUID sequenceId, String userId, UUID applicationId, InternalApiInboundPort internalApiInboundPort) {
        return Mono.just(YesNoResponse.yes());
    }

    @Override
    public Mono<YesNoResponse> isUserOwnerOfSraObject(UUID sequenceId, String userId, String sraObjectId, InternalApiInboundPort internalApiInboundPort) {
        return Mono.just(YesNoResponse.yes());
    }
}
