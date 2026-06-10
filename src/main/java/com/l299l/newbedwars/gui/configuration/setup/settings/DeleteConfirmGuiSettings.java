package com.l299l.newbedwars.gui.configuration.setup.settings;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.setup.guis.DeleteConfirmGUI;
import com.l299l.newbedwars.utils.DecoUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class DeleteConfirmGuiSettings implements Listener {
    private final Messages msg;

    public DeleteConfirmGuiSettings() {
        msg = NewBedwars.plugin.getMessages();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        if (!(e.getClickedInventory().getHolder() instanceof DeleteConfirmGUI gui)) return;

        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        int slot = e.getSlot();

        if (slot >= 0 && slot < 4) {
            player.closeInventory();
            deleteArena(gui.getArenaName(), player);
        } else if (slot >= 5 && slot < 9) {
            player.closeInventory();
        }
    }

    private void deleteArena(String arenaName, Player player) {
        IArena arena = Arena.arenaByName.get(arenaName);
        if (arena == null) {
            msg.send(player, "ArenaNotExists");
            return;
        }

        GameStatus status = arena.status();
        if (status == GameStatus.playing || status == GameStatus.starting || status == GameStatus.ending) {
            msg.send(player, "ArenaCannotBeDeleted", new HashMap<>() {{
                put("/arenaname/", arenaName);
            }});
            return;
        }

        World arenaWorld = arena.getArenaWorld();
        File worldFolder = arenaWorld.getWorldFolder();

        DecoUtils.removeAllArmorStands(arena);

        Location lobby = NewBedwars.plugin.getLobbyLocation();
        for (Player p : new ArrayList<>(arenaWorld.getPlayers())) {
            p.setInvulnerable(false);
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            p.teleport(lobby);
        }

        Arena.arenaByName.remove(arenaName);
        Arena.arenaByWorld.remove(arenaWorld);

        arenaWorld.setAutoSave(false);
        Bukkit.unloadWorld(arenaWorld, false);

        deleteFolder(worldFolder);
        new File("plugins/NewBedwars/data/arenas/" + arenaName + ".json").delete();

        msg.send(player, "ArenaDeleted", new HashMap<>() {{
            put("/arenaname/", arenaName);
        }});
    }

    private void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }
        folder.delete();
    }
}
