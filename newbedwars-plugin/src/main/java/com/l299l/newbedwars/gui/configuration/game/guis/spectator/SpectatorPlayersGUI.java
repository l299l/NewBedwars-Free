package com.l299l.newbedwars.gui.configuration.game.guis.spectator;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GUIManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SpectatorPlayersGUI extends BasicGUI {
    private final Inventory inv;
    private final IArena arena;

    public SpectatorPlayersGUI(GUIManager guiManager, Player player, IArena arena) {
        super(guiManager, player);
        this.arena = arena;
        int rows = Math.max(1, (int) Math.ceil(countAlivePlayers() / 9.0));
        inv = Bukkit.createInventory(this, rows * 9, getMsg().getMsg(player, "SpectatorPlayersGuiName"));
        init();
    }

    public static boolean ownTeamOnly(IArena arena, Player viewer) {
        return arena.isPlayerInArena(viewer) && arena.getTeam(viewer) != null
                && Boolean.FALSE.equals(arena.getGamerules().AllowSpectatingOtherTeams);
    }

    private boolean visible(Team team) {
        if (!team.isAlive()) return false;
        return !ownTeamOnly(arena, getPlayer()) || team.equals(arena.getTeam(getPlayer()));
    }

    private int countAlivePlayers() {
        int count = 0;
        for (Team t : arena.getTeams().values()) {
            if (visible(t)) count += t.getPlayers().size();
        }
        return Math.max(count, 1);
    }

    private void init() {
        String lore = getMsg().getMsg(getPlayer(), "SpectatorPlayerLore");
        List<String> loreList = Arrays.stream(lore.split("\n")).toList();
        int slot = 0;
        for (Team team : arena.getTeams().values()) {
            if (!visible(team)) continue;
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
