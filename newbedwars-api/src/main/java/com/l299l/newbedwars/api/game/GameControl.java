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
}
