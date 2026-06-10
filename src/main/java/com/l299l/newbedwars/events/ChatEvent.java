package com.l299l.newbedwars.events;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class ChatEvent implements Listener {
    private final NewBedwars plugin;

    public ChatEvent(NewBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        IArena arena = Arena.arenaByWorld.get(sender.getWorld());

        if (arena == null) {
            // Sender is in the lobby — remove arena players from recipients
            event.getRecipients().removeIf(p -> Arena.arenaByWorld.get(p.getWorld()) != null);
            return;
        }

        // Sender is in an arena — take full control of routing
        event.setCancelled(true);

        if (!Properties.ArenaChatEnabled) {
            plugin.getMessages().send(sender, "ArenaChatDisabled");
            return;
        }

        String message = event.getMessage();
        boolean teamPrefix = message.startsWith("!");

        if (teamPrefix && Boolean.TRUE.equals(arena.getGamerules().AllowTeamChat)) {
            String stripped = message.substring(1).trim();
            if (stripped.isEmpty()) return;
            Team team = arena.getTeam(sender);
            if (team == null) return;
            String formatted = ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getMsg(sender, "TeamChatFormat")
                            .replace("/player/", sender.getName())
                            .replace("/team/", team.getName())
                            .replace("/message/", stripped));
            for (Player member : team.getPlayers()) {
                member.sendMessage(formatted);
            }
        } else if (Boolean.TRUE.equals(arena.getGamerules().AllowGlobalChat)) {
            String formatted = ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getMsg(sender, "ArenaChatFormat")
                            .replace("/player/", sender.getName())
                            .replace("/message/", message));
            for (UUID uuid : arena.getPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.sendMessage(formatted);
            }
            for (Player spec : arena.getSpectators()) {
                spec.sendMessage(formatted);
            }
        } else {
            plugin.getMessages().send(sender, "ArenaChatDisabled");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player joined = event.getPlayer();
        Arena.updateTablist(joined);
        String msg = ChatColor.YELLOW + joined.getName() + " joined the game";
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Arena.arenaByWorld.get(p.getWorld()) == null) {
                p.sendMessage(msg);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player quitting = event.getPlayer();
        String msg = ChatColor.YELLOW + quitting.getName() + " left the game";
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(quitting) && Arena.arenaByWorld.get(p.getWorld()) == null) {
                p.sendMessage(msg);
            }
        }
    }
}
