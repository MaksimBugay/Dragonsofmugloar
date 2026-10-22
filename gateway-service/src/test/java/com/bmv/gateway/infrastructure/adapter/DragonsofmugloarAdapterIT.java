/**
 * Classification: Trimble Confidential.
 */
package com.bmv.gateway.infrastructure.adapter;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.bmv.dragonsofmugloar.domain.model.Ad;
import com.bmv.dragonsofmugloar.domain.model.AdProbability;
import com.bmv.dragonsofmugloar.domain.model.Game;
import com.bmv.dragonsofmugloar.domain.model.PurchaseResult;
import com.bmv.dragonsofmugloar.domain.model.ShopItem;
import com.bmv.dragonsofmugloar.domain.model.ShopItemId;
import com.bmv.dragonsofmugloar.domain.model.SolveAdResult;
import com.bmv.dragonsofmugloar.infrastructure.adapter.DragonsofmugloarAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live integration test against <a href="https://dragonsofmugloar.com/api/v2">...</a>, without mocks or credentials.
 * The IT suffix keeps this test outside the default Surefire test selection.
 * Run from the repository root with:
 * mvn -pl gateway-service -am -Dtest=DragonsofmugloarAdapterIT -Dsurefire.failIfNoSpecifiedTests=false test
 */
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class DragonsofmugloarAdapterIT {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(65);

  private DragonsofmugloarAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new DragonsofmugloarAdapter();
  }

  @AfterEach
  void tearDown() {
    adapter.destroy();
  }

  @Test
  void startsGameListsShopAttemptsPurchaseListsAdsAndSolvesAnAd() {
    Game game = adapter.startGame().block(REQUEST_TIMEOUT);

    assertNotNull(game);
    assertNotNull(game.gameId());
    assertFalse(game.gameId().isBlank());
    assertTrue(game.lives() > 0);
    assertEquals(0, game.gold());
    assertEquals(0, game.score());

    List<ShopItem> items = adapter.getShopItems(game.gameId()).block(REQUEST_TIMEOUT);

    assertNotNull(items);
    assertFalse(items.isEmpty());
    items.forEach(item -> {
      assertNotNull(item.id());
      assertNotNull(item.name());
      assertFalse(item.name().isBlank());
      assertTrue(item.cost() > 0);
    });
    ShopItem healingPotion = items.stream()
        .filter(item -> item.id() == ShopItemId.HEALING_POTION)
        .findFirst()
        .orElseThrow(() -> new AssertionError("The live shop must offer a healing potion"));
    assertTrue(healingPotion.cost() > game.gold());

    // A fresh game cannot afford a potion. A rejected purchase must still advance its turn.
    PurchaseResult purchase = adapter.buyItem(game.gameId(), healingPotion.id()).block(REQUEST_TIMEOUT);

    assertNotNull(purchase);
    assertFalse(purchase.shoppingSuccess());
    assertEquals(game.gold(), purchase.gold());
    assertEquals(game.lives(), purchase.lives());
    assertEquals(game.level(), purchase.level());
    assertEquals(game.turn() + 1, purchase.turn());

    List<Ad> ads = adapter.getAds(game.gameId()).block(REQUEST_TIMEOUT);

    assertNotNull(ads);
    assertFalse(ads.isEmpty());
    ads.forEach(ad -> {
      assertNotNull(ad.adId());
      assertFalse(ad.adId().isBlank());
      assertNotNull(ad.message());
      assertFalse(ad.message().isBlank());
      assertTrue(ad.reward() >= 0);
      assertTrue(ad.expiresIn() > 0);
      assertNotNull(ad.probability());
        assertNotSame(AdProbability.UNKNOWN, ad.probability(), "The live ad probability label must be recognized");
    });
    Ad selectedAd = ads.stream()
        .max(Comparator.comparingInt((Ad ad) -> ad.probability().getSuccessPercentage()).thenComparingLong(Ad::reward))
        .orElseThrow();

    SolveAdResult result = adapter.solveAd(game.gameId(), selectedAd.adId()).block(REQUEST_TIMEOUT);

    assertNotNull(result);
    assertEquals(purchase.turn() + 1, result.turn());
    assertNotNull(result.message());
    assertFalse(result.message().isBlank());
    assertTrue(result.highScore() >= 0);
    // Mission outcomes are random; verify the state transition for either outcome.
    if (result.success()) {
      assertEquals(purchase.lives(), result.lives());
      assertEquals(purchase.gold() + selectedAd.reward(), result.gold());
      assertEquals(game.score() + selectedAd.reward(), result.score());
    } else {
      assertEquals(purchase.lives() - 1, result.lives());
      assertEquals(purchase.gold(), result.gold());
      assertEquals(game.score(), result.score());
    }
  }
}
