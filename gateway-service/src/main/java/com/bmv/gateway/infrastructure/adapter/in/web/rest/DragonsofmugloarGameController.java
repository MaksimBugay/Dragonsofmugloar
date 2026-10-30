package com.bmv.gateway.infrastructure.adapter.in.web.rest;

import com.bmv.dragonsofmugloar.application.commandment.command.PlayDragonsofmugloarGameCommand;
import com.bmv.dragonsofmugloar.application.commandment.request.PlayGameRequest;
import com.bmv.dragonsofmugloar.application.commandment.response.PlayGameResponse;
import com.bmv.gateway.infrastructure.adapter.in.GatewayCommandmentRouter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class DragonsofmugloarGameController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonsofmugloarGameController.class);

    private final GatewayCommandmentRouter commandmentRouter;

    public DragonsofmugloarGameController(GatewayCommandmentRouter commandmentRouter) {
        this.commandmentRouter = commandmentRouter;
    }

    @PostMapping(path = "/bmv/dragonsofmugloar/api/v1/autoplay", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<PlayGameResponse> playGame(@Valid @RequestBody Mono<PlayGameRequest> requestMono) {
        return requestMono
                .flatMap(
                        playGameRequest -> commandmentRouter.execute(
                                new PlayDragonsofmugloarGameCommand(playGameRequest)
                        )
                )
                .doOnNext(
                        playGameResponse -> LOGGER.info(
                                "Game results: {}",
                                playGameResponse
                        )
                );
    }
}
