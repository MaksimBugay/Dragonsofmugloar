package com.bmv.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.trimble.transportation.reference.data.management.domain.model.cluster.RdmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SuppressWarnings("SpringBootApplicationProperties")
@SpringBootTest
@TestPropertySource(properties = {
    "rdm.service.internal-api.url.GATEWAY=http://test-gateway.internal/api",
    "rdm.service.internal-api.url.DATA_READER=http://test-datareader.internal/api",
    "rdm.service.internal-api.url.DATA_MODIFIER=http://test-datamodifier.internal/api",
    "rdm.service.internal-api.url.TRANSLATION=http://test-translation.internal/api",
    "rdm.service.internal-api.url.ACCOUNT=http://test-account.internal/api",
    "rdm.cluster.secret=TEST_SECRET"
})
@ActiveProfiles("test")
public class RdmServiceDiscoveryConfigTest {

  @Autowired
  private RdmServiceDiscoveryConfig rdmServiceDiscoveryConfig;

  @Test
  void getAccessEndpointUrlTest() {
    assertEquals("http://test-gateway.internal/api", rdmServiceDiscoveryConfig.getAccessEndpointUrl(RdmService.GATEWAY));
    assertEquals("http://test-datareader.internal/api", rdmServiceDiscoveryConfig.getAccessEndpointUrl(RdmService.DATA_READER));
    assertEquals("http://test-datamodifier.internal/api", rdmServiceDiscoveryConfig.getAccessEndpointUrl(RdmService.DATA_MODIFIER));
    assertEquals("http://ttc-rdm-sra-service:8080/internal-api", rdmServiceDiscoveryConfig.getAccessEndpointUrl(RdmService.SRA));
    assertEquals("http://test-translation.internal/api", rdmServiceDiscoveryConfig.getAccessEndpointUrl(RdmService.TRANSLATION));
    assertEquals("http://test-account.internal/api", rdmServiceDiscoveryConfig.getAccessEndpointUrl(RdmService.ACCOUNT));
  }
}
