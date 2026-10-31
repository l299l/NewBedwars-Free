package com.l299l.newbedwars.api.view;

import org.bukkit.Material;

import java.util.List;

/** Read-only view of an arena group: maps the waiting lobby votes between. */
public interface ArenaGroupView {
    String getName();

    /** Names of the group's arenas, in the order they were added (some may not exist any more). */
    List<String> getArenaNames();

    /** The group's arenas that exist. */
    List<ArenaView> getArenas();

    /** Icon of the group's maps in the vote menu. */
    Material getDisplayItem();

    /** Why the group does not work as it should (missing arenas, different team sizes); empty when fine. */
    List<String> getProblems();
}
