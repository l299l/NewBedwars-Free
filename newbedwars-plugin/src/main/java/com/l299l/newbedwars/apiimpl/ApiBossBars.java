package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.bossbar.AddonBossBar;
import com.l299l.newbedwars.api.bossbar.BossBarService;
import com.l299l.newbedwars.apiimpl.adapter.ApiRegistrations;
import com.l299l.newbedwars.bossbar.AddonBossBars;
import com.l299l.newbedwars.bossbar.BossBarManager;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ApiBossBars implements BossBarService {
    private final ApiRegistrations registrations;
    private final Set<Plugin> tracked = new HashSet<>();

    public ApiBossBars(ApiRegistrations registrations) {
        this.registrations = registrations;
    }

    private static BossBarManager manager() {
        return NewBedwars.plugin == null ? null : NewBedwars.plugin.getBossBarManager();
    }

    @Override
    public List<String> getBossBars() {
        BossBarManager manager = manager();
        return manager == null ? List.of() : manager.getIds();
    }

    @Override
    public boolean isEnabled(String id) {
        BossBarManager manager = manager();
        return manager != null && manager.isEnabled(id);
    }

    @Override
    public AddonBossBar show(Plugin owner, Player player, String text, BarColor color, BarStyle style, double progress) {
        AddonBossBars bars = NewBedwars.plugin == null ? null : NewBedwars.plugin.getAddonBossBars();
        if (bars == null) throw new IllegalStateException("NewBedwars is not enabled");
        AddonBossBar bar = bars.show(owner, player, text, color, style, progress);
        if (tracked.add(owner)) {
            registrations.add(owner, () -> {
                tracked.remove(owner);
                bars.removeForOwner(owner);
            });
        }
        return bar;
    }
}
