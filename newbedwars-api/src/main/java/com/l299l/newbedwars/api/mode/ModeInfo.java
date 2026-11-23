package com.l299l.newbedwars.api.mode;

/**
 * A game mode from {@code modes.yml}: the settings {@link ModeService#applyMode} copies into an arena.
 *
 * @param id         mode name, e.g. {@code Squads}
 * @param generators generator config id, or {@code null} when the mode leaves it unchanged
 * @param gamePhases phase list id, or {@code null} when the mode leaves it unchanged
 * @param shopGui    shop GUI id, or {@code null} when the mode leaves it unchanged
 * @param upgradeGui upgrade GUI id, or {@code null} when the mode leaves it unchanged
 * @param maxInTeam  players per team, 0 when the mode leaves it unchanged
 * @param teams      team count the mode is made for (only a hint, never applied), 0 when not set
 * @param builtIn    {@code true} for a mode that ships with NewBedwars
 * @since 2.8-beta
 */
public record ModeInfo(String id, String generators, String gamePhases, String shopGui, String upgradeGui,
                       int maxInTeam, int teams, boolean builtIn) {
}
