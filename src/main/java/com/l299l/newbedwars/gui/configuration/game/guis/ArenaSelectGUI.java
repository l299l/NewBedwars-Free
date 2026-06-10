package com.l299l.newbedwars.gui.configuration.game.guis;

import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArenaSelectGUI implements InventoryHolder {
    private final Inventory inv;
    private final Map<Integer, IArena> slotToArena = new HashMap<>();

    public ArenaSelectGUI(Player player) {
        List<IArena> arenas = Arena.arenaByName.values().stream()
                .filter(IArena::isEnabled)
                .sorted((a, b) -> a.getArenaName().compareToIgnoreCase(b.getArenaName()))
                .toList();

        int size = Math.max(9, (int) Math.ceil(arenas.size() / 9.0) * 9);
        size = Math.min(size, 54);
        inv = Bukkit.createInventory(this, size, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Select Arena");

        for (int i = 0; i < arenas.size() && i < size; i++) {
            IArena arena = arenas.get(i);
            inv.setItem(i, buildArenaItem(arena));
            slotToArena.put(i, arena);
        }
    }

    private ItemStack buildArenaItem(IArena arena) {
        GameStatus status = arena.status();
        Material mat;
        String statusLabel;
        switch (status) {
            case waiting  -> { mat = Material.LIME_WOOL;   statusLabel = ChatColor.GREEN + "Waiting"; }
            case starting -> { mat = Material.YELLOW_WOOL; statusLabel = ChatColor.YELLOW + "Starting"; }
            case playing  -> { mat = Material.RED_WOOL;    statusLabel = ChatColor.RED + "In Progress"; }
            case ending   -> { mat = Material.ORANGE_WOOL; statusLabel = ChatColor.GOLD + "Ending"; }
            default       -> { mat = Material.GRAY_WOOL;   statusLabel = ChatColor.GRAY + "Restarting"; }
        }

        int current = arena.getPlayers().size();
        int max = arena.getTeams().size() * arena.getMaxInTeam();

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + arena.getArenaName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Status: " + statusLabel);
        lore.add(ChatColor.GRAY + "Players: " + ChatColor.WHITE + current + ChatColor.GRAY + "/" + ChatColor.WHITE + max);
        lore.add(ChatColor.GRAY + "Teams: " + ChatColor.WHITE + arena.getTeams().size());
        lore.add("");
        if (status == GameStatus.waiting || status == GameStatus.starting) {
            lore.add(ChatColor.GREEN + "Click to join!");
        } else {
            lore.add(ChatColor.RED + "Game already in progress.");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public IArena getArena(int slot) {
        return slotToArena.get(slot);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
