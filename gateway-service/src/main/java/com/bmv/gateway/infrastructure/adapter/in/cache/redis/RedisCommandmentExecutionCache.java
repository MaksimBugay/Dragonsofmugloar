package com.bmv.gateway.infrastructure.adapter.in.cache.redis;

import com.trimble.transportation.reference.data.management.application.port.cache.CommandmentExecutionBinaryCacheWithRedis;
import com.trimble.transportation.reference.data.management.redis.RedisConnectionManager;
import com.trimble.transportation.reference.data.management.serialization.protobuf.RdmProtobufSerializerWithCompression;

public class RedisCommandmentExecutionCache extends CommandmentExecutionBinaryCacheWithRedis {
  public RedisCommandmentExecutionCache(RedisConnectionManager connectionManager) {
    super(
        new RdmProtobufSerializerWithCompression(),
        connectionManager
    );
  }
}
