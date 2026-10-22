package com.bmv.gateway.infrastructure.adapter.out.http;

import java.nio.charset.StandardCharsets;

import com.trimble.transportation.reference.data.management.application.port.InternalApiPortMode;
import com.trimble.transportation.reference.data.management.application.service.authorization.InternalAuthorizationBasicService;
import com.trimble.transportation.reference.data.management.application.service.authorization.InternalAuthorizationService;
import com.trimble.transportation.reference.data.management.domain.model.api.Commandment;
import com.trimble.transportation.reference.data.management.domain.model.api.CommandmentWithDefinition;
import com.trimble.transportation.reference.data.management.domain.model.api.error.WithErrorDetails;
import com.trimble.transportation.reference.data.management.domain.model.auth.ClientAuthorization;
import com.bmv.gateway.config.GatewayHttpWebClientConfig;
import com.trimble.transportation.reference.data.management.infrastructure.adapter.out.http.DiscoveryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Profile("integration-test")
@Component
@Qualifier(value = "GatewayCommandmentExecutor")
public class GatewayCommandmentTestExecutor extends GatewayCommandmentExecutor {

  private final InternalAuthorizationService internalAuthorizationService;

  public GatewayCommandmentTestExecutor(DiscoveryService simpleDiscoveryService,
                                        @Value("${rdm.internal-api.port-mode:HTTP1}") InternalApiPortMode internalApiPortMode,
                                        GatewayHttpWebClientConfig httpWebClientConfig,
                                        @Value("${rdm.application.commandment-execution-timeout-sec:600}") long commandmentExecutionTimeout,
                                        @Value("${rdm.cluster.secret}") String rdmClusterSecret) {
    super(simpleDiscoveryService, internalApiPortMode, httpWebClientConfig);
    this.internalAuthorizationService = new InternalAuthorizationBasicService(
        commandmentExecutionTimeout,
        rdmClusterSecret.getBytes(StandardCharsets.UTF_8)
    );
  }

  @Override
  public <A extends ClientAuthorization, P, R extends WithErrorDetails, C extends Commandment<A, P, R>> Mono<R> executeNoCache(
      CommandmentWithDefinition<A, P, R, C> commandment) {

    //check that every commandment has an internal token before forwarding to another rdm service
    return internalAuthorizationService.authorize(commandment)
        .then(
            super.executeNoCache(commandment)
        );
  }
}
