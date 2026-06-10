package com.l299l.newbedwars.gui.configuration.game.guis.spectator;

import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GuiManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SpectatorEffectsGUI extends BasicGUI {
    private final Inventory inv;

    public SpectatorEffectsGUI(GuiManager guiManager, Player player) {
        super(guiManager, player);
        inv = Bukkit.createInventory(this, 9, getMsg().getMsg(player, "SpectatorEffectsGuiName"));
        init();
    }

    private void init() {
        Player p = getPlayer();
        boolean hasNightVision = p.hasPotionEffect(PotionEffectType.NIGHT_VISION);
        boolean hasSpeed = p.hasPotionEffect(PotionEffectType.SPEED);

        inv.setItem(2, buildEffectItem(Material.GOLDEN_CARROT,
                getMsg().getMsg(p, "SpectatorNightVisionName"),
                getMsg().getMsg(p, "SpectatorNightVisionLore"),
                hasNightVision));
        inv.setItem(6, buildEffectItem(Material.SUGAR,
                getMsg().getMsg(p, "SpectatorSpeedName"),
                getMsg().getMsg(p, "SpectatorSpeedLore"),
                hasSpeed));
    }

    private org.bukkit.inventory.ItemStack buildEffectItem(Material mat, String name, String lore, boolean active) {
        String statusKey = active ? "SpectatorEffectEnabled" : "SpectatorEffectDisabled";
        String status = getMsg().getMsg(getPlayer(), statusKey);
        List<String> loreList = Arrays.asList(lore, status);
        return getGuiManager().createItem(name, mat, loreList, active);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
