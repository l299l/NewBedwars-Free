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

public class DeleteConfirmGUI extends BasicGUI {
    private final Inventory inv;
    private final String arenaName;

    public DeleteConfirmGUI(GuiManager guiManager, Player player, String arenaName) {
        super(guiManager, player);
        this.arenaName = arenaName;
        String title = getMsg().getMsg(getPlayer(), "ConfirmDeleteGuiName");
        inv = Bukkit.createInventory(this, 9, title);
        init();
    }

    public String getArenaName() {
        return arenaName;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    private void init() {
        for (int i = 0; i < 4; i++) {
            String lore = getMsg().getMsg(getPlayer(), "DeleteYesLore");
            List<String> loreLines = Arrays.asList(lore.split(";"));
            inv.setItem(i, getGuiManager().createItem(
                    getMsg().getYes(getPlayer(), true), Material.GREEN_STAINED_GLASS_PANE, loreLines, false));
        }

        String infoLore = getMsg().getMsg(getPlayer(), "DeleteInfoLore")
                .replaceAll("/arenaname/", arenaName);
        String infoName = getMsg().getMsg(getPlayer(), "DeleteInfo")
                .replaceAll("/arenaname/", arenaName);
        List<String> infoLoreLines = Arrays.asList(infoLore.split(";"));
        inv.setItem(4, getGuiManager().createItem(infoName, Material.BARRIER, infoLoreLines, true));

        for (int i = 5; i < 9; i++) {
            String lore = getMsg().getMsg(getPlayer(), "DeleteNoLore");
            List<String> loreLines = Arrays.asList(lore.split(";"));
            inv.setItem(i, getGuiManager().createItem(
                    getMsg().getYes(getPlayer(), false), Material.RED_STAINED_GLASS_PANE, loreLines, false));
        }
    }
}
