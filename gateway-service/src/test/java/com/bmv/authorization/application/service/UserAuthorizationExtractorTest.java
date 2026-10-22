package com.bmv.authorization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Base64;
import java.util.Set;

import com.trimble.transportation.reference.data.management.domain.model.api.exception.RdmUnauthorizedException;
import com.trimble.transportation.reference.data.management.domain.model.auth.RdmRole;
import com.trimble.transportation.reference.data.management.domain.model.auth.UserAuthorization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserAuthorizationExtractorTest {

  @InjectMocks
  private TrimbleUserAuthorizationExtractor userAuthorizationExtractor;

  @Test
  void testExtract_ValidToken() {
    String jwtToken =
        "header." + Base64.getEncoder().encodeToString("{\"sub\":\"12345\",\"roles\":[\"RDM_DATA_READER_ROLE\"]}".getBytes()) +
            ".signature";

    Mono<UserAuthorization> result = userAuthorizationExtractor.authorizeByToken(jwtToken);

    StepVerifier.create(result)
        .assertNext(response -> {
          assertEquals("12345", response.getId());
          assertEquals(Set.of(RdmRole.DATA_READER), response.getRoles());
        })
        .verifyComplete();
  }

  @Test
  void testExtract_InvalidTokenFormat() {
    String jwtToken = "invalidFormat";

    Mono<UserAuthorization> result = userAuthorizationExtractor.authorizeByToken(jwtToken);

    StepVerifier.create(result)
        .expectError(RdmUnauthorizedException.class)
        .verify();
  }

  @Test
  void testExtract_MissingTrimbleId() {
    String jwtToken = "header." + Base64.getEncoder().encodeToString("{\"roles\":[\"ADMIN\"]}".getBytes()) + ".signature";

    Mono<UserAuthorization> result = userAuthorizationExtractor.authorizeByToken(jwtToken);

    StepVerifier.create(result)
        .expectError(RdmUnauthorizedException.class)
        .verify();
  }

  @Test
  void testExtract_InvalidRole() {
    String jwtToken =
        "header." + Base64.getEncoder().encodeToString("{\"sub\":\"12345\",\"roles\":[\"INVALID_ROLE\"]}".getBytes()) +
            ".signature";

    Mono<UserAuthorization> result = userAuthorizationExtractor.authorizeByToken(jwtToken);

    StepVerifier.create(result)
        .assertNext(response -> {
          assertEquals("12345", response.getId());
          assertEquals(Set.of(), response.getRoles());
        })
        .verifyComplete();
  }

  @Test
  void testExtract_ExceptionDuringParsing() {
    Mono<UserAuthorization> result = userAuthorizationExtractor.authorizeByToken("exceptionToken");

    StepVerifier.create(result)
        .expectError(RdmUnauthorizedException.class)
        .verify();
  }
}