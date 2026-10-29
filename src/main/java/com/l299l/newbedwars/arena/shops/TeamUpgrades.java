package com.l299l.newbedwars.arena.shops;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.gui.configuration.game.guis.ShopGUI;
import com.l299l.newbedwars.utils.JsonUtils;
import com.l299l.newbedwars.version.VersionCompat;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class TeamUpgrades {
    private final HashMap<Upgrade, Integer> upgrades;
    private final Location location;
    private LivingEntity npc;
    private BukkitTask npcWatchdog;
    private BukkitTask healPoolTask;
    private final UUID uuid;
    private final EntityType entityType;
    private final Team team;

    public TeamUpgrades(Location location, EntityType entityType, Team team) {
        upgrades = new HashMap<>();
        this.location = location;
        this.entityType = entityType;
        this.team = team;
        uuid = UUID.randomUUID();
    }

    public TeamUpgrades(Location location, EntityType entityType, Team team, UUID uuid) {
        upgrades = new HashMap<>();
        this.location = location;
        this.entityType = entityType;
        this.team = team;
        this.uuid = uuid;
    }

    public void start() {
        initUpgrades();
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
        com.l299l.newbedwars.utils.DecoUtils.makeAdult(npc);
        npc.setInvulnerable(true);
        npc.setCustomName(team.getColor() + team.getName() + " Upgrades");
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
        if (healPoolTask != null) {
            healPoolTask.cancel();
            healPoolTask = null;
        }
        location.getChunk().setForceLoaded(false);
        if (npc != null) {
            npc.remove();
            npc = null;
        }
        // bought levels belong to one game; the next start() must not equip armor with the old Protection
        upgrades.clear();
    }

    public Integer getUpgradeLevel(Upgrade upgrade) {
        return upgrades.getOrDefault(upgrade, 0);
    }

    public void upgrade(Upgrade upgrade) {
        upgrades.put(upgrade, getUpgradeLevel(upgrade) + 1);
        if (!upgrade.handler().isTrap()) upgrade.handler().onPurchase(this, team, getUpgradeLevel(upgrade));
    }

    /** Uses up one level of every bought trap and fires it at the enemy who entered the base. */
    public void triggerTraps(Player enemy, IArena arena) {
        for (Upgrade upgrade : Upgrade.values()) {
            if (!upgrade.handler().isTrap() || getUpgradeLevel(upgrade) <= 0) continue;
            upgrades.put(upgrade, getUpgradeLevel(upgrade) - 1);
            upgrade.handler().trigger(enemy, team, arena);
        }
    }

    void startHealPool() {
        if (healPoolTask != null) healPoolTask.cancel();
        healPoolTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : team.getPlayers()) {
                    if (team.isInBase(player.getLocation())) {
                        // Only (re-)apply if the effect is absent or nearly expired to avoid
                        // resetting the internal heal-pulse counter before it can fire.
                        PotionEffect existing = player.getPotionEffect(PotionEffectType.REGENERATION);
                        if (existing == null || existing.getDuration() <= 50) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0, true, false));
                        }
                    }
                }
            }
        }.runTaskTimer(NewBedwars.plugin, 0L, 10L);
    }

    /** Re-applies the upgrades that live on the player (sword enchant, haste, ...) after a respawn. */
    public void applyPlayerUpgrades(Player player) {
        for (Upgrade upgrade : Upgrade.values()) {
            int level = getUpgradeLevel(upgrade);
            if (level > 0 && upgrade.handler().reapplyOnRespawn()) upgrade.handler().apply(player, team, level);
        }
    }

    public void open(Player player) {
        IArena arena = Arena.arenaByWorld.get(player.getWorld());
        ShopGUI gui = new ShopGUI(NewBedwars.plugin.getGuiManager(), player, arena.getPlayer(player.getUniqueId()).getPlayerUpgradesGui(), null);
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

    private void initUpgrades() {
        for (Upgrade up : Upgrade.values()) {
            upgrades.put(up, 0);
        }
    }
}
