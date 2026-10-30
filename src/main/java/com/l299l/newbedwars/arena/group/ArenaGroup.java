package com.l299l.newbedwars.arena.group;

import com.l299l.newbedwars.arena.IArena;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** A named set of arenas (maps) players vote between in the waiting lobby. Saved in {@code data/groups.json}. */
public final class ArenaGroup {
    public static final String DEFAULT_ITEM = "PAPER";

    private String name;
    private List<String> arenas = new ArrayList<>();
    /** Material name of the icon shown for the group's maps in the vote menu. */
    private String displayItem = DEFAULT_ITEM;

    public ArenaGroup() {}

    public ArenaGroup(String name, String displayItem) {
        this.name = name;
        this.displayItem = displayItem == null ? DEFAULT_ITEM : displayItem;
    }

    public String getName() {
        return name;
    }

    /** Arena names in the order they were added. */
    public List<String> getArenas() {
        if (arenas == null) arenas = new ArrayList<>();
        return arenas;
    }

    public String getDisplayItem() {
        return displayItem == null ? DEFAULT_ITEM : displayItem;
    }

    public void setDisplayItem(String displayItem) {
        this.displayItem = displayItem;
    }

    public boolean contains(String arenaName) {
        for (String a : getArenas()) {
            if (a.equalsIgnoreCase(arenaName)) return true;
        }
        return false;
    }

    public void add(String arenaName) {
        if (!contains(arenaName)) getArenas().add(arenaName);
    }

    public boolean remove(String arenaName) {
        return getArenas().removeIf(a -> a.equalsIgnoreCase(arenaName));
    }

    /**
     * Why {@code candidate} can not join this group, or null when it can: every arena of a group needs the
     * same team count and players per team.
     */
    public String incompatibility(IArena candidate, Function<String, IArena> lookup) {
        for (String other : getArenas()) {
            IArena a = lookup.apply(other);
            if (a == null || a == candidate) continue;
            if (a.getTeams().size() != candidate.getTeams().size() || a.getMaxInTeam() != candidate.getMaxInTeam()) {
                return a.getArenaName() + " has " + size(a) + ", " + candidate.getArenaName() + " has " + size(candidate);
            }
        }
        return null;
    }

    /** Problems of the group as it is now: missing arenas and arenas with a different team size. */
    public List<String> problems(Function<String, IArena> lookup) {
        List<String> problems = new ArrayList<>();
        IArena first = null;
        for (String name : getArenas()) {
            IArena a = lookup.apply(name);
            if (a == null) {
                problems.add("arena " + name + " does not exist");
                continue;
            }
            if (first == null) {
                first = a;
            } else if (a.getTeams().size() != first.getTeams().size() || a.getMaxInTeam() != first.getMaxInTeam()) {
                problems.add(a.getArenaName() + " has " + size(a) + " but " + first.getArenaName() + " has " + size(first));
            }
        }
        if (getArenas().size() < 2) problems.add("needs at least 2 arenas for voting");
        return problems;
    }

    /** The group's arenas that exist (in group order). */
    public List<IArena> resolve(Function<String, IArena> lookup) {
        List<IArena> result = new ArrayList<>();
        for (String name : getArenas()) {
            IArena a = lookup.apply(name);
            if (a != null) result.add(a);
        }
        return result;
    }

    static String size(IArena a) {
        return a.getTeams().size() + "x" + a.getMaxInTeam();
    }
}
