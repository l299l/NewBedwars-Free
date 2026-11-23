package com.l299l.newbedwars.api.mode;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Game modes and phase lists from {@code modes.yml}. Every method can be called from any thread; the work runs on the
 * server thread and the futures complete there.
 *
 * @since 2.8-beta
 */
public interface ModeService {
    /** Modes in the order of {@code modes.yml}, then built-in modes the file doesn't list. */
    CompletableFuture<List<ModeInfo>> getModes();

    /** Every loaded phase list. */
    CompletableFuture<List<PhaseListInfo>> getPhaseLists();

    /** The current mode-related settings of every arena, sorted by name. */
    CompletableFuture<List<ArenaModeInfo>> getArenaModes();

    /** Ids of every loaded generator config, sorted. */
    CompletableFuture<List<String>> getGeneratorConfigs();

    /**
     * Copies a mode into an arena once, like {@code /bw setMode}: generator config, phase list, GUIs and team size,
     * each only when the mode sets it. The arena must be waiting for players. Later edits to the mode don't change the
     * arena. Mode names are matched ignoring case.
     */
    CompletableFuture<ModeResult> applyMode(String arena, String mode);
}
