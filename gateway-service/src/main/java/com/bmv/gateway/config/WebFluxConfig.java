package com.bmv.gateway.config;

import com.trimble.transportation.reference.data.management.application.WebFluxConfigurerWithCustomSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.netty.http.HttpResources;
import reactor.netty.resources.LoopResources;

@Configuration
@EnableWebFlux
public class WebFluxConfig extends WebFluxConfigurerWithCustomSerializer {

  @Value("${spring.application.name:gateway-service}")
  private String serverName;

  @Value("#{environment['RDM_GATEWAY_FLUX_SELECTORS_POOL_SIZE'] ?: ${webflux.http.server.selectors:2}}")
  private int numberOfSelectors;

  @Value("#{environment['RDM_GATEWAY_FLUX_WORKERS_POOL_SIZE'] ?: ${webflux.http.server.workers:20}}")
  private int numberOfWorkers;

  @Value("${webflux.http.server.prefer-native:true}")
  private boolean preferNative;

  public WebFluxConfig(Environment environment) {
    super(environment);
  }

  @Bean
  public HttpResources httpResources() {

    return HttpResources.set(
        LoopResources.create(
            serverName,
            numberOfSelectors,
            numberOfWorkers,
            preferNative
        )
    );
  }

  private CorsWebFilter createCorsWebFilter(String... allowedOrigins) {
    CorsConfiguration corsConfig = new CorsConfiguration();
    for (String origin : allowedOrigins) {
      corsConfig.addAllowedOrigin(origin);
    }
    corsConfig.addAllowedMethod("GET");
    corsConfig.addAllowedMethod("POST");
    corsConfig.addAllowedMethod("PUT");
    corsConfig.addAllowedMethod("DELETE");
    corsConfig.addAllowedHeader("*");
    corsConfig.setAllowCredentials(true);
    corsConfig.setMaxAge(8000L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);

    return new CorsWebFilter(source);
  }

  @Bean
  @Profile("local")
  CorsWebFilter corsWebFilterLocal() {
    return createCorsWebFilter(
        "http://localhost:3000/"
    );
  }

  @Bean
  @Profile("dev")
  CorsWebFilter corsWebFilterDev() {
    return createCorsWebFilter(
        "http://localhost:3000/",
        "https://rdm-web-ui.dev.eu.trimble-transportation.com/",
        "https://developers.dev.trimble-transportation.com/"
    );
  }

  @Bean
  @Profile("staging")
  CorsWebFilter corsWebFilterStage() {
    return createCorsWebFilter(
        "https://rdm-web-ui.stg.eu.trimble-transportation.com/",
        "https://developers.stg.trimble-transportation.com/"
    );
  }

  @Bean
  @Profile("prod")
  CorsWebFilter corsWebFilterProd() {
    return createCorsWebFilter(
        "https://rdm-web-ui.eu.trimble-transportation.com/",
        "https://developers.trimble-transportation.com/"
    );
  }
}
