package com.l299l.newbedwars.arena.generators;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.utils.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class EmeraldGenerator implements Generator {

    private final Location location;
    private final GeneratorType type = GeneratorType.EMERALD;
    private int level;
    private double emeraldSpawnRate;
    private int emeraldSpawnAmount;
    private double spawnDelay;
    private BukkitTask task;
    private BukkitTask rotatingTask;
    private Hologram hologram;

    public EmeraldGenerator(Location location) {
        level = 0;
        this.location = location;
        IArena arena = ArenaRegistry.byWorld(location.getWorld());
        if (arena == null) {
            throw new RuntimeException("Arena not found");
        }
        upgrade();
    }

    private com.l299l.newbedwars.arena.generators.leveling.GeneratorSettings settings() {
        IArena arena = ArenaRegistry.byWorld(location.getWorld());
        GeneratorLeveling leveling = arena == null ? null : arena.getGeneratorsLeveling();
        return leveling == null ? null : leveling.getEmeraldGeneratorSettings();
    }

    public int getMaxLevel() {
        com.l299l.newbedwars.arena.generators.leveling.GeneratorSettings settings = settings();
        return settings == null ? 0 : settings.getMaxLevel();
    }

    public int getLevel() {
        return level;
    }

    @Override
    public void start() {
        if (level == 0) upgrade();
        if (getMaxLevel() == 0 || level == 0) {
            return;
        }
        if (task != null) { task.cancel(); task = null; }
        if (rotatingTask != null) { rotatingTask.cancel(); rotatingTask = null; }
        if (hologram != null) { hologram.remove(); hologram = null; }
        com.l299l.newbedwars.arena.generators.leveling.GeneratorSettings current = settings();
        boolean showTime = current == null || current.isShowNextSpawnTime();
        hologram = new Hologram(GeneratorHologramText.of(
                NewBedwars.plugin.getMessages().getMsgToConsole("EmeraldGenerator"), showTime), location.clone(),
                new HashMap<>(Map.of("/tier/", String.valueOf(level), "/delay/", String.valueOf(delayInSeconds()))));
        ArmorStand armorStand = hologram.getArmorStand();
        armorStand.setHelmet(new ItemStack(Material.EMERALD_BLOCK));
        final int[] remaining = {delayInSeconds()};

        task = new BukkitRunnable() {
            @Override
            public void run() {
                remaining[0]--;
                if (remaining[0] <= 0) {
                    remaining[0] = delayInSeconds();
                    location.getWorld().dropItem(location.clone().add(0, 0.5, 0), new ItemStack(Material.EMERALD, Math.max(1, emeraldSpawnAmount)));
                }
                hologram.reload(new HashMap<>(Map.of("/tier/", String.valueOf(level), "/delay/", String.valueOf(remaining[0]))));
            }
        }.runTaskTimer(NewBedwars.plugin, 20, 20);
        rotatingTask = new BukkitRunnable() {
            @Override
            public void run() {
                armorStand.setHeadPose(armorStand.getHeadPose().add(0, 0.1, 0));
            }
        }.runTaskTimer(NewBedwars.plugin, 0, 1);
    }

    private int delayInSeconds() {
        return Math.max(1, (int) Math.ceil(spawnDelay));
    }

    @Override
    public boolean upgrade() {
        com.l299l.newbedwars.arena.generators.leveling.GeneratorSettings settings = settings();
        if (settings == null || level >= settings.getMaxLevel()) {
            return false;
        }
        level++;
        emeraldSpawnRate = GeneratorLists.doubleAt(settings.getEmeraldSpawnRate(), level - 1, 0);
        emeraldSpawnAmount = GeneratorLists.intAt(settings.getEmeraldSpawnAmount(), level - 1, 1);
        spawnDelay = GeneratorLists.doubleAt(settings.getSpawnDelay(), level - 1, 30);
        return true;
    }

    @Override
    public boolean upgrade(Integer level) {
        if (level > getMaxLevel() || this.level >= level) {
            return false;
        }
        for (int i = this.level; i < level; i++) {
            upgrade();
        }
        return true;
    }

    @Override
    public void stop() {
        if (task != null) { task.cancel(); task = null; }
        if (rotatingTask != null) { rotatingTask.cancel(); rotatingTask = null; }
        if (hologram != null) { hologram.remove(); hologram = null; }
        level = 0;
        upgrade();
    }

    @Override
    public GeneratorType getType() {
        return type;
    }

    @Override
    public Location getLocation() {
        return location;
    }
}
