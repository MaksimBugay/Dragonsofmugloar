package com.bmv.gateway.infrastructure.adapter.out.http;

import com.trimble.transportation.reference.data.management.cluster.RdmLoadBalancer;
import com.trimble.transportation.reference.data.management.cluster.RdmLoadBalancerWithRedis;
import com.trimble.transportation.reference.data.management.cluster.RdmServiceInstance;
import com.trimble.transportation.reference.data.management.domain.model.cluster.RdmService;
import com.bmv.gateway.config.RdmServiceDiscoveryConfig;
import com.trimble.transportation.reference.data.management.infrastructure.adapter.out.http.DiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


//@Component
@SuppressWarnings("unused")
@ConditionalOnProperty(prefix = "rdm.redis", name = "enabled", havingValue = "true")
public class RdmDiscoveryService implements DiscoveryService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RdmDiscoveryService.class);

  private final RdmLoadBalancer rdmLoadBalancer;

  private final SimpleDiscoveryService simpleDiscoveryService;

  public RdmDiscoveryService(RdmServiceDiscoveryConfig rdmServiceDiscoveryConfig, RdmLoadBalancer rdmLoadBalancer) {
    this.rdmLoadBalancer = rdmLoadBalancer;
    this.simpleDiscoveryService = new SimpleDiscoveryService(rdmServiceDiscoveryConfig);
  }

  @Override
  public Mono<String> getAccessEndpointUrl(RdmService rdmService) {
    if (rdmLoadBalancer instanceof RdmLoadBalancerWithRedis rdmLoadBalancerWithRedis && !rdmLoadBalancerWithRedis.isActivated()) {
      LOGGER.warn("RDM load balancer is not activated, static fallback discovery was applied");
      return getAccessEndpointUrlFallback(rdmService);
    }

    return rdmLoadBalancer.getInstance(rdmService)
        .switchIfEmpty(
            getAccessEndpointUrlFallback(rdmService)
                .doOnNext(
                    url -> LOGGER.warn("Service discovery static fallback was applied for service {}: {}", rdmService.name(), url)
                )
                .map(url -> new RdmServiceInstance(rdmService, null, url))
        )
        .map(this::buildInternalApiRestUrl);
  }

  @Override
  public Flux<String> getInstanceAccessEndpointUrls(RdmService rdmService) {
    return null;
  }

  private Mono<String> getAccessEndpointUrlFallback(RdmService rdmService) {
    return simpleDiscoveryService.getAccessEndpointUrl(rdmService);
  }

  private String buildInternalApiRestUrl(RdmServiceInstance instance) {
    if (instance.baseHttpApiUrl() != null) {
      return instance.baseHttpApiUrl();
    }
    LOGGER.info("Resolved access endpoint url for service {}: {}", instance.service(), instance.directIp());
    return "http://%s:8080/internal-api".formatted(instance.directIp());
  }
}
