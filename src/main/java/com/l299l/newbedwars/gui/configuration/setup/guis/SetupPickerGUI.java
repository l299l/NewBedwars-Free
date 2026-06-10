package com.l299l.newbedwars.gui.configuration.setup.guis;

import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GuiManager;
import com.l299l.newbedwars.gui.configuration.setup.SetupGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SetupPickerGUI extends BasicGUI {

    private final Inventory inv;
    private final List<SetupGUI> setups;

    public SetupPickerGUI(GuiManager guiManager, Player player, List<SetupGUI> setups) {
        super(guiManager, player);
        this.setups = setups;
        inv = Bukkit.createInventory(this, getInvSize(), getMsg().getMsg(player, "SetupPickerGuiName"));
        init();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
    public List<SetupGUI> getSetups() {
        return setups;
    }

    private void init() {
        for (int i = 0; i < setups.size(); i++) {
            inv.setItem(i, setups.get(i).getIcon());
        }
    }

    private int getInvSize() {
        return (int) Math.ceil(setups.size() / 9.0) * 9;
    }
}
