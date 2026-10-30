package com.l299l.newbedwars.gui.configuration.game.guis;

import com.l299l.newbedwars.gui.ItemBuilder;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GUIManager;
import com.l299l.newbedwars.player.PlayerProfile;
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

    public ProfileGUI(GUIManager guiManager, Player viewer, Player target) {
        super(guiManager, viewer);
        this.target = target;
        inv = Bukkit.createInventory(this, 27,
                ChatColor.GOLD + "" + ChatColor.BOLD + text("GuiProfileTitle").replace("/player/", target.getName()));
        init();
    }

    private void init() {
        PlayerStats stats = NewBedwars.plugin.getPlayerManager().getStats(target.getName());

        inv.setItem(4, getGuiManager().getPlayerHead(target.getName(),
                ChatColor.GOLD + "" + ChatColor.BOLD + target.getName(),
                List.of(ChatColor.GRAY + text("GuiGamesPlayed") + ": " + ChatColor.WHITE + stats.gamesPlayed())));

        inv.setItem(10, statItem(Material.GREEN_STAINED_GLASS_PANE,
                ChatColor.GREEN + text("GuiWins"), stats.wins()));
        inv.setItem(11, statItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + text("GuiLosses"), stats.losses()));
        double wl = stats.losses() == 0 ? stats.wins() : (double) stats.wins() / stats.losses();
        inv.setItem(12, statItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.YELLOW + text("GuiWinLossRatio"), String.format("%.2f", wl)));
        inv.setItem(13, statItem(Material.YELLOW_STAINED_GLASS_PANE,
                ChatColor.YELLOW + text("GuiKills"), stats.kills()));
        inv.setItem(14, statItem(Material.GRAY_STAINED_GLASS_PANE,
                ChatColor.GRAY + text("GuiDeaths"), stats.deaths()));
        double kd = stats.deaths() == 0 ? stats.kills() : (double) stats.kills() / stats.deaths();
        inv.setItem(15, statItem(Material.CYAN_STAINED_GLASS_PANE,
                ChatColor.AQUA + text("GuiKillDeathRatio"), String.format("%.2f", kd)));
        inv.setItem(16, statItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                ChatColor.AQUA + text("GuiFinalKills"), stats.finalKills()));
        inv.setItem(17, statItem(Material.ORANGE_STAINED_GLASS_PANE,
                ChatColor.GOLD + text("GuiBedsBroken"), stats.bedsBroken()));

        PlayerProfile ins = NewBedwars.plugin.getPlayerManager().getPlayer(target.getName());
        String langName = ins != null ? ins.language().name() : "ENGLISH";
        inv.setItem(20, statItem(Material.PAPER,
                ChatColor.LIGHT_PURPLE + text("GuiLanguage"), langName));

        // Customize fast-buy button (only visible to the profile owner)
        if (getPlayer().equals(target)) {
            ItemStack fastBuyBtn = new ItemStack(Material.CHEST);
            ItemMeta meta = fastBuyBtn.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + text("GuiCustomizeFastBuy"));
            meta.setLore(List.of(ChatColor.GRAY + text("GuiCustomizeFastBuyLore")));
            fastBuyBtn.setItemMeta(meta);
            inv.setItem(22, fastBuyBtn);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + text("GuiClose"));
        close.setItemMeta(closeMeta);
        inv.setItem(26, close);
    }

    private ItemStack statItem(Material mat, String name, int value) {
        return statItem(mat, name, String.valueOf(value));
    }

    private ItemStack statItem(Material mat, String name, String value) {
        return ItemBuilder.of(mat).name(name).lore(ChatColor.WHITE + value).build();
    }

    public Player getTarget() {
        return target;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
