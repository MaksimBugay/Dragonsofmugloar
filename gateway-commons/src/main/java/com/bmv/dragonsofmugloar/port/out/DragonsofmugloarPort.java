package com.bmv.dragonsofmugloar.port.out;

import java.util.List;

import com.bmv.dragonsofmugloar.domain.model.Ad;
import com.bmv.dragonsofmugloar.domain.model.Game;
import com.bmv.dragonsofmugloar.domain.model.PurchaseResult;
import com.bmv.dragonsofmugloar.domain.model.Reputation;
import com.bmv.dragonsofmugloar.domain.model.ShopItem;
import com.bmv.dragonsofmugloar.domain.model.ShopItemId;
import com.bmv.dragonsofmugloar.domain.model.SolveAdResult;
import reactor.core.publisher.Mono;

/**
 * Outbound operations for the Dragons of Mugloar game API.
 */
public interface DragonsofmugloarPort {

  /**
   * Starts a game and returns its identifier and initial state.
   */
  Mono<Game> startGame();

  /**
   * Fetches the current ads. Expiration is measured in game turns.
   */
  Mono<List<Ad>> getAds(String gameId);

  /**
   * Attempts one ad and returns the outcome and updated game state.
   */
  Mono<SolveAdResult> solveAd(String gameId, String adId);

  /**
   * Lists the items currently available in the game's shop.
   */
  Mono<List<ShopItem>> getShopItems(String gameId);

  /**
   * Attempts to buy an item. The turn advances even if the purchase fails.
   */
  Mono<PurchaseResult> buyItem(String gameId, ShopItemId itemId);

  /**
   * Investigates the player's reputation with people, the state and the underworld.
   */
  Mono<Reputation> investigateReputation(String gameId);
}
