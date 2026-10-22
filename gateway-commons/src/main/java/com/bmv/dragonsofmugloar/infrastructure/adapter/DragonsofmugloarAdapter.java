package com.bmv.dragonsofmugloar.infrastructure.adapter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import com.trimble.transportation.reference.data.management.application.port.InternalApiPortMode;
import com.trimble.transportation.reference.data.management.config.HttpWebClientConfig;
import com.bmv.dragonsofmugloar.domain.exception.DragonsofmugloarServerException;
import com.bmv.dragonsofmugloar.port.out.DragonsofmugloarPort;
import com.bmv.dragonsofmugloar.domain.model.Ad;
import com.bmv.dragonsofmugloar.domain.model.AdProbability;
import com.bmv.dragonsofmugloar.domain.model.Game;
import com.bmv.dragonsofmugloar.domain.model.PurchaseResult;
import com.bmv.dragonsofmugloar.domain.model.Reputation;
import com.bmv.dragonsofmugloar.domain.model.ShopItem;
import com.bmv.dragonsofmugloar.domain.model.ShopItemId;
import com.bmv.dragonsofmugloar.domain.model.SolveAdResult;
import com.trimble.transportation.reference.data.management.infrastructure.adapter.out.http.HttpWebClientFactory;
import com.trimble.transportation.reference.data.management.infrastructure.adapter.out.http.WebClientWithPool;
import com.trimble.transportation.reference.data.management.serialization.json.JsonUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * REST adapter for the public Dragons of Mugloar API.
 */
public class DragonsofmugloarAdapter implements DragonsofmugloarPort, DisposableBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(DragonsofmugloarAdapter.class);
  private static final String API_URL = "https://dragonsofmugloar.com/api/v2";

  private final WebClientWithPool webClient;
  private final Duration timeout;

  public DragonsofmugloarAdapter() {
    this(
        HttpWebClientFactory.createWebClient(InternalApiPortMode.HTTP1),
        Duration.ofMillis(HttpWebClientConfig.DEFAULT_CONFIG.readTimeoutMs())
    );
  }

  DragonsofmugloarAdapter(WebClientWithPool webClient, Duration timeout) {
    this.webClient = webClient;
    this.timeout = timeout;
  }

  @Override
  public Mono<Game> startGame() {
    return send(HttpMethod.POST, "/game/start", Game.class);
  }

  @Override
  public Mono<List<Ad>> getAds(String gameId) {
    return send(HttpMethod.GET, "/{gameId}/messages", AdResponse[].class, gameId)
        .map(ads -> Arrays.stream(ads).map(this::toAd).toList());
  }

  @Override
  public Mono<SolveAdResult> solveAd(String gameId, String adId) {
    return send(HttpMethod.POST, "/{gameId}/solve/{adId}", SolveAdResult.class, gameId, adId);
  }

  @Override
  public Mono<List<ShopItem>> getShopItems(String gameId) {
    return send(HttpMethod.GET, "/{gameId}/shop", ShopItemResponse[].class, gameId)
        .map(items -> Arrays.stream(items).map(this::toShopItem).toList());
  }

  @Override
  public Mono<PurchaseResult> buyItem(String gameId, ShopItemId itemId) {
    return send(HttpMethod.POST, "/{gameId}/shop/buy/{itemId}", PurchaseResult.class, gameId, itemId.getId());
  }

  @Override
  public Mono<Reputation> investigateReputation(String gameId) {
    return send(HttpMethod.POST, "/{gameId}/investigate/reputation", Reputation.class, gameId);
  }

  @Override
  public void destroy() {
    webClient.destroy();
  }

  private <T> Mono<T> send(HttpMethod method, String path, Class<T> responseType, Object... uriVariables) {
    return webClient.getWebClient().method(method)
        .uri(API_URL + path, uriVariables)
        .retrieve()
        .onStatus(status -> !status.is2xxSuccessful(), ClientResponse::createException)
        .bodyToMono(String.class)
        .filter(body -> !body.isBlank())
        .switchIfEmpty(Mono.error(new IllegalStateException("Empty Dragons of Mugloar response: " + path)))
        .timeout(timeout)
        .onErrorMap(error -> !isClientError(error),
            error -> new DragonsofmugloarServerException(error, method + " " + API_URL + path, statusCode(error)))
        .map(body -> JsonUtility.fromJson(body, responseType))
        .doOnError(error -> LOGGER.error("Dragons of Mugloar request failed: {} {}", method, path, error));
  }

  /** 400 and 401 are caused by the request itself, so they are propagated unchanged. */
  private static boolean isClientError(Throwable error) {
    return error instanceof WebClientResponseException responseError
        && (responseError.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()
        || responseError.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value());
  }

  /** The response status, or 503 when the call produced no usable response (connection failure, timeout, empty body). */
  private static int statusCode(Throwable error) {
    return error instanceof WebClientResponseException responseError
        ? responseError.getStatusCode().value()
        : HttpStatus.SERVICE_UNAVAILABLE.value();
  }

  private Ad toAd(AdResponse response) {
    String probability = decode(response.probability(), response.encrypted());
    AdProbability estimate = Arrays.stream(AdProbability.values())
        .filter(value -> value.getLabel().equals(probability))
        .findFirst()
        .orElse(AdProbability.UNKNOWN);
    return new Ad(
        decode(response.adId(), response.encrypted()),
        decode(response.message(), response.encrypted()),
        response.reward(),
        response.expiresIn(),
        response.encrypted(),
        estimate
    );
  }

  private ShopItem toShopItem(ShopItemResponse response) {
    ShopItemId itemId = Arrays.stream(ShopItemId.values())
        .filter(value -> value.getId().equals(response.id()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown shop item ID: " + response.id()));
    return new ShopItem(itemId, response.name(), response.cost());
  }

  private String decode(String value, Integer encrypted) {
    if (value == null) {
      return null;
    }
    return switch (encrypted) {
      case null -> value;
      case 0 -> value;
      case 1 -> new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
      case 2 -> rot13(value);
      default -> throw new IllegalArgumentException("Unsupported ad encryption: " + encrypted);
    };
  }

  private String rot13(String value) {
    StringBuilder result = new StringBuilder(value.length());
    for (char character : value.toCharArray()) {
      if (character >= 'a' && character <= 'z') {
        result.append((char) ('a' + (character - 'a' + 13) % 26));
      } else if (character >= 'A' && character <= 'Z') {
        result.append((char) ('A' + (character - 'A' + 13) % 26));
      } else {
        result.append(character);
      }
    }
    return result.toString();
  }

  private record AdResponse(
      String adId,
      String message,
      long reward,
      int expiresIn,
      Integer encrypted,
      String probability
  ) {
  }

  private record ShopItemResponse(
      String id,
      String name,
      long cost
  ) {
  }
}
