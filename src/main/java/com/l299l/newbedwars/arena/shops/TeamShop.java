package com.l299l.newbedwars.arena.shops;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.gui.configuration.game.guis.ShopGUI;
import com.l299l.newbedwars.utils.JsonUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class TeamShop {
    private final Location location;
    private final UUID uuid;
    private final EntityType entityType;
    private final Team team;
    private LivingEntity npc;
    private BukkitTask npcWatchdog;

    public TeamShop(Location location, EntityType entityType, Team team) {
        this.location = location;
        this.entityType = entityType;
        this.team = team;
        uuid = UUID.randomUUID();
    }

    public TeamShop(Location location, EntityType entityType, Team team, UUID uuid) {
        this.location = location;
        this.entityType = entityType;
        this.team = team;
        this.uuid = uuid;
    }

    public void start() {
        location.getChunk().load(true);
        location.getWorld().getNearbyEntities(location, 1.5, 1.5, 1.5).stream()
                .filter(e -> e.getType() == entityType)
                .forEach(Entity::remove);
        location.getChunk().setForceLoaded(true);
        spawnNpc();
        npcWatchdog = new BukkitRunnable() {
            @Override
            public void run() {
                if (npc == null || !npc.isValid()) {
                    spawnNpc();
                } else {
                    if (npc.getFireTicks() > 0) {
                        npc.setFireTicks(0);
                        npc.setVisualFire(false);
                    }
                }
            }
        }.runTaskTimer(NewBedwars.plugin, 20L, 20L);
    }

    private void spawnNpc() {
        if (npc != null && npc.isValid()) {
            npc.remove();
        }
        npc = (LivingEntity) location.getWorld().spawnEntity(location, entityType);
        npc.setInvulnerable(true);
        npc.setCustomName(team.getColor() + team.getName() + " Shop");
        npc.setCustomNameVisible(true);
        npc.setSilent(true);
        npc.setGravity(false);
        npc.setPersistent(true);
        npc.setAI(false);
        npc.setCollidable(false);
        npc.setCanPickupItems(false);
        npc.setVisualFire(false);
        npc.setFireTicks(0);
        npc.setRotation(location.getYaw(), 0f);
    }

    public void stop() {
        if (npcWatchdog != null) {
            npcWatchdog.cancel();
            npcWatchdog = null;
        }
        location.getChunk().setForceLoaded(false);
        if (npc != null) {
            npc.remove();
            npc = null;
        }
    }

    public void open(Player player) {
        IArena arena = Arena.arenaByWorld.get(player.getWorld());
        ShopGUI gui = new ShopGUI(NewBedwars.plugin.getGuiManager(), player, arena.getPlayer(player.getUniqueId()).getPlayerShopGui(), null);
        player.openInventory(gui.getInventory());
    }

    public Location getLocation() {
        return location;
    }

    public LivingEntity getNpc() {
        return npc;
    }

    public UUID getUuid() {
        return uuid;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Team getTeam() {
        return team;
    }
    public String toJson() {
        String sb = "{" +
                "\"location\": " + JsonUtils.locationToJson(location) + "," +
                "\"uuid\": \"" + uuid + "\"," +
                "\"entityType\": \"" + entityType + "\"" +
                "}";
        return sb;
    }
}
