package com.l299l.newbedwars.gui.configuration.game.guis;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GuiManager;
import com.l299l.newbedwars.player.PlayerIns;
import com.l299l.newbedwars.player.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ProfileGUI extends BasicGUI {
    private final Inventory inv;
    private final Player target;

    public ProfileGUI(GuiManager guiManager, Player viewer, Player target) {
        super(guiManager, viewer);
        this.target = target;
        inv = Bukkit.createInventory(this, 27,
                ChatColor.GOLD + "" + ChatColor.BOLD + target.getName() + "'s Profile");
        init();
    }

    private void init() {
        PlayerStats stats = NewBedwars.plugin.getPlayerManager().getStats(target.getName());

        // Player head
        inv.setItem(4, getGuiManager().getPlayerHead(target.getName(),
                ChatColor.GOLD + "" + ChatColor.BOLD + target.getName(),
                List.of(ChatColor.GRAY + "Games Played: " + ChatColor.WHITE + stats.gamesPlayed())));

        // Stats items
        inv.setItem(10, statItem(Material.GREEN_STAINED_GLASS_PANE,
                ChatColor.GREEN + "Wins", stats.wins()));
        inv.setItem(11, statItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "Losses", stats.losses()));
        double wl = stats.losses() == 0 ? stats.wins() : (double) stats.wins() / stats.losses();
        inv.setItem(12, statItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.YELLOW + "W/L Ratio", String.format("%.2f", wl)));
        inv.setItem(13, statItem(Material.YELLOW_STAINED_GLASS_PANE,
                ChatColor.YELLOW + "Kills", stats.kills()));
        inv.setItem(14, statItem(Material.GRAY_STAINED_GLASS_PANE,
                ChatColor.GRAY + "Deaths", stats.deaths()));
        double kd = stats.deaths() == 0 ? stats.kills() : (double) stats.kills() / stats.deaths();
        inv.setItem(15, statItem(Material.CYAN_STAINED_GLASS_PANE,
                ChatColor.AQUA + "K/D Ratio", String.format("%.2f", kd)));
        inv.setItem(16, statItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                ChatColor.AQUA + "Final Kills", stats.finalKills()));
        inv.setItem(17, statItem(Material.ORANGE_STAINED_GLASS_PANE,
                ChatColor.GOLD + "Beds Broken", stats.bedsBroken()));

        // Language item (slot 20)
        PlayerIns ins = NewBedwars.plugin.getPlayerManager().getPlayer(target.getName());
        String langName = ins != null ? ins.language().name() : "ENGLISH";
        inv.setItem(20, statItem(Material.PAPER,
                ChatColor.LIGHT_PURPLE + "Language", langName));

        // Customize fast-buy button (only visible to the profile owner)
        if (getPlayer().equals(target)) {
            ItemStack fastBuyBtn = new ItemStack(Material.CHEST);
            ItemMeta meta = fastBuyBtn.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + "Customize Fast-Buy");
            meta.setLore(List.of(ChatColor.GRAY + "Personalize your shop home page."));
            fastBuyBtn.setItemMeta(meta);
            inv.setItem(22, fastBuyBtn);
        }

        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        close.setItemMeta(closeMeta);
        inv.setItem(26, close);
    }

    private ItemStack statItem(Material mat, String name, int value) {
        return statItem(mat, name, String.valueOf(value));
    }

    private ItemStack statItem(Material mat, String name, String value) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(ChatColor.WHITE + value));
        item.setItemMeta(meta);
        return item;
    }

    public Player getTarget() {
        return target;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
