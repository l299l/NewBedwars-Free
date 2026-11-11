package com.l299l.newbedwars.api.game;

import com.l299l.newbedwars.api.maintenance.MaintenanceResult;

import java.util.concurrent.CompletableFuture;

/**
 * Controls a running game like {@code /bw game}. Get it with {@code NewBedwarsAPI.getGameControl()}.
 * Every method can be called from any thread; the work runs on the server thread. A refused action
 * (wrong game state) completes normally with {@link MaintenanceResult.Code#NOT_READY}; an unknown arena with
 * {@link MaintenanceResult.Code#NOT_FOUND}. {@code actor} is the name shown to players (e.g. {@code "Panel: Alex"}).
 * Available from 2.7-beta.
 */
public interface GameControl {
    /** Starts the countdown of a waiting arena ({@code /bw game start}). */
    CompletableFuture<MaintenanceResult> start(String arena, String actor);

    /** Starts the game now, skipping the countdown; the arena must be waiting or starting. */
    CompletableFuture<MaintenanceResult> forceStart(String arena, String actor);

    /** Ends a running game as a draw (statistics are saved). */
    CompletableFuture<MaintenanceResult> stop(String arena, String actor);

    /** Stops the arena at once in any state and resets it. */
    CompletableFuture<MaintenanceResult> forceStop(String arena, String actor);

    /** Skips to the next game phase of a running game. */
    CompletableFuture<MaintenanceResult> nextPhase(String arena, String actor);

    /**
     * Sends a chat message to every player and spectator of the arena, in the {@code AdminBroadcast} format.
     * {@code &} colour codes are allowed; 1 to 256 characters ({@link MaintenanceResult.Code#INVALID_NAME} otherwise).
     */
    CompletableFuture<MaintenanceResult> broadcast(String arena, String actor, String message);
}
