package com.bmv.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RdmCacheConfig {

  @Value("${rdm.cache.expire-after-sec:86400}")
  private int expireAfterSec;

  @Value("${rdm.cache.max-size:1000}")
  private int maximumSize;

  public int getExpireAfterSec() {
    return expireAfterSec;
  }

  public int getMaximumSize() {
    return maximumSize;
  }
}
