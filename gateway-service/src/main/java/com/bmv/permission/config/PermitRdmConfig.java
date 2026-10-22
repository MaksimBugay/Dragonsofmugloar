package com.bmv.permission.config;

import com.trimble.transportation.reference.data.management.application.service.permission.CheckCommandmentPermissionsHelper;
import com.trimble.transportation.reference.data.management.application.service.permission.CheckPermissionsService;
import com.trimble.transportation.reference.data.management.application.service.permission.RdmCheckCommandmentPermissionsHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PermitRdmConfig {

  @Bean
  public CheckCommandmentPermissionsHelper checkCommandmentPermissionsHelper() {
    return new RdmCheckCommandmentPermissionsHelper();
  }

  @Bean
  public CheckPermissionsService checkPermissionsService(CheckCommandmentPermissionsHelper checkCommandmentPermissionsHelper) {
    return new CheckPermissionsService(checkCommandmentPermissionsHelper);
  }

}
