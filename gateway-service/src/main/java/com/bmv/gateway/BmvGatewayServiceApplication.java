package com.bmv.gateway;

import com.bmv.gateway.config.BmvServiceDiscoveryConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
		"com.bmv.gateway",
		"com.bmv.authorization",
		"com.bmv.permission"
})
@EnableConfigurationProperties(BmvServiceDiscoveryConfig.class)
public class BmvGatewayServiceApplication {

	static void main(String[] args) {
		SpringApplication.run(BmvGatewayServiceApplication.class, args);
	}

}
