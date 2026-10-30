package com.bmv.gateway.config;

import com.bmv.flow.foundry.config.HttpWebClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bmv.service.internal-api.http-web-client")
public class GatewayHttpWebClientConfig {

  private Integer poolSize;
  private Integer maxQueueSize;
  private Integer connectionTimeoutMs;
  private Integer readTimeoutMs;
  private Integer memoryBufferSize;

  public HttpWebClientConfig getHttpWebClientConfig() {
    return new HttpWebClientConfig.Builder()
        .poolSize(poolSize)
        .maxQueueSize(maxQueueSize)
        .connectionTimeoutMs(connectionTimeoutMs)
        .readTimeoutMs(readTimeoutMs)
        .memoryBufferSize(memoryBufferSize)
        .build();
  }

  public Integer getPoolSize() {
    return poolSize;
  }

  public void setPoolSize(Integer poolSize) {
    this.poolSize = poolSize;
  }

  public Integer getMaxQueueSize() {
    return maxQueueSize;
  }

  public void setMaxQueueSize(Integer maxQueueSize) {
    this.maxQueueSize = maxQueueSize;
  }

  public Integer getConnectionTimeoutMs() {
    return connectionTimeoutMs;
  }

  public void setConnectionTimeoutMs(Integer connectionTimeoutMs) {
    this.connectionTimeoutMs = connectionTimeoutMs;
  }

  public Integer getReadTimeoutMs() {
    return readTimeoutMs;
  }

  public void setReadTimeoutMs(Integer readTimeoutMs) {
    this.readTimeoutMs = readTimeoutMs;
  }

  public Integer getMemoryBufferSize() {
    return memoryBufferSize;
  }

  public void setMemoryBufferSize(Integer memoryBufferSize) {
    this.memoryBufferSize = memoryBufferSize;
  }
}