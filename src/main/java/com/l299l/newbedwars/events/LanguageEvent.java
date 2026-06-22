package com.l299l.newbedwars.events;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class LanguageEvent implements Listener {
    private final NewBedwars plugin;

    public LanguageEvent(NewBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Language language;
        try {
            language = plugin.getPlayerManager().getPlayer(event.getPlayer().getName()).language();
        } catch (Exception e) {
            language = null;
        }
        if (!event.getPlayer().hasPlayedBefore() || language == null) {
            Language lang = Language.fromCode(Properties.DefaultLanguage);
            if (lang == null) lang = Language.English;
            plugin.getPlayerManager().addPlayer(event.getPlayer(), lang);
        }
    }
}
