package com.bmv.gateway.infrastructure.adapter.in.cache;

import com.trimble.transportation.reference.data.management.application.port.cache.ConditionalCommandmentExecutionCache;
import com.trimble.transportation.reference.data.management.domain.model.api.Query;
import com.bmv.gateway.infrastructure.adapter.in.cache.caffeine.CaffeineCommandmentExecutionCache;
import com.bmv.gateway.infrastructure.adapter.in.cache.redis.RedisCommandmentExecutionCache;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

public class GatewayConditionalCommandmentExecutionCache extends ConditionalCommandmentExecutionCache {

    private final CaffeineCommandmentExecutionCache caffeineCommandmentExecutionCache;

    private static RedisCommandmentExecutionCache getRedisCommandmentExecutionCache(ApplicationContext applicationContext) {
        RedisCommandmentExecutionCache redisCommandmentExecutionCache;
        try {
            redisCommandmentExecutionCache = applicationContext.getBean(RedisCommandmentExecutionCache.class);
        } catch (NoSuchBeanDefinitionException _) {
            // all good, just redis is disabled in config
            redisCommandmentExecutionCache = null;
        }
        return redisCommandmentExecutionCache;
    }

    public GatewayConditionalCommandmentExecutionCache(
            CaffeineCommandmentExecutionCache caffeineCommandmentExecutionCache,
            ApplicationContext applicationContext) {
        RedisCommandmentExecutionCache redisCommandmentExecutionCache = getRedisCommandmentExecutionCache(applicationContext);
        super(
                typeDefinition -> {
                    if (typeDefinition.getName().equals(Query.Name.GET_REF_RECORDS.name())) {
                        return redisCommandmentExecutionCache;
                    }
                    return caffeineCommandmentExecutionCache;
                }
        );
        this.caffeineCommandmentExecutionCache = caffeineCommandmentExecutionCache;
    }

    public Mono<Void> resetAll() {
        return caffeineCommandmentExecutionCache.reset();
    }
}
