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

    /**
     * Creates a mode or replaces the one with the same id (ignoring case, the existing spelling is kept). Only that
     * block of {@code modes.yml} is rewritten, comments elsewhere stay. {@link ModeInfo#builtIn()} is ignored.
     * Valid ids match {@code [A-Za-z0-9_-]{1,32}}; generators, phase list, GUIs and tab list layout must be {@code null}/blank
     * (unchanged) or exist; {@code maxInTeam} and {@code teams} must be 0..100. Arenas are never changed.
     */
    CompletableFuture<EditResult> saveMode(ModeInfo mode);

    /** Deletes a mode. Built-in modes can be edited but not deleted ({@link EditResult.Code#BUILT_IN}). */
    CompletableFuture<EditResult> deleteMode(String id);

    /**
     * Creates a phase list or replaces the one with the same id. It needs at least one phase, durations of 0 or more
     * and registered actions. Arenas are never changed.
     */
    CompletableFuture<EditResult> savePhaseList(PhaseListInfo list);

    /**
     * Deletes a phase list. Refused with {@link EditResult.Code#IN_USE} while a mode or an arena uses it and with
     * {@link EditResult.Code#BUILT_IN} for the lists that ship with NewBedwars.
     */
    CompletableFuture<EditResult> deletePhaseList(String id);

    /** Every generator config file with its id, sorted by id. */
    CompletableFuture<List<GeneratorConfigInfo>> getGeneratorConfigFiles();

    /** The raw YAML text of the generator config with that id, or {@code null} when there is none. */
    CompletableFuture<String> getGeneratorConfigText(String id);

    /**
     * Creates {@code generators/<id>.yml} or replaces the file of that id, then reloads the generator configs. The text
     * must be YAML of at most 64 KB, its {@code ConfigurationId} must equal {@code id} and it must load as a valid
     * generator config. Arenas keep the config object they already use; nothing is re-applied.
     */
    CompletableFuture<EditResult> saveGeneratorConfig(String id, String yaml);

    /**
     * Deletes a generator config file. Refused with {@link EditResult.Code#IN_USE} while a mode or an arena uses it and
     * with {@link EditResult.Code#BUILT_IN} for the configs that ship with NewBedwars.
     */
    CompletableFuture<EditResult> deleteGeneratorConfig(String id);
}
