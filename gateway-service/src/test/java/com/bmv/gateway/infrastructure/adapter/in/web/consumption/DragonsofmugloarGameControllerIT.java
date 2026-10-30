package com.bmv.gateway.infrastructure.adapter.in.web.consumption;

import static com.trimble.transportation.reference.data.management.serialization.json.JsonUtility.toJson;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.trimble.transportation.reference.data.management.application.service.RdmServiceContext;
import com.trimble.transportation.reference.data.management.application.service.ServiceContextSupplier;
import com.trimble.transportation.reference.data.management.application.service.authorization.AuthorizationService;
import com.trimble.transportation.reference.data.management.application.service.gateway.AuthorizeWithPermissionCheckService;
import com.trimble.transportation.reference.data.management.application.service.permission.CheckPermissionsService;
import com.trimble.transportation.reference.data.management.application.service.permission.RdmCheckCommandmentPermissionsHelper;
import com.bmv.dragonsofmugloar.application.commandment.request.PlayGameRequest;
import com.bmv.dragonsofmugloar.application.commandment.response.PlayGameResponse;
import com.bmv.gateway.config.DragonsofmugloarGameConfig;
import com.bmv.gateway.config.RdmCacheConfig;
import com.bmv.gateway.config.RdmConfig;
import com.bmv.gateway.infrastructure.adapter.in.GatewayCommandmentRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * Live HTTP integration test with the real controller, gateway router and Dragons of Mugloar adapter.
 * Uses default game settings except for a one-minute game limit, without mocks or credentials.
 * Run from the repository root with:
 * mvn -pl gateway-service -am -Dtest=DragonsofmugloarGameControllerIT -Dsurefire.failIfNoSpecifiedTests=false test
 */
@Timeout(value = 3, unit = TimeUnit.MINUTES)
class DragonsofmugloarGameControllerIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(DragonsofmugloarGameControllerIT.class);
  private static final Duration GAME_TIME_LIMIT = Duration.ofMinutes(1);
  private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);

  @Test
  void playsGameWithDefaultsAndOneMinuteLimitThroughHttpAndPrintsResults() throws Exception {
    var request = new PlayGameRequest(null, null, null, null, null, null, GAME_TIME_LIMIT.toMillis(), null);
    var config = new RdmConfig();
    ReflectionTestUtils.setField(config, "commandmentExecutionTimeoutSec", REQUEST_TIMEOUT.toSeconds());
    var router = new GatewayCommandmentRouter(
        List.of(
            new AuthorizeWithPermissionCheckService(),
            new AuthorizationService(null, Set.of()),
            new CheckPermissionsService(new RdmCheckCommandmentPermissionsHelper())
        ),
        null, null, null, new RdmCacheConfig(), config, "TEST_SECRET", null);
    DisposableServer server = null;

    try (var context = new AnnotationConfigApplicationContext()) {
      new ConfigDataApplicationContextInitializer().initialize(context);
      context.getBeanFactory().registerSingleton("commandmentRouter", router);
      context.register(DragonsofmugloarGameConfig.class, WebConfig.class);
      context.registerBean(DragonsofmugloarGameController.class, () -> new DragonsofmugloarGameController(router));
      context.refresh();
      ServiceContextSupplier.resetServiceContext();
      ServiceContextSupplier.setServiceContext(new RdmServiceContext(context));
      server = HttpServer.create()
          .host("127.0.0.1")
          .port(0)
          .handle(new ReactorHttpHandlerAdapter(WebHttpHandlerBuilder.applicationContext(context).build()))
          .bindNow();

      PlayGameResponse response = WebTestClient.bindToServer()
          .baseUrl("http://127.0.0.1:" + server.port())
          .responseTimeout(REQUEST_TIMEOUT)
          .build()
          .post()
          .uri("/bmv/dragonsofmugloar/api/v1/autoplay")
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isOk()
          .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
          .expectBody(PlayGameResponse.class)
          .returnResult()
          .getResponseBody();

      LOGGER.info("Final endpoint response (default settings, one-minute limit): {}", toJson(response));
      assertNotNull(response, "The endpoint must return the game result as JSON");
      assertNull(response.errorDetails(), () -> "Game execution failed: " + response.errorDetails());
      assertNotNull(response.gameId());
      assertFalse(response.gameId().isBlank());
      assertTrue(response.turn() > 0, "The game must execute at least one turn");
    } finally {
      if (server != null) {
        server.disposeNow();
      }
      ServiceContextSupplier.resetServiceContext();
      router.destroy();
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebFlux
  static class WebConfig {
  }
}
