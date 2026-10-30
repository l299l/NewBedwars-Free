package com.l299l.newbedwars.gui.configuration.setup;

import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GUIManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class SetupGUI extends BasicGUI {

    protected SetupGUI(GUIManager guiManager, Player player) {
        super(guiManager, player);
    }
    public abstract ItemStack getIcon();

}
