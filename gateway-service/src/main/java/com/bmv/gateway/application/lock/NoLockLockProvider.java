package com.bmv.gateway.application.lock;

import com.trimble.transportation.reference.data.management.application.lock.LockProvider;
import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;

public class NoLockLockProvider implements LockProvider {
  @Override
  public Mono<Boolean> acquireLock(@NotBlank String lockId, @NotBlank String sessionId, long lockTtlMillis) {
    return Mono.just(true);
  }

  @Override
  public Mono<Boolean> releaseLock(@NotBlank String lockId, @NotBlank String sessionId) {
    return Mono.just(true);
  }

  @Override
  public Mono<Void> resetAll() {
    return Mono.empty();
  }

  @Override
  public void close() {
  }
}
