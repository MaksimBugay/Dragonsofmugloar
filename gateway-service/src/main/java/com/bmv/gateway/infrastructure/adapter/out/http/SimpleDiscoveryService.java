package com.bmv.gateway.infrastructure.adapter.out.http;

import com.bmv.flow.foundry.domain.model.cluster.BmvService;
import com.bmv.gateway.config.BmvServiceDiscoveryConfig;
import com.bmv.flow.foundry.infrastructure.adapter.out.http.DiscoveryService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SimpleDiscoveryService implements DiscoveryService {

  private final BmvServiceDiscoveryConfig bmvServiceDiscoveryConfig;

  public SimpleDiscoveryService(BmvServiceDiscoveryConfig bmvServiceDiscoveryConfig) {
    this.bmvServiceDiscoveryConfig = bmvServiceDiscoveryConfig;
  }

  @Override
  public Mono<String> getAccessEndpointUrl(BmvService bmvService) {

    return Mono.just(
        bmvServiceDiscoveryConfig.getAccessEndpointUrl(bmvService)
    );
  }

  @Override
  public Flux<String> getInstanceAccessEndpointUrls(BmvService bmvService) {
    return getAccessEndpointUrl(bmvService)
        .flatMapMany(
            Flux::just
        );
  }
}
