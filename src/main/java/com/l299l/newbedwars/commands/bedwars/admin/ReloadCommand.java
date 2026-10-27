package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.utils.LobbyService;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class ReloadCommand extends SubCommand {
    private final Messages msg;

    public ReloadCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reload the plugin configuration and data.";
    }

    @Override
    public String getSyntax() {
        return "/bw reload";
    }

    @Override
    public String getExample() {
        return "/bw reload";
    }

    @Override
    public void perform(Player p, String[] args, IArena a) {
        if (!p.hasPermission("newbedwars.bw.admin") && !p.isOp()) {
            msg.send(p, "NoPermissions");
            return;
        }

        int active = 0;
        for (IArena arena : Arena.arenaByName.values()) {
            if (!arena.getPlayers().isEmpty() || !arena.getSpectators().isEmpty()) active++;
            else if (!arena.getPlayersOnSetup().isEmpty()) {
                arena.getPlayersOnSetup().forEach(plu -> {
                    Player pl = Objects.requireNonNull(Bukkit.getPlayer(plu));
                    LobbyService.sendToLobby(pl, com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent.Reason.RELOAD);
                    pl.kickPlayer(ChatColor.RED + "Server went down!");});
            }
        }
        if (active > 0) {
            getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "[NewBedwars]: " + active + " arena(s) still active — forcing shutdown...");
        }
        for (IArena arena : new ArrayList<>(Arena.arenaByName.values())) {
            arena.forceShutdown();
        }

        NewBedwars.plugin.getDataManager().save();
        NewBedwars.plugin.reloadAll();
        NewBedwars.plugin.getDataManager().loadArenas();
        msg.send(p, "ReloadSuccess");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
