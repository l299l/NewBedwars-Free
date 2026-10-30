package com.l299l.newbedwars.gui.model;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public record GUIUpgrade(String id, CustomItem icon) implements GUIEntry {
    /** The upgrade, resolved when used so GUI files can reference upgrades an addon registers later. {@code null} while it is missing. */
    public Upgrade upgrade() {
        try {
            return Upgrade.valueOf(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String text(Player player, String key) {
        return NewBedwars.plugin.getMessages().getMsg(player, key);
    }

    @Override
    public ItemStack getIcon(Player player) {
        ItemStack item = icon.getIcon(NewBedwars.plugin.getPlayerManager().getPlayer(player.getName()).language());
        Upgrade upgrade = upgrade();
        if (upgrade == null) return item;
        Arena arena = (Arena) ArenaRegistry.byWorld(player.getWorld());
        if (arena == null) return item;
        Team team = arena.getTeam(player);
        if (team == null) return item;
        TeamUpgrades tu = team.getTeamUpgrades();
        if (tu == null) return item;
        int currentLevel = tu.getUpgradeLevel(upgrade);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(ChatColor.GRAY + text(player, "GuiUpgradeLevel") + ": " + ChatColor.WHITE + currentLevel + "/" + upgrade.maxLevel);
        if (currentLevel < upgrade.maxLevel) {
            int nextPrice = Properties.getUpgradePrice(upgrade, currentLevel);
            lore.add(ChatColor.GRAY + text(player, "GuiUpgradePrice") + ": " + ChatColor.YELLOW + nextPrice + " " + Properties.UpgradePriceType.name());
        } else {
            lore.add(ChatColor.GREEN + text(player, "GuiUpgradeMaxed"));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
