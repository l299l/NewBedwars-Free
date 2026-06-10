package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.configuration.game.guis.ArenaSelectGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class JoinCommand extends SubCommand {
    private final Messages msg;

    public JoinCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Command to join arena";
    }

    @Override
    public String getSyntax() {
        return "/bw join (<arena>)";
    }

    @Override
    public String getExample() {
        return "/bw join arena1";
    }

    @Override
    public void perform(Player p, String[] args, IArena old) {
        if (Properties.RequireJoinPermission && !p.hasPermission("newbedwars.bw.join") && !p.isOp()) {
            msg.send(p, "NoPermissions");
            return;
        }
        for (IArena existing : Arena.arenaByName.values()) {
            if (existing.isPlayerInArena(p)) {
                msg.send(p, "ArenaJoinError");
                return;
            }
        }

        if (args.length > 1) {
            IArena arena = Arena.arenaByName.get(args[1]);
            if (arena == null) {
                msg.send(p, "ArenaNotExists");
                return;
            }
            GameStatus status = arena.status();
            if (!arena.isEnabled()) {
                msg.send(p, "ArenaNotEnabled", new HashMap<>() {{
                    put("/arenaname/", arena.getArenaName());
                }});
                return;
            }
            if (status == GameStatus.waiting || status == GameStatus.starting) {
                if (!arena.join(p)) {
                    msg.send(p, "ArenaIsFullError");
                }
            } else if (status == GameStatus.playing) {
                msg.send(p, "ArenaRunningError");
            } else if (status == GameStatus.restarting) {
                msg.send(p, "ArenaRestartingError");
            } else {
                msg.send(p, "ArenaJoinError");
            }
        } else {
            p.openInventory(new ArenaSelectGUI(p).getInventory());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            return Arena.arenaByName.keySet().stream().toList();
        }
        return null;
    }
}
