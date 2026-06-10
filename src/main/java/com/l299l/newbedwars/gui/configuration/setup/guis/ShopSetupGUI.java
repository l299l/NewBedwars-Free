package com.l299l.newbedwars.gui.configuration.setup.guis;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.arena.shops.customitems.CustomItemManager;
import com.l299l.newbedwars.gui.GuiManager;
import com.l299l.newbedwars.gui.configuration.game.guis.ShopGUI;
import com.l299l.newbedwars.gui.configuration.game.guis.other.GuiCategory;
import com.l299l.newbedwars.gui.configuration.setup.SetupGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bukkit.Bukkit.createInventory;

public class ShopSetupGUI extends SetupGUI {
    private final Inventory inv;
    private final ShopGUI shopGUI;
    private final CustomItemManager customItemManager;
    private boolean editMainPageConfirmed = false;

    public ShopSetupGUI(GuiManager guiManager, Player player, CustomItemManager customItemManager) {
        super(guiManager, player);
        this.customItemManager = customItemManager;
        //shopGUI = new ShopGUI(guiManager, player);
        shopGUI = null;
        inv = createInventory(this, 54, getMsg().getMsg(getPlayer(), "ShopSetupGuiName"));
        init();
    }

    @Override
    public ItemStack getIcon() {
        return getGuiManager().createIcon(getMsg().getMsg(getPlayer(), "ShopSetupGuiIcon"), getMsg().getMsg(getPlayer(), "ShopSetupGuiIconName"),
                getMsg().getMsg(getPlayer(), "ShopSetupGuiIconLore"), false);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
    public ShopGUI getShopGUI() {
        return shopGUI;
    }

    public boolean isEditMainPageConfirmed() {
        return editMainPageConfirmed;
    }

    public void setEditMainPageConfirmed(boolean editMainPageConfirmed) {
        this.editMainPageConfirmed = editMainPageConfirmed;
    }

    private void init() {
        inv.setItem(0, getEditMainPageItem());
        inv.setItem(3, getCreateCategoryItem());
        inv.setItem(4, getInfoBookItem());
        inv.setItem(5, getCreateCustomItemItem());
        inv.setItem(8, getShowShopSymulationItem());
        for(int i = 9; i < 18; i++) {
            inv.setItem(i, getGuiManager().createItem(" ", Material.BLACK_STAINED_GLASS_PANE, null, false));
        }
        for(int i = 21; i < 54; i+=9) {
            inv.setItem(i, getGuiManager().createItem(" ", Material.BLACK_STAINED_GLASS_PANE, null, false));
        }
        ItemStack[] categoryItems = getCategoryItems();
        int i = 18;
        for(ItemStack item : categoryItems) {
            inv.setItem(i, item);
            i++;
            if (i == 21) i = 27;
            if (i == 30) i = 36;
            if (i == 39) i = 45;
            if (i == 48) break;
        }
        List<CustomItem> customItems = customItemManager.getCustomItems();
        i = 22;
        for(CustomItem item : customItems) {
            inv.setItem(i, item.getIcon(NewBedwars.plugin.getPlayerManager().getPlayer(getPlayer().getName()).language()));
            i++;
            if (i == 27) i = 31;
            if (i == 36) i = 40;
            if (i == 45) break;
        }
        inv.setItem(49, getGuiManager().getLeftArrow(NewBedwars.plugin.getMessages(), getPlayer()));
        for (i = 50; i < 53; i++) inv.setItem(i, getGuiManager().createItem(" ", Material.BLACK_STAINED_GLASS_PANE, null, false));
        inv.setItem(50, getGuiManager().createItem(" ", Material.BLACK_STAINED_GLASS_PANE, null, false));
        inv.setItem(53, getGuiManager().getRightArrow(NewBedwars.plugin.getMessages(), getPlayer()));
    }

    private ItemStack getEditMainPageItem() {
        return getGuiManager().createItem(getMsg().getMsg(getPlayer(), "ShopSetupGuiEditMainPageItemName"), Material.GOLDEN_APPLE,
                new ArrayList<>(Arrays.asList(getMsg().getMsg(getPlayer(), "ShopSetupGuiEditMainPageItemLore").split("\n"))),true);
    }

    private ItemStack getCreateCategoryItem() {
        return getGuiManager().createItem(getMsg().getMsg(getPlayer(), "ShopSetupGuiCreateCategoryItemName"), Material.STRUCTURE_BLOCK,
                new ArrayList<>(Arrays.asList(getMsg().getMsg(getPlayer(), "ShopSetupGuiCreateCategoryItemLore").split("\n"))), true);
    }

    private ItemStack getCreateCustomItemItem() {
        return getGuiManager().createItem(getMsg().getMsg(getPlayer(), "ShopSetupGuiCreateCustomItemItemName"), Material.COMMAND_BLOCK,
                new ArrayList<>(Arrays.asList(getMsg().getMsg(getPlayer(), "ShopSetupGuiCreateCustomItemItemLore").split("\n"))), true);
    }

    private ItemStack getShowShopSymulationItem() {
        return getIcon();
    }

    private ItemStack getInfoBookItem() {
        return getGuiManager().createItem(getMsg().getMsg(getPlayer(), "ShopSetupGuiInfoBookItemName"), Material.BOOK,
                new ArrayList<>(Arrays.asList(getMsg().getMsg(getPlayer(), "ShopSetupGuiInfoBookItemLore").split("\n"))), true);
    }

    private ItemStack[] getCategoryItems() {
        List<GuiCategory> categories = shopGUI.getCategories();
        ItemStack[] items = new ItemStack[categories.size()];
        for(int i = 0; i < categories.size(); i++) {
            items[i] = categories.get(i).getIcon(getPlayer());
        }
        return items;
    }
}
