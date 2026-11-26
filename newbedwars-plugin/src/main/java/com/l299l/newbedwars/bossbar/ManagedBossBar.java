package com.l299l.newbedwars.bossbar;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.events.BossBarUpdateEvent;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.placeholder.PapiText;
import com.l299l.newbedwars.text.TextAnimator;
import com.l299l.newbedwars.text.TextEffect;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ManagedBossBar {
    private final String id;
    private final BossBarSave save;
    private final IArena arena;
    private final boolean perPlayer;
    private final BossBar shared;
    private final Map<UUID, Viewer> viewers = new LinkedHashMap<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private BukkitTask effectTask;
    private int step;
    private BarColor color;
    private BarStyle style;
    private String overrideText;
    private boolean visible = true;
    private boolean removed;

    ManagedBossBar(String id, BossBarSave save, IArena arena, boolean perPlayer) {
        this.id = id;
        this.save = save;
        this.arena = arena;
        this.perPlayer = perPlayer;
        this.color = save.getColor()[0];
        this.style = save.getStyle()[0];
        this.shared = perPlayer ? null : Bukkit.createBossBar(BossBarManager.render(save.getTexts()[0], arena, null), color, style);
        if (shared != null) shared.setVisible(save.isEnabled());
    }

    void start() {
        showStep(0, true);
        if (save.isAnimated()) {
            tasks.add(new BukkitRunnable() {
                int i = 1;

                @Override
                public void run() {
                    showStep(i, false);
                    i++;
                    if (i == save.getTexts().length) i = 0;
                }
            }.runTaskTimer(NewBedwars.plugin, 0, 20L * save.getTime()));
        }
    }

    public boolean isPerPlayer() {
        return perPlayer;
    }

    public BossBar getSharedBar() {
        return shared;
    }

    public List<Player> getPlayers() {
        if (shared != null) return shared.getPlayers();
        List<Player> players = new ArrayList<>();
        for (Viewer viewer : viewers.values()) players.add(viewer.player);
        return players;
    }

    public void addPlayer(Player player) {
        if (removed || player == null) return;
        if (shared != null) {
            shared.addPlayer(player);
            return;
        }
        if (viewers.containsKey(player.getUniqueId())) return;
        Viewer viewer = new Viewer(player);
        viewers.put(player.getUniqueId(), viewer);
        viewer.animator = newAnimator(player);
        viewer.bar = Bukkit.createBossBar(titleFor(viewer), color, style);
        viewer.bar.setVisible(visible);
        viewer.bar.addPlayer(player);
    }

    public void removePlayer(Player player) {
        if (player == null) return;
        if (shared != null) {
            shared.removePlayer(player);
            return;
        }
        Viewer viewer = viewers.remove(player.getUniqueId());
        if (viewer != null) viewer.discard();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (shared != null) shared.setVisible(visible);
        for (Viewer viewer : viewers.values()) viewer.bar.setVisible(visible);
    }

    void remove() {
        removed = true;
        for (BukkitTask task : tasks) task.cancel();
        tasks.clear();
        effectTask = null;
        for (Viewer viewer : viewers.values()) viewer.discard();
        viewers.clear();
        if (shared != null) {
            shared.removeAll();
            shared.setVisible(false);
        }
    }

    private void showStep(int i, boolean first) {
        if (removed) return;
        String template = save.getTexts()[i];
        String resolved = BossBarManager.render(template, arena, null);
        BarColor newColor = save.getColor()[i];
        BarStyle newStyle = save.getStyle()[i];
        String text = resolved;
        BossBarUpdateEvent event = ApiEvents.bossBarUpdate(arena, id, resolved, newColor, newStyle);
        if (event == null) {
            if (!first) return;
        } else {
            text = event.getText();
            newColor = event.getColor();
            newStyle = event.getStyle();
        }
        step = i;
        color = newColor;
        style = newStyle;
        overrideText = text.equals(resolved) ? null : text;
        if (effectTask != null) {
            effectTask.cancel();
            tasks.remove(effectTask);
            effectTask = null;
        }
        if (shared != null) {
            shared.setTitle(text);
            shared.setColor(color);
            shared.setStyle(style);
        } else {
            prune();
            for (Viewer viewer : viewers.values()) {
                viewer.animator = newAnimator(viewer.player);
                viewer.bar.setTitle(titleFor(viewer));
                viewer.bar.setColor(color);
                viewer.bar.setStyle(style);
            }
        }
        if (overrideText == null) startEffect(template);
    }

    private void startEffect(String template) {
        TextEffect effect = save.getEffects()[step];
        TextAnimator probe = new TextAnimator(effect, BossBarManager.render(template, arena, null), null);
        long period = probe.period(1);
        long delay = probe.getEffect() == TextEffect.NONE ? 20 : 0;
        BukkitRunnable runnable;
        if (shared != null) {
            runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    shared.setTitle(probe.next(BossBarManager.render(template, arena, null)));
                }
            };
        } else {
            runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    prune();
                    for (Viewer viewer : viewers.values()) {
                        viewer.bar.setTitle(viewer.animator.next(BossBarManager.render(template, arena, viewer.player)));
                    }
                }
            };
        }
        effectTask = runnable.runTaskTimer(NewBedwars.plugin, delay, period);
        tasks.add(effectTask);
    }

    private TextAnimator newAnimator(Player player) {
        String template = save.getTexts()[step];
        return new TextAnimator(save.getEffects()[step], BossBarManager.render(template, arena, player), null);
    }

    private String titleFor(Viewer viewer) {
        if (overrideText != null) return PapiText.apply(viewer.player, overrideText);
        return viewer.animator.next(BossBarManager.render(save.getTexts()[step], arena, viewer.player));
    }

    private void prune() {
        Iterator<Viewer> it = viewers.values().iterator();
        while (it.hasNext()) {
            Viewer viewer = it.next();
            if (!viewer.player.isOnline()) {
                it.remove();
                viewer.discard();
            }
        }
    }

    private static final class Viewer {
        final Player player;
        BossBar bar;
        TextAnimator animator;

        Viewer(Player player) {
            this.player = player;
        }

        void discard() {
            if (bar == null) return;
            bar.removeAll();
            bar.setVisible(false);
        }
    }
}
