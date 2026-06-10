package com.l299l.newbedwars.gui.configuration.setup.settings;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.setup.guis.ShopSetupGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class ShopSetupGuiSettings implements Listener {
    private final Messages msg;

    public ShopSetupGuiSettings() {
        msg = NewBedwars.plugin.getMessages();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        if (e.getClickedInventory().getHolder() instanceof ShopSetupGUI gui) {
            if (e.getSlot() == 0) {
                if(gui.isEditMainPageConfirmed()) {
                    player.closeInventory();
                    player.openInventory(gui.getShopGUI().getInventory());
                }else {
                    gui.setEditMainPageConfirmed(true);
                    player.closeInventory();
                    msg.send(player, "setup.gui.shop.main-page-edit-confirmation");
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.openInventory(gui.getInventory());
                        }
                    }.runTaskLater(NewBedwars.plugin, 20 * 5);
                }
                //edit main page
            }else if (e.getSlot() == 3) {
                //create category
            }else if (e.getSlot() == 5) {
                //create custom item
            }else if (e.getSlot() == 8) {
                //show preview
            }else if (ifSlotIsCategory(e.getSlot())) {
                //edit category
            }else if (ifSlotIsItem(e.getSlot())) {
                //edit item
            }else if (e.getSlot() == 49) {
                //back
            }else if (e.getSlot() == 53) {
                //next
            }
            e.setCancelled(true);
        }
    }

    private boolean ifSlotIsCategory(int slot) {
        return (slot >= 18 && slot < 21) || (slot >= 27 && slot < 30) || (slot >= 36 && slot < 39) || (slot >= 45 && slot < 48);
    }

    private boolean ifSlotIsItem(int slot) {
        return (slot >= 22 && slot < 27) || (slot >= 31 && slot < 36) || (slot >= 40 && slot < 45);
    }
}
