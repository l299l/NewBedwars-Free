package com.l299l.newbedwars.api.mode;

import java.util.List;

/**
 * A phase list ({@code GamePhases} entry in {@code modes.yml}).
 *
 * @param id     phase list id
 * @param phases the phases in order
 * @since 2.8-beta
 */
public record PhaseListInfo(String id, List<PhaseInfo> phases) {
    public PhaseListInfo {
        phases = phases == null ? List.of() : List.copyOf(phases);
    }
}
