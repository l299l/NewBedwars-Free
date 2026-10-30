package com.l299l.newbedwars.gui.configuration.game.guis;

import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.gui.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SpectateSelectGUI extends ArenaListGUI {

    public SpectateSelectGUI(Player player) {
        super(player, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + text(player, "GuiSpectateArenaTitle"), a -> true);
    }

    private boolean canSpectate(IArena arena) {
        return (arena.status() == GameStatus.playing || arena.status() == GameStatus.ending)
                && arena.getGamerules() != null && arena.getGamerules().AllowSpectators;
    }

    @Override
    protected ItemStack buildArenaItem(IArena arena) {
        GameStatus status = arena.status();
        boolean spectatable = canSpectate(arena);
        String action;
        if (spectatable) {
            action = ChatColor.GREEN + text("GuiClickSpectate");
        } else if (arena.getGamerules() == null || !arena.getGamerules().AllowSpectators) {
            action = ChatColor.RED + text("GuiSpectatingDisabled");
        } else {
            action = ChatColor.RED + text("GuiGameNotStarted");
        }
        return ItemBuilder.of(spectatable ? statusMaterial(status) : Material.GRAY_WOOL)
                .name(spectatable
                        ? ChatColor.AQUA + "" + ChatColor.BOLD + arena.getArenaName()
                        : ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + arena.getArenaName())
                .lore(ChatColor.GRAY + text("GuiStatus") + ": " + statusLabel(status),
                        ChatColor.GRAY + text("GuiPlayers") + ": " + ChatColor.WHITE + arena.getActivePlayerCount()
                                + ChatColor.GRAY + "/" + ChatColor.WHITE + arena.getMaxPlayers(),
                        ChatColor.GRAY + text("GuiSpectators") + ": " + ChatColor.WHITE + arena.getSpectators().size(),
                        "",
                        action)
                .build();
    }
}
