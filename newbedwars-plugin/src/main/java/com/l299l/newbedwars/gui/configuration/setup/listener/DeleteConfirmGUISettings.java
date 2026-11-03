package com.l299l.newbedwars.gui.configuration.setup.listener;

import com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.player.LobbyService;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.world.template.ArenaMaps;
import com.l299l.newbedwars.world.template.MapTemplateManager;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.setup.guis.DeleteConfirmGUI;
import com.l299l.newbedwars.arena.setup.DecoUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;

public class DeleteConfirmGUISettings implements Listener {
    private final Messages msg;

    public DeleteConfirmGUISettings() {
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
        IArena arena = ArenaRegistry.byName(arenaName);
        if (arena == null) {
            msg.send(player, "ArenaNotExists");
            return;
        }

        GameStatus status = arena.status();
        if (status == GameStatus.playing || status == GameStatus.starting || status == GameStatus.ending) {
            msg.send(player, "ArenaCannotBeDeleted", Map.of("/arenaname/", arenaName));
            return;
        }

        World arenaWorld = arena.getArenaWorld();
        Path worldFolder = arenaWorld.getWorldFolder().toPath();
        if (arenaWorld.equals(Bukkit.getWorlds().get(0))) {
            msg.send(player, "ArenaDeleteFailed");
            return;
        }

        DecoUtils.removeAllArmorStands(arena);

        Location lobby = NewBedwars.plugin.getLobbyLocation();
        for (Player p : new ArrayList<>(arenaWorld.getPlayers())) {
            p.setInvulnerable(false);
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            if (!LobbyService.sendToLobby(p, PlayerSendToLobbyEvent.Reason.ARENA_DELETED) && p.getWorld().equals(arenaWorld)) {
                p.teleport(lobby);
            }
        }

        arenaWorld.setAutoSave(false);
        if (!Bukkit.unloadWorld(arenaWorld, false)) {
            msg.send(player, "ArenaDeleteFailed");
            return;
        }
        ArenaRegistry.unregister(arenaName, arenaWorld);

        IOExecutor.flush();
        boolean complete = deleteQuietly(worldFolder);
        complete &= deleteQuietly(NewBedwars.plugin.getDataFolder().toPath().resolve("data/arenas/" + arenaName + ".json"));

        complete &= deleteQuietly(ArenaMaps.mapsRoot().resolve(arenaName));

        msg.send(player, complete ? "ArenaDeleted" : "ArenaDeletedPartially", Map.of("/arenaname/", arenaName));
    }

    private boolean deleteQuietly(Path path) {
        try {
            MapTemplateManager.deleteTree(path);
            return true;
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not delete " + path, e);
            return false;
        }
    }
}
