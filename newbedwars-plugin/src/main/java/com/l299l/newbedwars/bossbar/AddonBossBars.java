package com.l299l.newbedwars.bossbar;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.bossbar.AddonBossBar;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class AddonBossBars {
    private static final Pattern PLACEHOLDER = Pattern.compile("/[A-Za-z0-9_.]+/");

    private final Map<UUID, List<Bar>> bars = new HashMap<>();
    private BukkitTask task;

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(NewBedwars.plugin, this::refresh, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeAll();
    }

    public AddonBossBar show(Plugin owner, Player player, String text, BarColor color, BarStyle style, double progress) {
        if (owner == null) throw new IllegalArgumentException("owner is null");
        if (player == null) throw new IllegalArgumentException("player is null");
        if (color == null) throw new IllegalArgumentException("color is null");
        if (style == null) throw new IllegalArgumentException("style is null");
        Bar bar = new Bar(owner, player, text == null ? "" : text, color, style, progress, arenaNameOf(player));
        bars.computeIfAbsent(player.getUniqueId(), id -> new ArrayList<>()).add(bar);
        return bar;
    }

    public void removeForPlayer(Player player) {
        List<Bar> list = bars.get(player.getUniqueId());
        if (list != null) for (Bar bar : new ArrayList<>(list)) bar.remove();
    }

    public void removeForArena(Player player, String arenaName) {
        List<Bar> list = bars.get(player.getUniqueId());
        if (list == null) return;
        for (Bar bar : new ArrayList<>(list)) {
            if (arenaName.equals(bar.arenaName)) bar.remove();
        }
    }

    public void removeForOwner(Plugin owner) {
        for (List<Bar> list : new ArrayList<>(bars.values())) {
            for (Bar bar : new ArrayList<>(list)) {
                if (bar.owner.equals(owner)) bar.remove();
            }
        }
    }

    public void removeAll() {
        for (List<Bar> list : new ArrayList<>(bars.values())) {
            for (Bar bar : new ArrayList<>(list)) bar.remove();
        }
    }

    public int count() {
        int n = 0;
        for (List<Bar> list : bars.values()) n += list.size();
        return n;
    }

    public void refresh() {
        for (List<Bar> list : new ArrayList<>(bars.values())) {
            for (Bar bar : new ArrayList<>(list)) {
                if (!bar.player.isOnline()) bar.remove();
                else if (bar.dynamic) bar.render();
            }
        }
    }

    static String arenaNameOf(Player player) {
        IArena arena = ArenaRegistry.byWorld(player.getWorld());
        if (arena == null) return null;
        boolean member = arena.getPlayers().contains(player.getUniqueId()) || arena.getSpectators().contains(player);
        return member ? arena.getArenaName() : null;
    }

    private static boolean hasPlaceholders(String text) {
        return text.indexOf('%') >= 0 || PLACEHOLDER.matcher(text).find();
    }

    private final class Bar implements AddonBossBar {
        private final Plugin owner;
        private final Player player;
        private final BossBar bar;
        private final String arenaName;
        private String raw;
        private boolean dynamic;
        private boolean removed;

        Bar(Plugin owner, Player player, String text, BarColor color, BarStyle style, double progress, String arenaName) {
            this.owner = owner;
            this.player = player;
            this.arenaName = arenaName;
            this.raw = text;
            this.dynamic = hasPlaceholders(text);
            this.bar = Bukkit.createBossBar(resolve(), color, style);
            this.bar.setProgress(clamp(progress));
            this.bar.addPlayer(player);
            this.bar.setVisible(true);
        }

        private String resolve() {
            IArena arena = arenaName == null ? null : ArenaRegistry.byWorld(player.getWorld());
            return BossBarManager.render(raw, arena, player);
        }

        void render() {
            bar.setTitle(resolve());
        }

        private static double clamp(double value) {
            if (Double.isNaN(value)) return 0;
            return Math.max(0, Math.min(1, value));
        }

        @Override
        public Player getPlayer() {
            return player;
        }

        @Override
        public void setText(String text) {
            raw = text == null ? "" : text;
            dynamic = hasPlaceholders(raw);
            if (!removed) render();
        }

        @Override
        public void setColor(BarColor color) {
            if (color != null && !removed) bar.setColor(color);
        }

        @Override
        public void setStyle(BarStyle style) {
            if (style != null && !removed) bar.setStyle(style);
        }

        @Override
        public void setProgress(double progress) {
            if (!removed) bar.setProgress(clamp(progress));
        }

        @Override
        public boolean isVisible() {
            return !removed;
        }

        @Override
        public void remove() {
            if (removed) return;
            removed = true;
            bar.removeAll();
            bar.setVisible(false);
            List<Bar> list = bars.get(player.getUniqueId());
            if (list != null) {
                list.remove(this);
                if (list.isEmpty()) bars.remove(player.getUniqueId());
            }
        }
    }
}
