package com.bmv.dragonsofmugloar.domain.api.commandment.definition;

import static com.trimble.transportation.reference.data.management.domain.model.auth.RdmRole.ANONYMOUS_USER;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import com.trimble.transportation.reference.data.management.application.port.in.InternalApiInboundPort;
import com.trimble.transportation.reference.data.management.domain.cflow.CFlowStageCompletedListener;
import com.trimble.transportation.reference.data.management.domain.cflow.CFlowStateWithMutableBuffer;
import com.trimble.transportation.reference.data.management.domain.cflow.CommandmentStageExecutor;
import com.trimble.transportation.reference.data.management.domain.cflow.ConditionalLoopAttributes;
import com.trimble.transportation.reference.data.management.domain.model.api.CommandmentWithMultipleStagesAndMutableBufferTypeDefinitionBase;
import com.trimble.transportation.reference.data.management.domain.model.api.CommandmentWithMutableBufferTypeData;
import com.trimble.transportation.reference.data.management.domain.model.api.CustomRunTimeAttributes;
import com.trimble.transportation.reference.data.management.domain.model.api.ExecutionRuleWithLinkedService;
import com.trimble.transportation.reference.data.management.domain.model.api.ExecutionRuleWithLinkedServiceAndConditionalLoop;
import com.trimble.transportation.reference.data.management.domain.model.api.ExecutionRuleWithSubCommandmentAndStaticMappers;
import com.trimble.transportation.reference.data.management.domain.model.api.error.ErrorDetails;
import com.trimble.transportation.reference.data.management.domain.model.auth.AnonymousAuthorization;
import com.trimble.transportation.reference.data.management.domain.model.cluster.RdmService;
import com.bmv.dragonsofmugloar.application.service.PlayDragonsofmugloarGameService;
import com.bmv.dragonsofmugloar.domain.api.commandment.PlayDragonsofmugloarGameCommand;
import com.bmv.dragonsofmugloar.domain.api.commandment.PlayDragonsofmugloarGameCommand.Buffer;
import com.bmv.dragonsofmugloar.domain.api.commandment.PlayGameTurnCommand;
import com.bmv.dragonsofmugloar.domain.api.request.PlayGameRequest;
import com.bmv.dragonsofmugloar.domain.api.response.PlayGameResponse;

public class PlayDragonsofmugloarGameCommandDefinition extends CommandmentWithMultipleStagesAndMutableBufferTypeDefinitionBase<
    AnonymousAuthorization, PlayGameRequest, PlayGameResponse, PlayDragonsofmugloarGameCommand, Buffer> {

  public static final int START_GAME = 1;
  public static final int PLAY_TURNS = 2;

  public PlayDragonsofmugloarGameCommandDefinition() {
    super(
        Set.of(ANONYMOUS_USER),
        new CommandmentWithMutableBufferTypeData<>(PlayDragonsofmugloarGameCommand.NAME,
            PlayDragonsofmugloarGameCommand.class, AnonymousAuthorization.class, PlayGameRequest.class,
            PlayGameResponse.class, Buffer.class, State.class),
        RdmService.GATEWAY,
        Map.of(
            START_GAME, ExecutionRuleWithLinkedService.of(
                PlayDragonsofmugloarGameService.class, PlayDragonsofmugloarGameService::startGame),
            PLAY_TURNS, ExecutionRuleWithLinkedServiceAndConditionalLoop.of(
                ExecutionRuleWithSubCommandmentAndStaticMappers.of(
                    RdmService.CURRENT, null, PlayGameTurnCommand.TYPE_DEFINITION,
                    PlayDragonsofmugloarGameService::mapTurnRequest,
                    PlayDragonsofmugloarGameService::mapTurnResponse,
                    false),
                new ConditionalLoopAttributes<>(PlayDragonsofmugloarGameService::shouldStop, Integer.MAX_VALUE))
        )
    );
  }

  @Override
  public Buffer createMutableBuffer() {
    return Buffer.empty();
  }

  @Override
  public PlayDragonsofmugloarGameCommand createInstance(UUID id, AnonymousAuthorization authorization,
      PlayGameRequest request, PlayGameResponse response, CustomRunTimeAttributes attributes, int stageNumber, Buffer buffer) {
    return new PlayDragonsofmugloarGameCommand(id, authorization, request, response, attributes, stageNumber, buffer);
  }

  @Override
  public PlayGameResponse createErrorResponse(ErrorDetails errorDetails) {
    return new PlayGameResponse(null, 0, 0, 0, 0, 0, 0, null, 0, errorDetails);
  }

  @Override
  public BiFunction<PlayGameResponse, PlayGameResponse, PlayGameResponse> getResponseMergeFunction() {
    // A successful turn must replace a preceding retryable server error.
    return (previous, current) -> current == null ? previous : current;
  }

  @Override
  public State createState(PlayDragonsofmugloarGameCommand commandment, Map<Object, Object> commandmentContext,
      List<CFlowStageCompletedListener<AnonymousAuthorization, PlayGameRequest, PlayGameResponse,
          PlayDragonsofmugloarGameCommand>> listeners,
      InternalApiInboundPort internalApiInboundPort, CommandmentStageExecutor stageExecutor) {
    return new State(commandment, commandmentContext, listeners, internalApiInboundPort, stageExecutor);
  }

  public static class State extends CFlowStateWithMutableBuffer<
      AnonymousAuthorization, PlayGameRequest, PlayGameResponse, PlayDragonsofmugloarGameCommand, Buffer> {

    private Buffer buffer;

    public State(PlayDragonsofmugloarGameCommand commandment, Map<Object, Object> commandmentContext,
        List<CFlowStageCompletedListener<AnonymousAuthorization, PlayGameRequest, PlayGameResponse,
            PlayDragonsofmugloarGameCommand>> listeners,
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
    public PlayDragonsofmugloarGameCommand assembleCommandment() {
      return PlayDragonsofmugloarGameCommand.TYPE_DEFINITION.createInstance(getId(), getAuthorization(), getRequestObject(),
          getResponseObject(), getCustomRunTimeAttributes(), getStageNumber(), buffer);
    }
  }
}
