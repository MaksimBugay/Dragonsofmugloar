package com.bmv.gateway.config;

import java.time.Duration;
import java.util.Locale;

import com.bmv.flow.foundry.application.lock.LockProviderWithRedis;
import com.bmv.flow.foundry.application.lock.LockService;
import com.bmv.flow.foundry.application.lock.local.LocalLockProvider;
import com.bmv.flow.foundry.application.port.cache.BasicCacheWithNullableValues;
import com.bmv.flow.foundry.application.port.cache.BasicCacheWithRedis;
import com.bmv.flow.foundry.application.port.cache.LocalBasicCache;
import com.bmv.flow.foundry.application.port.out.async.AsyncCommandmentExecutionAdapter;
import com.bmv.flow.foundry.application.service.BmvServiceContext;
import com.bmv.flow.foundry.application.service.ServiceContextSupplier;
import com.bmv.flow.foundry.application.service.gateway.AuthorizeWithPermissionCheckService;
import com.bmv.flow.foundry.application.service.gateway.InvalidateCommandmentCacheInternalService;
import com.bmv.flow.foundry.cluster.BmvLoadBalancer;
import com.bmv.flow.foundry.cluster.BmvLoadBalancerWithRedis;
import com.bmv.flow.foundry.cluster.BmvServiceDiscoveryClientWithRedis;
import com.bmv.flow.foundry.domain.model.api.DefaultLocaleSupplier;
import com.bmv.flow.foundry.domain.model.cluster.BmvService;
import com.bmv.gateway.application.lock.NoLockLockProvider;
import com.bmv.gateway.infrastructure.adapter.in.cache.GatewayConditionalCommandmentExecutionCache;
import com.bmv.gateway.infrastructure.adapter.in.cache.caffeine.CaffeineCommandmentExecutionCache;
import com.bmv.gateway.infrastructure.adapter.in.cache.redis.RedisCommandmentExecutionCache;
import com.bmv.gateway.infrastructure.adapter.out.http.SimpleDiscoveryService;
import com.bmv.flow.foundry.infrastructure.adapter.out.ai.azure.gpt_5_4.AiAgentToolAzureGpt54OutboundPort;
import com.bmv.flow.foundry.infrastructure.adapter.out.ai.azure.gpt_5_4.conf.AzureAiGpt54Config;
import com.bmv.flow.foundry.infrastructure.adapter.out.http.DiscoveryService;
import com.bmv.flow.foundry.redis.BmvRedisService;
import com.bmv.flow.foundry.redis.RedisConnectionManager;
import com.bmv.flow.foundry.serialization.BmvRedisJsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class RdmConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(RdmConfig.class);

  @Value("${rdm.application.default-locale:en}")
  private String defaultLocale;

  @Value("${rdm.application.commandment-execution-timeout-sec:600}")
  private long commandmentExecutionTimeoutSec;

  @Value("${rdm.error.base-uri:https://developers.dev.trimble-transportation.com/Data_Governance_RDM/Reference_Data_Management_Service/how-tos}")
  private String errorResponseBaseUri;

  @Value("${rdm.ai.cloud.azure.gpt-54.url:}")
  private String azureAiCloudGpt54Url;

  @Value("${rdm.ai.cloud.azure.gpt-54.access-key:}")
  private String azureAiCloudGpt54AccessKey;

  @EventListener(ApplicationReadyEvent.class)
  public void initializeDefaultLocaleAndServiceContextSupplier(ApplicationReadyEvent applicationReadyEvent) {
    DefaultLocaleSupplier.resetDefaultLocale();
    DefaultLocaleSupplier.setDefaultLocale(
        new Locale.Builder()
            .setLanguage(defaultLocale)
            .build()
    );

    ServiceContextSupplier.setServiceContext(
        new BmvServiceContext(applicationReadyEvent.getApplicationContext())
    );
  }

  // ----------------------------------------------------------------------------
  // Ports
  // ----------------------------------------------------------------------------

  @Bean
  public AiAgentToolAzureGpt54OutboundPort aiAgentToolAzureGpt54OutboundPort() {
    return new AiAgentToolAzureGpt54OutboundPort(
        new AzureAiGpt54Config() {
          @Override
          public String accessToken() {
            return azureAiCloudGpt54AccessKey;
          }

          @Override
          public String apiUrl() {
            return azureAiCloudGpt54Url;
          }
        }
    );
  }

  // ----------------------------------------------------------------------------
  // Async commandment execution
  // ----------------------------------------------------------------------------

  @Bean
  @ConditionalOnProperty(prefix = "bmv.redis", name = "enabled", havingValue = "true")
  public BasicCacheWithNullableValues pendingResponseStorage(RedisConnectionManager connectionManager) {
    return new BasicCacheWithNullableValues(
        new BmvRedisJsonSerializer(),
        rdmSerializer -> new BasicCacheWithRedis(rdmSerializer, connectionManager)
    );
  }

  @Bean
  @ConditionalOnMissingBean(name = "pendingResponseStorage")
  public BasicCacheWithNullableValues pendingResponseStorageLocal() {
    return new BasicCacheWithNullableValues(
        new BmvRedisJsonSerializer(),
        _ -> new LocalBasicCache()
    );
  }

  @Bean
  public AsyncCommandmentExecutionAdapter asyncCommandmentExecutionAdapter(
      BasicCacheWithNullableValues pendingResponseStorage,
      LockService lockService) {
    return new AsyncCommandmentExecutionAdapter(pendingResponseStorage, lockService);
  }

  // ----------------------------------------------------------------------------
  // Services
  // ----------------------------------------------------------------------------

  @Bean
  public AuthorizeWithPermissionCheckService authorizeWithPermissionCheckService() {
    return new AuthorizeWithPermissionCheckService();
  }

  @Bean
  public InvalidateCommandmentCacheInternalService invalidateCommandmentCacheInternalService() {
    return new InvalidateCommandmentCacheInternalService();
  }

  // ----------------------------------------------------------------------------
  // Distributed lock
  // ----------------------------------------------------------------------------

  @Bean
  @ConditionalOnProperty(prefix = "rdm.cmd-execution-lock", name = "disabled", havingValue = "true")
  public LockService noLockLockService() {
    LOGGER.info("Initializing dummy skip locking LockService with NoLock provider");
    return new LockService(new NoLockLockProvider());
  }

  @Bean
  @ConditionalOnProperty(prefix = "bmv.redis", name = "enabled", havingValue = "true")
  @ConditionalOnMissingBean(LockService.class)
  public LockService lockServiceWithRedis(BmvRedisService rdmRedisService) {
    LOGGER.info("Initializing LockService with Redis provider");
    return new LockService(
        new LockProviderWithRedis(rdmRedisService)
    );
  }

  @Bean
  @ConditionalOnMissingBean(LockService.class)
  public LockService lockService() {
    LOGGER.info("Initializing LockService with Local provider");
    return new LockService(new LocalLockProvider());
  }

  // ----------------------------------------------------------------------------
  // Service discovery
  // ----------------------------------------------------------------------------

  @Bean
  @ConditionalOnMissingBean(DiscoveryService.class)
  public DiscoveryService simpleDiscoveryService(RdmServiceDiscoveryConfig rdmServiceDiscoveryConfig) {
    return new SimpleDiscoveryService(rdmServiceDiscoveryConfig);
  }

  @Bean
  @ConditionalOnProperty(prefix = "bmv.redis", name = "enabled", havingValue = "true")
  public BmvServiceDiscoveryClientWithRedis rdmServiceDiscoveryClient(BmvRedisService rdmRedisService) {
    return new BmvServiceDiscoveryClientWithRedis(BmvService.GATEWAY, rdmRedisService, 5);
  }

  // ----------------------------------------------------------------------------
  // Load Balancer
  // ----------------------------------------------------------------------------

  @Bean
  @ConditionalOnProperty(prefix = "bmv.redis", name = "enabled", havingValue = "true")
  public BmvLoadBalancer rdmLoadBalancer(BmvRedisService rdmRedisService) {
    return new BmvLoadBalancerWithRedis(
        rdmRedisService,
        10
    );
  }

  // ----------------------------------------------------------------------------
  // Caches
  // ----------------------------------------------------------------------------

  @Bean
  @Qualifier(value = "RedisCommandmentExecutionCache")
  @ConditionalOnProperty(prefix = "bmv.redis", name = "enabled", havingValue = "true")
  public RedisCommandmentExecutionCache redisCommandmentExecutionCache(RedisConnectionManager connectionManager) {
    return new RedisCommandmentExecutionCache(connectionManager);
  }

  @Bean
  @Qualifier(value = "GatewayConditionalCommandmentExecutionCache")
  public GatewayConditionalCommandmentExecutionCache gatewayConditionalCommandmentExecutionCache(
      CaffeineCommandmentExecutionCache caffeineCommandmentExecutionCache,
      ApplicationContext applicationContext
  ) {
    return new GatewayConditionalCommandmentExecutionCache(
        caffeineCommandmentExecutionCache,
        applicationContext
    );
  }

  public Duration getCommandmentExecutionTimeout() {
    return Duration.ofSeconds(commandmentExecutionTimeoutSec);
  }

  public String getErrorResponseBaseUri() {
    return errorResponseBaseUri;
  }
}
