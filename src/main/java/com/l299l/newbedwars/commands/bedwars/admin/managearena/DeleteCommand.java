package com.l299l.newbedwars.commands.bedwars.admin.managearena;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.gui.configuration.setup.guis.DeleteConfirmGUI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class DeleteCommand extends BwCommand {


    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Permanently deletes an arena, its world and all saved data.";
    }

    @Override
    public String getSyntax() {
        return "/bw arena delete <arenaName>";
    }

    @Override
    public String getExample() {
        return "/bw arena delete castle";
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        if (args.length != 3) {
            usage(player);
            return;
        }

        IArena arena = ArenaRegistry.byName(args[2]);
        if (arena == null) {
            msg.send(player, "ArenaNotExists");
            return;
        }

        GameStatus status = arena.status();
        if (status == GameStatus.playing || status == GameStatus.starting || status == GameStatus.ending) {
            msg.send(player, "ArenaCannotBeDeleted", Map.of("/arenaname/", arena.getArenaName()));
            return;
        }

        player.openInventory(
                new DeleteConfirmGUI(NewBedwars.plugin.getGuiManager(), player, arena.getArenaName()).getInventory());
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 3) {
            return new ArrayList<>(ArenaRegistry.names());
        }
        return null;
    }
}
