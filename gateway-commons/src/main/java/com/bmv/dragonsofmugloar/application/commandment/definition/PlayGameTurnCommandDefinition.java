package com.bmv.dragonsofmugloar.application.commandment.definition;

import static com.bmv.flow.foundry.domain.model.auth.BmvRole.BMV_CLUSTER_SERVICE;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.bmv.flow.foundry.application.port.in.InternalApiInboundPort;
import com.bmv.flow.foundry.application.service.ServiceContextSupplier;
import com.bmv.flow.foundry.application.core.cflow.CFlowStageCompletedListener;
import com.bmv.flow.foundry.application.core.cflow.CFlowStateWithMutableBuffer;
import com.bmv.flow.foundry.application.core.cflow.CommandmentStageExecutor;
import com.bmv.flow.foundry.application.core.CommandmentWithMultipleStagesAndMutableBufferDefinitionBase;
import com.bmv.flow.foundry.application.core.CommandmentWithMutableBufferTypeData;
import com.bmv.flow.foundry.application.core.CustomRunTimeAttributes;
import com.bmv.flow.foundry.application.core.ExecutionRuleWithLinkedService;
import com.bmv.flow.foundry.application.core.ExecutionRuleWithLinkedServiceAndConditionalStage;
import com.bmv.flow.foundry.domain.model.api.error.ErrorDetails;
import com.bmv.flow.foundry.domain.model.auth.BmvServiceAuthorization;
import com.bmv.flow.foundry.domain.model.cluster.BmvService;
import com.bmv.dragonsofmugloar.application.service.ChooseGameActionService;
import com.bmv.dragonsofmugloar.application.service.PlayGameTurnService;
import com.bmv.dragonsofmugloar.application.commandment.PlayGameTurnCommand;
import com.bmv.dragonsofmugloar.application.commandment.PlayGameTurnCommand.Buffer;
import com.bmv.dragonsofmugloar.application.commandment.request.PlayGameTurnRequest;
import com.bmv.dragonsofmugloar.application.commandment.response.PlayGameTurnResponse;

public class PlayGameTurnCommandDefinition extends CommandmentWithMultipleStagesAndMutableBufferDefinitionBase<
    BmvServiceAuthorization, PlayGameTurnRequest, PlayGameTurnResponse, PlayGameTurnCommand, Buffer> {

  public static final int FETCH_SHOP_ITEMS = 1;
  public static final int CHOOSE_CRITICAL_HEALING = 2;
  public static final int FETCH_ADS = 3;
  public static final int CHOOSE_ACTION = 4;
  public static final int EXECUTE_ACTION = 5;

  public PlayGameTurnCommandDefinition() {
    super(
        Set.of(BMV_CLUSTER_SERVICE),
        new CommandmentWithMutableBufferTypeData<>(PlayGameTurnCommand.NAME, PlayGameTurnCommand.class,
            BmvServiceAuthorization.class, PlayGameTurnRequest.class, PlayGameTurnResponse.class, Buffer.class, State.class),
        BmvService.GATEWAY,
        Map.of(
            FETCH_SHOP_ITEMS, ExecutionRuleWithLinkedService.of(PlayGameTurnService.class, PlayGameTurnService::fetchShopItems),
            CHOOSE_CRITICAL_HEALING, ExecutionRuleWithLinkedServiceAndConditionalStage.of(
                ExecutionRuleWithLinkedService.of(PlayGameTurnService.class, PlayGameTurnService::chooseCriticalHealing),
                state -> ServiceContextSupplier.getObjectFromServiceContext(ChooseGameActionService.class)
                    .chooseCriticalHealing(state.getRequestObject().gameState(), ((State) state).getMutableBuffer().shopItems()),
                EXECUTE_ACTION),
            FETCH_ADS, ExecutionRuleWithLinkedService.of(PlayGameTurnService.class, PlayGameTurnService::fetchAds),
            CHOOSE_ACTION, ExecutionRuleWithLinkedService.of(PlayGameTurnService.class, PlayGameTurnService::chooseAction),
            EXECUTE_ACTION, ExecutionRuleWithLinkedService.of(PlayGameTurnService.class, PlayGameTurnService::executeAction)
        )
    );
  }

  @Override
  public Buffer createMutableBuffer() {
    return Buffer.empty();
  }

  @Override
  public PlayGameTurnCommand createInstance(UUID id, BmvServiceAuthorization authorization, PlayGameTurnRequest request,
                                            PlayGameTurnResponse response, CustomRunTimeAttributes attributes,
                                            int stageNumber, Buffer buffer) {
    return new PlayGameTurnCommand(id, authorization, request, response, attributes, stageNumber, buffer);
  }

  @Override
  public PlayGameTurnResponse createErrorResponse(ErrorDetails errorDetails) {
    return new PlayGameTurnResponse(null, errorDetails);
  }

  @Override
  public State createState(PlayGameTurnCommand commandment, Map<Object, Object> commandmentContext,
                           List<CFlowStageCompletedListener<BmvServiceAuthorization, PlayGameTurnRequest,
                               PlayGameTurnResponse, PlayGameTurnCommand>> listeners,
                           InternalApiInboundPort internalApiInboundPort, CommandmentStageExecutor stageExecutor) {
    return new State(commandment, commandmentContext, listeners, internalApiInboundPort, stageExecutor);
  }

  public static class State extends CFlowStateWithMutableBuffer<
      BmvServiceAuthorization, PlayGameTurnRequest, PlayGameTurnResponse, PlayGameTurnCommand, Buffer> {

    private Buffer buffer;

    public State(PlayGameTurnCommand commandment, Map<Object, Object> commandmentContext,
                 List<CFlowStageCompletedListener<BmvServiceAuthorization, PlayGameTurnRequest,
                     PlayGameTurnResponse, PlayGameTurnCommand>> listeners,
                 InternalApiInboundPort internalApiInboundPort, CommandmentStageExecutor stageExecutor) {
      super(commandment, commandmentContext, listeners, internalApiInboundPort, stageExecutor);
      buffer = commandment.getMutableBuffer();
    }

    @Override
    public Buffer getMutableBuffer() {
      return buffer;
    }

    public void setBuffer(Buffer newBuffer) {
      buffer = newBuffer;
    }

    @Override
    public PlayGameTurnCommand assembleCommandment() {
      return PlayGameTurnCommand.TYPE_DEFINITION.createInstance(getId(), getAuthorization(), getRequestObject(),
          getResponseObject(), getCustomRunTimeAttributes(), getStageNumber(), buffer);
    }
  }
}
