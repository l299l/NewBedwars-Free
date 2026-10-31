package com.l299l.newbedwars.events.lifecycle;

import com.l299l.newbedwars.arena.ArenaRegistry;
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
        IArena arena = ArenaRegistry.byWorld(sender.getWorld());

        if (arena == null) {
            event.getRecipients().removeIf(p -> ArenaRegistry.byWorld(p.getWorld()) != null);
            return;
        }

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
                            .replace("/player/", team.colored(sender.getName()))
                            .replace("/team/", team.colored(team.getName()))
                            .replace("/message/", stripped));
            for (Player member : team.getPlayers()) {
                member.sendMessage(formatted);
            }
        } else if (Boolean.TRUE.equals(arena.getGamerules().AllowGlobalChat)) {
            String formatted = ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getMsg(sender, "ArenaChatFormat")
                            .replace("/player/", arena.coloredName(sender))
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
        if (!showServerJoinMessages()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (ArenaRegistry.byWorld(p.getWorld()) == null) {
                plugin.getMessages().send(p, "PlayerJoinedServer", java.util.Map.of("/player/", joined.getName()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player quitting = event.getPlayer();
        if (!showServerJoinMessages()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(quitting) && ArenaRegistry.byWorld(p.getWorld()) == null) {
                plugin.getMessages().send(p, "PlayerLeftServer", java.util.Map.of("/player/", quitting.getName()));
            }
        }
    }

    /**
     * "X joined/left the game" for players outside arenas. Never on network game servers (arenas announce joins
     * themselves, and players only pass through); on lobby servers {@code Network.LobbyJoinMessages}.
     */
    static boolean showServerJoinMessages() {
        com.l299l.newbedwars.api.view.NetworkMode mode = com.l299l.newbedwars.network.NetworkManager.mode();
        if (mode == com.l299l.newbedwars.api.view.NetworkMode.GAME) return false;
        if (mode == com.l299l.newbedwars.api.view.NetworkMode.LOBBY) {
            return !Boolean.FALSE.equals(com.l299l.newbedwars.config.properties.Properties.NetworkLobbyJoinMessages);
        }
        return true;
    }
}
