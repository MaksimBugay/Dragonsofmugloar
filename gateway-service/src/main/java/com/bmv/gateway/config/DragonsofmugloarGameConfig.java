package com.bmv.gateway.config;

import com.bmv.dragonsofmugloar.application.service.ChooseGameActionService;
import com.bmv.dragonsofmugloar.application.service.PerformGameActionService;
import com.bmv.dragonsofmugloar.infrastructure.adapter.DragonsofmugloarAdapter;
import com.bmv.dragonsofmugloar.port.out.DragonsofmugloarPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DragonsofmugloarGameConfig {

  @Bean
  public DragonsofmugloarPort dragonsofmugloarPort(@Value("${dragonsofmugloar.api.url}") String apiUrl) {
    return new DragonsofmugloarAdapter(apiUrl);
  }

  @Bean
  public ChooseGameActionService chooseGameActionService() {
    return new ChooseGameActionService();
  }

  @Bean
  public PerformGameActionService performGameActionService(DragonsofmugloarPort port) {
    return new PerformGameActionService(port);
  }
}
