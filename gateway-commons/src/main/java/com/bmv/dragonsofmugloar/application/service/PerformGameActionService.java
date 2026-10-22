package com.bmv.dragonsofmugloar.application.service;

import com.bmv.dragonsofmugloar.domain.model.Ad;
import com.bmv.dragonsofmugloar.domain.model.GameConfig;
import com.bmv.dragonsofmugloar.domain.model.GameState;
import com.bmv.dragonsofmugloar.domain.model.PurchaseResult;
import com.bmv.dragonsofmugloar.domain.model.ShopItem;
import com.bmv.dragonsofmugloar.domain.model.SolveAdResult;
import com.bmv.dragonsofmugloar.domain.model.GameAction;
import com.bmv.dragonsofmugloar.domain.model.GameActionWithExecutionContext;
import com.bmv.dragonsofmugloar.port.out.DragonsofmugloarPort;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static com.bmv.dragonsofmugloar.application.service.ChooseGameActionService.isProfitable;
import static com.bmv.dragonsofmugloar.application.service.ChooseGameActionService.isSuspectedTrap;

/**
 * Executes a {@link GameActionWithExecutionContext} selected by {@link ChooseGameActionService}:
 * ranks the filtered ad candidates or buys the selected shop item, then applies the API result to the game state.
 */
public class PerformGameActionService {

    /**
     * Higher expected value, then higher success probability, then sooner expiry.
     */
    static final Comparator<Ad> VALUE_RANKING = Comparator
            .comparingDouble(ChooseGameActionService::expectedValue).reversed()
            .thenComparing(PerformGameActionService::successPercentage, Comparator.reverseOrder())
            .thenComparingInt(Ad::expiresIn);

    /**
     * Higher success probability, then higher reward, then sooner expiry.
     */
    static final Comparator<Ad> SURVIVAL_RANKING = Comparator
            .comparing(PerformGameActionService::successPercentage, Comparator.reverseOrder())
            .thenComparing(Ad::reward, Comparator.reverseOrder())
            .thenComparingInt(Ad::expiresIn);

    /**
     * Ordinary ads before suspected traps, then by survival ranking.
     */
    private static final Comparator<Ad> ORDINARY_FIRST_SURVIVAL_RANKING = Comparator
            .comparing(ChooseGameActionService::isSuspectedTrap)
            .thenComparing(SURVIVAL_RANKING);

    private final DragonsofmugloarPort dragonsofmugloarPort;

    public PerformGameActionService(DragonsofmugloarPort dragonsofmugloarPort) {
        this.dragonsofmugloarPort = dragonsofmugloarPort;
    }

    /**
     * Starts a new game and records it as the last action. A new game has a known neutral reputation.
     */
    public Mono<GameState> startGame() {
        return dragonsofmugloarPort.startGame()
                .map(game -> withLastAction(new GameState(game.gameId(), game.gold(), game.lives(), game.level(),
                        game.turn(), game.score(), game.highScore()), GameAction.START_GAME));
    }

    /**
     * Buys the healing potion already selected by the chooser.
     */
    public Mono<GameState> buyHealingPotion(GameState currentState, ShopItem shopItem) {
        return buy(currentState, GameAction.BUY_HEALING_POTION, shopItem);
    }

    /**
     * Investigates reputation. The investigation takes a turn, so the local turn is incremented.
     */
    public Mono<GameState> refreshReputation(GameState currentState) {
        return dragonsofmugloarPort.investigateReputation(currentState.gameId())
                .map(reputation -> {
                    long turn = currentState.turn() + 1;
                    return new GameState(currentState.gameId(), currentState.gold(), currentState.lives(),
                            currentState.level(), turn, currentState.score(), currentState.highScore(),
                            reputation, turn, currentState.startedAt(), GameAction.REFRESH_REPUTATION);
                });
    }

    /**
     * Solves the ordinary ad with the best survival ranking, or the best suspected trap if no ordinary ad exists.
     */
    public Mono<GameState> solveSafestAdOnLastLife(GameState currentState, List<Ad> ads) {
        return solve(currentState, ads, ORDINARY_FIRST_SURVIVAL_RANKING, GameAction.SOLVE_SAFEST_AD_ON_LAST_LIFE);
    }

    /**
     * Solves the best-valued ordinary ad with an easy probability and a positive expected value.
     */
    public Mono<GameState> solveProfitableAd(GameState currentState, List<Ad> ads, GameConfig gameConfig) {
        return solve(currentState, ordinary(ads).filter(ad -> isProfitable(gameConfig, ad)).toList(), VALUE_RANKING,
                GameAction.SOLVE_PROFITABLE_AD);
    }

    /**
     * Buys the upgrade already selected by the chooser.
     */
    public Mono<GameState> buyUpgrade(GameState currentState, ShopItem shopItem) {
        return buy(currentState, GameAction.BUY_UPGRADE, shopItem);
    }

    /**
     * Solves the best-valued ordinary ad, even if its expected value is nonpositive.
     */
    public Mono<GameState> solveBestRemainingAd(GameState currentState, List<Ad> ads) {
        return solve(currentState, ordinary(ads).toList(), VALUE_RANKING, GameAction.SOLVE_BEST_REMAINING_AD);
    }

    /**
     * Attempts the suspected trap with the best survival ranking.
     */
    public Mono<GameState> attemptSafestTrap(GameState currentState, List<Ad> ads) {
        return solve(currentState, ads.stream().filter(ChooseGameActionService::isSuspectedTrap).toList(),
                SURVIVAL_RANKING, GameAction.ATTEMPT_SAFEST_TRAP);
    }

    /**
     * Records the skipped turn without advancing the turn or calling the API.
     */
    public Mono<GameState> skipTurnNoAvailableAds(GameState currentState) {
        return Mono.just(withLastAction(currentState, GameAction.SKIP_TURN_NO_AVAILABLE_ADS));
    }

    public Mono<GameState> performGameAction(GameState currentState, GameActionWithExecutionContext executionContext) {
        List<Ad> ads = executionContext.ads();
        GameConfig gameConfig = executionContext.gameConfig();
        return switch (executionContext.gameAction()) {
            case BUY_HEALING_POTION -> buyHealingPotion(currentState, executionContext.shopItem());
            case REFRESH_REPUTATION -> refreshReputation(currentState);
            case SOLVE_SAFEST_AD_ON_LAST_LIFE -> solveSafestAdOnLastLife(currentState, ads);
            case SOLVE_PROFITABLE_AD -> solveProfitableAd(currentState, ads, gameConfig);
            case BUY_UPGRADE -> buyUpgrade(currentState, executionContext.shopItem());
            case SOLVE_BEST_REMAINING_AD -> solveBestRemainingAd(currentState, ads);
            case ATTEMPT_SAFEST_TRAP -> attemptSafestTrap(currentState, ads);
            case SKIP_TURN_NO_AVAILABLE_ADS -> skipTurnNoAvailableAds(currentState);
            // Lifecycle actions are never chosen for a turn: startGame starts a game, the game loop stops it.
            case START_GAME, STOP_GAME -> throw new IllegalArgumentException(
                    "Not a turn action: " + executionContext.gameAction());
        };
    }

    private Mono<GameState> buy(GameState currentState, GameAction gameAction, ShopItem shopItem) {
        return dragonsofmugloarPort.buyItem(currentState.gameId(), shopItem.id())
                .map(result -> withPurchaseResult(currentState, result, gameAction));
    }

    private Mono<GameState> solve(GameState currentState, List<Ad> candidates, Comparator<Ad> ranking,
                                  GameAction gameAction) {
        return Mono.fromCallable(() -> candidates.stream().min(ranking).orElseThrow())
                .flatMap(ad -> dragonsofmugloarPort.solveAd(currentState.gameId(), ad.adId()))
                .map(result -> withSolveAdResult(currentState, result, gameAction));
    }

    private static Stream<Ad> ordinary(List<Ad> ads) {
        return ads.stream().filter(ad -> !isSuspectedTrap(ad));
    }

    private static GameState withSolveAdResult(GameState currentState, SolveAdResult result,
                                               GameAction gameAction) {
        return new GameState(currentState.gameId(), result.gold(), result.lives(), currentState.level(),
                result.turn(), result.score(), result.highScore(), currentState.lastInvestigatedReputation(),
                currentState.lastReputationInvestigationTurn(), currentState.startedAt(), gameAction);
    }

    private static GameState withPurchaseResult(GameState currentState, PurchaseResult result,
                                                GameAction gameAction) {
        return new GameState(currentState.gameId(), result.gold(), result.lives(), result.level(),
                result.turn(), currentState.score(), currentState.highScore(),
                currentState.lastInvestigatedReputation(), currentState.lastReputationInvestigationTurn(),
                currentState.startedAt(), gameAction);
    }

    private static GameState withLastAction(GameState currentState, GameAction gameAction) {
        return new GameState(currentState.gameId(), currentState.gold(), currentState.lives(), currentState.level(),
                currentState.turn(), currentState.score(), currentState.highScore(),
                currentState.lastInvestigatedReputation(), currentState.lastReputationInvestigationTurn(),
                currentState.startedAt(), gameAction);
    }

    private static int successPercentage(Ad ad) {
        return ad.probability().getSuccessPercentage();
    }
}
