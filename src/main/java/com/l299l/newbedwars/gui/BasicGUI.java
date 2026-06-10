package com.l299l.newbedwars.gui;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

public abstract class BasicGUI implements InventoryHolder {
    private final GuiManager guiManager;
    private final Player player;
    private final Messages msg;

    protected BasicGUI(GuiManager guiManager, Player player) {
        this.guiManager = guiManager;
        this.player = player;
        this.msg = NewBedwars.plugin.getMessages();
    }

    protected GuiManager getGuiManager() {
        return guiManager;
    }

    protected Player getPlayer() {
        return player;
    }

    protected Messages getMsg() {
        return msg;
    }

}
