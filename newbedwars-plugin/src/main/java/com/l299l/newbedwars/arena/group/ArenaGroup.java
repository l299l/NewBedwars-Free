package com.l299l.newbedwars.arena.group;

import com.l299l.newbedwars.arena.IArena;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ArenaGroup {
    public static final String DEFAULT_ITEM = "PAPER";

    private String name;
    private List<String> arenas = new ArrayList<>();

    private String displayItem = DEFAULT_ITEM;

    public ArenaGroup() {}

    public ArenaGroup(String name, String displayItem) {
        this.name = name;
        this.displayItem = displayItem == null ? DEFAULT_ITEM : displayItem;
    }

    public String getName() {
        return name;
    }

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

    public record Member(String name, int teams, int maxInTeam) {
        public static Member of(IArena a) {
            return new Member(a.getArenaName(), a.getTeams().size(), a.getMaxInTeam());
        }

        String size() {
            return teams + "x" + maxInTeam;
        }

        boolean sameSize(Member o) {
            return teams == o.teams && maxInTeam == o.maxInTeam;
        }
    }

    public String incompatibility(IArena candidate, Function<String, IArena> lookup) {
        return incompatibility(List.of(Member.of(candidate)), local(lookup));
    }

    public String incompatibility(List<Member> candidate, Function<String, List<Member>> resolve) {
        for (String other : getArenas()) {
            for (Member a : resolve.apply(other)) {
                for (Member c : candidate) {
                    if (!a.equals(c) && !a.sameSize(c)) {
                        return a.name() + " has " + a.size() + ", " + c.name() + " has " + c.size();
                    }
                }
            }
        }
        return null;
    }

    public List<String> problems(Function<String, IArena> lookup) {
        return problemsOf(local(lookup));
    }

    public List<String> problemsOf(Function<String, List<Member>> resolve) {
        List<String> problems = new ArrayList<>();
        Member first = null;
        int count = 0;
        for (String name : getArenas()) {
            List<Member> found = resolve.apply(name);
            if (found.isEmpty()) {
                problems.add("arena " + name + " does not exist");
                continue;
            }
            for (Member a : found) {
                count++;
                if (first == null) {
                    first = a;
                } else if (!a.sameSize(first)) {
                    problems.add(a.name() + " has " + a.size() + " but " + first.name() + " has " + first.size());
                }
            }
        }
        if (getArenas().size() < 2 && count < 2) problems.add("needs at least 2 arenas for voting");
        return problems;
    }

    private static Function<String, List<Member>> local(Function<String, IArena> lookup) {
        return name -> {
            IArena a = lookup.apply(name);
            return a == null ? List.of() : List.of(Member.of(a));
        };
    }

    public List<IArena> resolve(Function<String, IArena> lookup) {
        List<IArena> result = new ArrayList<>();
        for (String name : getArenas()) {
            IArena a = lookup.apply(name);
            if (a != null) result.add(a);
        }
        return result;
    }
}
