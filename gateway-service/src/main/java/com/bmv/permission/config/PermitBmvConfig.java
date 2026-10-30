package com.bmv.permission.config;

import com.bmv.flow.foundry.application.service.permission.CheckCommandmentPermissionsHelper;
import com.bmv.flow.foundry.application.service.permission.CheckPermissionsService;
import com.bmv.permission.NoRestrictionsCheckCommandmentPermissionsHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PermitBmvConfig {

    @Bean
    public CheckCommandmentPermissionsHelper checkCommandmentPermissionsHelper() {
        return new NoRestrictionsCheckCommandmentPermissionsHelper();
    }

    @Bean
    public CheckPermissionsService checkPermissionsService(CheckCommandmentPermissionsHelper checkCommandmentPermissionsHelper) {
        return new CheckPermissionsService(checkCommandmentPermissionsHelper);
    }

}
