package com.bmv.gateway.infrastructure.adapter.in;

import static com.bmv.flow.foundry.serialization.json.JsonUtility.toJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.bmv.authorization.config.AuthBmvConfig;
import com.bmv.flow.foundry.application.service.BmvServiceContext;
import com.bmv.flow.foundry.application.service.ServiceContextSupplier;
import com.bmv.permission.config.PermitBmvConfig;
import com.bmv.gateway.config.DragonsofmugloarGameConfig;
import com.bmv.gateway.config.BmvCacheConfig;
import com.bmv.gateway.config.BmvConfig;
import com.bmv.dragonsofmugloar.application.commandment.PlayDragonsofmugloarGameCommand;
import com.bmv.dragonsofmugloar.application.commandment.PlayGameTurnCommand;
import com.bmv.dragonsofmugloar.application.commandment.request.PlayGameRequest;
import com.bmv.dragonsofmugloar.application.commandment.request.PlayGameTurnRequest;
import com.bmv.dragonsofmugloar.application.commandment.response.PlayGameResponse;
import com.bmv.dragonsofmugloar.application.commandment.response.PlayGameTurnResponse;
import com.bmv.dragonsofmugloar.domain.model.Game;
import com.bmv.dragonsofmugloar.domain.model.GameConfig;
import com.bmv.dragonsofmugloar.domain.model.GameState;
import com.bmv.dragonsofmugloar.port.out.DragonsofmugloarPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Live integration tests that run game commands through the real {@link GatewayCommandmentRouter}
 * against <a href="https://dragonsofmugloar.com/api/v2">...</a>, without mocks or credentials.
 * The full-game test uses default settings with a one-minute game limit and prints the final response.
 * Run from the repository root with:
 * mvn -pl gateway-service -am -Dtest=PlayGameIT -Dsurefire.failIfNoSpecifiedTests=false test
 */
@Timeout(value = 5, unit = TimeUnit.MINUTES)
class PlayGameIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayGameIT.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(65);
    private static final Duration GAME_TIME_LIMIT = Duration.ofMinutes(1);
    private static final int TURNS = 5;

    private AnnotationConfigApplicationContext context;
    private GatewayCommandmentRouter router;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        new ConfigDataApplicationContextInitializer().initialize(context);
        context.register(DragonsofmugloarGameConfig.class, AuthBmvConfig.class, PermitBmvConfig.class);
        context.refresh();
        ServiceContextSupplier.resetServiceContext();
        ServiceContextSupplier.setServiceContext(new BmvServiceContext(context));

        var config = new BmvConfig();
        ReflectionTestUtils.setField(config, "commandmentExecutionTimeoutSec", GAME_TIME_LIMIT.toSeconds());
        router = new GatewayCommandmentRouter(
                null,
                null,
                null,
                null,
                null,
                new BmvCacheConfig(),
                config,
                "TEST_SECRET",
                null
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        ServiceContextSupplier.resetServiceContext();
        router.destroy();
        context.close();
    }

    @Test
    void playsGameWithDefaultsAndOneMinuteLimitAndPrintsResults() {
        var request = new PlayGameRequest(null, null, null, null, null, null, GAME_TIME_LIMIT.toMillis(), null);
        var command = new PlayDragonsofmugloarGameCommand(request);

        PlayGameResponse response = router.execute(command).block(GAME_TIME_LIMIT);

        LOGGER.info("Final game response (default settings, one-minute limit): {}", toJson(response));
        assertNotNull(response, "The router must return the game result");
        assertNull(response.errorDetails(), () -> "Game execution failed: " + response.errorDetails());
        assertTrue(response.turn() > 0, "The game must execute at least one turn");
    }

    @Test
    void playsSequentialTurnsOfANewlyStartedGame() {
        Game game = context.getBean(DragonsofmugloarPort.class).startGame().block(TIMEOUT);
        assertNotNull(game);
        var gameConfig = new GameConfig();
        PlayGameTurnResponse response = new PlayGameTurnResponse(new GameState(game.gameId(), game.gold(), game.lives(),
                game.level(), game.turn(), game.score(), game.highScore()), null);

        for (int turn = 1; turn <= TURNS; turn++) {
            var command = new PlayGameTurnCommand(new PlayGameTurnRequest(response.gameState(), gameConfig));
            router.getInternalAuthorizationService().assignInternalToken(command);

            response = router.execute(command).block(TIMEOUT);

            assertNotNull(response, "The router must return the turn result");
            PlayGameTurnResponse turnResponse = response;
            assertNull(turnResponse.errorDetails(), () -> "Turn execution failed: " + turnResponse.errorDetails());
            assertNotNull(turnResponse.gameState());
            assertEquals(game.gameId(), turnResponse.gameState().gameId());
            assertNotNull(turnResponse.gameState().lastAction(), "The executed action must be recorded");
        }

        LOGGER.info("Final response after {} turns: {}", TURNS, toJson(response));
    }
}
