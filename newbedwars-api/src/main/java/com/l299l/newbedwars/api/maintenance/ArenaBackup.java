package com.l299l.newbedwars.api.maintenance;

/**
 * One backup of an arena.
 *
 * @param id           pass it to {@link ArenaMaintenance#restore}; a timestamp, newer ids sort higher
 * @param hasArenaFile the backup contains the arena settings
 * @param hasMap       the backup contains the map
 */
public record ArenaBackup(String id, boolean hasArenaFile, boolean hasMap) {
}
