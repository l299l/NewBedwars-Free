package com.l299l.newbedwars.gui.configuration.setup.guis;

import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GuiManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class ConfirmLeaveGUI extends BasicGUI {
    private final Inventory inv;
    private final Boolean all;

    public ConfirmLeaveGUI(GuiManager guiManager, Player player, Boolean all) {
        super(guiManager, player);
        this.all = all;
        inv = Bukkit.createInventory(this, 9, getMsg().getMsg(getPlayer(), "ConfirmLeaveGuiName"));
        init();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    public Boolean getAll() {
        return all;
    }

    private void init() {
        String lore;
        String[] ulore;
        List<String> newLore;
        for (int i = 0; i < 4; i++) {
            lore = getMsg().getMsg(getPlayer(), "YesLore");
            ulore =  lore.split(";");
            newLore = Arrays.asList(ulore);
            inv.setItem(i, getGuiManager().createItem(getMsg().getYes(getPlayer(), true), Material.GREEN_STAINED_GLASS_PANE, newLore, false));
        }
        lore = getMsg().getMsg(getPlayer(), "LeaveInfoLore");
        ulore =  lore.split(";");
        newLore = Arrays.asList(ulore);
        inv.setItem(4, getGuiManager().createItem(getMsg().getMsg(getPlayer(), "LeaveInfo"), Material.ENCHANTED_BOOK, newLore, true));
        for (int i = 5; i < 9; i++) {
            lore = getMsg().getMsg(getPlayer(), "NoLore");
            ulore =  lore.split(";");
            newLore = Arrays.asList(ulore);
            inv.setItem(i, getGuiManager().createItem(getMsg().getYes(getPlayer(), false), Material.RED_STAINED_GLASS_PANE, newLore, false));
        }
    }
}
