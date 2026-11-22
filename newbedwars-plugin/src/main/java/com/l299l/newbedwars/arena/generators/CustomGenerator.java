package com.l299l.newbedwars.arena.generators;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.leveling.CustomGeneratorSettings;
import com.l299l.newbedwars.utils.holograms.Hologram;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class CustomGenerator implements Generator {
    private final Location location;
    private final String id;
    private BukkitTask task;
    private BukkitTask rotatingTask;
    private Hologram hologram;

    public CustomGenerator(Location location, String id) {
        this.location = location;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    private CustomGeneratorSettings settings() {
        IArena arena = ArenaRegistry.byWorld(location.getWorld());
        if (arena == null || arena.getGeneratorsLeveling() == null) return null;
        return arena.getGeneratorsLeveling().getCustomGenerator(id);
    }

    @Override
    public void start() {
        stopTasks();
        CustomGeneratorSettings settings = settings();
        if (settings == null) {
            NewBedwars.plugin.getLogger().warning("Custom generator " + id + " at " + location.getBlockX() + " "
                    + location.getBlockY() + " " + location.getBlockZ() + " is not defined in the generators file of its arena");
            return;
        }
        String title = GeneratorHologramText.of(NewBedwars.plugin.getMessages().getMsgToConsole("CustomGenerator"),
                settings.showNextSpawnTime());
        Map<String, String> values = new HashMap<>(Map.of("/name/", ChatColor.translateAlternateColorCodes('&', settings.name()),
                "/delay/", String.valueOf(settings.spawnDelay())));
        hologram = new Hologram(title, location.clone(), new HashMap<>(values));
        ArmorStand armorStand = hologram.getArmorStand();
        armorStand.setHelmet(new ItemStack(settings.display()));
        final int[] remaining = {settings.spawnDelay()};
        task = new BukkitRunnable() {
            @Override
            public void run() {
                remaining[0]--;
                if (remaining[0] <= 0) {
                    remaining[0] = settings.spawnDelay();
                    location.getWorld().dropItem(location.clone().add(0, 0.5, 0), new ItemStack(settings.item(), settings.amount()));
                }
                values.put("/delay/", String.valueOf(remaining[0]));
                hologram.reload(new HashMap<>(values));
            }
        }.runTaskTimer(NewBedwars.plugin, 20, 20);
        rotatingTask = new BukkitRunnable() {
            @Override
            public void run() {
                armorStand.setHeadPose(armorStand.getHeadPose().add(0, 0.1, 0));
            }
        }.runTaskTimer(NewBedwars.plugin, 0, 1);
    }

    private void stopTasks() {
        if (task != null) { task.cancel(); task = null; }
        if (rotatingTask != null) { rotatingTask.cancel(); rotatingTask = null; }
        if (hologram != null) { hologram.remove(); hologram = null; }
    }

    @Override
    public void stop() {
        stopTasks();
    }

    @Override
    public GeneratorType getType() {
        return GeneratorType.OTHER;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public boolean upgrade() {
        return false;
    }

    @Override
    public boolean upgrade(Integer level) {
        return false;
    }
}
