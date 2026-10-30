package com.bmv.authorization.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.bmv.flow.foundry.application.service.authorization.AuthorizationService;
import com.bmv.flow.foundry.application.service.authorization.UserAuthorizationProvider;
import com.bmv.flow.foundry.application.service.authorization.UserIdentityExtractor;
import com.bmv.authorization.application.service.PushcaUserAuthorizationExtractor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthBmvConfig {

    @Bean
    public UserIdentityExtractor userIdentityExtractor() {
        return PushcaUserAuthorizationExtractor.createUserIdentityExtractor();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bmv.authz-integration", name = "enabled", havingValue = "false", matchIfMissing = true)
    public UserAuthorizationProvider defaultUserAuthorizationProvider() {
        return new PushcaUserAuthorizationExtractor();
    }

    @Bean
    public AuthorizationService authorizationService(UserAuthorizationProvider userAuthorizationProvider,
                                                     @Value("${bmv.authorization.data-sync-agent-ids:}") String dataSyncAgentIds) {
        Set<String> agentIds = StringUtils.isBlank(dataSyncAgentIds)
                ? null
                : Arrays.stream(dataSyncAgentIds.split(";"))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        return new AuthorizationService(userAuthorizationProvider, null, agentIds);
    }
}
