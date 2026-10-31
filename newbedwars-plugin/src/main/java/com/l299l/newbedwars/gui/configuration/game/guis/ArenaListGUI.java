package com.l299l.newbedwars.gui.configuration.game.guis;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** A chest listing the enabled arenas (sorted by name), one icon per arena; subclasses draw the icon. */
public abstract class ArenaListGUI implements InventoryHolder {
    private final Inventory inv;
    private final Map<Integer, IArena> slotToArena = new HashMap<>();
    protected final Player viewer;

    protected ArenaListGUI(Player viewer, String title, Predicate<IArena> filter) {
        this.viewer = viewer;
        List<IArena> arenas = ArenaRegistry.all().stream()
                .filter(IArena::isEnabled)
                .filter(filter)
                .sorted((a, b) -> a.getArenaName().compareToIgnoreCase(b.getArenaName()))
                .toList();

        int size = Math.min(54, Math.max(9, (int) Math.ceil(arenas.size() / 9.0) * 9));
        inv = Bukkit.createInventory(this, size, title);

        for (int i = 0; i < arenas.size() && i < size; i++) {
            IArena arena = arenas.get(i);
            inv.setItem(i, buildArenaItem(arena));
            slotToArena.put(i, arena);
        }
    }

    protected abstract ItemStack buildArenaItem(IArena arena);

    protected static Material statusMaterial(GameStatus status) {
        return switch (status) {
            case waiting -> Material.LIME_WOOL;
            case starting -> Material.YELLOW_WOOL;
            case playing -> Material.RED_WOOL;
            case ending -> Material.ORANGE_WOOL;
            default -> Material.GRAY_WOOL;
        };
    }

    protected String statusLabel(GameStatus status) {
        return switch (status) {
            case waiting -> ChatColor.GREEN + text("GuiStatusWaiting");
            case starting -> ChatColor.YELLOW + text("GuiStatusStarting");
            case playing -> ChatColor.RED + text("GuiStatusPlaying");
            case ending -> ChatColor.GOLD + text("GuiStatusEnding");
            default -> ChatColor.GRAY + text("GuiStatusRestarting");
        };
    }

    /** A message in the viewer's language. */
    protected String text(String key) {
        return text(viewer, key);
    }

    protected static String text(Player viewer, String key) {
        return NewBedwars.plugin.getMessages().getMsg(viewer, key);
    }

    public IArena getArena(int slot) {
        return slotToArena.get(slot);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
