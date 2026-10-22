package com.bmv.gateway.config;

import java.time.Duration;
import java.util.Locale;

import com.trimble.transportation.reference.data.management.application.lock.LockProviderWithRedis;
import com.trimble.transportation.reference.data.management.application.lock.LockService;
import com.trimble.transportation.reference.data.management.application.lock.local.LocalLockProvider;
import com.trimble.transportation.reference.data.management.application.port.cache.BasicCacheWithNullableValues;
import com.trimble.transportation.reference.data.management.application.port.cache.BasicCacheWithRedis;
import com.trimble.transportation.reference.data.management.application.port.cache.LocalBasicCache;
import com.trimble.transportation.reference.data.management.application.port.out.async.AsyncCommandmentExecutionAdapter;
import com.trimble.transportation.reference.data.management.application.service.RdmServiceContext;
import com.trimble.transportation.reference.data.management.application.service.ServiceContextSupplier;
import com.trimble.transportation.reference.data.management.application.service.current.RefRecordsValidationHelper;
import com.trimble.transportation.reference.data.management.application.service.gateway.AddRefTableOwnerGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.AdvancedSearchForRefTablesGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.AdvancedSearchForRefTablesInternalGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.ApproveRefTableRevisionGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.AssignReviewerToRefTableRevisionGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.AuthorizeWithPermissionCheckService;
import com.trimble.transportation.reference.data.management.application.service.gateway.CreateApplicationService;
import com.trimble.transportation.reference.data.management.application.service.gateway.CreateOrUpdateApplicationTableSubscriptionService;
import com.trimble.transportation.reference.data.management.application.service.gateway.CreateRefDataRevisionService;
import com.trimble.transportation.reference.data.management.application.service.gateway.CreateRefRecordsService;
import com.trimble.transportation.reference.data.management.application.service.gateway.CreateRefTableRevisionGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.DeleteApplicationOwnerGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.DeleteApplicationService;
import com.trimble.transportation.reference.data.management.application.service.gateway.DeleteApplicationTableSubscriptionGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.DeleteRefTableOwnerGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.DeleteUserInviteService;
import com.trimble.transportation.reference.data.management.application.service.gateway.DgGetRefRecordsService;
import com.trimble.transportation.reference.data.management.application.service.gateway.FindSimilarRefRecordsForColumnGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.FindSimilarRefRecordsService;
import com.trimble.transportation.reference.data.management.application.service.gateway.GetApplicationOwnersGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.GetApplicationTableSubscriptionsGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.GetApplicationsService;
import com.trimble.transportation.reference.data.management.application.service.gateway.GetRefDataRevisionRecordsService;
import com.trimble.transportation.reference.data.management.application.service.gateway.GetRefRecordByIdService;
import com.trimble.transportation.reference.data.management.application.service.gateway.GetRefRecordsService;
import com.trimble.transportation.reference.data.management.application.service.gateway.InvalidateCommandmentCacheInternalService;
import com.trimble.transportation.reference.data.management.application.service.gateway.PopulateSimilaritySearchDataForPageService;
import com.trimble.transportation.reference.data.management.application.service.gateway.PopulateSimilaritySearchDataForTableGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.RdmApplicationCommonsHelper;
import com.trimble.transportation.reference.data.management.application.service.gateway.RefTableHelper;
import com.trimble.transportation.reference.data.management.application.service.gateway.RefTableRevisionHelper;
import com.trimble.transportation.reference.data.management.application.service.gateway.RefTableService;
import com.trimble.transportation.reference.data.management.application.service.gateway.RejectRefTableRevisionGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.RemoveRefTableRevisionService;
import com.trimble.transportation.reference.data.management.application.service.gateway.ReturnRefTableRevisionGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.RevokeApplicationAccessKeyGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.SearchForPreparedRefRecordsGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.SendNotificationToTableOwnersService;
import com.trimble.transportation.reference.data.management.application.service.gateway.SraGatewayHelper;
import com.trimble.transportation.reference.data.management.application.service.gateway.SubmitRefTableRevisionForReviewService;
import com.trimble.transportation.reference.data.management.application.service.gateway.UnassignReviewerFromRefTableRevisionGatewayService;
import com.trimble.transportation.reference.data.management.application.service.gateway.UpdateRefDataRevisionService;
import com.trimble.transportation.reference.data.management.application.service.gateway.UpdateRefTableRevisionGatewayService;
import com.trimble.transportation.reference.data.management.application.service.translation.TranslationResolver;
import com.trimble.transportation.reference.data.management.cluster.RdmLoadBalancer;
import com.trimble.transportation.reference.data.management.cluster.RdmLoadBalancerWithRedis;
import com.trimble.transportation.reference.data.management.cluster.RdmServiceDiscoveryClientWithRedis;
import com.trimble.transportation.reference.data.management.domain.model.api.DefaultLocaleSupplier;
import com.trimble.transportation.reference.data.management.domain.model.cluster.RdmService;
import com.bmv.gateway.application.lock.NoLockLockProvider;
import com.bmv.gateway.infrastructure.adapter.in.cache.GatewayConditionalCommandmentExecutionCache;
import com.bmv.gateway.infrastructure.adapter.in.cache.caffeine.CaffeineCommandmentExecutionCache;
import com.bmv.gateway.infrastructure.adapter.in.cache.redis.RedisCommandmentExecutionCache;
import com.bmv.gateway.infrastructure.adapter.out.http.SimpleDiscoveryService;
import com.trimble.transportation.reference.data.management.infrastructure.adapter.out.ai.azure.gpt_5_4.AiAgentToolAzureGpt54OutboundPort;
import com.trimble.transportation.reference.data.management.infrastructure.adapter.out.ai.azure.gpt_5_4.conf.AzureAiGpt54Config;
import com.trimble.transportation.reference.data.management.infrastructure.adapter.out.http.DiscoveryService;
import com.trimble.transportation.reference.data.management.redis.RdmRedisService;
import com.trimble.transportation.reference.data.management.redis.RedisConnectionManager;
import com.trimble.transportation.reference.data.management.serialization.RdmRedisJsonSerializer;
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
        new RdmServiceContext(applicationReadyEvent.getApplicationContext())
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
  // Helpers
  // ----------------------------------------------------------------------------

  @Bean
  public RdmApplicationCommonsHelper rdmApplicationCommonsHelper() {
    return new RdmApplicationCommonsHelper();
  }

  @Bean
  public RefTableHelper refTableHelper(TranslationResolver translationResolver) {
    return new RefTableHelper(translationResolver);
  }

  @Bean
  public RefTableRevisionHelper refTableRevisionHelper(RefTableHelper refTableHelper, SraGatewayHelper sraGatewayHelper) {
    return new RefTableRevisionHelper(refTableHelper, sraGatewayHelper);
  }

  @Bean
  public SraGatewayHelper sraGatewayHelper(RefTableHelper refTableHelper) {
    return new SraGatewayHelper(refTableHelper);
  }

  // ----------------------------------------------------------------------------
  // Async commandment execution
  // ----------------------------------------------------------------------------

  @Bean
  @ConditionalOnProperty(prefix = "rdm.redis", name = "enabled", havingValue = "true")
  public BasicCacheWithNullableValues pendingResponseStorage(RedisConnectionManager connectionManager) {
    return new BasicCacheWithNullableValues(
        new RdmRedisJsonSerializer(),
        rdmSerializer -> new BasicCacheWithRedis(rdmSerializer, connectionManager)
    );
  }

  @Bean
  @ConditionalOnMissingBean(name = "pendingResponseStorage")
  public BasicCacheWithNullableValues pendingResponseStorageLocal() {
    return new BasicCacheWithNullableValues(
        new RdmRedisJsonSerializer(),
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
  public CreateApplicationService createApplicationService() {
    return new CreateApplicationService();
  }

  @Bean
  public CreateOrUpdateApplicationTableSubscriptionService createOrUpdateApplicationTableSubscriptionService() {
    return new CreateOrUpdateApplicationTableSubscriptionService();
  }

  @Bean
  public RefRecordsValidationHelper refRecordsValidationHelper() {
    return new RefRecordsValidationHelper();
  }

  @Bean
  public CreateRefRecordsService createRefRecordsService(TranslationResolver translationResolver,
                                                         RefRecordsValidationHelper refRecordsValidationHelper,
                                                         RefTableHelper refTableHelper) {
    return new CreateRefRecordsService(
        translationResolver,
        refRecordsValidationHelper,
        refTableHelper
    );
  }

  @Bean
  public DgGetRefRecordsService dgGetRefRecordsService() {
    return new DgGetRefRecordsService();
  }

  @Bean
  public GetRefDataRevisionRecordsService getRefDataRevisionRecordsService(RefTableHelper refTableHelper) {
    return new GetRefDataRevisionRecordsService(refTableHelper);
  }

  @Bean
  public TranslationResolver translationResolver() {
    return new TranslationResolver();
  }

  @Bean
  public CreateRefTableRevisionGatewayService createRefTableRevisionGatewayService(RefTableHelper refTableHelper,
                                                                                   RefTableRevisionHelper refTableRevisionHelper) {
    return new CreateRefTableRevisionGatewayService(refTableHelper, refTableRevisionHelper);
  }

  @Bean
  public CreateRefDataRevisionService createRefDataRevisionService(RefTableHelper refTableHelper,
                                                                   RefRecordsValidationHelper refRecordsValidationHelper,
                                                                   RefTableRevisionHelper refTableRevisionHelper) {
    return new CreateRefDataRevisionService(refTableHelper, refRecordsValidationHelper, refTableRevisionHelper);
  }

  @Bean
  public GetApplicationOwnersGatewayService getApplicationOwnersGatewayService(RdmApplicationCommonsHelper rdmApplicationCommonsHelper) {
    return new GetApplicationOwnersGatewayService(rdmApplicationCommonsHelper);
  }

  @Bean
  public GetApplicationTableSubscriptionsGatewayService getApplicationTableSubscriptionsService(
      RdmApplicationCommonsHelper rdmApplicationCommonsHelper) {
    return new GetApplicationTableSubscriptionsGatewayService(rdmApplicationCommonsHelper);
  }

  @Bean
  public GetApplicationsService getApplicationsService() {
    return new GetApplicationsService();
  }

  @Bean
  public GetRefRecordByIdService getRefRecordByIdService() {
    return new GetRefRecordByIdService();
  }

  @Bean
  public GetRefRecordsService getRefRecordsService() {
    return new GetRefRecordsService();
  }

  @Bean
  public SearchForPreparedRefRecordsGatewayService searchForPreparedRefRecordsGatewayService() {
    return new SearchForPreparedRefRecordsGatewayService();
  }

  @Bean
  public RefTableService refTableService() {
    return new RefTableService();
  }

  @Bean
  public AddRefTableOwnerGatewayService addRefTableOwnerGatewayService() {
    return new AddRefTableOwnerGatewayService();
  }

  @Bean
  public RevokeApplicationAccessKeyGatewayService revokeApplicationAccessKeyGatewayService() {
    return new RevokeApplicationAccessKeyGatewayService();
  }

  @Bean
  public DeleteApplicationTableSubscriptionGatewayService deleteApplicationTableSubscriptionGatewayService() {
    return new DeleteApplicationTableSubscriptionGatewayService();
  }

  @Bean
  public DeleteApplicationService deleteApplicationService() {
    return new DeleteApplicationService();
  }

  @Bean
  public DeleteApplicationOwnerGatewayService deleteApplicationOwnerService() {
    return new DeleteApplicationOwnerGatewayService();
  }

  @Bean
  public DeleteRefTableOwnerGatewayService deleteRefTableOwnerGatewayService() {
    return new DeleteRefTableOwnerGatewayService();
  }

  @Bean
  public SubmitRefTableRevisionForReviewService submitRefTableRevisionForReviewService(RefTableRevisionHelper refTableRevisionHelper,
                                                                                       SraGatewayHelper sraGatewayHelper) {
    return new SubmitRefTableRevisionForReviewService(refTableRevisionHelper, sraGatewayHelper);
  }

  @Bean
  public UpdateRefDataRevisionService updateRefDataRevisionService(RefRecordsValidationHelper refRecordsValidationHelper,
                                                                   RefTableRevisionHelper refTableRevisionHelper) {
    return new UpdateRefDataRevisionService(refRecordsValidationHelper, refTableRevisionHelper);
  }

  @Bean
  public UpdateRefTableRevisionGatewayService updateRefTableRevisionGatewayService(RefTableHelper refTableHelper,
                                                                                   RefTableRevisionHelper refTableRevisionHelper) {
    return new UpdateRefTableRevisionGatewayService(refTableHelper, refTableRevisionHelper);
  }

  @Bean
  public RemoveRefTableRevisionService removeRefTableRevisionService(RefTableRevisionHelper refTableRevisionHelper) {
    return new RemoveRefTableRevisionService(refTableRevisionHelper);
  }

  @Bean
  public ApproveRefTableRevisionGatewayService approveRefTableRevisionGatewayService(RefTableRevisionHelper refTableRevisionHelper,
                                                                                     RefTableHelper refTableHelper) {
    return new ApproveRefTableRevisionGatewayService(refTableRevisionHelper, refTableHelper);
  }

  @Bean
  public RejectRefTableRevisionGatewayService rejectRefTableRevisionGatewayService(RefTableRevisionHelper refTableRevisionHelper) {
    return new RejectRefTableRevisionGatewayService(refTableRevisionHelper);
  }

  @Bean
  public ReturnRefTableRevisionGatewayService returnRefTableRevisionGatewayService(RefTableRevisionHelper refTableRevisionHelper) {
    return new ReturnRefTableRevisionGatewayService(refTableRevisionHelper);
  }

  @Bean
  public AssignReviewerToRefTableRevisionGatewayService assignReviewerToRefTableRevisionGatewayService(SraGatewayHelper sraGatewayHelper) {
    return new AssignReviewerToRefTableRevisionGatewayService(sraGatewayHelper);
  }

  @Bean
  public UnassignReviewerFromRefTableRevisionGatewayService unassignReviewerFromRefTableRevisionGatewayService(
      SraGatewayHelper sraGatewayHelper) {
    return new UnassignReviewerFromRefTableRevisionGatewayService(sraGatewayHelper);
  }

  @Bean
  public DeleteUserInviteService deleteUserInviteService() {
    return new DeleteUserInviteService();
  }

  @Bean
  public AdvancedSearchForRefTablesGatewayService advancedSearchForRefTablesGatewayService() {
    return new AdvancedSearchForRefTablesGatewayService();
  }

  @Bean
  public AdvancedSearchForRefTablesInternalGatewayService advancedSearchForRefTablesInternalGatewayService() {
    return new AdvancedSearchForRefTablesInternalGatewayService();
  }

  @Bean
  public PopulateSimilaritySearchDataForTableGatewayService populateSimilaritySearchDataForTableGatewayService() {
    return new PopulateSimilaritySearchDataForTableGatewayService();
  }

  @Bean
  public PopulateSimilaritySearchDataForPageService populateSimilaritySearchDataForPageService() {
    return new PopulateSimilaritySearchDataForPageService();
  }

  @Bean
  public FindSimilarRefRecordsService findSimilarRefRecordsService() {
    return new FindSimilarRefRecordsService();
  }

  @Bean
  public FindSimilarRefRecordsForColumnGatewayService findSimilarRefRecordsForColumnGatewayService() {
    return new FindSimilarRefRecordsForColumnGatewayService();
  }

  @Bean
  public InvalidateCommandmentCacheInternalService invalidateCommandmentCacheInternalService() {
    return new InvalidateCommandmentCacheInternalService();
  }

  @Bean
  public SendNotificationToTableOwnersService sendNotificationToTableOwnersService() {
    return new SendNotificationToTableOwnersService();
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
  @ConditionalOnProperty(prefix = "rdm.redis", name = "enabled", havingValue = "true")
  @ConditionalOnMissingBean(LockService.class)
  public LockService lockServiceWithRedis(RdmRedisService rdmRedisService) {
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
  @ConditionalOnProperty(prefix = "rdm.redis", name = "enabled", havingValue = "true")
  public RdmServiceDiscoveryClientWithRedis rdmServiceDiscoveryClient(RdmRedisService rdmRedisService) {
    return new RdmServiceDiscoveryClientWithRedis(RdmService.GATEWAY, rdmRedisService, 5);
  }

  // ----------------------------------------------------------------------------
  // Load Balancer
  // ----------------------------------------------------------------------------

  @Bean
  @ConditionalOnProperty(prefix = "rdm.redis", name = "enabled", havingValue = "true")
  public RdmLoadBalancer rdmLoadBalancer(RdmRedisService rdmRedisService) {
    return new RdmLoadBalancerWithRedis(
        rdmRedisService,
        10
    );
  }

  // ----------------------------------------------------------------------------
  // Caches
  // ----------------------------------------------------------------------------

  @Bean
  @Qualifier(value = "RedisCommandmentExecutionCache")
  @ConditionalOnProperty(prefix = "rdm.redis", name = "enabled", havingValue = "true")
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
