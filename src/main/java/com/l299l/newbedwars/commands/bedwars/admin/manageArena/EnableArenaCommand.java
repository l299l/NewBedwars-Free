package com.l299l.newbedwars.commands.bedwars.admin.manageArena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class EnableArenaCommand extends SubCommand {
    private final Messages msg;

    public EnableArenaCommand() {
        msg = NewBedwars.plugin.getMessages();
    }
    @Override
    public String getName() {
        return "enable";
    }

    @Override
    public String getDescription() {
        return "Command to enable arena!";
    }

    @Override
    public String getSyntax() {
        return "/bw arena enable <arenaName>";
    }

    @Override
    public String getExample() {
        return "/bw arena enable castle";
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        if (args.length == 3) {
            IArena arena = Arena.arenaByName.get(args[2]);
            if (arena != null) {
                if (arena.isEnabled()) {
                    msg.send(player, "AlreadyEnabled", new HashMap<>() {{
                        put("/arenaname/", arena.getArenaName());
                    }});
                    return;
                }
                if (!arena.canBeEnabled()) {
                    msg.send(player, "ArenaCantBeEnabled", new HashMap<>() {{
                        put("/arenaname/", arena.getArenaName());
                    }});
                    return;
                }
                arena.enable();
                msg.send(player, "ArenaEnabled", new HashMap<>() {{
                    put("/arenaname/", arena.getArenaName());
                }});
            } else {
                msg.send(player, "ArenaNotExists");
            }
        } else {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 3) {
            return Arena.arenaByName.keySet().stream().toList();
        }
        return null;
    }
}
