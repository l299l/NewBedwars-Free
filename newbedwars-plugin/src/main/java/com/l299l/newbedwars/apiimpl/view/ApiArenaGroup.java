package com.l299l.newbedwars.apiimpl.view;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.api.view.ArenaGroupView;
import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public final class ApiArenaGroup implements ArenaGroupView {
    private final ArenaGroup group;

    public ApiArenaGroup(ArenaGroup group) {
        this.group = group;
    }

    @Override
    public String getName() {
        return group.getName();
    }

    @Override
    public List<String> getArenaNames() {
        return List.copyOf(group.getArenas());
    }

    @Override
    public List<ArenaView> getArenas() {
        List<ArenaView> result = new ArrayList<>();
        for (IArena a : group.resolve(ArenaRegistry::find)) result.add(new ApiArena(a));
        return result;
    }

    @Override
    public Material getDisplayItem() {
        Material m = Material.matchMaterial(group.getDisplayItem());
        return m == null ? Material.PAPER : m;
    }

    @Override
    public List<String> getProblems() {
        return group.problems(ArenaRegistry::find);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ApiArenaGroup other && other.group == group;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(group);
    }
}
