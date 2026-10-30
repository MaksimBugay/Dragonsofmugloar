package com.bmv.dragonsofmugloar.application.commandment.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bmv.flow.foundry.application.port.in.InternalApiInboundPort;
import com.bmv.flow.foundry.application.service.BmvServiceContext;
import com.bmv.flow.foundry.application.service.ServiceContextSupplier;
import com.bmv.flow.foundry.application.core.cflow.CFlowStageCompletedListener;
import com.bmv.flow.foundry.application.core.ExecutionRuleWithLinkedService;
import com.bmv.flow.foundry.application.core.ExecutionRuleWithLinkedServiceAndConditionalStage;
import com.bmv.flow.foundry.domain.model.auth.BmvRole;
import com.bmv.flow.foundry.domain.model.auth.BmvServiceAuthorization;
import com.bmv.flow.foundry.domain.model.cluster.BmvService;
import com.bmv.dragonsofmugloar.application.service.ChooseGameActionService;
import com.bmv.dragonsofmugloar.application.service.PerformGameActionService;
import com.bmv.dragonsofmugloar.application.service.PlayGameTurnService;
import com.bmv.dragonsofmugloar.application.commandment.command.PlayGameTurnCommand.Buffer;
import com.bmv.dragonsofmugloar.application.commandment.command.definition.PlayGameTurnCommandDefinition;
import com.bmv.dragonsofmugloar.application.commandment.request.PlayGameTurnRequest;
import com.bmv.dragonsofmugloar.application.commandment.response.PlayGameTurnResponse;
import com.bmv.dragonsofmugloar.domain.model.Ad;
import com.bmv.dragonsofmugloar.domain.model.AdProbability;
import com.bmv.dragonsofmugloar.domain.model.GameConfig;
import com.bmv.dragonsofmugloar.domain.model.GameState;
import com.bmv.dragonsofmugloar.domain.model.PurchaseResult;
import com.bmv.dragonsofmugloar.domain.model.Reputation;
import com.bmv.dragonsofmugloar.domain.model.ShopItem;
import com.bmv.dragonsofmugloar.domain.model.ShopItemId;
import com.bmv.dragonsofmugloar.domain.model.SolveAdResult;
import com.bmv.dragonsofmugloar.domain.model.GameAction;
import com.bmv.flow.foundry.infrastructure.adapter.in.CFlowStageCommandmentExecutor;
import com.bmv.dragonsofmugloar.port.out.DragonsofmugloarPort;
import com.bmv.flow.foundry.serialization.json.JsonUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import reactor.core.publisher.Mono;

class PlayGameTurnCommandTest {

  private static final GameConfig CONFIG = new GameConfig(100, 60_000);
  private static final ShopItem HEALING = new ShopItem(ShopItemId.HEALING_POTION, "Healing potion", 50);
  private static final ShopItem UPGRADE = new ShopItem(ShopItemId.CLAW_SHARPENING, "Claw sharpening", 100);
  private static final Ad AD = new Ad("live", "Help a farmer", 100, 2, null, AdProbability.SURE_THING);
  private static final Ad EXPIRED = new Ad("expired", "Help a farmer", 10_000, 0, null, AdProbability.SURE_THING);

  private final DragonsofmugloarPort port = mock(DragonsofmugloarPort.class);
  private final InternalApiInboundPort inboundPort = mock(InternalApiInboundPort.class);
  private final ChooseGameActionService chooser = new ChooseGameActionService();
  private GenericApplicationContext serviceContext;
  private CFlowStageCommandmentExecutor stageExecutor;

  @BeforeEach
  void setUp() {
    serviceContext = new GenericApplicationContext();
    serviceContext.registerBean(ChooseGameActionService.class, () -> chooser);
    serviceContext.registerBean(PlayGameTurnService.class,
        () -> new PlayGameTurnService(port, chooser, new PerformGameActionService(port)));
    serviceContext.refresh();
    ServiceContextSupplier.resetServiceContext();
    ServiceContextSupplier.setServiceContext(new BmvServiceContext(serviceContext));
    when(inboundPort.getBmvService()).thenReturn(BmvService.GATEWAY);
    when(inboundPort.getCommandmentExecutionTimeout()).thenReturn(Duration.ofSeconds(5));
    stageExecutor = new CFlowStageCommandmentExecutor(BmvService.GATEWAY, null);
  }

  @AfterEach
  void tearDown() {
    ServiceContextSupplier.resetServiceContext();
    serviceContext.close();
  }

  @Test
  void healingReturnsThePurchaseStateWithoutFetchingAdsOrBuyingTwice() {
    var input = gameState(1, 50, GameAction.BUY_HEALING_POTION);
    when(port.getShopItems("game")).thenReturn(Mono.just(List.of(HEALING)));
    when(port.buyItem("game", ShopItemId.HEALING_POTION)).thenReturn(Mono.just(new PurchaseResult(true, 0, 2, 4, 21)));

    var listener = new CFlowStageCompletedListener<BmvServiceAuthorization, PlayGameTurnRequest,
        PlayGameTurnResponse, PlayGameTurnCommand>(PlayGameTurnCommandDefinition.CHOOSE_CRITICAL_HEALING, state -> {
          assertNull(state.getResponseObject());
          assertEquals(PlayGameTurnCommandDefinition.EXECUTE_ACTION, state.getStageNumber());
          var context = ((PlayGameTurnCommandDefinition.State) state).getMutableBuffer().executionContext();
          assertEquals(GameAction.BUY_HEALING_POTION, context.gameAction());
          assertEquals(HEALING, context.shopItem());
          verify(port, never()).buyItem(anyString(), any());
        });
    var response = execute(input, List.of(listener));

    assertEquals(new GameState("game", 0, 2, 4, 21, 500, 900, null, 20L, 1_000,
        GameAction.BUY_HEALING_POTION), response.gameState());
    assertNull(response.errorDetails());
    var order = inOrder(port);
    order.verify(port).getShopItems("game");
    order.verify(port).buyItem("game", ShopItemId.HEALING_POTION);
    verifyNoMoreInteractions(port);
  }

  @Test
  void normalFlowFetchesShopThenAdsAndExecutesOneFilteredAction() {
    when(port.getShopItems("game")).thenReturn(Mono.just(List.of(UPGRADE, HEALING)));
    when(port.getAds("game")).thenReturn(Mono.just(List.of(EXPIRED, AD)));
    when(port.solveAd("game", "live")).thenReturn(Mono.just(new SolveAdResult(true, 3, 220, 600, 900, 21, "done")));

    var snapshots = new ArrayList<Buffer>();
    var listener = new CFlowStageCompletedListener<BmvServiceAuthorization, PlayGameTurnRequest,
        PlayGameTurnResponse, PlayGameTurnCommand>(PlayGameTurnCommandDefinition.CHOOSE_ACTION,
        state -> snapshots.add(((PlayGameTurnCommandDefinition.State) state).getMutableBuffer()));
    var response = execute(gameState(3, 120, null), List.of(listener));

    assertEquals(GameAction.SOLVE_PROFITABLE_AD, response.gameState().lastAction());
    assertEquals(600, response.gameState().score());
    assertNull(response.errorDetails());
    assertEquals(List.of(AD), snapshots.getFirst().executionContext().ads());
    assertEquals(List.of(AD), snapshots.getFirst().ads());
    var order = inOrder(port);
    order.verify(port).getShopItems("game");
    order.verify(port).getAds("game");
    order.verify(port).solveAd("game", "live");
    verifyNoMoreInteractions(port);
  }

  @Test
  void unavailableHealingFallsThroughToAdsOnTheLastLife() {
    when(port.getShopItems("game")).thenReturn(Mono.just(List.of(UPGRADE)));
    when(port.getAds("game")).thenReturn(Mono.just(List.of(AD)));
    when(port.solveAd("game", "live")).thenReturn(Mono.just(new SolveAdResult(true, 1, 150, 600, 900, 21, "done")));

    assertEquals(GameAction.SOLVE_SAFEST_AD_ON_LAST_LIFE, execute(gameState(1, 50, null)).gameState().lastAction());
    verify(port, never()).buyItem(anyString(), any());
  }

  @Test
  void repeatedEmptyBoardUsesTheBufferedShopForRecovery() {
    when(port.getShopItems("game")).thenReturn(Mono.just(List.of(UPGRADE, HEALING)));
    when(port.getAds("game")).thenReturn(Mono.just(List.of(EXPIRED)));
    when(port.buyItem("game", ShopItemId.CLAW_SHARPENING)).thenReturn(Mono.just(new PurchaseResult(true, 0, 3, 5, 21)));

    var response = execute(gameState(3, 100, GameAction.SKIP_TURN_NO_AVAILABLE_ADS));

    assertEquals(GameAction.BUY_UPGRADE, response.gameState().lastAction());
    assertEquals(0, response.gameState().gold());
    var order = inOrder(port);
    order.verify(port).getShopItems("game");
    order.verify(port).getAds("game");
    order.verify(port).buyItem("game", ShopItemId.CLAW_SHARPENING);
    verifyNoMoreInteractions(port);
  }

  @Test
  void repeatedEmptyBoardWithoutPurchasesRefreshesReputation() {
    var reputation = new Reputation(1.5, -2.0, 0.5);
    when(port.getShopItems("game")).thenReturn(Mono.just(List.of()));
    when(port.getAds("game")).thenReturn(Mono.just(List.of(EXPIRED)));
    when(port.investigateReputation("game")).thenReturn(Mono.just(reputation));

    var response = execute(gameState(3, 0, GameAction.SKIP_TURN_NO_AVAILABLE_ADS));

    assertEquals(new GameState("game", 0, 3, 4, 21, 500, 900, reputation, 21L, 1_000,
        GameAction.REFRESH_REPUTATION), response.gameState());
    verify(port, never()).buyItem(anyString(), any());
    verify(port, never()).solveAd(anyString(), anyString());
  }

  @Test
  void shopFailureReturnsAnErrorAndDoesNotFetchAds() {
    when(port.getShopItems("game")).thenReturn(Mono.error(new IllegalStateException("shop failed")));

    var response = execute(gameState(3, 120, null));

    assertTrue(response.hasError());
    assertNull(response.gameState());
    verify(port).getShopItems("game");
    verifyNoMoreInteractions(port);
  }

  @Test
  void failedPurchaseReturnsAnErrorWithoutRetryingOrFetchingAds() {
    when(port.getShopItems("game")).thenReturn(Mono.just(List.of(HEALING)));
    when(port.buyItem("game", ShopItemId.HEALING_POTION)).thenReturn(Mono.error(new IllegalStateException("connection lost")));

    var response = execute(gameState(1, 50, null));

    assertTrue(response.hasError());
    verify(port).buyItem("game", ShopItemId.HEALING_POTION);
    verify(port, never()).getAds(anyString());
  }

  @Test
  void bufferSnapshotsAreImmutableAndSurviveAssemblyAndSerialization() {
    var command = new PlayGameTurnCommand(new PlayGameTurnRequest(gameState(3, 150, null), CONFIG));
    var state = PlayGameTurnCommand.TYPE_DEFINITION.createState(command, Map.of(), List.of(), inboundPort, stageExecutor);
    var items = new ArrayList<>(List.of(UPGRADE));
    var original = state.getMutableBuffer();
    var withShop = original.withShopItems(items);
    items.clear();
    var withAds = withShop.withAds(List.of(AD));
    var context = chooser.chooseGameAction(CONFIG, command.getRequestObject().gameState(), List.of(AD), withShop.shopItems());
    state.setBuffer(withAds.withExecutionContext(context));
    state.updateStageNumber(_ -> PlayGameTurnCommandDefinition.EXECUTE_ACTION);

    var assembled = state.assembleCommandment();
    var restored = JsonUtility.fromJson(JsonUtility.toJson(assembled), PlayGameTurnCommand.class);

    assertTrue(original.shopItems().isEmpty());
    assertTrue(withShop.ads().isEmpty());
    assertEquals(List.of(UPGRADE), withShop.shopItems());
    assertThrows(UnsupportedOperationException.class, () -> withShop.shopItems().clear());
    assertThrows(UnsupportedOperationException.class, () -> withAds.ads().clear());
    assertEquals(command.getId(), restored.getId());
    assertEquals(command.getRequestObject(), restored.getRequestObject());
    assertInstanceOf(BmvServiceAuthorization.class, restored.getAuthorization());
    assertEquals(command.getAuthorization().getId(), restored.getAuthorization().getId());
    assertEquals(Set.of(BmvRole.BMV_CLUSTER_SERVICE), restored.getAuthorization().getRoles());
    assertEquals(state.getMutableBuffer(), restored.getMutableBuffer());
    assertEquals(PlayGameTurnCommandDefinition.EXECUTE_ACTION, restored.getStageNumber());
    assertEquals(Buffer.empty(), command.getMutableBuffer());
    assertSame(PlayGameTurnCommand.TYPE_DEFINITION, restored.getCommandmentDefinition());
  }

  @Test
  void allFiveStagesUseLinkedServiceRulesAndHealingUsesAConditionalStage() {
    var definition = PlayGameTurnCommand.TYPE_DEFINITION;

    assertEquals(5, definition.getTotalNumberOfStages());
    assertEquals(BmvServiceAuthorization.class, definition.getAuthorizationClass());
    assertEquals(Set.of(BmvRole.BMV_CLUSTER_SERVICE), definition.getAcceptedRoles());
    for (int stage = 1; stage <= 5; stage++) {
      assertInstanceOf(ExecutionRuleWithLinkedService.class, definition.getExecutionRuleByStageNumber(stage));
    }
    assertInstanceOf(ExecutionRuleWithLinkedServiceAndConditionalStage.class,
        definition.getExecutionRuleByStageNumber(PlayGameTurnCommandDefinition.CHOOSE_CRITICAL_HEALING));
  }

  private PlayGameTurnResponse execute(GameState gameState) {
    return execute(gameState, List.of());
  }

  private PlayGameTurnResponse execute(GameState gameState,
                                       List<CFlowStageCompletedListener<BmvServiceAuthorization, PlayGameTurnRequest,
                                           PlayGameTurnResponse, PlayGameTurnCommand>> listeners) {
    var command = new PlayGameTurnCommand(new PlayGameTurnRequest(gameState, CONFIG));
    var response = PlayGameTurnCommand.TYPE_DEFINITION.getStartExecutionRule().getFlow()
        .execute(command, listeners, inboundPort, stageExecutor).block();
    assertNotNull(response);
    return response;
  }

  private static GameState gameState(int lives, long gold, GameAction lastAction) {
    return new GameState("game", gold, lives, 4, 20, 500, 900, null, 20L, 1_000, lastAction);
  }
}
