package com.l299l.newbedwars.events.lifecycle;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.view.NetworkMode;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.network.NetworkManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

public class ChatEvent implements Listener {
    static final String COLOR_PERMISSION = "newbedwars.chat.color";
    private final NewBedwars plugin;

    public ChatEvent(NewBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (Bukkit.isPrimaryThread()) {
            handleChat(event);
            return;
        }
        try {
            Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                handleChat(event);
                return null;
            }).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            plugin.getLogger().log(Level.WARNING, "Arena chat handling failed", e);
        }
    }

    private void handleChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
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
            String formatted = format(
                    plugin.getMessages().getMsg(sender, "TeamChatFormat")
                            .replace("/player/", team.colored(sender.getName()))
                            .replace("/team/", team.colored(team.getName())),
                    stripped, sender.hasPermission(COLOR_PERMISSION));
            for (Player member : team.getPlayers()) {
                member.sendMessage(formatted);
            }
        } else if (Boolean.TRUE.equals(arena.getGamerules().AllowGlobalChat)) {
            String formatted = format(
                    plugin.getMessages().getMsg(sender, "ArenaChatFormat")
                            .replace("/player/", arena.coloredName(sender)),
                    message, sender.hasPermission(COLOR_PERMISSION));
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

    static String format(String template, String message, boolean allowColor) {
        String body = ChatColor.translateAlternateColorCodes('&', message);
        if (!allowColor) body = ChatColor.stripColor(body);
        return ChatColor.translateAlternateColorCodes('&', template).replace("/message/", body);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player joined = event.getPlayer();
        Arena.updateTablist(joined);
        if (!showServerJoinMessages()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (ArenaRegistry.byWorld(p.getWorld()) == null) {
                plugin.getMessages().send(p, "PlayerJoinedServer", Map.of("/player/", joined.getName()));
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
                plugin.getMessages().send(p, "PlayerLeftServer", Map.of("/player/", quitting.getName()));
            }
        }
    }

    static boolean showServerJoinMessages() {
        NetworkMode mode = NetworkManager.mode();
        if (mode == NetworkMode.GAME) return false;
        if (mode == NetworkMode.LOBBY) {
            return !Boolean.FALSE.equals(Properties.NetworkLobbyJoinMessages);
        }
        return true;
    }
}
