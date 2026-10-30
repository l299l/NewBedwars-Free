package com.l299l.newbedwars.gui;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

public abstract class BasicGUI implements InventoryHolder {
    private final GUIManager guiManager;
    private final Player player;
    private final Messages msg;

    protected BasicGUI(GUIManager guiManager, Player player) {
        this.guiManager = guiManager;
        this.player = player;
        this.msg = NewBedwars.plugin.getMessages();
    }

    protected GUIManager getGuiManager() {
        return guiManager;
    }

    protected Player getPlayer() {
        return player;
    }

    protected Messages getMsg() {
        return msg;
    }

}
