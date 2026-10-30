package com.bmv.gateway.infrastructure.adapter.out.http;

import com.bmv.flow.foundry.cluster.BmvLoadBalancer;
import com.bmv.flow.foundry.cluster.BmvLoadBalancerWithRedis;
import com.bmv.flow.foundry.cluster.BmvServiceInstance;
import com.bmv.flow.foundry.domain.model.cluster.BmvService;
import com.bmv.gateway.config.RdmServiceDiscoveryConfig;
import com.bmv.flow.foundry.infrastructure.adapter.out.http.DiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


//@Component
@SuppressWarnings("unused")
@ConditionalOnProperty(prefix = "bmv.redis", name = "enabled", havingValue = "true")
public class RdmDiscoveryService implements DiscoveryService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RdmDiscoveryService.class);

  private final BmvLoadBalancer rdmLoadBalancer;

  private final SimpleDiscoveryService simpleDiscoveryService;

  public RdmDiscoveryService(RdmServiceDiscoveryConfig rdmServiceDiscoveryConfig, BmvLoadBalancer rdmLoadBalancer) {
    this.rdmLoadBalancer = rdmLoadBalancer;
    this.simpleDiscoveryService = new SimpleDiscoveryService(rdmServiceDiscoveryConfig);
  }

  @Override
  public Mono<String> getAccessEndpointUrl(BmvService rdmService) {
    if (rdmLoadBalancer instanceof BmvLoadBalancerWithRedis rdmLoadBalancerWithRedis && !rdmLoadBalancerWithRedis.isActivated()) {
      LOGGER.warn("RDM load balancer is not activated, static fallback discovery was applied");
      return getAccessEndpointUrlFallback(rdmService);
    }

    return rdmLoadBalancer.getInstance(rdmService)
        .switchIfEmpty(
            getAccessEndpointUrlFallback(rdmService)
                .doOnNext(
                    url -> LOGGER.warn("Service discovery static fallback was applied for service {}: {}", rdmService.name(), url)
                )
                .map(url -> new BmvServiceInstance(rdmService, null, url))
        )
        .map(this::buildInternalApiRestUrl);
  }

  @Override
  public Flux<String> getInstanceAccessEndpointUrls(BmvService rdmService) {
    return null;
  }

  private Mono<String> getAccessEndpointUrlFallback(BmvService rdmService) {
    return simpleDiscoveryService.getAccessEndpointUrl(rdmService);
  }

  private String buildInternalApiRestUrl(BmvServiceInstance instance) {
    if (instance.baseHttpApiUrl() != null) {
      return instance.baseHttpApiUrl();
    }
    LOGGER.info("Resolved access endpoint url for service {}: {}", instance.service(), instance.directIp());
    return "http://%s:8080/internal-api".formatted(instance.directIp());
  }
}
