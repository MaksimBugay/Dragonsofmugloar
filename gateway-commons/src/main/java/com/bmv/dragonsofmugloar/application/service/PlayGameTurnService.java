package com.bmv.dragonsofmugloar.application.service;

import java.util.Objects;

import com.trimble.transportation.reference.data.management.application.service.ServiceContextSupplier;
import com.trimble.transportation.reference.data.management.domain.cflow.CFlowState;
import com.trimble.transportation.reference.data.management.domain.model.auth.RdmServiceAuthorization;
import com.bmv.dragonsofmugloar.domain.api.commandment.PlayGameTurnCommand;
import com.bmv.dragonsofmugloar.domain.api.commandment.definition.PlayGameTurnCommandDefinition.State;
import com.bmv.dragonsofmugloar.domain.api.request.PlayGameTurnRequest;
import com.bmv.dragonsofmugloar.domain.api.response.PlayGameTurnResponse;
import com.bmv.dragonsofmugloar.domain.model.GameActionWithExecutionContext;
import com.bmv.dragonsofmugloar.port.out.DragonsofmugloarPort;
import reactor.core.publisher.Mono;

public class PlayGameTurnService {

  private final DragonsofmugloarPort port;
  private final ChooseGameActionService chooser;
  private final PerformGameActionService performer;

  @SuppressWarnings("unused")
  public PlayGameTurnService() {
    this(ServiceContextSupplier.getObjectFromServiceContext(DragonsofmugloarPort.class),
        ServiceContextSupplier.getObjectFromServiceContext(ChooseGameActionService.class),
        ServiceContextSupplier.getObjectFromServiceContext(PerformGameActionService.class));
  }

  public PlayGameTurnService(DragonsofmugloarPort port, ChooseGameActionService chooser, PerformGameActionService performer) {
    this.port = port;
    this.chooser = chooser;
    this.performer = performer;
  }

  public Mono<PlayGameTurnResponse> fetchShopItems(
      CFlowState<RdmServiceAuthorization, PlayGameTurnRequest, PlayGameTurnResponse, PlayGameTurnCommand> flowState) {
    State state = (State) flowState;
    return port.getShopItems(state.getRequestObject().gameState().gameId())
        .doOnNext(items -> state.setBuffer(state.getMutableBuffer().withShopItems(items)))
        .then(Mono.empty());
  }

  public Mono<PlayGameTurnResponse> chooseCriticalHealing(
      CFlowState<RdmServiceAuthorization, PlayGameTurnRequest, PlayGameTurnResponse, PlayGameTurnCommand> flowState) {
    State state = (State) flowState;
    return Mono.fromRunnable(() -> {
      PlayGameTurnRequest request = state.getRequestObject();
      GameActionWithExecutionContext context = Objects.requireNonNull(chooser.chooseCriticalHealingAction(
          request.gameConfig(), request.gameState(), state.getMutableBuffer().shopItems()));
      state.setBuffer(state.getMutableBuffer().withExecutionContext(context));
    }).then(Mono.empty());
  }

  public Mono<PlayGameTurnResponse> fetchAds(
      CFlowState<RdmServiceAuthorization, PlayGameTurnRequest, PlayGameTurnResponse, PlayGameTurnCommand> flowState) {
    State state = (State) flowState;
    return port.getAds(state.getRequestObject().gameState().gameId())
        .map(ads -> ads.stream().filter(ad -> ad.expiresIn() > 0).toList())
        .doOnNext(ads -> state.setBuffer(state.getMutableBuffer().withAds(ads)))
        .then(Mono.empty());
  }

  public Mono<PlayGameTurnResponse> chooseAction(
      CFlowState<RdmServiceAuthorization, PlayGameTurnRequest, PlayGameTurnResponse, PlayGameTurnCommand> flowState) {
    State state = (State) flowState;
    return Mono.fromRunnable(() -> {
      PlayGameTurnRequest request = state.getRequestObject();
      GameActionWithExecutionContext context = chooser.chooseGameAction(request.gameConfig(), request.gameState(),
          state.getMutableBuffer().ads(), state.getMutableBuffer().shopItems());
      state.setBuffer(state.getMutableBuffer().withExecutionContext(context));
    }).then(Mono.empty());
  }

  public Mono<PlayGameTurnResponse> executeAction(
      CFlowState<RdmServiceAuthorization, PlayGameTurnRequest, PlayGameTurnResponse, PlayGameTurnCommand> flowState) {
    State state = (State) flowState;
    return performer.performGameAction(state.getRequestObject().gameState(), state.getMutableBuffer().executionContext())
        .map(gameState -> new PlayGameTurnResponse(gameState, null));
  }
}
