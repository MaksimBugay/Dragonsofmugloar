package com.bmv.dragonsofmugloar.application.commandment.definition;

import static com.bmv.flow.foundry.domain.model.auth.BmvRole.ANONYMOUS_USER;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.bmv.flow.foundry.application.port.in.InternalApiInboundPort;
import com.bmv.flow.foundry.application.core.cflow.CFlowStageCompletedListener;
import com.bmv.flow.foundry.application.core.cflow.CFlowState;
import com.bmv.flow.foundry.application.core.cflow.CommandmentStageExecutor;
import com.bmv.flow.foundry.application.core.CommandmentTypeData;
import com.bmv.flow.foundry.application.core.CommandmentDefinitionBase;
import com.bmv.flow.foundry.application.core.CustomRunTimeAttributes;
import com.bmv.flow.foundry.application.core.ExecutionRuleWithSubCommandmentAndStaticMappers;
import com.bmv.dragonsofmugloar.application.commandment.GenerateHiddenMessageQuery;
import com.bmv.dragonsofmugloar.application.commandment.BuildHiddenMessageAiTool;
import com.bmv.flow.foundry.domain.model.api.error.ErrorDetails;
import com.bmv.dragonsofmugloar.application.commandment.request.BuildHiddenMessageRequest;
import com.bmv.dragonsofmugloar.application.commandment.response.HiddenMessageResponse;
import com.bmv.flow.foundry.domain.model.auth.AnonymousAuthorization;
import com.bmv.flow.foundry.domain.model.cluster.BmvService;

/**
 * Public query that delegates hidden-message generation to the internal AI tool.
 */
public class GenerateHiddenMessageQueryDefinition
        extends CommandmentDefinitionBase<AnonymousAuthorization, BuildHiddenMessageRequest,
        HiddenMessageResponse, GenerateHiddenMessageQuery> {

    public GenerateHiddenMessageQueryDefinition() {
        super(
                Set.of(ANONYMOUS_USER),
                new CommandmentTypeData<>(
                        GenerateHiddenMessageQuery.NAME,
                        GenerateHiddenMessageQuery.class,
                        AnonymousAuthorization.class,
                        BuildHiddenMessageRequest.class,
                        HiddenMessageResponse.class,
                        State.class),
                BmvService.GATEWAY,
                ExecutionRuleWithSubCommandmentAndStaticMappers.withSubResponsePropagation(
                        BuildHiddenMessageAiTool.TYPE_DEFINITION, CFlowState::getRequestObject
                )
        );
    }

    @Override
    public GenerateHiddenMessageQuery createInstance(UUID id, AnonymousAuthorization authorization,
                                                     BuildHiddenMessageRequest requestObject,
                                                     HiddenMessageResponse responseObject,
                                                     CustomRunTimeAttributes customRunTimeAttributes,
                                                     int stageNumber) {
        return new GenerateHiddenMessageQuery(id, authorization, requestObject, responseObject, customRunTimeAttributes);
    }

    @Override
    public HiddenMessageResponse createErrorResponse(ErrorDetails errorDetails) {
        return new HiddenMessageResponse(null, errorDetails);
    }

    @Override
    public CFlowState<AnonymousAuthorization, BuildHiddenMessageRequest, HiddenMessageResponse, GenerateHiddenMessageQuery> createState(
            GenerateHiddenMessageQuery commandment,
            Map<Object, Object> commandmentContext,
            List<CFlowStageCompletedListener<AnonymousAuthorization, BuildHiddenMessageRequest, HiddenMessageResponse, GenerateHiddenMessageQuery>> listeners,
            InternalApiInboundPort internalApiInboundPort,
            CommandmentStageExecutor commandmentStageExecutor) {
        return new State(commandment, commandmentContext, listeners, internalApiInboundPort, commandmentStageExecutor);
    }

    public static class State
            extends CFlowState<AnonymousAuthorization, BuildHiddenMessageRequest, HiddenMessageResponse, GenerateHiddenMessageQuery> {
        public State(GenerateHiddenMessageQuery commandment,
                     Map<Object, Object> commandmentContext,
                     List<CFlowStageCompletedListener<AnonymousAuthorization, BuildHiddenMessageRequest, HiddenMessageResponse, GenerateHiddenMessageQuery>> listeners,
                     InternalApiInboundPort internalApiInboundPort,
                     CommandmentStageExecutor commandmentStageExecutor) {
            super(commandment, commandmentContext, listeners, internalApiInboundPort, commandmentStageExecutor);
        }
    }
}
