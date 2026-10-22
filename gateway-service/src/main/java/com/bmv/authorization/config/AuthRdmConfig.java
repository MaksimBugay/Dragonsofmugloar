package com.bmv.authorization.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.trimble.transportation.reference.data.management.application.service.authorization.AuthorizationService;
import com.trimble.transportation.reference.data.management.application.service.authorization.AuthzUserAuthorizationProvider;
import com.trimble.transportation.reference.data.management.application.service.authorization.GenerateApplicationAccessKeyService;
import com.trimble.transportation.reference.data.management.application.service.authorization.UserAuthorizationProvider;
import com.trimble.transportation.reference.data.management.application.service.authorization.UserIdentityExtractor;
import com.bmv.authorization.application.service.TrimbleUserAuthorizationExtractor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthRdmConfig {

  @Bean
  @ConditionalOnProperty(prefix = "rdm.authz-integration", name = "enabled", havingValue = "true")
  public UserIdentityExtractor userIdentityExtractor() {
    return TrimbleUserAuthorizationExtractor.createUserIdentityExtractor();
  }

  @Bean
  @ConditionalOnProperty(prefix = "rdm.authz-integration", name = "enabled", havingValue = "true")
  public UserAuthorizationProvider authzUserAuthorizationProvider(UserIdentityExtractor userIdentityExtractor) {
    return new AuthzUserAuthorizationProvider(userIdentityExtractor);
  }

  @Bean
  @ConditionalOnProperty(prefix = "rdm.authz-integration", name = "enabled", havingValue = "false", matchIfMissing = true)
  public UserAuthorizationProvider defaultUserAuthorizationProvider() {
    return new TrimbleUserAuthorizationExtractor();
  }

  @Bean
  public AuthorizationService authorizationService(UserAuthorizationProvider userAuthorizationProvider,
                                                   @Value("${rdm.authorization.data-sync-agent-ids:}") String dataSyncAgentIds) {
    Set<String> agentIds = StringUtils.isBlank(dataSyncAgentIds)
        ? null
        : Arrays.stream(dataSyncAgentIds.split(";"))
        .map(String::trim)
        .filter(id -> !id.isEmpty())
        .collect(Collectors.toUnmodifiableSet());
    return new AuthorizationService(userAuthorizationProvider, agentIds);
  }

  @Bean
  public GenerateApplicationAccessKeyService generateApplicationAccessKeyService() {
    return new GenerateApplicationAccessKeyService();
  }
}
