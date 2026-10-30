package com.bmv.dragonsofmugloar.application.commandment.command.definition;

import static com.bmv.flow.foundry.domain.model.auth.BmvRole.ANONYMOUS_USER;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import com.bmv.flow.foundry.application.port.in.InternalApiInboundPort;
import com.bmv.flow.foundry.application.core.cflow.CFlowStageCompletedListener;
import com.bmv.flow.foundry.application.core.cflow.CFlowStateWithMutableBuffer;
import com.bmv.flow.foundry.application.core.cflow.CommandmentStageExecutor;
import com.bmv.flow.foundry.application.core.cflow.ConditionalLoopAttributes;
import com.bmv.flow.foundry.application.core.CommandmentWithMultipleStagesAndMutableBufferDefinitionBase;
import com.bmv.flow.foundry.application.core.CommandmentWithMutableBufferTypeData;
import com.bmv.flow.foundry.application.core.CustomRunTimeAttributes;
import com.bmv.flow.foundry.application.core.ExecutionRuleWithLinkedService;
import com.bmv.flow.foundry.application.core.ExecutionRuleWithLinkedServiceAndConditionalLoop;
import com.bmv.flow.foundry.application.core.ExecutionRuleWithSubCommandmentAndStaticMappers;
import com.bmv.flow.foundry.domain.model.api.error.ErrorDetails;
import com.bmv.flow.foundry.domain.model.auth.AnonymousAuthorization;
import com.bmv.flow.foundry.domain.model.cluster.BmvService;
import com.bmv.dragonsofmugloar.application.service.PlayDragonsofmugloarGameService;
import com.bmv.dragonsofmugloar.application.commandment.command.PlayDragonsofmugloarGameCommand;
import com.bmv.dragonsofmugloar.application.commandment.command.PlayDragonsofmugloarGameCommand.Buffer;
import com.bmv.dragonsofmugloar.application.commandment.command.PlayGameTurnCommand;
import com.bmv.dragonsofmugloar.application.commandment.request.PlayGameRequest;
import com.bmv.dragonsofmugloar.application.commandment.response.PlayGameResponse;

public class PlayDragonsofmugloarGameCommandDefinition extends CommandmentWithMultipleStagesAndMutableBufferDefinitionBase<
    AnonymousAuthorization, PlayGameRequest, PlayGameResponse, PlayDragonsofmugloarGameCommand, Buffer> {

  public static final int START_GAME = 1;
  public static final int PLAY_TURNS = 2;

  public PlayDragonsofmugloarGameCommandDefinition() {
    super(
        Set.of(ANONYMOUS_USER),
        new CommandmentWithMutableBufferTypeData<>(PlayDragonsofmugloarGameCommand.NAME,
            PlayDragonsofmugloarGameCommand.class, AnonymousAuthorization.class, PlayGameRequest.class,
            PlayGameResponse.class, Buffer.class, State.class),
        BmvService.GATEWAY,
        Map.of(
            START_GAME, ExecutionRuleWithLinkedService.of(
                PlayDragonsofmugloarGameService.class, PlayDragonsofmugloarGameService::startGame),
            PLAY_TURNS, ExecutionRuleWithLinkedServiceAndConditionalLoop.of(
                ExecutionRuleWithSubCommandmentAndStaticMappers.of(
                    BmvService.CURRENT, null, PlayGameTurnCommand.TYPE_DEFINITION,
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
