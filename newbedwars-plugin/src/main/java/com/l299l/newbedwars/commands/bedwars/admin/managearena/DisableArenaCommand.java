package com.l299l.newbedwars.commands.bedwars.admin.managearena;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.List;

public class DisableArenaCommand extends BwCommand {

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
            IArena arena = ArenaRegistry.byName(args[2]);
            if (arena != null) {
                if (!arena.isEnabled()) {
                    msg.send(player, "AlreadyDisabled", Map.of("/arenaname/", arena.getArenaName()));
                    return;
                }
                if (!(arena.status() == GameStatus.waiting)) {
                    msg.send(player, "ArenaCantBeDisabled", Map.of("/arenaname/", arena.getArenaName()));
                    return;
                }
                arena.disable();
                msg.send(player, "ArenaDisabled", Map.of("/arenaname/", arena.getArenaName()));
            } else {
                msg.send(player, "ArenaNotExists");
            }
        } else {
            usage(player);
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 3) {
            return ArenaRegistry.names().stream().toList();
        }
        return null;
    }
}
