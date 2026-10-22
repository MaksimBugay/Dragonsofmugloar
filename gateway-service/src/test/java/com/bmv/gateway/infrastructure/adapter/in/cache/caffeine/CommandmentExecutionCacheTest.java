package com.bmv.gateway.infrastructure.adapter.in.cache.caffeine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.trimble.transportation.reference.data.management.application.port.cache.CommandmentExecutionCacheProvider;
import com.trimble.transportation.reference.data.management.application.port.in.InternalApiInboundPort;
import com.trimble.transportation.reference.data.management.domain.cflow.CFlowStageCompletedListener;
import com.trimble.transportation.reference.data.management.domain.cflow.CFlowState;
import com.trimble.transportation.reference.data.management.domain.cflow.CommandmentStageExecutor;
import com.trimble.transportation.reference.data.management.domain.model.api.CommandmentImpl;
import com.trimble.transportation.reference.data.management.domain.model.api.CommandmentTypeData;
import com.trimble.transportation.reference.data.management.domain.model.api.CommandmentTypeDefinition;
import com.trimble.transportation.reference.data.management.domain.model.api.CommandmentTypeDefinitionBase;
import com.trimble.transportation.reference.data.management.domain.model.api.CommandmentWithDefinition;
import com.trimble.transportation.reference.data.management.domain.model.api.CustomRunTimeAttributes;
import com.trimble.transportation.reference.data.management.domain.model.api.Query;
import com.trimble.transportation.reference.data.management.domain.model.api.cache.CacheableCommandment;
import com.trimble.transportation.reference.data.management.domain.model.api.commandment.internal.query.GetApplicationAuthorizationAttributesByIdQuery;
import com.trimble.transportation.reference.data.management.domain.model.api.error.ErrorDetails;
import com.trimble.transportation.reference.data.management.domain.model.api.request.IdRequest;
import com.trimble.transportation.reference.data.management.domain.model.api.request.RefTableRevisionRequest;
import com.trimble.transportation.reference.data.management.domain.model.api.response.ApplicationAuthorizationAttributesResponse;
import com.trimble.transportation.reference.data.management.domain.model.api.response.YesNoResponse;
import com.trimble.transportation.reference.data.management.domain.model.app.RdmApplication;
import com.trimble.transportation.reference.data.management.domain.model.app.RdmApplicationAccessKey;
import com.trimble.transportation.reference.data.management.domain.model.auth.ApiKeyBasedAuthorization;
import com.trimble.transportation.reference.data.management.domain.model.auth.RdmServiceAuthorization;
import com.trimble.transportation.reference.data.management.domain.model.cluster.RdmService;
import com.trimble.transportation.reference.data.management.domain.model.core.RefTableRevision;
import com.trimble.transportation.reference.data.management.util.RdmObjectUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CommandmentExecutionCacheTest {

    @Test
    void shouldCacheAndRetrieveCommandmentNullExecutionResult() {
        CommandmentExecutionCacheProvider<Object> cache = new CaffeineCommandmentExecutionCache(300, 10_000);

        // Given: generate cacheable commandment
        UUID id = RdmObjectUtils.generateUUID();
        UUID authId = RdmObjectUtils.generateUUID();

        GetApplicationAuthorizationAttributesByIdQuery query = GetApplicationAuthorizationAttributesByIdQuery.TYPE_DEFINITION.createInstance(
                id,
                null,
                new IdRequest(authId),
                null,
                null,
                0
        );

        // When: Store and retrieve the value using reactive approach
        AtomicInteger executeNoCacheCounter = new AtomicInteger(0);
        Supplier<Mono<ApplicationAuthorizationAttributesResponse>> executor = () -> {
            executeNoCacheCounter.incrementAndGet();
            return Mono.delay(Duration.ofMillis(100)).flatMap(ignored -> Mono.empty());
        };
        @SuppressWarnings("DataFlowIssue") Mono<Void> cacheTest = Flux.range(1, 10)
                .concatMap(i ->
                        cache.executeWithCache(query, executor, 300, TimeUnit.SECONDS)
                                .doOnNext(
                                        tmpResponse -> assertNull(tmpResponse, "Response should be null for iteration " + i)
                                )
                )
                .then()
                .delayElement(Duration.ofSeconds(1)); // Final delay to ensure all operations complete

        // Then: Verify the reactive chain completes successfully
        StepVerifier.create(cacheTest)
                .expectComplete()
                .verify(Duration.ofSeconds(30)); // Allow sufficient time for all operations

        // Verify caching effectiveness - should execute only once (first time)
        // Comment out for now due to potential timing issues
        assertEquals(1, executeNoCacheCounter.get(), "Should execute 1 times with cache");
    }

    @Test
    void shouldCacheAndRetrieveCommandmentExecutionResult() {
        CommandmentExecutionCacheProvider<Object> cache = new CaffeineCommandmentExecutionCache(300, 10_000);

        // Given: generate cacheable commandment
        UUID id = RdmObjectUtils.generateUUID();
        UUID authId = RdmObjectUtils.generateUUID();
        String apiKeyHash = UUID.randomUUID().toString();
        String apiKeySalt = UUID.randomUUID().toString();
        RdmApplication application = new RdmApplication(
                RdmObjectUtils.generateUUID(),
                "TestApp",
                "mbugai",
                Instant.now().toEpochMilli(),
                null,
                Instant.now().toEpochMilli()
        );
        ApplicationAuthorizationAttributesResponse response = new ApplicationAuthorizationAttributesResponse(
                null,
                new RdmApplicationAccessKey(
                        authId,
                        application.getId(),
                        new ApiKeyBasedAuthorization.ApiKeyPersistentAttributes(
                                apiKeySalt,
                                apiKeyHash
                        ),
                        "MBugai",
                        Instant.now().toEpochMilli()
                ), null
        );
        GetApplicationAuthorizationAttributesByIdQuery query = GetApplicationAuthorizationAttributesByIdQuery.TYPE_DEFINITION.createInstance(
                id,
                null,
                new IdRequest(authId),
                null,
                null,
                0
        );

        // When: Store and retrieve the value using reactive approach
        AtomicInteger executeNoCacheCounter = new AtomicInteger(0);
        Supplier<Mono<ApplicationAuthorizationAttributesResponse>> executor = () -> {
            executeNoCacheCounter.incrementAndGet();
            return Mono.delay(Duration.ofMillis(100)).flatMap(ignored -> Mono.just(response));
        };
        Mono<Void> cacheTest = Flux.range(1, 10)
                .concatMap(i ->
                        cache.executeWithCache(query, executor, 300, TimeUnit.SECONDS)
                                .doOnNext(
                                        tmpResponse -> assertEquals(response, tmpResponse, "Response should match for iteration " + i)
                                )
                                .doOnError(error ->
                                        System.err.println("Test iteration " + i + " failed: " + error.getMessage())
                                )
                )
                .then()
                .delayElement(Duration.ofSeconds(1)); // Final delay to ensure all operations complete

        // Then: Verify the reactive chain completes successfully
        StepVerifier.create(cacheTest)
                .expectComplete()
                .verify(Duration.ofSeconds(30)); // Allow sufficient time for all operations

        // Verify caching effectiveness - should execute only once (first time)
        // Comment out for now due to potential timing issues
        assertEquals(1, executeNoCacheCounter.get(), "Should execute 1 times with cache");
    }

    @Test
    void shouldEvictOnlyEntriesMatchingPrefix() {
        // given a cache
        CommandmentExecutionCacheProvider<Object> cache = new CaffeineCommandmentExecutionCache(300, 10_000);
        // given two ref table revisions for table A (same table id, different version)
        UUID refTableIdA = RdmObjectUtils.generateUUID();
        RefTableRevision refTableRevisionA1 = new RefTableRevision(refTableIdA, 1);
        RefTableRevision refTableRevisionA2 = new RefTableRevision(refTableIdA, 2);
        // given one ref table revision for table B (different table id)
        UUID refTableIdB = RdmObjectUtils.generateUUID();
        RefTableRevision refTableRevisionB1 = new RefTableRevision(refTableIdB, 1);
        // given cacheable queries that use {table id}::{version} as the cache key
        CacheableTestQuery queryA1 = new CacheableTestQuery(new RdmServiceAuthorization(), new RefTableRevisionRequest(refTableRevisionA1));
        CacheableTestQuery queryA2 = new CacheableTestQuery(new RdmServiceAuthorization(), new RefTableRevisionRequest(refTableRevisionA2));
        CacheableTestQuery queryB1 = new CacheableTestQuery(new RdmServiceAuthorization(), new RefTableRevisionRequest(refTableRevisionB1));
        // given helpers to execute the queries multiple times and verify how often they were actually executed
        CountingExecutor executorA1 = CountingExecutor.create();
        CountingExecutor executorA2 = CountingExecutor.create();
        CountingExecutor executorB1 = CountingExecutor.create();
        Supplier<Mono<Void>> cacheTestA1 = createCacheTestSupplier(cache, queryA1, executorA1.supplier());
        Supplier<Mono<Void>> cacheTestA2 = createCacheTestSupplier(cache, queryA2, executorA2.supplier());
        Supplier<Mono<Void>> cacheTestB1 = createCacheTestSupplier(cache, queryB1, executorB1.supplier());

        // when executing each query 10 times
        executeCacheTest(cacheTestA1);
        executeCacheTest(cacheTestA2);
        executeCacheTest(cacheTestB1);

        // then they were cached after the first execution
        assertEquals(1, executorA1.executionCount().get(), "Query A1 should have been cached after first execution");
        assertEquals(1, executorA2.executionCount().get(), "Query A2 should have been cached after first execution");
        assertEquals(1, executorB1.executionCount().get(), "Query B1 should have been cached after first execution");

        // when evicting the cache using the full cache key of A1 as the prefix
        evictCacheByPrefix(cache, queryA1.getCommandmentTypeDefinition(), queryA1.getCacheKey());
        // and executing each query 10 times again
        executeCacheTest(cacheTestA1);
        executeCacheTest(cacheTestA2);
        executeCacheTest(cacheTestB1);

        // then only query A1 got executed one more time, A2 and B1 remained in cache
        assertEquals(2, executorA1.executionCount().get(), "Query A1 should have been executed one more time after cache eviction");
        assertEquals(1, executorA2.executionCount().get(), "Query A2 should not have been executed since it was not evicted from cache");
        assertEquals(1, executorB1.executionCount().get(), "Query B1 should not have been executed since it was not evicted from cache");

        // when evicting the cache using table A's id as the prefix
        evictCacheByPrefix(cache, queryA1.getCommandmentTypeDefinition(), refTableIdA.toString());
        // and executing each query 10 times again
        executeCacheTest(cacheTestA1);
        executeCacheTest(cacheTestA2);
        executeCacheTest(cacheTestB1);

        // then both table A queries were re-executed, but table B query remained in cache
        assertEquals(3, executorA1.executionCount().get(), "Query A1 should have been executed one more time after cache eviction");
        assertEquals(2, executorA2.executionCount().get(), "Query A2 should have been executed one more time after cache eviction");
        assertEquals(1, executorB1.executionCount().get(), "Query B1 should not have been executed since it belongs to a different table");
    }

    private record CountingExecutor(AtomicInteger executionCount, Supplier<Mono<YesNoResponse>> supplier) {
        static CountingExecutor create() {
            AtomicInteger count = new AtomicInteger(0);
            return new CountingExecutor(count, () -> {
                count.incrementAndGet();
                return Mono.just(YesNoResponse.yes());
            });
        }
    }

    private static void evictCacheByPrefix(CommandmentExecutionCacheProvider<Object> cache,
                                           CommandmentTypeDefinition<?, ?, ?, ?> commandmentTypeDefinition, String prefix) {
        StepVerifier.create(cache.evictByPrefix(commandmentTypeDefinition, prefix))
                .expectComplete()
                .verify();
    }

    private static Supplier<Mono<Void>> createCacheTestSupplier(CommandmentExecutionCacheProvider<Object> cache, CacheableTestQuery query,
                                                                Supplier<Mono<YesNoResponse>> executor) {
        return () -> Flux.range(1, 10)
                .concatMap(_ ->
                        cache.executeWithCache(query, executor, 300, TimeUnit.SECONDS)
                                .doOnNext(Assertions::assertNotNull)
                )
                .then();
    }

    private static void executeCacheTest(Supplier<Mono<Void>> cacheTest) {
        StepVerifier.create(cacheTest.get())
                .expectComplete()
                .verify();
    }

    public static class CacheableTestQuery
            extends CommandmentImpl<RdmServiceAuthorization, RefTableRevisionRequest, YesNoResponse, CacheableTestQuery>
            implements Query<RdmServiceAuthorization, RefTableRevisionRequest, YesNoResponse, CacheableTestQuery>,
            CacheableCommandment<RdmServiceAuthorization, RefTableRevisionRequest, YesNoResponse, CacheableTestQuery> {

        protected CacheableTestQuery(RdmServiceAuthorization authorization, RefTableRevisionRequest requestObject) {
            super(authorization, requestObject);
        }

        @Override
        public CommandmentTypeDefinition<RdmServiceAuthorization, RefTableRevisionRequest, YesNoResponse, CacheableTestQuery> getCommandmentTypeDefinition() {
            return new CommandmentTypeDefinitionBase<>(
                    Set.of(),
                    new CommandmentTypeData<>(
                            "CacheableTestQuery",
                            CacheableTestQuery.class,
                            RdmServiceAuthorization.class,
                            RefTableRevisionRequest.class,
                            YesNoResponse.class,
                            State.class
                    ),
                    RdmService.GATEWAY
            ) {
                @Override
                public CommandmentWithDefinition<RdmServiceAuthorization, RefTableRevisionRequest, YesNoResponse, CacheableTestQuery> createInstance(
                        UUID id, RdmServiceAuthorization authorization, RefTableRevisionRequest requestObject, YesNoResponse responseObject,
                        CustomRunTimeAttributes customRunTimeAttributes,
                        int stageNumber) {
                    return null;
                }

                @Override
                public YesNoResponse createErrorResponse(ErrorDetails errorDetails) {
                    return null;
                }

                @Override
                public CFlowState<RdmServiceAuthorization, RefTableRevisionRequest, YesNoResponse, CacheableTestQuery> createState(
                        CacheableTestQuery commandment,
                        Map<Object, Object> commandmentContext,
                        List<CFlowStageCompletedListener<RdmServiceAuthorization, RefTableRevisionRequest, YesNoResponse, CacheableTestQuery>> cFlowStageCompletedListeners,
                        InternalApiInboundPort internalApiInboundPort,
                        CommandmentStageExecutor commandmentStageExecutor) {
                    return new State(commandment, commandmentContext, cFlowStageCompletedListeners, internalApiInboundPort, commandmentStageExecutor);
                }
            };
        }

        @Override
        public String getCacheKey() {
            return getRequestObject().refTableId() + "::" + getRequestObject().version();
        }

        public static class State
                extends CFlowState<RdmServiceAuthorization, RefTableRevisionRequest, YesNoResponse, CacheableTestQuery> {
            public State(
                    CommandmentWithDefinition<RdmServiceAuthorization, RefTableRevisionRequest, YesNoResponse, CacheableTestQuery> commandment,
                    Map<Object, Object> commandmentContext,
                    List<CFlowStageCompletedListener<RdmServiceAuthorization, RefTableRevisionRequest, YesNoResponse, CacheableTestQuery>> cFlowStageCompletedListeners,
                    InternalApiInboundPort internalApiInboundPort,
                    CommandmentStageExecutor commandmentStageExecutor) {
                super(commandment, commandmentContext, cFlowStageCompletedListeners, internalApiInboundPort, commandmentStageExecutor);
            }
        }
    }

}
