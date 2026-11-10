package com.l299l.newbedwars.update;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.common.update.ReleaseInfo;
import com.l299l.newbedwars.config.properties.Properties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;

public final class UpdateNotifier implements Listener {
    public static final String PERMISSION = "newbedwars.update.notify";
    private static final long DELAY_TICKS = 60L;

    private final NewBedwars plugin;
    private final UpdateService service;

    public UpdateNotifier(NewBedwars plugin, UpdateService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        ReleaseInfo release = service.available();
        if (release == null || !Properties.Updates.notifyAdmins() || !p.hasPermission(PERMISSION)) return;
        String key = p.getUniqueId() + "/" + release.version();
        if (service.wasNotified(key)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && service.markNotified(key)) tell(p, release, service);
        }, DELAY_TICKS);
    }

    public static void tell(Player p, ReleaseInfo release, UpdateService service) {
        Map<String, String> r = Map.of(
                "/latest/", release.version().toString(),
                "/current/", NewBedwars.plugin.getDescription().getVersion());
        if (service.isDownloaded(release)) {
            NewBedwars.plugin.getMessages().send(p, "UpdateReady", r);
            return;
        }
        NewBedwars.plugin.getMessages().send(p, "UpdateAvailable", r);
        String url = release.pageUrl();
        if (url == null) return;
        p.sendMessage(Component.text(NewBedwars.plugin.getMessages().getMsg(p, "UpdateButton"), NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(Component.text(url))));
    }
}
