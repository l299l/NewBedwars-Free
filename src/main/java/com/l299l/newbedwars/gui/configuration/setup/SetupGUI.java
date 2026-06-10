package com.l299l.newbedwars.gui.configuration.setup;

import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GuiManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class SetupGUI extends BasicGUI {

    protected SetupGUI(GuiManager guiManager, Player player) {
        super(guiManager, player);
    }
    public abstract ItemStack getIcon();

}
