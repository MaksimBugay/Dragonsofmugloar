package com.bmv.gateway.infrastructure.adapter.out.http;

import com.bmv.flow.foundry.cluster.BmvLoadBalancer;
import com.bmv.flow.foundry.cluster.BmvLoadBalancerWithRedis;
import com.bmv.flow.foundry.cluster.BmvServiceInstance;
import com.bmv.flow.foundry.domain.model.cluster.BmvService;
import com.bmv.gateway.config.BmvServiceDiscoveryConfig;
import com.bmv.flow.foundry.infrastructure.adapter.out.http.DiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


//@Component
@SuppressWarnings("unused")
@ConditionalOnProperty(prefix = "bmv.redis", name = "enabled", havingValue = "true")
public class BmvDiscoveryService implements DiscoveryService {

  private static final Logger LOGGER = LoggerFactory.getLogger(BmvDiscoveryService.class);

  private final BmvLoadBalancer bmvLoadBalancer;

  private final SimpleDiscoveryService simpleDiscoveryService;

  public BmvDiscoveryService(BmvServiceDiscoveryConfig bmvServiceDiscoveryConfig, BmvLoadBalancer bmvLoadBalancer) {
    this.bmvLoadBalancer = bmvLoadBalancer;
    this.simpleDiscoveryService = new SimpleDiscoveryService(bmvServiceDiscoveryConfig);
  }

  @Override
  public Mono<String> getAccessEndpointUrl(BmvService bmvService) {
    if (bmvLoadBalancer instanceof BmvLoadBalancerWithRedis bmvLoadBalancerWithRedis && !bmvLoadBalancerWithRedis.isActivated()) {
      LOGGER.warn("BMV load balancer is not activated, static fallback discovery was applied");
      return getAccessEndpointUrlFallback(bmvService);
    }

    return bmvLoadBalancer.getInstance(bmvService)
        .switchIfEmpty(
            getAccessEndpointUrlFallback(bmvService)
                .doOnNext(
                    url -> LOGGER.warn("Service discovery static fallback was applied for service {}: {}", bmvService.name(), url)
                )
                .map(url -> new BmvServiceInstance(bmvService, null, url))
        )
        .map(this::buildInternalApiRestUrl);
  }

  @Override
  public Flux<String> getInstanceAccessEndpointUrls(BmvService bmvService) {
    return null;
  }

  private Mono<String> getAccessEndpointUrlFallback(BmvService bmvService) {
    return simpleDiscoveryService.getAccessEndpointUrl(bmvService);
  }

  private String buildInternalApiRestUrl(BmvServiceInstance instance) {
    if (instance.baseHttpApiUrl() != null) {
      return instance.baseHttpApiUrl();
    }
    LOGGER.info("Resolved access endpoint url for service {}: {}", instance.service(), instance.directIp());
    return "http://%s:8080/internal-api".formatted(instance.directIp());
  }
}
