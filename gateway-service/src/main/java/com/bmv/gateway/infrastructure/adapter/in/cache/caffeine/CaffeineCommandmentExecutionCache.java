package com.bmv.gateway.infrastructure.adapter.in.cache.caffeine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.bmv.flow.foundry.application.port.cache.CommandmentExecutionCacheProvider;
import com.bmv.gateway.config.BmvCacheConfig;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Qualifier(value = "CaffeineCommandmentExecutionCache")
public class CaffeineCommandmentExecutionCache extends CommandmentExecutionCacheProvider<Object> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CaffeineCommandmentExecutionCache.class);

  private final AsyncCache<Object, Object> cache;

  CaffeineCommandmentExecutionCache(long expireAfterSec, long maximumSize) {
    super("null", Object.class);
    this.cache = Caffeine.newBuilder()
        .expireAfterAccess(expireAfterSec, TimeUnit.SECONDS)
        .maximumSize(maximumSize)
        .buildAsync((ignoredKey, ignoredExecutor) -> CompletableFuture.completedFuture(null));
  }

  @Autowired
  public CaffeineCommandmentExecutionCache(BmvCacheConfig bmvCacheConfig) {
    this(bmvCacheConfig.getExpireAfterSec(), bmvCacheConfig.getMaximumSize());
  }

  @Override
  public Object convertNotNullValueToInternalValue(Object value) {
    return value;
  }

  @Override
  public <R> R convertNotNullInternalValueToValue(Object internalValue, Class<R> clazz) {
    if (clazz.isAssignableFrom(internalValue.getClass())) {
      return clazz.cast(internalValue);
    }
    throw new ClassCastException(
        "Cannot convert commandment execution cache internal type" + internalValue.getClass() + " to " + clazz
    );
  }

  @Override
  public <R> Mono<R> get(String key, Class<R> clazz) {
    CompletableFuture<Object> valueFuture = cache.getIfPresent(key);
    if (valueFuture == null) {
      return Mono.empty(); // no entry
    }
    return Mono.fromFuture(valueFuture)
        .flatMap(value -> Mono.just(convertNotNullInternalValueToValue(value, clazz)))
        .doOnError(
            throwable -> LOGGER.error("Unexpected error during commandment execution cache lookup: key {}", key, throwable)
        )
        // if the future completed exceptionally, invalidate and treat as miss
        .onErrorResume(ignoredException -> {
          cache.synchronous().invalidate(key);
          return Mono.empty();
        });
  }

  @Override
  public Mono<Void> put(String key, @NotNull Object value,
                        //Ignored because only caffeine cache scoped ttl is supported
                        long ttl, TimeUnit timeUnit) {
    return put(key, value);
  }

  public Mono<Void> put(String key, @NotNull Object value) {
    return Mono.fromRunnable(() -> cache.put(key, CompletableFuture.completedFuture(value)));
  }

  @Override
  public Mono<Void> evict(String key) {
    return Mono.fromRunnable(() -> cache.synchronous().invalidate(key));
  }

  @Override
  public Mono<Void> evictByPrefix(String prefix) {
    return Mono.fromRunnable(
        () -> cache.synchronous().asMap().keySet()
            .removeIf(key -> key instanceof String stringKey && stringKey.startsWith(prefix))
    );
  }

  @Override
  public Mono<Void> reset() {
    return Mono.fromRunnable(() -> cache.synchronous().invalidateAll());
  }
}
