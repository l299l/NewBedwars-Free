package com.l299l.newbedwars.arena.generators;

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

public class DiamondGenerator implements Generator {

    private final Location location;
    private final GeneratorType type = GeneratorType.DIAMOND;
    private final int maxLevel;
    private int level;
    private double diamondSpawnRate;
    private int diamondSpawnAmount;
    private double spawnDelay;
    private BukkitTask task;
    private BukkitTask rotatingTask;
    private Hologram hologram;

    public DiamondGenerator(Location location) {
        level = 0;
        this.location = location;
        IArena arena = Arena.arenaByWorld.get(location.getWorld());
        if (arena == null) {
            throw new RuntimeException("Arena not found");
        }
        GeneratorLeveling generatorLeveling = arena.getGeneratorsLeveling();
        maxLevel = generatorLeveling.getDiamondGeneratorSettings().getMaxLevel();
        upgrade();
    }

    @Override
    public void start() {
        hologram = new Hologram(NewBedwars.plugin.getMessages().getMsgToConsole("DiamondGenerator"), location.clone(),
                new HashMap<>(Map.of("/tier/", String.valueOf(level), "/delay/", String.valueOf(spawnDelay))));
        ArmorStand armorStand = hologram.getArmorStand();
        armorStand.setHelmet(new ItemStack(Material.DIAMOND_BLOCK));
        final int[] delay = {(int) spawnDelay};
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (delay[0] == 0) {
                    delay[0] = (int) spawnDelay;
                    location.getWorld().dropItem(location.clone().add(0, 0.5, 0), new ItemStack(Material.DIAMOND, Math.max(1, diamondSpawnAmount)));
                } else {
                    delay[0]--;
                }
                hologram.reload(new HashMap<>(Map.of("/tier/", String.valueOf(level), "/delay/", String.valueOf(delay[0]))));
            }
        }.runTaskTimer(NewBedwars.plugin, 0, 20);
        rotatingTask = new BukkitRunnable() {
            @Override
            public void run() {
                armorStand.setHeadPose(armorStand.getHeadPose().add(0, 0.1, 0));
            }
        }.runTaskTimer(NewBedwars.plugin, 0, 1);
    }

    @Override
    public boolean upgrade() {
        if (level >= maxLevel) {
            return false;
        }
        level++;
        IArena arena = Arena.arenaByWorld.get(location.getWorld());
        if (arena == null) {
            throw new RuntimeException("Arena not found");
        }
        GeneratorLeveling generatorLeveling = arena.getGeneratorsLeveling();
        diamondSpawnRate = generatorLeveling.getDiamondGeneratorSettings().getDiamondSpawnRate().get(level - 1);
        diamondSpawnAmount = generatorLeveling.getDiamondGeneratorSettings().getDiamondSpawnAmount().get(level - 1);
        spawnDelay = generatorLeveling.getDiamondGeneratorSettings().getSpawnDelay().get(level - 1);
        return true;
    }

    @Override
    public boolean upgrade(Integer level) {
        if (level > maxLevel || this.level >= level) {
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
