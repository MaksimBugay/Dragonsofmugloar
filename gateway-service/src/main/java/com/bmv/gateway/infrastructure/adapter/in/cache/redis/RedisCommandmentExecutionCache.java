package com.bmv.gateway.infrastructure.adapter.in.cache.redis;

import com.bmv.flow.foundry.application.port.cache.CommandmentExecutionBinaryCacheWithRedis;
import com.bmv.flow.foundry.redis.RedisConnectionManager;
import com.bmv.flow.foundry.serialization.BmvRedisJsonSerializer;

public class RedisCommandmentExecutionCache extends CommandmentExecutionBinaryCacheWithRedis {
  public RedisCommandmentExecutionCache(RedisConnectionManager connectionManager) {
    super(
        new BmvRedisJsonSerializer(),
        connectionManager
    );
  }
}
