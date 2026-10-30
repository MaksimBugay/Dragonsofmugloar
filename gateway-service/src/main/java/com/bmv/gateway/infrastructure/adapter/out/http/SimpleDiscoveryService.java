package com.bmv.gateway.infrastructure.adapter.out.http;

import com.bmv.flow.foundry.domain.model.cluster.BmvService;
import com.bmv.gateway.config.RdmServiceDiscoveryConfig;
import com.bmv.flow.foundry.infrastructure.adapter.out.http.DiscoveryService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SimpleDiscoveryService implements DiscoveryService {

  private final RdmServiceDiscoveryConfig rdmServiceDiscoveryConfig;

  public SimpleDiscoveryService(RdmServiceDiscoveryConfig rdmServiceDiscoveryConfig) {
    this.rdmServiceDiscoveryConfig = rdmServiceDiscoveryConfig;
  }

  @Override
  public Mono<String> getAccessEndpointUrl(BmvService rdmService) {

    return Mono.just(
        rdmServiceDiscoveryConfig.getAccessEndpointUrl(rdmService)
    );
  }

  @Override
  public Flux<String> getInstanceAccessEndpointUrls(BmvService rdmService) {
    return getAccessEndpointUrl(rdmService)
        .flatMapMany(
            Flux::just
        );
  }
}
