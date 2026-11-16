package com.l299l.newbedwars.api.view;

import org.bukkit.Material;

import java.util.List;

/** An arena group: the maps players vote between at the end of a game. */
public interface ArenaGroupView {
    String getName();

    /** Arena names in the order they were added. Some of them may not exist anymore. */
    List<String> getArenaNames();

    /** Only the arenas that exist. */
    List<ArenaView> getArenas();

    /** Icon of the group's maps in the vote menu. */
    Material getDisplayItem();

    /** What is wrong with the group, e.g. missing arenas or different team sizes. Empty when nothing is. */
    List<String> getProblems();
}
