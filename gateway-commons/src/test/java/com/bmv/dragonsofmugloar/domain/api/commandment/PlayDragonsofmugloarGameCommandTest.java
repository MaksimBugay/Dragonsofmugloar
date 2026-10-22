package com.bmv.dragonsofmugloar.domain.api.commandment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.trimble.transportation.reference.data.management.application.port.in.InternalApiInboundPort;
import com.trimble.transportation.reference.data.management.application.service.RdmServiceContext;
import com.trimble.transportation.reference.data.management.application.service.ServiceContextSupplier;
import com.trimble.transportation.reference.data.management.domain.cflow.CFlowStageCompletedListener;
import com.trimble.transportation.reference.data.management.domain.model.api.ExecutionRuleWithLinkedServiceAndConditionalLoop;
import com.trimble.transportation.reference.data.management.domain.model.api.PublicCommandment;
import com.trimble.transportation.reference.data.management.domain.model.api.error.ErrorDetails;
import com.trimble.transportation.reference.data.management.domain.model.auth.AnonymousAuthorization;
import com.trimble.transportation.reference.data.management.domain.model.auth.RdmRole;
import com.trimble.transportation.reference.data.management.domain.model.cluster.RdmService;
import com.bmv.dragonsofmugloar.application.service.PlayDragonsofmugloarGameService;
import com.bmv.dragonsofmugloar.domain.api.commandment.PlayDragonsofmugloarGameCommand.Buffer;
import com.bmv.dragonsofmugloar.domain.api.commandment.definition.PlayDragonsofmugloarGameCommandDefinition;
import com.bmv.dragonsofmugloar.domain.api.commandment.definition.PlayDragonsofmugloarGameCommandDefinition.State;
import com.bmv.dragonsofmugloar.domain.api.request.PlayGameRequest;
import com.bmv.dragonsofmugloar.domain.api.request.PlayGameTurnRequest;
import com.bmv.dragonsofmugloar.domain.api.response.PlayGameResponse;
import com.bmv.dragonsofmugloar.domain.api.response.PlayGameTurnResponse;
import com.bmv.dragonsofmugloar.domain.exception.DragonsofmugloarServerException;
import com.bmv.dragonsofmugloar.domain.model.Game;
import com.bmv.dragonsofmugloar.domain.model.GameState;
import com.bmv.dragonsofmugloar.port.out.DragonsofmugloarPort;
import com.trimble.transportation.reference.data.management.infrastructure.adapter.in.CFlowStageCommandmentExecutor;
import com.trimble.transportation.reference.data.management.serialization.json.JsonUtility;
import com.trimble.transportation.reference.data.management.util.RdmObjectUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import reactor.core.publisher.Mono;

class PlayDragonsofmugloarGameCommandTest {

  private final DragonsofmugloarPort port = mock(DragonsofmugloarPort.class);
  private final InternalApiInboundPort inboundPort = mock(InternalApiInboundPort.class);
  private final List<PlayGameTurnRequest> turnRequests = new ArrayList<>();
  private final CFlowStageCommandmentExecutor executor = new CFlowStageCommandmentExecutor(RdmService.GATEWAY, null, null);
  private GenericApplicationContext serviceContext;
  private Function<PlayGameTurnRequest, PlayGameTurnResponse> turnRunner = request -> new PlayGameTurnResponse(
      advance(request.gameState(), request.gameState().turn() + 1, request.gameState().score() + 100), null);

  @BeforeEach
  void setUp() {
    serviceContext = new GenericApplicationContext();
    serviceContext.registerBean(PlayDragonsofmugloarGameService.class, () -> new PlayDragonsofmugloarGameService(port));
    serviceContext.refresh();
    ServiceContextSupplier.resetServiceContext();
    ServiceContextSupplier.setServiceContext(new RdmServiceContext(serviceContext));
    when(inboundPort.getRdmService()).thenReturn(RdmService.GATEWAY);
    when(inboundPort.getCommandmentExecutionTimeout()).thenReturn(Duration.ofSeconds(5));
    when(port.startGame()).thenReturn(Mono.just(new Game("game", 3, 10, 1, 0, 900, 0)));
    doAnswer(invocation -> {
      PlayGameTurnCommand command = invocation.getArgument(0);
      Function<PlayGameTurnResponse, Mono<PlayGameResponse>> mapper = invocation.getArgument(1);
      turnRequests.add(command.getRequestObject());
      assertSame(PlayGameTurnCommand.TYPE_DEFINITION, command.getCommandmentTypeDefinition());
      return mapper.apply(turnRunner.apply(command.getRequestObject()));
    }).when(inboundPort).executeSubCommandment(any(), any(), eq(false));
  }

  @AfterEach
  void tearDown() {
    ServiceContextSupplier.resetServiceContext();
    serviceContext.close();
  }

  @Test
  void startsGameBuffersConfigAndPassesUpdatedStateToEachTurnUntilLimit() {
    var request = request(3, 60_000L, 10_000L);
    var response = execute(request);

    assertEquals(3, turnRequests.size());
    assertEquals(0, turnRequests.getFirst().gameState().turn());
    assertEquals(2, turnRequests.getLast().gameState().turn());
    assertEquals(request.toGameConfig(), turnRequests.getFirst().gameConfig());
    assertEquals("game", turnRequests.getFirst().gameState().gameId());
    assertEquals("game", response.gameId());
    assertEquals(response, JsonUtility.fromJson(JsonUtility.toJson(response), PlayGameResponse.class));
    assertEquals(3, response.turn());
    assertEquals(300, response.score());
    assertEquals(900, response.highScore());
    assertEquals(10, response.gold());
    assertEquals(3, response.lives());
    assertEquals(1, response.level());
    assertNotNull(response.reputation());
    assertTrue(response.durationMs() >= 0);
    assertNull(response.errorDetails());
    verify(port).startGame();
  }

  @Test
  void stopsWhenTheLastLifeIsLostBeforeOtherLimits() {
    turnRunner = request -> {
      GameState state = request.gameState();
      return new PlayGameTurnResponse(new GameState(state.gameId(), state.gold(), state.lives() - 1, state.level(),
          state.turn() + 1, state.score() + 100, state.highScore(), state.lastInvestigatedReputation(),
          state.lastReputationInvestigationTurn(), state.startedAt(), state.lastAction()), null);
    };

    var response = execute(request(100, 60_000L, 10_000L));

    assertEquals(3, turnRequests.size());
    assertEquals(1, turnRequests.getLast().gameState().lives());
    assertEquals(0, response.lives());
    assertEquals(3, response.turn());
    assertEquals(300, response.score());
    assertEquals("game", response.gameId());
    assertNull(response.errorDetails());
  }

  @Test
  void skipsTurnsWhenInitialStateHasNoLives() {
    when(port.startGame()).thenReturn(Mono.just(new Game("game", 0, 10, 1, 0, 900, 0)));

    var response = execute(request(100, 60_000L, 10_000L));

    assertTrue(turnRequests.isEmpty());
    assertEquals(0, response.lives());
    assertEquals(0, response.turn());
    assertEquals("game", response.gameId());
    assertNull(response.errorDetails());
  }

  @Test
  void stopsAtScoreGoal() {
    var response = execute(request(100, 60_000L, 200L));

    assertEquals(2, turnRequests.size());
    assertEquals(200, response.score());
    assertNull(response.errorDetails());
  }

  @Test
  void stopsAtTimeLimitAfterATurn() {
    turnRunner = request -> {
      GameState state = request.gameState();
      return new PlayGameTurnResponse(new GameState(state.gameId(), state.gold(), state.lives(), state.level(),
          1, 100, state.highScore(), state.lastInvestigatedReputation(), state.lastReputationInvestigationTurn(),
          RdmObjectUtils.getCurrentTimeMillis() - 10_000, state.lastAction()), null);
    };
    var response = execute(request(100, 5_000L, 10_000L));

    assertEquals(1, turnRequests.size());
    assertTrue(response.durationMs() >= 10_000);
    assertNull(response.errorDetails());
  }

  @Test
  void retriesServerErrorsUsingLastStateAndClearsErrorAfterRecovery() {
    var serverError = new DragonsofmugloarServerException(new IllegalStateException("unavailable"), "/shop", 503)
        .errorDetails();
    turnRunner = request -> switch (turnRequests.size()) {
      case 2, 3 -> new PlayGameTurnResponse(null, serverError);
      default -> new PlayGameTurnResponse(
          advance(request.gameState(), request.gameState().turn() + 1, request.gameState().score() + 100), null);
    };

    var response = execute(request(2, 60_000L, 10_000L));

    assertEquals(4, turnRequests.size());
    assertEquals(turnRequests.get(1).gameState(), turnRequests.get(2).gameState());
    assertEquals(turnRequests.get(2).gameState(), turnRequests.get(3).gameState());
    assertEquals(2, response.turn());
    assertEquals(200, response.score());
    assertNull(response.errorDetails());
  }

  @Test
  void stopsOnOtherErrorsAndReturnsLatestGameStateAndError() {
    var error = new ErrorDetails(500, "Another error", true, null);
    turnRunner = request -> turnRequests.size() == 1
        ? new PlayGameTurnResponse(advance(request.gameState(), 1, 100), null)
        : new PlayGameTurnResponse(null, error);

    var response = execute(request(100, 60_000L, 10_000L));

    assertEquals(2, turnRequests.size());
    assertEquals(1, response.turn());
    assertEquals(100, response.score());
    assertEquals(error, response.errorDetails());
    assertEquals("game", response.gameId());
  }

  @Test
  void errorWithoutMessageAlsoStops() {
    var error = new ErrorDetails(500, null, true, null);
    turnRunner = _ -> new PlayGameTurnResponse(null, error);

    assertEquals(error, execute(request(100, 60_000L, 10_000L)).errorDetails());
    assertEquals(1, turnRequests.size());
  }

  @Test
  void retryableErrorStillHonorsTimeLimit() {
    var error = new DragonsofmugloarServerException(new IllegalStateException("unavailable"), "/shop", 503).errorDetails();
    var command = new PlayDragonsofmugloarGameCommand(request(100, 5_000L, 10_000L));
    State state = initializedState(command);
    GameState initial = state.getMutableBuffer().gameState();
    state.setBuffer(state.getMutableBuffer().withGameState(new GameState(initial.gameId(), initial.gold(),
        initial.lives(), initial.level(), initial.turn(), initial.score(), initial.highScore(),
        initial.lastInvestigatedReputation(), initial.lastReputationInvestigationTurn(),
        RdmObjectUtils.getCurrentTimeMillis() - 10_000, null)));
    state.setResponse(PlayDragonsofmugloarGameService.mapTurnResponse(new PlayGameTurnResponse(null, error), state));

    assertTrue(PlayDragonsofmugloarGameService.shouldStop(state));
  }

  @Test
  void skipsTurnsWhenInitialStateAlreadyMeetsTurnOrScoreLimit() {
    when(port.startGame()).thenReturn(Mono.just(new Game("game", 3, 10, 1, 500, 900, 10)));

    assertEquals(10, execute(request(10, 60_000L, 10_000L)).turn());
    assertEquals(500, execute(request(100, 60_000L, 500L)).score());
    assertTrue(turnRequests.isEmpty());
  }

  @Test
  void skipsTurnsWhenTimeLimitReachedBeforeLoop() {
    var listener = new CFlowStageCompletedListener<AnonymousAuthorization, PlayGameRequest, PlayGameResponse,
        PlayDragonsofmugloarGameCommand>(PlayDragonsofmugloarGameCommandDefinition.START_GAME, flowState -> {
          State state = (State) flowState;
          GameState initial = state.getMutableBuffer().gameState();
          state.setBuffer(state.getMutableBuffer().withGameState(new GameState(initial.gameId(), initial.gold(),
              initial.lives(), initial.level(), initial.turn(), initial.score(), initial.highScore(),
              initial.lastInvestigatedReputation(), initial.lastReputationInvestigationTurn(),
              RdmObjectUtils.getCurrentTimeMillis() - 10_000, null)));
        });

    execute(request(100, 5_000L, 10_000L), List.of(listener));
    assertTrue(turnRequests.isEmpty());
  }

  @Test
  void startFailureReturnsAnErrorWithoutPlayingTurns() {
    when(port.startGame()).thenReturn(Mono.error(new IllegalStateException("cannot start")));

    var response = execute(request(100, 60_000L, 10_000L));
    assertNotNull(response.errorDetails());
    assertNull(response.gameId());
    assertTrue(turnRequests.isEmpty());
  }

  @Test
  void publicCommandPreservesBufferAndStageAcrossJsonSerialization() {
    var command = new PlayDragonsofmugloarGameCommand(request(3, 60_000L, 10_000L));
    State state = initializedState(command);
    state.updateStageNumber(_ -> PlayDragonsofmugloarGameCommandDefinition.PLAY_TURNS);
    var restored = JsonUtility.fromJson(JsonUtility.toJson(state.assembleCommandment()),
        PlayDragonsofmugloarGameCommand.class);

    assertEquals(command.getId(), restored.getId());
    assertEquals(command.getRequestObject(), restored.getRequestObject());
    assertEquals(state.getMutableBuffer(), restored.getMutableBuffer());
    assertEquals(PlayDragonsofmugloarGameCommandDefinition.PLAY_TURNS, restored.getStageNumber());
    assertInstanceOf(PublicCommandment.class, restored);
    assertInstanceOf(AnonymousAuthorization.class, restored.getAuthorization());
    assertEquals(Buffer.empty(), command.getMutableBuffer());
    assertSame(PlayDragonsofmugloarGameCommand.TYPE_DEFINITION, restored.getCommandmentTypeDefinition());
    assertEquals(Set.of(RdmRole.ANONYMOUS_USER), PlayDragonsofmugloarGameCommand.TYPE_DEFINITION.getAcceptedRoles());
    assertEquals(2, PlayDragonsofmugloarGameCommand.TYPE_DEFINITION.getTotalNumberOfStages());
    assertInstanceOf(ExecutionRuleWithLinkedServiceAndConditionalLoop.class,
        PlayDragonsofmugloarGameCommand.TYPE_DEFINITION.getExecutionRuleByStageNumber(
            PlayDragonsofmugloarGameCommandDefinition.PLAY_TURNS));
  }

  private State initializedState(PlayDragonsofmugloarGameCommand command) {
    State state = PlayDragonsofmugloarGameCommand.TYPE_DEFINITION.createState(command, Map.of(), List.of(),
        inboundPort, executor);
    new PlayDragonsofmugloarGameService(port).startGame(state).block();
    return state;
  }

  private PlayGameResponse execute(PlayGameRequest request) {
    return execute(request, List.of());
  }

  private PlayGameResponse execute(PlayGameRequest request,
      List<CFlowStageCompletedListener<AnonymousAuthorization, PlayGameRequest, PlayGameResponse,
          PlayDragonsofmugloarGameCommand>> listeners) {
    var response = PlayDragonsofmugloarGameCommand.TYPE_DEFINITION.getStartExecutionRule().getFlow()
        .execute(new PlayDragonsofmugloarGameCommand(request), listeners, inboundPort, executor).block();
    assertNotNull(response);
    return response;
  }

  private static PlayGameRequest request(int turns, long time, long score) {
    return new PlayGameRequest(null, null, null, null, null, turns, time, score);
  }

  private static GameState advance(GameState state, long turn, long score) {
    return new GameState(state.gameId(), state.gold(), state.lives(), state.level(), turn, score, state.highScore(),
        state.lastInvestigatedReputation(), state.lastReputationInvestigationTurn(), state.startedAt(), state.lastAction());
  }
}
