package com.l299l.newbedwars.gui.configuration.setup.guis;

import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GuiManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class BasicConfigurationGUI extends BasicGUI {
    private final Inventory inv;

    public BasicConfigurationGUI(GuiManager guiManager, Player player) {
        super(guiManager, player);
        inv = Bukkit.createInventory(this, 27, getMsg().getMsg(player, "ConfigurationGuiName"));
        init();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    private void init(){
        for (int i = 0; i < 27; i++){
            if(i == 11) {
                String lore = getMsg().getMsg(getPlayer(), "NormalSetupLore");
                String[] ulore =  lore.split(";");
                List<String> newlore = Arrays.asList(ulore);
                inv.setItem(i, getGuiManager().createItem(getMsg().getMsg(getPlayer(), "NormalSetup"), Material.COMPASS, newlore, true));
            }else if(i == 13) {
                inv.setItem(i, getGuiManager().createItem(
                        "§8§m" + getMsg().getMsg(getPlayer(), "AdvancedSetup"),
                        Material.BARRIER,
                        Collections.singletonList("§c§lNot yet implemented"), false));
            }else if(i == 15) {
                inv.setItem(i, getGuiManager().createItem(
                        "§8§m" + getMsg().getMsg(getPlayer(), "AutomaticSetup"),
                        Material.BARRIER,
                        Collections.singletonList("§c§lNot yet implemented"), false));
            }else {
                inv.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
            }
        }
    }
}
