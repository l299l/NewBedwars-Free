package com.l299l.newbedwars.arena.generators;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class BasicGenerator implements Generator {

    private final Location location;
    private final GeneratorType type = GeneratorType.BASIC;
    private double ironSpawnRate;
    private double goldSpawnRate;
    private double emeraldSpawnRate;
    private double diamondSpawnRate;
    private int ironSpawnAmount;
    private int goldSpawnAmount;
    private int emeraldSpawnAmount;
    private int diamondSpawnAmount;
    private int level;
    private final int maxLevel;
    private double spawnDelay;
    private BukkitTask task;

    public BasicGenerator(Location location) {
        level = 0;
        this.location = location;
        IArena arena = Arena.arenaByWorld.get(location.getWorld());
        if (arena == null) {
            throw new RuntimeException("Arena not found");
        }
        GeneratorLeveling generatorLeveling = arena.getGeneratorsLeveling();
        maxLevel = generatorLeveling.getDefaultGeneratorSettings().getMaxLevel();
        upgrade();
    }

    @Override
    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                location.getWorld().dropItem(location.clone().add(0, 1.0, 0), getGeneratorItem());
            }
        }.runTaskTimer(NewBedwars.plugin, 0, (long) (spawnDelay * 20));
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
        ironSpawnRate = generatorLeveling.getDefaultGeneratorSettings().getIronSpawnRate().get(level - 1);
        goldSpawnRate = generatorLeveling.getDefaultGeneratorSettings().getGoldSpawnRate().get(level - 1);
        emeraldSpawnRate = generatorLeveling.getDefaultGeneratorSettings().getEmeraldSpawnRate().get(level - 1);
        diamondSpawnRate = generatorLeveling.getDefaultGeneratorSettings().getDiamondSpawnRate().get(level - 1);
        ironSpawnAmount = generatorLeveling.getDefaultGeneratorSettings().getIronSpawnAmount().get(level - 1);
        goldSpawnAmount = generatorLeveling.getDefaultGeneratorSettings().getGoldSpawnAmount().get(level - 1);
        emeraldSpawnAmount = generatorLeveling.getDefaultGeneratorSettings().getEmeraldSpawnAmount().get(level - 1);
        diamondSpawnAmount = generatorLeveling.getDefaultGeneratorSettings().getDiamondSpawnAmount().get(level - 1);
        spawnDelay = generatorLeveling.getDefaultGeneratorSettings().getSpawnDelay().get(level - 1);
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

    private ItemStack getGeneratorItem() {
        double total = ironSpawnRate + goldSpawnRate + emeraldSpawnRate + diamondSpawnRate;
        double random = Math.random() * total;
        if (random < ironSpawnRate) {
            return new ItemStack(org.bukkit.Material.IRON_INGOT, Math.max(1, (int) Math.floor(Math.random() * ironSpawnAmount)));
        } else if (random < ironSpawnRate + goldSpawnRate) {
            return new ItemStack(org.bukkit.Material.GOLD_INGOT, Math.max(1, (int) Math.floor(Math.random() * goldSpawnAmount)));
        } else if (random < ironSpawnRate + goldSpawnRate + emeraldSpawnRate) {
            return new ItemStack(org.bukkit.Material.EMERALD, Math.max(1, (int) Math.floor(Math.random() * emeraldSpawnAmount)));
        } else {
            return new ItemStack(org.bukkit.Material.DIAMOND, Math.max(1, (int) Math.floor(Math.random() * diamondSpawnAmount)));
        }
    }
}
