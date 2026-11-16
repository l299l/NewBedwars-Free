package com.l299l.newbedwars.events.lifecycle;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.player.PlayerProfile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class LanguageEvent implements Listener {
    private final NewBedwars plugin;

    public LanguageEvent(NewBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerManager().identify(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        PlayerProfile profile = plugin.getPlayerManager().getPlayer(event.getPlayer().getUniqueId());
        Language language = profile == null ? null : profile.language();
        if (!event.getPlayer().hasPlayedBefore() || language == null) {
            Language lang = Language.fromCode(Properties.DefaultLanguage);
            if (lang == null) lang = Language.English;
            plugin.getPlayerManager().addPlayer(event.getPlayer(), lang);
        }
    }
}
