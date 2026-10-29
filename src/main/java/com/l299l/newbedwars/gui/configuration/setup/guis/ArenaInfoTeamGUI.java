package com.l299l.newbedwars.gui.configuration.setup.guis;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ArenaInfoTeamGUI implements InventoryHolder {
    private final Inventory inv;
    private final Team team;
    private final IArena arena;

    public ArenaInfoTeamGUI(IArena arena, Team team) {
        this.arena = arena;
        this.team = team;
        inv = Bukkit.createInventory(this, 27, team.getColor() + "" + ChatColor.BOLD + team.getName() + ChatColor.GRAY + " - Details");
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

        slot = addSetting(slot, Material.RED_BED, "Bed", team.isBedSet(),
                team.isBedSet() ? locStr(team.getTeamBed().getLocation()) : null);

        slot = addSetting(slot, Material.BEDROCK, "Spawn", team.isSpawnSet(),
                team.isSpawnSet() ? locStr(team.getTeamSpawn()) : null);

        slot = addSetting(slot, Material.CHEST, "Shop", team.isShopSet(),
                team.isShopSet() ? locStr(team.getTeamShop().getLocation()) : null);

        slot = addSetting(slot, Material.ANVIL, "Upgrades", team.isUpgradesSet(),
                team.isUpgradesSet() ? locStr(team.getTeamUpgrades().getLocation()) : null);

        slot = addSetting(slot, Material.DROPPER, "Generator", team.isGeneratorSet(),
                team.isGeneratorSet() ? locStr(team.getGenerator().getLocation()) : null);

        slot = addAreaSetting(slot, Material.RED_CONCRETE, "BuildProt",
                team.isBuildProtAreaPosSet(),
                team.getTeamBuildProtAreaPos1(), team.getTeamBuildProtAreaPos2());

        slot = addAreaSetting(slot, Material.RED_STAINED_GLASS, "BaseArea",
                team.isBasePosSet(),
                team.getTeamBasePos1(), team.getTeamBasePos2());

        if (arena.getSetupMode() == Setup.ADVANCED_SETUP) {
            slot = addSetting(slot, Material.DIAMOND_HORSE_ARMOR, "Accessories", team.isAccessoriesSet(),
                    team.isAccessoriesSet() ? locStr(team.getTeamAccessories()) : null);
        }

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GRAY + "← Back to Arena Info");
        backItem.setItemMeta(backMeta);
        inv.setItem(22, backItem);
    }

    private int addSetting(int slot, Material material, String name, boolean isSet, String location) {
        if (slot >= 27) return slot;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((isSet ? ChatColor.GREEN : ChatColor.RED) + name + " " + (isSet ? "✔" : "✘"));
        List<String> lore = new ArrayList<>();
        if (isSet && location != null) {
            lore.add(ChatColor.GRAY + "Location: " + ChatColor.WHITE + location);
        } else {
            lore.add(ChatColor.RED + "Not set");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
        return slot + 1;
    }

    private int addAreaSetting(int slot, Material material, String name, boolean isSet, Location pos1, Location pos2) {
        if (slot >= 27) return slot;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((isSet ? ChatColor.GREEN : ChatColor.RED) + name + " " + (isSet ? "✔" : "✘"));
        List<String> lore = new ArrayList<>();
        if (isSet) {
            lore.add(ChatColor.GRAY + "Pos1: " + locStr(pos1));
            lore.add(ChatColor.GRAY + "Pos2: " + locStr(pos2));
        } else {
            lore.add(ChatColor.RED + "Not set");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
        return slot + 1;
    }

    private String locStr(Location loc) {
        if (loc == null) return ChatColor.RED + "not set";
        return ChatColor.WHITE + String.valueOf(loc.getBlockX()) + " " + loc.getBlockY() + " " + loc.getBlockZ();
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
