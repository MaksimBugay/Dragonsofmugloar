package com.bmv.gateway.infrastructure.adapter.out.http;

import com.trimble.transportation.reference.data.management.domain.model.cluster.RdmService;
import com.bmv.gateway.config.RdmServiceDiscoveryConfig;
import com.trimble.transportation.reference.data.management.infrastructure.adapter.out.http.DiscoveryService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SimpleDiscoveryService implements DiscoveryService {

  private final RdmServiceDiscoveryConfig rdmServiceDiscoveryConfig;

  public SimpleDiscoveryService(RdmServiceDiscoveryConfig rdmServiceDiscoveryConfig) {
    this.rdmServiceDiscoveryConfig = rdmServiceDiscoveryConfig;
  }

  @Override
  public Mono<String> getAccessEndpointUrl(RdmService rdmService) {

    return Mono.just(
        rdmServiceDiscoveryConfig.getAccessEndpointUrl(rdmService)
    );
  }

  @Override
  public Flux<String> getInstanceAccessEndpointUrls(RdmService rdmService) {
    return getAccessEndpointUrl(rdmService)
        .flatMapMany(
            Flux::just
        );
  }
}
