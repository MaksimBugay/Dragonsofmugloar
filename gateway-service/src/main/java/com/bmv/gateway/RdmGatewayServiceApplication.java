package com.bmv.gateway;

import com.bmv.gateway.config.RdmServiceDiscoveryConfig;
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
@EnableConfigurationProperties(RdmServiceDiscoveryConfig.class)
public class RdmGatewayServiceApplication {

	static void main(String[] args) {
		SpringApplication.run(RdmGatewayServiceApplication.class, args);
	}

}
