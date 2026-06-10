package com.l299l.newbedwars.gui.configuration.game.guis.spectator;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GuiManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SpectatorPlayersGUI extends BasicGUI {
    private final Inventory inv;
    private final IArena arena;

    public SpectatorPlayersGUI(GuiManager guiManager, Player player, IArena arena) {
        super(guiManager, player);
        this.arena = arena;
        int rows = Math.max(1, (int) Math.ceil(countAlivePlayers() / 9.0));
        inv = Bukkit.createInventory(this, rows * 9, getMsg().getMsg(player, "SpectatorPlayersGuiName"));
        init();
    }

    private int countAlivePlayers() {
        int count = 0;
        for (Team t : arena.getTeams().values()) {
            if (t.isAlive()) count += t.getPlayers().size();
        }
        return Math.max(count, 1);
    }

    private void init() {
        String lore = getMsg().getMsg(getPlayer(), "SpectatorPlayerLore");
        List<String> loreList = Arrays.stream(lore.split("\n")).toList();
        int slot = 0;
        for (Team team : arena.getTeams().values()) {
            if (!team.isAlive()) continue;
            for (Player target : team.getPlayers()) {
                if (slot >= inv.getSize()) break;
                String displayName = team.getColor() + target.getName();
                inv.setItem(slot++, getGuiManager().getPlayerHead(target.getName(), displayName, loreList));
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
