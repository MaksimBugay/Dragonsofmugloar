package com.bmv.gateway.infrastructure.adapter.out.http;

import com.bmv.flow.foundry.application.port.InternalApiPortMode;
import com.bmv.gateway.config.GatewayHttpWebClientConfig;
import com.bmv.flow.foundry.infrastructure.adapter.out.http.DiscoveryService;
import com.bmv.flow.foundry.infrastructure.adapter.out.http.HttpBasedCommandmentExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!integration-test")
@Component
@Qualifier(value = "GatewayCommandmentExecutor")
public class GatewayCommandmentExecutor extends HttpBasedCommandmentExecutor {

  public GatewayCommandmentExecutor(DiscoveryService discoveryService,
                                    @Value("${rdm.internal-api.port-mode:HTTP1}") InternalApiPortMode internalApiPortMode,
                                    GatewayHttpWebClientConfig httpWebClientConfig) {
    super(discoveryService, internalApiPortMode, httpWebClientConfig.getHttpWebClientConfig());
  }
}
