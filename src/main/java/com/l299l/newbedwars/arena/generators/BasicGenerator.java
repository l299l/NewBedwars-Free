package com.l299l.newbedwars.arena.generators;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
        schedule(0);
    }

    /** (Re)starts the drop timer with the current level's {@code spawnDelay}. */
    private void schedule(long firstDelayTicks) {
        if (task != null) task.cancel();
        long period = Math.max(1L, Math.round(spawnDelay * 20));
        task = new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack item = getGeneratorItem();
                if (item != null) {
                    location.getWorld().dropItem(location.clone().add(0, 1.0, 0), item);
                }
            }
        }.runTaskTimer(NewBedwars.plugin, firstDelayTicks, period);
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
        GeneratorSettings settings = generatorLeveling.getDefaultGeneratorSettings();
        int index = level - 1;
        ironSpawnRate = rate(settings.getIronSpawnRate(), index);
        goldSpawnRate = rate(settings.getGoldSpawnRate(), index);
        emeraldSpawnRate = rate(settings.getEmeraldSpawnRate(), index);
        diamondSpawnRate = rate(settings.getDiamondSpawnRate(), index);
        ironSpawnAmount = amount(settings.getIronSpawnAmount(), index);
        goldSpawnAmount = amount(settings.getGoldSpawnAmount(), index);
        emeraldSpawnAmount = amount(settings.getEmeraldSpawnAmount(), index);
        diamondSpawnAmount = amount(settings.getDiamondSpawnAmount(), index);
        spawnDelay = settings.getSpawnDelay().get(index);
        // a running generator picks up the new speed at once; the next drop is one full delay away
        if (task != null) schedule(Math.max(1L, Math.round(spawnDelay * 20)));
        return true;
    }

    /** A resource the config has no section for never spawns. */
    private static double rate(List<Double> values, int index) {
        return values == null || index >= values.size() ? 0 : values.get(index);
    }

    private static int amount(List<Integer> values, int index) {
        return values == null || index >= values.size() ? 0 : values.get(index);
    }

    @Override
    public boolean upgrade(Integer level) {
        level = Math.min(level, maxLevel);
        if (this.level >= level) {
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

    /** The next drop, or {@code null} when the picked resource has a MaxAmount of 0 at this level. */
    private ItemStack getGeneratorItem() {
        double total = ironSpawnRate + goldSpawnRate + emeraldSpawnRate + diamondSpawnRate;
        double random = Math.random() * total;
        if (random < ironSpawnRate) {
            return stack(Material.IRON_INGOT, ironSpawnAmount);
        } else if (random < ironSpawnRate + goldSpawnRate) {
            return stack(Material.GOLD_INGOT, goldSpawnAmount);
        } else if (random < ironSpawnRate + goldSpawnRate + emeraldSpawnRate) {
            return stack(Material.EMERALD, emeraldSpawnAmount);
        } else {
            return stack(Material.DIAMOND, diamondSpawnAmount);
        }
    }

    /** A random amount from 1 to {@code maxAmount}, both included. */
    private static ItemStack stack(Material material, int maxAmount) {
        if (maxAmount <= 0) return null;
        return new ItemStack(material, 1 + ThreadLocalRandom.current().nextInt(maxAmount));
    }
}
