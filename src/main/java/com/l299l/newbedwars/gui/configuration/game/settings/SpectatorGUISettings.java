package com.l299l.newbedwars.gui.configuration.game.settings;

import com.l299l.newbedwars.gui.configuration.game.guis.spectator.SpectatorEffectsGUI;
import com.l299l.newbedwars.gui.configuration.game.guis.spectator.SpectatorPlayersGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpectatorGUISettings implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;

        if (e.getClickedInventory().getHolder() instanceof SpectatorPlayersGUI) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getItemMeta() == null) return;
            if (!(clicked.getItemMeta() instanceof SkullMeta skull)) return;
            String targetName = skull.getOwner();
            if (targetName == null) return;
            Player target = org.bukkit.Bukkit.getPlayerExact(targetName);
            if (target != null && target.isOnline()) {
                player.closeInventory();
                player.teleport(target.getLocation());
            }
            return;
        }

        if (e.getClickedInventory().getHolder() instanceof SpectatorEffectsGUI) {
            e.setCancelled(true);
            int slot = e.getSlot();
            if (slot == 2) toggleEffect(player, PotionEffectType.NIGHT_VISION, 2);
            else if (slot == 6) toggleEffect(player, PotionEffectType.SPEED, 1);
            player.closeInventory();
        }
    }

    private void toggleEffect(Player player, PotionEffectType type, int amplifier) {
        if (player.hasPotionEffect(type)) {
            player.removePotionEffect(type);
        } else {
            player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, false, false));
        }
    }
}
