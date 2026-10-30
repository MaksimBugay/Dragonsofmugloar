
package com.bmv.dragonsofmugloar.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.ConnectException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bmv.dragonsofmugloar.domain.exception.DragonsofmugloarServerException;
import com.bmv.dragonsofmugloar.domain.model.Ad;
import com.bmv.dragonsofmugloar.domain.model.AdProbability;
import com.bmv.dragonsofmugloar.domain.model.Game;
import com.bmv.dragonsofmugloar.domain.model.PurchaseResult;
import com.bmv.dragonsofmugloar.domain.model.Reputation;
import com.bmv.dragonsofmugloar.domain.model.ShopItem;
import com.bmv.dragonsofmugloar.domain.model.ShopItemId;
import com.bmv.dragonsofmugloar.domain.model.SolveAdResult;
import com.bmv.flow.foundry.infrastructure.adapter.out.http.WebClientWithPool;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

class DragonsofmugloarAdapterTest {

  private static final String API_URL = "https://dragonsofmugloar.com/api/v2";

  private final List<ClientRequest> requests = new ArrayList<>();

  @Test
  void createsAndDisposesDefaultHttpClient() {
    new DragonsofmugloarAdapter(API_URL).destroy();
  }

  @Test
  void startsGameAndMapsInitialState() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.OK, """
        {"gameId":"game1","lives":3,"gold":0,"level":0,"score":0,"highScore":4000000000,"turn":0}
        """);

    assertEquals(new Game("game1", 3, 0, 0, 0, 4000000000L, 0), adapter.startGame().block());
    assertRequest(HttpMethod.POST, "/game/start");
  }

  @Test
  void investigatesReputationAndMapsSignedFractionalValues() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.OK, """
        {"people":-1.25,"state":2.5,"underworld":0}
        """);

    assertEquals(new Reputation(-1.25, 2.5, 0), adapter.investigateReputation("game1").block());
    assertRequest(HttpMethod.POST, "/game1/investigate/reputation");
  }

  @Test
  void mapsPlainAdsAndHandlesUnknownProbability() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.OK, """
        [
          {"adId":"ad1","message":"Help a dragon","reward":"35","expiresIn":7,
           "encrypted":null,"probability":"Piece of cake"},
          {"adId":"ad2","message":"Another mission","reward":20,"expiresIn":2,"probability":"New label"}
        ]
        """);

    List<Ad> ads = adapter.getAds("game1").block();
    assertNotNull(ads);

    assertEquals(new Ad("ad1", "Help a dragon", 35, 7, null, AdProbability.PIECE_OF_CAKE), ads.getFirst());
    assertEquals(AdProbability.UNKNOWN, ads.get(1).probability());
    assertNull(ads.get(1).encrypted());
    assertRequest(HttpMethod.GET, "/game1/messages");
  }

  @Test
  void decodesBase64AdIncludingIdentifierAndProbability() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.OK, """
        [{"adId":"c3BOek5vekE=",
          "message":"SW52ZXN0aWdhdGUgTHV2aW5pYSBFbmdsaXNoIGFuZCBmaW5kIG91dCB0aGVpciByZWxhdGlvbiB0byB0aGUgbWFnaWMgcG90Lg==",
          "reward":153,"expiresIn":3,"encrypted":1,"probability":"R2FtYmxl"}]
        """);

    List<Ad> ads = adapter.getAds("game1").block();
    assertNotNull(ads);
    Ad ad = ads.getFirst();

    assertEquals("spNzNozA", ad.adId());
    assertEquals("Investigate Luvinia English and find out their relation to the magic pot.", ad.message());
    assertEquals(153, ad.reward());
    assertEquals(1, ad.encrypted());
    assertEquals(AdProbability.GAMBLE, ad.probability());
  }

  @Test
  void decodesRot13AdIncludingIdentifierAndProbability() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.OK, """
        [{"adId":"NQ123","message":"Uryc gur qentba!","reward":12,
          "expiresIn":5,"encrypted":2,"probability":"Dhvgr yvxryl"}]
        """);

    assertEquals(
        List.of(new Ad("AD123", "Help the dragon!", 12, 5, 2, AdProbability.QUITE_LIKELY)),
        adapter.getAds("game1").block()
    );
  }

  @Test
  void solvesAdAndMapsBusinessFailureWithoutRetrying() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.OK, """
        {"success":false,"lives":2,"gold":97,"score":297,"highScore":0,"turn":17,"message":"You failed on the mission!"}
        """);

    assertEquals(
        new SolveAdResult(false, 2, 97, 297, 0, 17, "You failed on the mission!"),
        adapter.solveAd("game1", "ad1").block()
    );
    assertRequest(HttpMethod.POST, "/game1/solve/ad1");
    assertEquals(1, requests.size());
  }

  @Test
  void mapsShopItemIdentifiersToEnums() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.OK, """
        [{"id":"hpot","name":"Healing potion","cost":50},{"id":"wax","name":"Copper Plating","cost":100}]
        """);

    assertEquals(
        List.of(
            new ShopItem(ShopItemId.HEALING_POTION, "Healing potion", 50),
            new ShopItem(ShopItemId.COPPER_PLATING, "Copper Plating", 100)
        ),
        adapter.getShopItems("game1").block()
    );
    assertRequest(HttpMethod.GET, "/game1/shop");
  }

  @Test
  void buysItemUsingApiIdentifierAndMapsUpdatedState() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.OK, """
        {"shoppingSuccess":true,"gold":25,"lives":3,"level":2,"turn":8}
        """);

    assertEquals(
        new PurchaseResult(true, 25, 3, 2, 8),
        adapter.buyItem("game1", ShopItemId.HEALING_POTION).block()
    );
    assertRequest(HttpMethod.POST, "/game1/shop/buy/hpot");
  }

  @Test
  void preservesUnsuccessfulPurchaseAndItsTurn() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.OK, """
        {"shoppingSuccess":false,"gold":0,"lives":3,"level":0,"turn":1}
        """);

    PurchaseResult result = adapter.buyItem("game1", ShopItemId.CLAW_SHARPENING).block();
    assertNotNull(result);

    assertFalse(result.shoppingSuccess());
    assertEquals(1, result.turn());
    assertRequest(HttpMethod.POST, "/game1/shop/buy/cs");
  }

  @Test
  void handlesEmptyCollections() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.OK, "[]");

    assertEquals(List.of(), adapter.getAds("game1").block());
    assertEquals(List.of(), adapter.getShopItems("game1").block());
  }

  @Test
  void encodesIdentifiersAsUriVariables() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.OK, """
        {"success":true,"lives":3,"gold":1,"score":1,"highScore":0,"turn":1,"message":"Solved"}
        """);

    adapter.solveAd("game 1", "ad+1=").block();

    assertEquals(
        "https://dragonsofmugloar.com/api/v2/game%201/solve/ad%2B1%3D",
        requests.getFirst().url().toString()
    );
  }

  @Test
  void propagatesBadRequestAndUnauthorizedWithBodyWithoutRetrying() {
    List<HttpStatus> statuses = List.of(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
    for (HttpStatus status : statuses) {
      DragonsofmugloarAdapter adapter = adapter(status, "{\"error\":\"No game\"}");

      WebClientResponseException error = assertThrows(WebClientResponseException.class, () -> adapter.startGame().block());

      assertEquals(status.value(), error.getStatusCode().value());
      assertEquals("{\"error\":\"No game\"}", error.getResponseBodyAsString());
    }
    assertEquals(statuses.size(), requests.size());
  }

  @Test
  void mapsOtherHttpErrorsToServerExceptionWithEndpointAndStatusWithoutRetrying() {
    List<HttpStatus> statuses = List.of(HttpStatus.NOT_FOUND, HttpStatus.GONE, HttpStatus.TOO_MANY_REQUESTS,
        HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY);
    for (HttpStatus status : statuses) {
      DragonsofmugloarAdapter adapter = adapter(status, "{\"error\":\"Failure\"}");

      var error = assertThrows(DragonsofmugloarServerException.class, () -> adapter.solveAd("game1", "ad1").block());

      assertEquals(Map.of("endpoint", "POST https://dragonsofmugloar.com/api/v2/{gameId}/solve/{adId}",
          "status", String.valueOf(status.value())), error.errorDetails().params());
      assertTrue(error.errorDetails().recoverable());
    }
    assertEquals(statuses.size(), requests.size());
  }

  @Test
  void mapsConnectionFailureToUnavailableServerException() {
    WebClient client = WebClient.builder()
        .exchangeFunction(_ -> Mono.error(new ConnectException("Connection refused")))
        .build();
    DragonsofmugloarAdapter adapter = new DragonsofmugloarAdapter(
        API_URL, new WebClientWithPool(client, null), Duration.ofSeconds(2)
    );

    var error = assertThrows(DragonsofmugloarServerException.class, () -> adapter.getShopItems("game1").block());

    assertEquals("503", error.errorDetails().params().get("status"));
    assertEquals("GET https://dragonsofmugloar.com/api/v2/{gameId}/shop", error.errorDetails().params().get("endpoint"));
    assertTrue(error.getMessage().contains("Connection refused"));
  }

  @Test
  void rejectsMissingResponseBody() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.NO_CONTENT, "");

    var error = assertThrows(DragonsofmugloarServerException.class, () -> adapter.startGame().block());

    assertTrue(error.getMessage().contains("Empty Dragons of Mugloar response"));
    assertEquals("503", error.errorDetails().params().get("status"));
  }

  @Test
  void rejectsUnsupportedEncryptionRatherThanReturningCorruptFields() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.OK, """
        [{"adId":"ad1","message":"Unknown encoding","reward":1,"expiresIn":2,"encrypted":3,"probability":"Risky"}]
        """);

    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> adapter.getAds("game1").block());

    assertTrue(error.getMessage().contains("Unsupported ad encryption: 3"));
  }

  @Test
  void rejectsUnknownShopItemIdentifier() {
    DragonsofmugloarAdapter adapter = adapter(HttpStatus.OK, """
        [{"id":"new-item","name":"Unknown item","cost":50}]
        """);

    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> adapter.getShopItems("game1").block());

    assertTrue(error.getMessage().contains("Unknown shop item ID: new-item"));
  }

  @Test
  void timesOutWhenApiDoesNotRespond() {
    WebClient client = WebClient.builder().exchangeFunction(_ -> Mono.never()).build();
    DragonsofmugloarAdapter adapter = new DragonsofmugloarAdapter(
        API_URL, new WebClientWithPool(client, null), Duration.ofMillis(20)
    );

    var error = assertThrows(DragonsofmugloarServerException.class, () -> adapter.startGame().block());

    assertTrue(error.getMessage().contains("TimeoutException"), error.getMessage());
    assertEquals("503", error.errorDetails().params().get("status"));
  }

  @Test
  void disposesConnectionPool() {
    WebClientWithPool client = mock(WebClientWithPool.class);
    DragonsofmugloarAdapter adapter = new DragonsofmugloarAdapter(API_URL, client, Duration.ofSeconds(2));

    adapter.destroy();

    verify(client).destroy();
  }

  private DragonsofmugloarAdapter adapter(HttpStatus status, String body) {
    WebClient client = WebClient.builder()
        .exchangeFunction(request -> {
          requests.add(request);
          return Mono.just(
              ClientResponse.create(status)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .body(body)
                  .build()
          );
        })
        .build();
    return new DragonsofmugloarAdapter(API_URL, new WebClientWithPool(client, null), Duration.ofSeconds(2));
  }

  private void assertRequest(HttpMethod method, String path) {
    ClientRequest request = requests.getLast();
    assertEquals(method, request.method());
    assertEquals(API_URL + path, request.url().toString());
    assertNull(request.headers().getFirst(HttpHeaders.AUTHORIZATION));
  }
}
