package com.l299l.newbedwars.gui;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.game.listener.ArenaSelectGUISettings;
import com.l299l.newbedwars.gui.configuration.game.listener.FastBuyCustomizerGUISettings;
import com.l299l.newbedwars.gui.configuration.game.listener.ItemPickerGUISettings;
import com.l299l.newbedwars.gui.configuration.game.listener.ProfileGUISettings;
import com.l299l.newbedwars.gui.configuration.game.listener.ShopGUISettings;
import com.l299l.newbedwars.gui.configuration.game.listener.SpectateSelectGUISettings;
import com.l299l.newbedwars.gui.configuration.game.listener.SpectatorGUISettings;
import com.l299l.newbedwars.gui.configuration.setup.listener.ArenaInfoGUISettings;
import com.l299l.newbedwars.gui.configuration.setup.listener.BasicConfigurationGUISettings;
import com.l299l.newbedwars.gui.configuration.setup.listener.ConfirmLeaveGUISettings;
import com.l299l.newbedwars.gui.configuration.setup.listener.DeleteConfirmGUISettings;
import com.l299l.newbedwars.gui.configuration.setup.listener.SetupPickerGUISettings;
import com.l299l.newbedwars.version.VersionCompat;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class GUIManager {
    private final HashMap<String, GUISave> guis;

    public GUIManager() {
        guis = new HashMap<>();
    }
    public void reloadGuis() {
        NewBedwars plugin = NewBedwars.plugin;
        plugin.getServer().getPluginManager().registerEvents(new GUIProtectionListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ArenaInfoGUISettings(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new BasicConfigurationGUISettings(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ConfirmLeaveGUISettings(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DeleteConfirmGUISettings(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SetupPickerGUISettings(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ShopGUISettings(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SpectatorGUISettings(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ProfileGUISettings(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new FastBuyCustomizerGUISettings(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ItemPickerGUISettings(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ArenaSelectGUISettings(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SpectateSelectGUISettings(), plugin);
        guis.clear();
        List<GUISave> guiSaves = plugin.getDataManager().getGuiData();
        for (GUISave guiSave : guiSaves) {
            guis.put(guiSave.getGuiId(), guiSave);
        }
    }

    public GUISave getGui(String id) {
        return guis.get(id);
    }

    public HashMap<String, GUISave> getGuis() {
        return guis;
    }

    public ItemStack createIcon(String icon, String name, String lore, Boolean enchanted) {
        ItemStack it;
        ArrayList<String> loreList = new ArrayList<>(Arrays.asList(lore.split(";|\n")));
        if (Material.matchMaterial(icon) == null) {
            it = getPlayerHead(icon, name, loreList);
            if (enchanted) {
                ItemMeta meta = it.getItemMeta();
                VersionCompat.addGlowEffect(meta);
                it.setItemMeta(meta);
            }
        } else {
            it = createItem(name, Material.matchMaterial(icon), loreList, enchanted);
        }
        return it;
    }

    public ItemStack createItem(String name, Material mat, List<String> lore, Boolean enchanted) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(name);
        meta.setLore(lore);
        if (enchanted) {
            VersionCompat.addGlowEffect(meta);
        }
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getPlayerHead(String player, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        assert meta != null;
        VersionCompat.setSkullOwner(meta, player);
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getRightArrow(Messages messages, Player p) {
        return getPlayerHead("MHF_ArrowRight", messages.getMsg(p, "gui.right-arrow.name"), Arrays.stream(messages.getMsg(p, "gui.right-arrow.lore")
                .split("\n")).toList());
    }

    public ItemStack getLeftArrow(Messages messages, Player p) {
        return getPlayerHead("MHF_ArrowLeft", messages.getMsg(p, "gui.left-arrow.name"), Arrays.stream(messages.getMsg(p, "gui.left-arrow.lore")
                .split("\n")).toList());
    }

    public ItemStack getLeaveItem(Player p) {
        Messages messages = NewBedwars.plugin.getMessages();
        return createItem(messages.getMsg(p, "LeaveItemName"), Material.RED_BED, Arrays.stream(messages.getMsg(p, "LeaveItemLore")
                .split("\n")).toList(), false);
    }

    public ItemStack getSpectatorItem(Player p) {
        Messages messages = NewBedwars.plugin.getMessages();
        return createItem(messages.getMsg(p, "SpectatorItemName"), Material.COMPASS, Arrays.stream(messages.getMsg(p, "SpectatorItemLore")
                .split("\n")).toList(), false);
    }

    public ItemStack getSpectatorEffectsItem(Player p) {
        Messages messages = NewBedwars.plugin.getMessages();
        return createItem(messages.getMsg(p, "SpectatorEffectsItemName"), Material.COMPARATOR, Arrays.stream(messages.getMsg(p, "SpectatorEffectsItemLore")
                .split("\n")).toList(), false);
    }
}
