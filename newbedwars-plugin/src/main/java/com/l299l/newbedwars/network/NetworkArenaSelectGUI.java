package com.l299l.newbedwars.network;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.gui.ItemBuilder;
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

public final class NetworkArenaSelectGUI implements InventoryHolder {
    private final Inventory inv;
    private final Map<Integer, RemoteArena> slotToArena = new HashMap<>();
    private final Player viewer;
    private final boolean spectate;

    public NetworkArenaSelectGUI(Player viewer, List<RemoteArena> arenas) {
        this(viewer, arenas, false);
    }

    public NetworkArenaSelectGUI(Player viewer, List<RemoteArena> arenas, boolean spectate) {
        this.viewer = viewer;
        this.spectate = spectate;
        List<RemoteArena> shown = arenas.stream()
                .filter(a -> a.info().state != ArenaInfo.State.DISABLED)
                .filter(a -> !spectate || isRunning(a))
                .toList();
        int size = Math.min(54, Math.max(9, (int) Math.ceil(shown.size() / 9.0) * 9));
        inv = Bukkit.createInventory(this, size, ChatColor.DARK_AQUA + "" + ChatColor.BOLD
                + text(spectate ? "GuiSpectateArenaTitle" : "GuiSelectArenaTitle"));
        for (int i = 0; i < shown.size() && i < size; i++) {
            inv.setItem(i, item(shown.get(i)));
            slotToArena.put(i, shown.get(i));
        }
    }

    private ItemStack item(RemoteArena a) {
        ArenaInfo info = a.info();
        String action = spectate ? ChatColor.GREEN + text("GuiClickSpectate")
                : info.isJoinable() ? ChatColor.GREEN + text("GuiClickJoin") : ChatColor.RED + text("GuiGameInProgress");
        return ItemBuilder.of(material(info.state))
                .name(ChatColor.AQUA + "" + ChatColor.BOLD + a.getName())
                .lore(ChatColor.GRAY + text("GuiStatus") + ": " + label(info.state),
                        ChatColor.GRAY + text("GuiPlayers") + ": " + ChatColor.WHITE + info.players
                                + ChatColor.GRAY + "/" + ChatColor.WHITE + info.maxPlayers,
                        ChatColor.GRAY + text("GuiTeams") + ": " + ChatColor.WHITE + info.teams,
                        ChatColor.GRAY + text("GuiServer") + ": " + ChatColor.WHITE + a.getServer(),
                        "",
                        action)
                .build();
    }

    private static Material material(ArenaInfo.State s) {
        if (s == null) return Material.GRAY_WOOL;
        return switch (s) {
            case WAITING -> Material.LIME_WOOL;
            case STARTING -> Material.YELLOW_WOOL;
            case PLAYING -> Material.RED_WOOL;
            case ENDING -> Material.ORANGE_WOOL;
            default -> Material.GRAY_WOOL;
        };
    }

    private String label(ArenaInfo.State s) {
        if (s == null) return ChatColor.GRAY + text("GuiStatusRestarting");
        return switch (s) {
            case WAITING -> ChatColor.GREEN + text("GuiStatusWaiting");
            case STARTING -> ChatColor.YELLOW + text("GuiStatusStarting");
            case PLAYING -> ChatColor.RED + text("GuiStatusPlaying");
            case ENDING -> ChatColor.GOLD + text("GuiStatusEnding");
            default -> ChatColor.GRAY + text("GuiStatusRestarting");
        };
    }

    private String text(String key) {
        return NewBedwars.plugin.getMessages().getMsg(viewer, key);
    }

    static boolean isRunning(RemoteArena a) {
        return a.info().state == ArenaInfo.State.PLAYING || a.info().state == ArenaInfo.State.ENDING;
    }

    public boolean isSpectate() {
        return spectate;
    }

    public RemoteArena getArena(int slot) {
        return slotToArena.get(slot);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
