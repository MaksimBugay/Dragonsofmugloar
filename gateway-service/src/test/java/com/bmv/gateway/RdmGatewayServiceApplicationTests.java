package com.bmv.gateway;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("integration-test")
class RdmGatewayServiceApplicationTests {

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("rdm.cache.expire-after-sec", () -> 3);
    registry.add("rdm.service.internal-api.http-web-client.poolSize", () -> 30);
    registry.add("rdm.authorization.data-sync-agent-ids", () -> "%s;%s".formatted(UUID.randomUUID(), UUID.randomUUID()));
  }

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void contextLoads() {
    assertNotNull(applicationContext, "The application context should have been loaded.");
  }

}
