package com.l299l.newbedwars.api.mode;

/**
 * The mode-related settings an arena has right now (what {@link ModeService#applyMode} changes).
 *
 * @param arena      arena name
 * @param waiting    {@code true} while the arena waits for players, the only time a mode can be applied
 * @param generators generator config id, or {@code null}
 * @param gamePhases phase list id, or {@code null}
 * @param shopGui    shop GUI id, or {@code null}
 * @param upgradeGui upgrade GUI id, or {@code null}
 * @param maxInTeam  players per team
 * @param teams      number of teams
 * @since 2.8-beta
 */
public record ArenaModeInfo(String arena, boolean waiting, String generators, String gamePhases, String shopGui,
                            String upgradeGui, int maxInTeam, int teams) {
}
