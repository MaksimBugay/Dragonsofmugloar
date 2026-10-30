package com.bmv.gateway.infrastructure.adapter.in.web.rest;

import com.bmv.dragonsofmugloar.application.commandment.GenerateHiddenMessageQuery;
import com.bmv.dragonsofmugloar.application.commandment.request.BuildHiddenMessageRequest;
import com.bmv.dragonsofmugloar.application.commandment.response.HiddenMessageResponse;
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
public class HiddenMessageController {
    private static final Logger LOGGER = LoggerFactory.getLogger(HiddenMessageController.class);

    private final GatewayCommandmentRouter commandmentRouter;

    public HiddenMessageController(GatewayCommandmentRouter commandmentRouter) {
        this.commandmentRouter = commandmentRouter;
    }

    @PostMapping(path = "/generate-hidden-message", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<HiddenMessageResponse> generateHiddenMessage(@Valid @RequestBody Mono<BuildHiddenMessageRequest> requestMono) {
        return requestMono
                .flatMap(
                        buildHiddenMessageRequest -> commandmentRouter.execute(
                                new GenerateHiddenMessageQuery(buildHiddenMessageRequest)
                        )
                )
                .doOnNext(
                        playGameResponse -> LOGGER.info(
                                "New hidden message was generated: {}",
                                playGameResponse
                        )
                );
    }
}
