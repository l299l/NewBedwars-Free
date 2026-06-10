package com.l299l.newbedwars.commands.bedwars.admin.manageArena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class DisableArenaCommand extends SubCommand {
    private final Messages msg;

    public DisableArenaCommand() {
        msg = NewBedwars.plugin.getMessages();
    }
    @Override
    public String getName() {
        return "disable";
    }

    @Override
    public String getDescription() {
        return "Command to disable arena!";
    }

    @Override
    public String getSyntax() {
        return "/bw arena disable <arenaName>";
    }

    @Override
    public String getExample() {
        return "/bw arena disable castle";
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        if (args.length == 3) {
            IArena arena = Arena.arenaByName.get(args[2]);
            if (arena != null) {
                if (!arena.isEnabled()) {
                    msg.send(player, "AlreadyDisabled", new HashMap<>() {{
                        put("/arenaname/", arena.getArenaName());
                    }});
                    return;
                }
                if (!arena.canBeDisabled()) {
                    msg.send(player, "ArenaCantBeDisabled", new HashMap<>() {{
                        put("/arenaname/", arena.getArenaName());
                    }});
                    return;
                }
                arena.disable();
                msg.send(player, "ArenaDisabled", new HashMap<>() {{
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
