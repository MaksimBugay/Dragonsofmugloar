package com.bmv.authorization.application.service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.bmv.flow.foundry.application.service.authorization.UserAuthorizationExtractor;
import com.bmv.flow.foundry.application.service.authorization.UserIdentityExtractor;
import com.bmv.flow.foundry.application.commandment.internal.query.AuthorizationQuery;
import com.bmv.flow.foundry.domain.model.api.exception.BmvUnauthorizedException;
import com.bmv.flow.foundry.domain.model.auth.BmvRole;
import com.bmv.flow.foundry.domain.model.auth.UserAuthorization;
import com.bmv.flow.foundry.serialization.json.JsonUtility;
import com.bmv.flow.foundry.util.StringToBytesBase64BiConverter;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class PushcaUserAuthorizationExtractor implements UserAuthorizationExtractor {

  public static final String PUSHCA_ID_CLAIM_NAME = "sub";
  public static final String ROLES_CLAIM_NAME = "roles";
  public static final String JWT_TOKEN_PARTS_DELIMITER = "\\.";

  private static final Logger LOGGER = LoggerFactory.getLogger(PushcaUserAuthorizationExtractor.class);
  private final StringToBytesBase64BiConverter stringToBytesBase64BiConverter = new StringToBytesBase64BiConverter();

  @Override
  public String extractUserIdFromToken(String jwtToken) {
    Map<String, Object> payloadMap = extractPayloadMapFromJwtToken(jwtToken);

    return extractPushcaId(payloadMap);
  }

  @Override
  public Mono<UserAuthorization> authorizeByToken(String token) {
    return Mono.just(token)
        .map(this::parseJwtToken)
        .doOnError(
            error -> LOGGER.debug("Failed retrieve user authorization attempt", error)
        )
        .onErrorMap(
            _ -> new BmvUnauthorizedException()
        );
  }

  private Map<String, Object> extractPayloadMapFromJwtToken(String jwtToken) {
    if (jwtToken == null) {
      throw new IllegalArgumentException("Invalid JWT token: token cannot be null");
    }

    String[] parts = jwtToken.split(JWT_TOKEN_PARTS_DELIMITER);
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid JWT token format: Expected 3 parts but found " + parts.length);
    }

    byte[] jsonBytes = stringToBytesBase64BiConverter.restoreFromStringAsBytes(parts[1]);
    return JsonUtility.fromJsonToMap(jsonBytes, String.class, Object.class);
  }

  private UserAuthorization parseJwtToken(String jwtToken) {

    Map<String, Object> payloadMap = extractPayloadMapFromJwtToken(jwtToken);

    String PushcaId = extractPushcaId(payloadMap);

    Set<BmvRole> roles = extractRoles(payloadMap);

    return new UserAuthorization(PushcaId, roles, null);
  }

  private String extractPushcaId(Map<String, Object> payloadMap) {
    String PushcaId = (String) payloadMap.get(PUSHCA_ID_CLAIM_NAME);
    if (PushcaId == null) {
      throw new IllegalArgumentException("Missing 'sub' in JWT payload");
    }

    return PushcaId;
  }

  private Set<BmvRole> extractRoles(Map<String, Object> payloadMap) {
    return Optional.ofNullable(payloadMap.get(ROLES_CLAIM_NAME))
        .map(claim -> JsonUtility.fromJsonToList(JsonUtility.toJson(claim), String.class))
        .map(roleList -> roleList.stream()
            .map(this::mapToRdmRole)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()))
        .orElse(Set.of());
  }

  private BmvRole mapToRdmRole(String role) {
    try {
      return BmvRole.fromExternalIdentifier(role);
    } catch (IllegalArgumentException _) {
      return null;
    }
  }

  public static UserIdentityExtractor createUserIdentityExtractor() {
    PushcaUserAuthorizationExtractor pushcaUserAuthorizationExtractor = new PushcaUserAuthorizationExtractor();

    //noinspection Convert2Lambda
    return new UserIdentityExtractor() {

      @Override
      public String extractUserId(@NotNull AuthorizationQuery commandment) {
        return pushcaUserAuthorizationExtractor.extractUserId(commandment);
      }
    };
  }
}
