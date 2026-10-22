package com.bmv.dragonsofmugloar.application.service;

import com.trimble.transportation.reference.data.management.application.service.ServiceContextSupplier;
import com.trimble.transportation.reference.data.management.domain.cflow.CFlowState;
import com.trimble.transportation.reference.data.management.domain.model.api.error.ErrorDetails;
import com.trimble.transportation.reference.data.management.domain.model.auth.AnonymousAuthorization;
import com.bmv.dragonsofmugloar.domain.api.commandment.PlayDragonsofmugloarGameCommand;
import com.bmv.dragonsofmugloar.domain.api.commandment.PlayDragonsofmugloarGameCommand.Buffer;
import com.bmv.dragonsofmugloar.domain.api.commandment.definition.PlayDragonsofmugloarGameCommandDefinition.State;
import com.bmv.dragonsofmugloar.domain.api.request.PlayGameRequest;
import com.bmv.dragonsofmugloar.domain.api.request.PlayGameTurnRequest;
import com.bmv.dragonsofmugloar.domain.api.response.PlayGameResponse;
import com.bmv.dragonsofmugloar.domain.api.response.PlayGameTurnResponse;
import com.bmv.dragonsofmugloar.domain.model.GameConfig;
import com.bmv.dragonsofmugloar.domain.model.GameState;
import com.bmv.dragonsofmugloar.port.out.DragonsofmugloarPort;
import com.trimble.transportation.reference.data.management.util.RdmObjectUtils;
import reactor.core.publisher.Mono;

public class PlayDragonsofmugloarGameService {

    private static final String SERVER_ERROR_MESSAGE = "Dragonsofmugloar server error: ";

    private final DragonsofmugloarPort port;

    @SuppressWarnings("unused")
    public PlayDragonsofmugloarGameService() {
        this(ServiceContextSupplier.getObjectFromServiceContext(DragonsofmugloarPort.class));
    }

    public PlayDragonsofmugloarGameService(DragonsofmugloarPort port) {
        this.port = port;
    }

    public Mono<PlayGameResponse> startGame(
            CFlowState<AnonymousAuthorization, PlayGameRequest, PlayGameResponse, PlayDragonsofmugloarGameCommand> flowState) {
        State state = (State) flowState;
        return port.startGame().map(game -> {
            GameState gameState = new GameState(game.gameId(), game.gold(), game.lives(), game.level(),
                    game.turn(), game.score(), game.highScore());
            state.setBuffer(new Buffer(gameState, state.getRequestObject().toGameConfig()));
            return toResponse(gameState, null);
        });
    }

    public static PlayGameTurnRequest mapTurnRequest(
            CFlowState<AnonymousAuthorization, PlayGameRequest, PlayGameResponse, PlayDragonsofmugloarGameCommand> flowState) {
        Buffer buffer = ((State) flowState).getMutableBuffer();
        return new PlayGameTurnRequest(buffer.gameState(), buffer.gameConfig());
    }

    public static PlayGameResponse mapTurnResponse(PlayGameTurnResponse response,
                                                   CFlowState<AnonymousAuthorization, PlayGameRequest, PlayGameResponse, PlayDragonsofmugloarGameCommand> flowState) {
        State state = (State) flowState;

        if (response.gameState() != null) {
            state.setBuffer(state.getMutableBuffer().withGameState(response.gameState()));
        }
        return toResponse(state.getMutableBuffer().gameState(), response.errorDetails());
    }

    public static boolean shouldStop(
            CFlowState<AnonymousAuthorization, PlayGameRequest, PlayGameResponse, PlayDragonsofmugloarGameCommand> flowState) {
        PlayGameResponse response = flowState.getResponseObject();
        if (response != null && response.hasError()) {
            String message = response.errorDetails().message();
            if (message == null || !message.contains(SERVER_ERROR_MESSAGE)) {
                return true;
            }
        }
        Buffer buffer = ((State) flowState).getMutableBuffer();
        GameState gameState = buffer.gameState();
        GameConfig config = buffer.gameConfig();
        return gameState.turn() >= config.turnsLimit()
                || RdmObjectUtils.getCurrentTimeMillis() - gameState.startedAt() >= config.timeLimitMillis()
                || gameState.score() >= config.scoreGoal()
                || gameState.lives() == 0;
    }

    private static PlayGameResponse toResponse(GameState state, ErrorDetails errorDetails) {
        return new PlayGameResponse(state.gameId(), state.gold(), state.lives(), state.level(), state.turn(), state.score(),
                state.highScore(),
                state.lastInvestigatedReputation(), RdmObjectUtils.getCurrentTimeMillis() - state.startedAt(), errorDetails);
    }
}
