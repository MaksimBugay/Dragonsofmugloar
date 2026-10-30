package com.bmv.gateway.config;

import java.util.EnumMap;
import java.util.Map;

import com.bmv.flow.foundry.domain.model.cluster.BmvService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rdm.service.internal-api.url")
public class RdmServiceDiscoveryConfig {

  private final Map<BmvService, String> internalApiUrlRegistry = new EnumMap<>(BmvService.class);

  public RdmServiceDiscoveryConfig() {
    // Initialize with default values
    internalApiUrlRegistry.put(BmvService.GATEWAY, "http://rdm-gateway/internal-api");
    internalApiUrlRegistry.put(BmvService.DATA_READER, "http://rdm-datareader/internal-api");
    internalApiUrlRegistry.put(BmvService.DATA_MODIFIER, "http://rdm-datamodifier/internal-api");
    internalApiUrlRegistry.put(BmvService.SRA, "http://rdm-sra/internal-api");
    internalApiUrlRegistry.put(BmvService.TRANSLATION, "http://rdm-translation/internal-api");
    internalApiUrlRegistry.put(BmvService.ACCOUNT, "http://rdm-account/internal-api");
  }

  public Map<BmvService, String> getInternalApiUrlRegistry() {
    return internalApiUrlRegistry;
  }

  public void setGATEWAY(String url) {
    internalApiUrlRegistry.put(BmvService.GATEWAY, url);
  }

  public void setDATA_READER(String url) {
    internalApiUrlRegistry.put(BmvService.DATA_READER, url);
  }

  public void setDATA_MODIFIER(String url) {
    internalApiUrlRegistry.put(BmvService.DATA_MODIFIER, url);
  }

  public void setSRA(String url) {
    internalApiUrlRegistry.put(BmvService.SRA, url);
  }

  public void setTRANSLATION(String url) {
    internalApiUrlRegistry.put(BmvService.TRANSLATION, url);
  }

  public void setACCOUNT(String url) {
    internalApiUrlRegistry.put(BmvService.ACCOUNT, url);
  }

  public String getAccessEndpointUrl(BmvService rdmService) {
    return internalApiUrlRegistry.get(rdmService);
  }
}