package com.bmv.authorization.application.service;

import static com.bmv.flow.foundry.util.BmvTestUtil.BMV_CLUSTER_SECRET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import com.bmv.flow.foundry.application.service.authorization.AuthorizationProvider;
import com.bmv.flow.foundry.application.service.authorization.AuthorizationService;
import com.bmv.flow.foundry.application.service.authorization.InternalAuthorizationBasicService;
import com.bmv.flow.foundry.application.service.authorization.UserAuthorizationProvider;
import com.bmv.flow.foundry.application.commandment.internal.query.AuthorizationQuery;
import com.bmv.flow.foundry.application.commandment.request.AuthorizeCommandmentRequest;
import com.bmv.flow.foundry.application.commandment.response.AuthorizationResponse;
import com.bmv.flow.foundry.domain.model.auth.AnonymousAuthorization;
import com.bmv.flow.foundry.domain.model.auth.ApplicationAuthorization;
import com.bmv.flow.foundry.domain.model.auth.ApplicationOrUserAuthorization;
import com.bmv.flow.foundry.domain.model.auth.ClientType;
import com.bmv.flow.foundry.domain.model.auth.BmvBackendAuthorization;
import com.bmv.flow.foundry.domain.model.auth.UserAuthorization;
import com.bmv.flow.foundry.infrastructure.adapter.in.CommandmentRouter;
import com.bmv.flow.foundry.util.BmvObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

  @Mock
  private UserAuthorizationProvider userAuthorizationProvider;

  @Mock
  private AuthorizationProvider mockApplicationAuthorizationProvider;

  private static final UUID DATA_SYNC_AGENT_ID = BmvObjectUtils.generateUUID();

  private AuthorizationService authorizationService;

  private CommandmentRouter commandmentRouter;

  @BeforeEach
  void setUp() {
    authorizationService = new AuthorizationService(
        userAuthorizationProvider,
        mockApplicationAuthorizationProvider,
        Set.of(DATA_SYNC_AGENT_ID.toString())
    );


    commandmentRouter = Mockito.mock(CommandmentRouter.class);
    lenient().when(commandmentRouter.getInternalAuthorizationService())
        .thenReturn(new InternalAuthorizationBasicService(30_000L, BMV_CLUSTER_SECRET));
  }

  private Mono<AuthorizationResponse> authorize(AuthorizationService service, AuthorizationQuery query) {
    return service.authorize(AuthorizationQuery.DEFINITION.createState(query, new java.util.HashMap<>(), java.util.List.of(), commandmentRouter, null));
  }

  private static byte[] buildJwtCredentials() {
    Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
    String header = urlEncoder.encodeToString(
        "{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    String payload = urlEncoder.encodeToString(
        "{\"sub\":\"user-123\"}".getBytes(StandardCharsets.UTF_8));
    String signature = urlEncoder.encodeToString(
        "fake-signature".getBytes(StandardCharsets.UTF_8));
    return (header + "." + payload + "." + signature).getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] buildApiKeyCredentials() {
    String composedKey = Base64.getEncoder().encodeToString(
        (BmvObjectUtils.generateUUID() + ":plainKey").getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encode(composedKey.getBytes(StandardCharsets.UTF_8));
  }

  @Nested
  class AdjustClientType {

    @Test
    void applicationOrUser_withJwtCredentials_routesToUserProvider() {
      byte[] jwtCreds = buildJwtCredentials();
      AuthorizationQuery query = new AuthorizationQuery(
          new AuthorizeCommandmentRequest(new ApplicationOrUserAuthorization(jwtCreds))
      );

      AuthorizationResponse userResponse = new AuthorizationResponse(
          new UserAuthorization("user-id", Set.of(), null), null
      );
      when(userAuthorizationProvider.authorize(argThat(q ->
          q.getClientType() == ClientType.USER
      ))).thenReturn(Mono.just(userResponse));

      StepVerifier.create(authorize(authorizationService, query))
          .assertNext(response -> {
            assertNotNull(response.authorization());
            assertInstanceOf(UserAuthorization.class, response.authorization());
          })
          .verifyComplete();

      verify(userAuthorizationProvider).authorize(any());
      verifyNoInteractions(mockApplicationAuthorizationProvider);
    }

    @Test
    void applicationOrUser_withApiKeyCredentials_routesToApplicationProvider() {
      byte[] apiKeyCreds = buildApiKeyCredentials();
      AuthorizationQuery query = new AuthorizationQuery(
          new AuthorizeCommandmentRequest(new ApplicationOrUserAuthorization(apiKeyCreds))
      );

      UUID appId = BmvObjectUtils.generateUUID();
      AuthorizationResponse appResponse = new AuthorizationResponse(
          new ApplicationAuthorization(appId), null
      );
      when(mockApplicationAuthorizationProvider.authorize(argThat(q ->
          q.getClientType() == ClientType.APPLICATION
      ))).thenReturn(Mono.just(appResponse));

      StepVerifier.create(authorize(authorizationService, query))
          .assertNext(response -> {
            assertNotNull(response.authorization());
            assertInstanceOf(ApplicationAuthorization.class, response.authorization());
          })
          .verifyComplete();

      verify(mockApplicationAuthorizationProvider).authorize(any());
      verifyNoInteractions(userAuthorizationProvider);
    }

    @Test
    void applicationOrUser_withEmptyCredentials_returnsUnauthorized() {
      AuthorizationQuery query = new AuthorizationQuery(
          new AuthorizeCommandmentRequest(new ApplicationOrUserAuthorization(new byte[0]))
      );

      StepVerifier.create(authorize(authorizationService, query))
          .assertNext(response -> {
            assertNull(response.authorization());
            assertNotNull(response.errorDetails());
          })
          .verifyComplete();

      verifyNoInteractions(userAuthorizationProvider);
      verifyNoInteractions(mockApplicationAuthorizationProvider);
    }

    @Test
    void applicationOrUser_withNullCredentials_returnsUnauthorized() {
      AuthorizationQuery query = new AuthorizationQuery(
          new AuthorizeCommandmentRequest(new ApplicationOrUserAuthorization(null))
      );

      StepVerifier.create(authorize(authorizationService, query))
          .assertNext(response -> {
            assertNull(response.authorization());
            assertNotNull(response.errorDetails());
          })
          .verifyComplete();

      verifyNoInteractions(userAuthorizationProvider);
      verifyNoInteractions(mockApplicationAuthorizationProvider);
    }
  }

  @Nested
  class DirectClientTypes {

    @Test
    void nullRequestObject_returnsAnonymousAuthorization() {
      AuthorizationQuery query = new AuthorizationQuery(
          BmvObjectUtils.generateUUID(), null
      );

      StepVerifier.create(authorize(authorizationService, query))
          .assertNext(response -> {
            assertNotNull(response.authorization());
            assertInstanceOf(AnonymousAuthorization.class, response.authorization());
            assertNull(response.errorDetails());
          })
          .verifyComplete();

      verifyNoInteractions(userAuthorizationProvider);
      verifyNoInteractions(mockApplicationAuthorizationProvider);
    }

    @Test
    void nullAuthCredentials_returnsAnonymousAuthorization() {
      AuthorizationQuery query = new AuthorizationQuery(
          new AuthorizeCommandmentRequest(null)
      );

      StepVerifier.create(authorize(authorizationService, query))
          .assertNext(response -> {
            assertNotNull(response.authorization());
            assertInstanceOf(AnonymousAuthorization.class, response.authorization());
            assertNull(response.errorDetails());
          })
          .verifyComplete();

      verifyNoInteractions(userAuthorizationProvider);
      verifyNoInteractions(mockApplicationAuthorizationProvider);
    }
  }

  @Nested
  class DataSyncAgentDetection {

    @Test
    void applicationInDataSyncAgentIds_returnsDataSyncAgentAuthorization() {
      byte[] apiKeyCreds = buildApiKeyCredentials();
      AuthorizationQuery query = new AuthorizationQuery(
          new AuthorizeCommandmentRequest(new ApplicationAuthorization(null, apiKeyCreds))
      );

      AuthorizationResponse appResponse = new AuthorizationResponse(
          new ApplicationAuthorization(DATA_SYNC_AGENT_ID), null
      );
      when(mockApplicationAuthorizationProvider.authorize(any())).thenReturn(Mono.just(appResponse));

      StepVerifier.create(authorize(authorizationService, query))
          .assertNext(response -> {
            assertNotNull(response.authorization());
            assertInstanceOf(BmvBackendAuthorization.class, response.authorization());
            assertEquals(DATA_SYNC_AGENT_ID.toString(), response.authorization().getId());
          })
          .verifyComplete();
    }

    @Test
    void applicationNotInDataSyncAgentIds_returnsOriginalAuthorization() {
      byte[] apiKeyCreds = buildApiKeyCredentials();
      UUID regularAppId = BmvObjectUtils.generateUUID();
      AuthorizationQuery query = new AuthorizationQuery(
          new AuthorizeCommandmentRequest(new ApplicationAuthorization(null, apiKeyCreds))
      );

      AuthorizationResponse appResponse = new AuthorizationResponse(
          new ApplicationAuthorization(regularAppId), null
      );
      when(mockApplicationAuthorizationProvider.authorize(any())).thenReturn(Mono.just(appResponse));

      StepVerifier.create(authorize(authorizationService, query))
          .assertNext(response -> {
            assertNotNull(response.authorization());
            assertInstanceOf(ApplicationAuthorization.class, response.authorization());
            assertEquals(regularAppId.toString(), response.authorization().getId());
          })
          .verifyComplete();
    }

    @Test
    void applicationWithNullId_returnsOriginalAuthorization() {
      byte[] apiKeyCreds = buildApiKeyCredentials();
      AuthorizationQuery query = new AuthorizationQuery(
          new AuthorizeCommandmentRequest(new ApplicationAuthorization(null, apiKeyCreds))
      );

      AuthorizationResponse appResponse = new AuthorizationResponse(
          new ApplicationAuthorization((UUID) null), null
      );
      when(mockApplicationAuthorizationProvider.authorize(any())).thenReturn(Mono.just(appResponse));

      StepVerifier.create(authorize(authorizationService, query))
          .assertNext(response -> {
            assertNotNull(response.authorization());
            assertInstanceOf(ApplicationAuthorization.class, response.authorization());
          })
          .verifyComplete();
    }
  }

  @Nested
  class NullDataSyncAgentIds {

    @Test
    void nullDataSyncAgentIds_doesNotThrow() {
      AuthorizationService serviceWithNullIds = new AuthorizationService(userAuthorizationProvider, mockApplicationAuthorizationProvider, null);

      AuthorizationQuery query = new AuthorizationQuery(
          BmvObjectUtils.generateUUID(), null
      );

      StepVerifier.create(authorize(serviceWithNullIds, query))
          .assertNext(response -> assertInstanceOf(AnonymousAuthorization.class, response.authorization()))
          .verifyComplete();
    }
  }
}
