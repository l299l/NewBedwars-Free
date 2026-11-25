package com.l299l.newbedwars.tablist;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.l299l.newbedwars.apiimpl.ApiEvents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class TablistManager {
    private static final long PERIOD_TICKS = 20L;

    private final TablistConfig config;
    private final Logger log;
    private final Map<UUID, String[]> applied = new HashMap<>();
    private final Map<UUID, String[]> rendered = new HashMap<>();
    private final Map<UUID, String[]> overrides = new HashMap<>();
    private final Set<String> warned = new HashSet<>();
    private BukkitTask task;
    private boolean headerFooterUnavailable;

    public TablistManager(TablistConfig config, Logger log) {
        this.config = config == null ? TablistConfig.empty() : config;
        this.log = log;
    }

    public static TablistManager load(FileConfiguration conf, Logger log) {
        return new TablistManager(TablistConfig.parse(conf, log), log);
    }

    public TablistConfig getConfig() {
        return config;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(NewBedwars.plugin, this::updateAll, 20L, PERIOD_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (UUID id : new HashSet<>(applied.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) restore(player);
        }
        applied.clear();
        rendered.clear();
    }

    public void inheritOverrides(TablistManager old) {
        for (Map.Entry<UUID, String[]> e : old.overrides.entrySet()) {
            String[] value = e.getValue();
            TablistLayout layout = config.find(value[0]);
            if (layout != null) overrides.put(e.getKey(), new String[]{layout.id(), value[1]});
        }
    }

    public void updateAll() {
        applied.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        rendered.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        overrides.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        for (Player player : Bukkit.getOnlinePlayers()) update(player);
    }

    public boolean hasLayout(String id) {
        return config.find(id) != null;
    }

    public TablistLayout layoutFor(IArena arena) {
        return layoutFor(arena, null);
    }

    public TablistLayout layoutFor(IArena arena, Player player) {
        String[] override = player == null ? null : overrides.get(player.getUniqueId());
        TablistLayout forced = override == null ? null : config.find(override[0]);
        if (forced != null) return forced;
        String id = arena == null ? null : arena.getTablistId();
        if (id == null || id.isBlank()) id = Properties.DefaultTablist;
        TablistLayout layout = config.find(id);
        if (layout != null) return layout;
        if (warned.add(id) && log != null) {
            log.warning("tablists.yml has no layout '" + id + "'; using '" + TablistConfig.DEFAULT_ID + "' instead");
        }
        return config.find(TablistConfig.DEFAULT_ID);
    }

    public String effectiveLayoutId(String arenaLayout) {
        String id = arenaLayout == null || arenaLayout.isBlank() ? Properties.DefaultTablist : arenaLayout;
        TablistLayout layout = config.find(id);
        if (layout == null) layout = config.find(TablistConfig.DEFAULT_ID);
        return layout == null ? TablistConfig.DEFAULT_ID : layout.id();
    }

    public void setOverride(Player player, String layoutId) {
        TablistLayout layout = config.find(layoutId);
        if (layout == null) throw new IllegalArgumentException("there is no tab list layout " + layoutId);
        IArena arena = membership(player);
        overrides.put(player.getUniqueId(), new String[]{layout.id(), arena == null ? null : arena.getArenaName()});
        refresh(player);
    }

    public void clearOverride(Player player) {
        if (overrides.remove(player.getUniqueId()) != null) refresh(player);
    }

    public void forget(Player player) {
        overrides.remove(player.getUniqueId());
    }

    public void refresh(Player player) {
        if (player == null) return;
        rendered.remove(player.getUniqueId());
        update(player);
    }

    public void refresh(IArena arena) {
        if (arena == null) return;
        for (UUID id : new HashSet<>(arena.getPlayers())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) refresh(p);
        }
        for (Player p : new ArrayList<>(arena.getSpectators())) refresh(p);
    }

    private static IArena membership(Player player) {
        IArena arena = ArenaRegistry.byWorld(player.getWorld());
        if (arena == null) return null;
        return arena.getSpectators().contains(player) || arena.getPlayers().contains(player.getUniqueId()) ? arena : null;
    }

    public void update(Player player) {
        if (player == null) return;
        if (!Properties.TablistEnabled) {
            restore(player);
            return;
        }
        IArena arena = ArenaRegistry.byWorld(player.getWorld());
        IArena member = arena == null || !(arena.getSpectators().contains(player)
                || arena.getPlayers().contains(player.getUniqueId())) ? null : arena;
        String[] override = overrides.get(player.getUniqueId());
        if (override != null && !Objects.equals(override[1], member == null ? null : member.getArenaName())) {
            overrides.remove(player.getUniqueId());
        }
        TablistSection section = null;
        IArena context = null;
        String layoutId = null;
        if (arena != null) {
            if (member != null) {
                TablistLayout layout = layoutFor(member, player);
                if (layout != null) {
                    section = layout.resolve(stateOf(member, member.getSpectators().contains(player)));
                    context = member;
                    layoutId = layout.id();
                }
            }
        } else if (config.isLobbyEnabled()) {
            section = config.lobby();
        }
        if (section == null) {
            restore(player);
            return;
        }
        apply(player, context, layoutId, section);
    }

    public String[] preview(Player player) {
        if (player == null) return null;
        IArena arena = ArenaRegistry.byWorld(player.getWorld());
        IArena member = membership(player);
        TablistSection section = null;
        if (member != null) {
            TablistLayout layout = layoutFor(member, player);
            if (layout != null) section = layout.resolve(stateOf(member, member.getSpectators().contains(player)));
        } else if (arena == null && config.isLobbyEnabled()) {
            section = config.lobby();
        }
        if (section == null) return null;
        return new String[]{
                TablistRenderer.join(section.header(), player, member, player),
                TablistRenderer.join(section.footer(), player, member, player),
                section.playerFormat() == null ? null : TablistRenderer.render(section.playerFormat(), player, member, player)};
    }

    static TablistLayout.State stateOf(IArena arena, boolean spectator) {
        if (spectator) return TablistLayout.State.SPECTATOR;
        GameStatus status = arena.status();
        return status == GameStatus.waiting || status == GameStatus.starting
                ? TablistLayout.State.WAITING : TablistLayout.State.PLAYING;
    }

    private void apply(Player player, IArena arena, String layoutId, TablistSection section) {
        String header = TablistRenderer.join(section.header(), player, arena, player);
        String footer = TablistRenderer.join(section.footer(), player, arena, player);
        String name = section.playerFormat() == null ? null
                : TablistRenderer.render(section.playerFormat(), player, arena, player);
        String[] draft = {header, footer, name};
        String[] seen = rendered.get(player.getUniqueId());
        if (seen != null && Arrays.equals(seen, draft)) return;
        rendered.put(player.getUniqueId(), draft);
        String[] now = ApiEvents.tablistUpdate(player, arena, layoutId, draft);
        if (now == null) return;
        String[] before = applied.get(player.getUniqueId());
        if (before != null && Arrays.equals(before, now)) return;
        applied.put(player.getUniqueId(), now);
        setHeaderFooter(player, now[0], now[1]);
        player.setPlayerListName(now[2]);
    }

    public void restore(Player player) {
        rendered.remove(player.getUniqueId());
        if (applied.remove(player.getUniqueId()) == null) return;
        setHeaderFooter(player, null, null);
        player.setPlayerListName(null);
    }

    private void setHeaderFooter(Player player, String header, String footer) {
        if (headerFooterUnavailable) return;
        try {
            player.setPlayerListHeaderFooter(header, footer);
        } catch (NoSuchMethodError | UnsupportedOperationException e) {
            headerFooterUnavailable = true;
            if (log != null) log.warning("This server does not support tab list headers/footers; only player names are customised");
        }
    }
}
