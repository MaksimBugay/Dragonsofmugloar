package com.bmv.gateway.config;

import java.util.EnumMap;
import java.util.Map;

import com.trimble.transportation.reference.data.management.domain.model.cluster.RdmService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rdm.service.internal-api.url")
public class RdmServiceDiscoveryConfig {

  private final Map<RdmService, String> internalApiUrlRegistry = new EnumMap<>(RdmService.class);

  public RdmServiceDiscoveryConfig() {
    // Initialize with default values
    internalApiUrlRegistry.put(RdmService.GATEWAY, "http://rdm-gateway/internal-api");
    internalApiUrlRegistry.put(RdmService.DATA_READER, "http://rdm-datareader/internal-api");
    internalApiUrlRegistry.put(RdmService.DATA_MODIFIER, "http://rdm-datamodifier/internal-api");
    internalApiUrlRegistry.put(RdmService.SRA, "http://rdm-sra/internal-api");
    internalApiUrlRegistry.put(RdmService.TRANSLATION, "http://rdm-translation/internal-api");
    internalApiUrlRegistry.put(RdmService.ACCOUNT, "http://rdm-account/internal-api");
  }

  public Map<RdmService, String> getInternalApiUrlRegistry() {
    return internalApiUrlRegistry;
  }

  public void setGATEWAY(String url) {
    internalApiUrlRegistry.put(RdmService.GATEWAY, url);
  }

  public void setDATA_READER(String url) {
    internalApiUrlRegistry.put(RdmService.DATA_READER, url);
  }

  public void setDATA_MODIFIER(String url) {
    internalApiUrlRegistry.put(RdmService.DATA_MODIFIER, url);
  }

  public void setSRA(String url) {
    internalApiUrlRegistry.put(RdmService.SRA, url);
  }

  public void setTRANSLATION(String url) {
    internalApiUrlRegistry.put(RdmService.TRANSLATION, url);
  }

  public void setACCOUNT(String url) {
    internalApiUrlRegistry.put(RdmService.ACCOUNT, url);
  }

  public String getAccessEndpointUrl(RdmService rdmService) {
    return internalApiUrlRegistry.get(rdmService);
  }
}