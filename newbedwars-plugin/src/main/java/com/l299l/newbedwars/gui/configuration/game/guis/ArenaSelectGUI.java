package com.l299l.newbedwars.gui.configuration.game.guis;

import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.gui.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

public class ArenaSelectGUI extends ArenaListGUI {

    public ArenaSelectGUI(Player player) {
        this(player, a -> true);
    }

    /** Lists only the enabled arenas that match {@code filter}. */
    public ArenaSelectGUI(Player player, Predicate<IArena> filter) {
        super(player, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + text(player, "GuiSelectArenaTitle"), filter);
    }

    @Override
    protected ItemStack buildArenaItem(IArena arena) {
        GameStatus status = arena.status();
        String action;
        if (arena.isPlayerInArena(viewer) && status == GameStatus.playing) {
            action = ChatColor.GREEN + text("GuiClickRejoin");
        } else if (status == GameStatus.waiting || status == GameStatus.starting) {
            action = ChatColor.GREEN + text("GuiClickJoin");
        } else {
            action = ChatColor.RED + text("GuiGameInProgress");
        }
        return ItemBuilder.of(statusMaterial(status))
                .name(ChatColor.AQUA + "" + ChatColor.BOLD + arena.getArenaName())
                .lore(ChatColor.GRAY + text("GuiStatus") + ": " + statusLabel(status),
                        ChatColor.GRAY + text("GuiPlayers") + ": " + ChatColor.WHITE + arena.getActivePlayerCount()
                                + ChatColor.GRAY + "/" + ChatColor.WHITE + arena.getMaxPlayers(),
                        ChatColor.GRAY + text("GuiTeams") + ": " + ChatColor.WHITE + arena.getTeams().size(),
                        "",
                        action)
                .build();
    }
}
