package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.tablist.TablistPlaceholder;
import com.l299l.newbedwars.api.tablist.TablistService;
import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.apiimpl.adapter.ApiRegistrations;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.tablist.TablistManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ApiTablists implements TablistService {
    private final ApiRegistrations registrations;

    public ApiTablists(ApiRegistrations registrations) {
        this.registrations = registrations;
    }

    private static TablistManager manager() {
        return NewBedwars.plugin == null ? null : NewBedwars.plugin.getTablistManager();
    }

    @Override
    public boolean isEnabled() {
        return Properties.TablistEnabled;
    }

    @Override
    public List<String> getLayouts() {
        TablistManager manager = manager();
        if (manager == null) return List.of();
        List<String> ids = new ArrayList<>(manager.getConfig().layouts().keySet());
        ids.sort(Comparator.naturalOrder());
        return ids;
    }

    @Override
    public String getDefaultLayout() {
        return Properties.DefaultTablist;
    }

    @Override
    public String getEffectiveLayout(ArenaView arena) {
        TablistManager manager = manager();
        String own = arena == null ? null : arena.getTablist();
        if (manager != null) return manager.effectiveLayoutId(own);
        return own == null || own.isBlank() ? Properties.DefaultTablist : own;
    }

    @Override
    public boolean isLobbyEnabled() {
        TablistManager manager = manager();
        return manager != null && manager.getConfig().isLobbyEnabled();
    }

    @Override
    public void registerPlaceholder(Plugin owner, String name, TablistPlaceholder placeholder) {
        if (placeholder == null) throw new IllegalArgumentException("placeholder is null");
        new ApiPlaceholders(registrations).register(owner, name, placeholder::resolve);
    }

    @Override
    public void setPlayerLayout(Player player, String layoutId) {
        if (player == null) throw new IllegalArgumentException("player is null");
        TablistManager manager = manager();
        if (manager == null) throw new IllegalArgumentException("there is no tab list layout " + layoutId);
        manager.setOverride(player, layoutId);
    }

    @Override
    public void clearPlayerLayout(Player player) {
        TablistManager manager = manager();
        if (player != null && manager != null) manager.clearOverride(player);
    }

    @Override
    public void refresh(Player player) {
        TablistManager manager = manager();
        if (player != null && manager != null) manager.refresh(player);
    }

    @Override
    public void refresh(ArenaView arena) {
        TablistManager manager = manager();
        IArena internal = arena == null ? null : ArenaRegistry.byName(arena.getName());
        if (manager != null && internal != null) manager.refresh(internal);
    }
}
