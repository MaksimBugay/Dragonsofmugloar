package com.bmv.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bmv.flow.foundry.config.HttpWebClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "rdm.service.internal-api.http-web-client.poolSize=15",
    "rdm.service.internal-api.http-web-client.maxQueueSize=95",
    "rdm.service.internal-api.http-web-client.connectionTimeoutMs=100",
    "rdm.service.internal-api.http-web-client.readTimeoutMs=200",
    "rdm.cluster.secret=TEST_SECRET"
})
@ActiveProfiles("test")
public class GatewayHttpWebClientConfigTest {

  @Autowired
  private GatewayHttpWebClientConfig gatewayHttpWebClientConfig;

  @Test
  void getHttpWebClientConfigTest() {
    HttpWebClientConfig httpWebClientConfig = gatewayHttpWebClientConfig.getHttpWebClientConfig();
    assertEquals(15, httpWebClientConfig.poolSize());
    assertEquals(95, httpWebClientConfig.maxQueueSize());
    assertEquals(100, httpWebClientConfig.connectionTimeoutMs());
    assertEquals(200, httpWebClientConfig.readTimeoutMs());
    assertEquals(32 * 1024 * 1024, httpWebClientConfig.memoryBufferSize());//default value
  }
}
