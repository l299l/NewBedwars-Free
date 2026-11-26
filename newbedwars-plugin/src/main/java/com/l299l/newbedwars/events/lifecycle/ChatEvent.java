package com.l299l.newbedwars.events.lifecycle;

import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.view.NetworkMode;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.network.NetworkManager;
import com.l299l.newbedwars.placeholder.MessageContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class ChatEvent implements Listener {
    static final String COLOR_PERMISSION = "newbedwars.chat.color";
    static final String PRIVATE_CHAT_BYPASS = "newbedwars.bw.admin";
    static final Set<String> PRIVATE_COMMANDS = Set.of("msg", "tell", "w", "whisper", "m", "t", "pm",
            "emsg", "epm", "etell", "ewhisper", "r", "reply", "er", "ereply");
    static final Set<String> REPLY_COMMANDS = Set.of("r", "reply", "er", "ereply");
    private final NewBedwars plugin;

    public ChatEvent(NewBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (Bukkit.isPrimaryThread()) {
            handleChat(event);
            return;
        }
        Player sender = event.getPlayer();
        if (!ArenaRegistry.isArenaWorld(sender.getWorld())) {
            event.getRecipients().removeIf(p -> ArenaRegistry.isArenaWorld(p.getWorld()));
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!sender.isOnline()) return;
                IArena arena = ArenaRegistry.byWorld(sender.getWorld());
                if (arena != null) arenaChat(sender, arena, message);
            });
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.FINE, "Arena chat dropped while the plugin is disabled", e);
        }
    }

    private void handleChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        IArena arena = ArenaRegistry.byWorld(sender.getWorld());

        if (arena == null) {
            event.getRecipients().removeIf(p -> ArenaRegistry.byWorld(p.getWorld()) != null);
            return;
        }

        event.setCancelled(true);
        arenaChat(sender, arena, event.getMessage());
    }

    void arenaChat(Player sender, IArena arena, String message) {
        if (!Properties.ArenaChatEnabled) {
            plugin.getMessages().send(sender, "ArenaChatDisabled");
            return;
        }

        boolean prefixed = message.startsWith("!");
        boolean teamAllowed = Boolean.TRUE.equals(arena.getGamerules().AllowTeamChat);
        Team team = arena.getTeam(sender);
        boolean teamByDefault = Boolean.TRUE.equals(arena.getGamerules().TeamChatByDefault);
        boolean toTeam = teamByDefault ? !prefixed && teamAllowed && team != null : prefixed && teamAllowed;
        String body = prefixed ? message.substring(1).trim() : message;
        if (prefixed && (teamByDefault || toTeam) && body.isEmpty()) return;
        if (prefixed && !teamByDefault && !toTeam) body = message;

        if (toTeam) {
            if (team == null) return;
            boolean allowColor = sender.hasPermission(COLOR_PERMISSION);
            for (Player member : team.getPlayers()) {
                String template = chatTemplate(member, sender, "TeamChatFormat")
                        .replace("/player/", team.colored(sender.getName()))
                        .replace("/team/", team.colored(team.getDisplayName()));
                member.sendMessage(format(template, body, allowColor));
            }
            ApiEvents.arenaChat(arena, sender, plain(body), true, team);
        } else if (Boolean.TRUE.equals(arena.getGamerules().AllowGlobalChat)) {
            boolean allowColor = sender.hasPermission(COLOR_PERMISSION);
            Set<Player> recipients = new LinkedHashSet<>();
            for (UUID uuid : arena.getPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) recipients.add(p);
            }
            recipients.addAll(arena.getSpectators());
            for (Player recipient : recipients) {
                String template = chatTemplate(recipient, sender, "ArenaChatFormat")
                        .replace("/player/", arena.coloredName(sender));
                recipient.sendMessage(format(template, body, allowColor));
            }
            ApiEvents.arenaChat(arena, sender, plain(body), false, team);
        } else {
            plugin.getMessages().send(sender, "ArenaChatDisabled");
        }
    }

    private String chatTemplate(Player viewer, Player sender, String key) {
        return plugin.getMessages().getMsg(viewer, sender, key, MessageContext.about(sender));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrivateMessage(PlayerCommandPreprocessEvent event) {
        String[] parts = event.getMessage().trim().split("\\s+");
        if (parts.length == 0 || parts[0].length() < 2) return;
        String command = commandName(parts[0].substring(1));
        if (!PRIVATE_COMMANDS.contains(command)) return;
        Player sender = event.getPlayer();
        if (sender.hasPermission(PRIVATE_CHAT_BYPASS)) return;
        boolean blocked = privateChatBlocked(sender);
        if (!blocked && !REPLY_COMMANDS.contains(command) && parts.length > 1) {
            Player target = Bukkit.getPlayerExact(parts[1]);
            blocked = target != null && privateChatBlocked(target);
        }
        if (!blocked) return;
        event.setCancelled(true);
        plugin.getMessages().send(sender, "PrivateChatDisabled");
    }

    static String commandName(String label) {
        String name = label.toLowerCase(Locale.ROOT);
        int colon = name.indexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name;
    }

    static boolean privateChatBlocked(Player player) {
        IArena arena = ArenaRegistry.byWorld(player.getWorld());
        if (arena == null || arena.status() != GameStatus.playing || arena.getGamerules() == null) return false;
        if (!arena.isPlayerInArena(player) && !arena.getSpectators().contains(player)) return false;
        return Boolean.FALSE.equals(arena.getGamerules().AllowPrivateChat);
    }

    static String plain(String message) {
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message));
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
                sendServerMessage(p, "PlayerJoinedServer", joined);
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
                sendServerMessage(p, "PlayerLeftServer", quitting);
            }
        }
    }

    private void sendServerMessage(Player viewer, String key, Player subject) {
        viewer.sendMessage(com.l299l.newbedwars.text.Tokens.apply(
                plugin.getMessages().getMsg(viewer, key, MessageContext.about(subject)),
                Map.of("player", subject.getName())));
    }

    static boolean showServerJoinMessages() {
        if (Boolean.FALSE.equals(Properties.LobbyJoinMessages)) return false;
        NetworkMode mode = NetworkManager.mode();
        if (mode == NetworkMode.GAME) return false;
        if (mode == NetworkMode.LOBBY) {
            return !Boolean.FALSE.equals(Properties.NetworkLobbyJoinMessages);
        }
        return true;
    }
}
