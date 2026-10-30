package com.l299l.newbedwars.gui.configuration.setup.guis;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.setup.ArenaInfo;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.gui.ItemBuilder;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArenaInfoTeamGUI implements InventoryHolder {
    private final Inventory inv;
    private final Team team;
    private final IArena arena;
    private final Player viewer;

    public ArenaInfoTeamGUI(Player viewer, IArena arena, Team team) {
        this.viewer = viewer;
        this.arena = arena;
        this.team = team;
        inv = Bukkit.createInventory(this, 27, team.getColor() + "" + ChatColor.BOLD + team.getName()
                + ChatColor.GRAY + " - " + ArenaInfo.text(viewer, "Info-TeamDetails"));
        init();
    }

    private void init() {
        for (int i = 0; i < 9; i++) inv.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        for (int i = 18; i < 27; i++) inv.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        inv.setItem(9, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        inv.setItem(11, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        inv.setItem(17, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        inv.setItem(19, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        inv.setItem(26, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));

        int slot = 10;

        for (ArenaInfo.TeamSetting setting : ArenaInfo.teamSettings(team, arena.getSetupMode() == Setup.ADVANCED_SETUP)) {
            if (slot >= 27) break;
            List<String> lore = !setting.set() ? List.of(ChatColor.RED + ArenaInfo.text(viewer, "Info-NotSet"))
                    : setting.area() ? List.of(ChatColor.GRAY + "Pos1: " + ArenaInfo.loc(viewer, setting.pos1()),
                            ChatColor.GRAY + "Pos2: " + ArenaInfo.loc(viewer, setting.pos2()))
                    : List.of(ChatColor.GRAY + ArenaInfo.text(viewer, "Info-Location") + ": " + ArenaInfo.loc(viewer, setting.pos1()));
            inv.setItem(slot++, ItemBuilder.of(setting.icon())
                    .name((setting.set() ? ChatColor.GREEN : ChatColor.RED) + setting.label(viewer) + " " + (setting.set() ? "✔" : "✘"))
                    .lore(lore)
                    .build());
        }

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GRAY + ArenaInfo.text(viewer, "Info-Back"));
        backItem.setItemMeta(backMeta);
        inv.setItem(22, backItem);
    }

    public Team getTeam() {
        return team;
    }

    public IArena getArena() {
        return arena;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
