package com.bmv.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BmvCacheConfig {

  @Value("${bmv.cache.expire-after-sec:86400}")
  private int expireAfterSec;

  @Value("${bmv.cache.max-size:1000}")
  private int maximumSize;

  public int getExpireAfterSec() {
    return expireAfterSec;
  }

  public int getMaximumSize() {
    return maximumSize;
  }
}
