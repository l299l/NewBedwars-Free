package com.l299l.newbedwars.api.game;

import com.l299l.newbedwars.api.maintenance.MaintenanceResult;

import java.util.concurrent.CompletableFuture;

/**
 * Controls games like {@code /bw game}. Get it with {@code NewBedwarsAPI.getGameControl()}.
 * <p>
 * Every method can be called from any thread. The work runs on the server thread and the future completes
 * there. An action the arena's state doesn't allow completes with {@link MaintenanceResult.Code#NOT_READY},
 * an unknown arena with {@link MaintenanceResult.Code#NOT_FOUND}. {@code actor} is the name players see,
 * e.g. {@code "Panel: Alex"}.
 *
 * @since 2.7-beta
 */
public interface GameControl {
    /** Starts the countdown of a waiting arena. */
    CompletableFuture<MaintenanceResult> start(String arena, String actor);

    /** Starts the game now, skipping the countdown. The arena must be waiting or counting down. */
    CompletableFuture<MaintenanceResult> forceStart(String arena, String actor);

    /** Ends a running game as a draw. Stats are saved. */
    CompletableFuture<MaintenanceResult> stop(String arena, String actor);

    /** Stops the arena at once, in any state, and resets it. */
    CompletableFuture<MaintenanceResult> forceStop(String arena, String actor);

    /** Skips to the next phase of a running game. */
    CompletableFuture<MaintenanceResult> nextPhase(String arena, String actor);

    /**
     * Sends a highlighted chat message to every player and spectator of the arena. {@code &} color codes
     * work.
     *
     * @param message 1 to 256 characters, otherwise the result is {@link MaintenanceResult.Code#INVALID_NAME}
     */
    CompletableFuture<MaintenanceResult> broadcast(String arena, String actor, String message);

    /**
     * Destroys the bed of a team in a running game, as if an enemy broke it: the team can't respawn any more.
     * {@link com.l299l.newbedwars.api.events.BedBreakEvent} is fired with a null breaker and can cancel it.
     * A team whose bed is already gone completes with {@link MaintenanceResult.Code#ALREADY_DONE}, an unknown
     * team with {@link MaintenanceResult.Code#NOT_FOUND}.
     *
     * @since 2.8-beta
     */
    CompletableFuture<MaintenanceResult> destroyBed(String arena, String team, String actor);

    /**
     * Puts a team out of a running game: its bed is destroyed and its players become spectators, then the win
     * check runs (the game ends when only one team is left). A team that is already out completes with
     * {@link MaintenanceResult.Code#ALREADY_DONE}.
     *
     * @since 2.8-beta
     */
    CompletableFuture<MaintenanceResult> eliminateTeam(String arena, String team, String actor);

    /**
     * Sets the level of a team upgrade or the number of a trap in a running game, without a price. Raising an
     * upgrade applies each level like a purchase; an upgrade can't be lowered (its effects can't be taken back),
     * only traps can. {@link com.l299l.newbedwars.api.events.TeamUpgradeEvent} is not fired.
     *
     * @param upgrade an upgrade name such as {@code SHARPNESS} or the name of an addon upgrade
     * @param level 0 to the upgrade's highest level; otherwise the result is {@link MaintenanceResult.Code#INVALID_NAME}
     * @since 2.8-beta
     */
    CompletableFuture<MaintenanceResult> setUpgradeLevel(String arena, String team, String upgrade, int level, String actor);
}
