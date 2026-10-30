package com.l299l.newbedwars.npc;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.extensions.PluginDetector;
import com.l299l.newbedwars.npc.fallback.FallbackNpc;
import com.l299l.newbedwars.npc.fallback.FallbackNpcStore;
import com.l299l.newbedwars.utils.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Join NPCs: Citizens NPCs with the NewBedwars trait and fallback NPCs from {@code data/npcs.json}.
 * Keeps the live holograms above them, respawns fallback entities and handles clicks.
 */
public final class JoinNpcManager implements Listener {
    private static final String CITIZENS = "Citizens";
    /**
     * Where the hologram base goes relative to the NPC top. Holograms use marker armor stands (no hitbox, so
     * they never block clicks on the NPC); the lowest line then shows about 0.8 blocks above the NPC, over its name.
     */
    private static final double BASE_BELOW_TOP = 0.2;
    private static final long CLICK_COOLDOWN_MS = 500;

    private final List<FallbackNpc> fallbackNpcs = new ArrayList<>();
    private final Map<UUID, FallbackNpc> fallbackByEntity = new HashMap<>();
    private final Map<String, ShownHologram> holograms = new HashMap<>();
    private final Map<UUID, Long> lastClick = new HashMap<>();
    private CitizensBridge citizens;
    private BukkitTask task;
    private boolean started;

    private record ShownHologram(Hologram hologram, Location base, JoinNpcAction.Type type) {}

    /** Called once from {@code onEnable}: loads fallback NPCs, hooks Citizens if it is already enabled. */
    public void start() {
        started = true;
        try {
            fallbackNpcs.clear();
            fallbackNpcs.addAll(FallbackNpcStore.load(storeFile()));
        } catch (IOException | RuntimeException e) {
            NewBedwars.plugin.getLogger().warning("Could not read data/npcs.json: " + e.getMessage());
        }
        if (PluginDetector.isCitizensPresent()) hookCitizens();
        restartTask();
    }

    /** Called after {@code HandlerList.unregisterAll} on every (re)load. */
    public void registerListeners() {
        NewBedwars.plugin.getServer().getPluginManager().registerEvents(this, NewBedwars.plugin);
        if (citizens != null) citizens.registerListeners();
        if (started) restartTask();
    }

    public void shutdown() {
        if (task != null) task.cancel();
        task = null;
        for (ShownHologram shown : holograms.values()) shown.hologram().remove();
        holograms.clear();
        for (FallbackNpc npc : fallbackNpcs) despawn(npc);
        if (citizens != null) citizens.shutdown();
        citizens = null;
        started = false;
    }

    private void restartTask() {
        if (task != null) task.cancel();
        long period = Math.max(1, Properties.JoinNpcUpdateSeconds) * 20L;
        task = Bukkit.getScheduler().runTaskTimer(NewBedwars.plugin, this::refresh, 1L, period);
    }

    /** Refreshes on the next tick (after an NPC was added, changed or removed). */
    public void refreshSoon() {
        Bukkit.getScheduler().runTask(NewBedwars.plugin, this::refresh);
    }

    public CitizensBridge getCitizens() {
        return citizens;
    }

    public List<FallbackNpc> getFallbackNpcs() {
        return Collections.unmodifiableList(fallbackNpcs);
    }

    /** Every attached NPC (Citizens first). */
    public List<JoinNpc> allNpcs() {
        List<JoinNpc> all = new ArrayList<>();
        if (citizens != null) {
            try {
                all.addAll(citizens.npcs());
            } catch (RuntimeException e) {
                NewBedwars.plugin.getLogger().warning("Could not read Citizens NPCs: " + e);
            }
        }
        for (FallbackNpc npc : fallbackNpcs) {
            JoinNpc view = view(npc);
            if (view != null) all.add(view);
        }
        return all;
    }

    // ---- Fallback NPCs ------------------------------------------------------------

    public FallbackNpc createFallback(Location loc, EntityType type, JoinNpcAction action) {
        int id = fallbackNpcs.stream().mapToInt(n -> n.id).max().orElse(0) + 1;
        FallbackNpc npc = new FallbackNpc(id, type.name(), loc, action);
        fallbackNpcs.add(npc);
        spawn(npc);
        save();
        refreshSoon();
        return npc;
    }

    public boolean removeFallback(int id) {
        FallbackNpc npc = fallback(id);
        if (npc == null) return false;
        despawn(npc);
        fallbackNpcs.remove(npc);
        save();
        refreshSoon();
        return true;
    }

    public FallbackNpc fallback(int id) {
        for (FallbackNpc npc : fallbackNpcs) {
            if (npc.id == id) return npc;
        }
        return null;
    }

    public static boolean isUsableEntityType(EntityType type) {
        if (type == null || !type.isAlive() || !type.isSpawnable()) return false;
        return switch (type.name()) {
            case "PLAYER", "ENDER_DRAGON", "WITHER" -> false;
            default -> true;
        };
    }

    private void save() {
        List<FallbackNpc> snapshot = new ArrayList<>(fallbackNpcs);
        Path file = storeFile();
        IOExecutor.submit(() -> {
            try {
                FallbackNpcStore.save(file, snapshot);
            } catch (IOException e) {
                NewBedwars.plugin.getLogger().warning("Could not save data/npcs.json: " + e.getMessage());
            }
        });
    }

    private Path storeFile() {
        return NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("npcs.json");
    }

    private Entity entity(FallbackNpc npc) {
        if (npc.entity == null) return null;
        Entity e = Bukkit.getEntity(npc.entity);
        return e != null && e.isValid() ? e : null;
    }

    private void spawn(FallbackNpc npc) {
        if (entity(npc) != null) return;
        Location loc = npc.location();
        if (loc == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;
        EntityType type;
        try {
            type = EntityType.valueOf(npc.entityType);
        } catch (IllegalArgumentException e) {
            return;
        }
        if (!isUsableEntityType(type)) return;
        Entity e = loc.getWorld().spawnEntity(loc, type);
        e.setPersistent(false);
        e.setInvulnerable(true);
        e.setSilent(true);
        e.setGravity(false);
        if (e instanceof LivingEntity le) {
            le.setAI(false);
            le.setCollidable(false);
            le.setRemoveWhenFarAway(false);
            le.setCanPickupItems(false);
        }
        if (npc.entity != null) fallbackByEntity.remove(npc.entity);
        npc.entity = e.getUniqueId();
        fallbackByEntity.put(npc.entity, npc);
    }

    private void despawn(FallbackNpc npc) {
        Entity e = entity(npc);
        if (e != null) e.remove();
        if (npc.entity != null) fallbackByEntity.remove(npc.entity);
        npc.entity = null;
    }

    private JoinNpc view(FallbackNpc npc) {
        JoinNpcAction action = npc.joinAction();
        if (action == null) return null;
        return new JoinNpc() {
            @Override
            public String key() {
                return "fallback:" + npc.id;
            }

            @Override
            public String displayName() {
                return "#" + npc.id + " " + npc.entityType;
            }

            @Override
            public JoinNpcAction action() {
                return action;
            }

            @Override
            public Location location() {
                Entity e = entity(npc);
                return e == null ? null : e.getLocation();
            }

            @Override
            public double height() {
                Entity e = entity(npc);
                return e == null ? 1.8 : e.getHeight();
            }
        };
    }

    // ---- Holograms ----------------------------------------------------------------

    private void refresh() {
        for (FallbackNpc npc : fallbackNpcs) spawn(npc);
        Set<String> seen = new HashSet<>();
        for (JoinNpc npc : allNpcs()) {
            Location loc = npc.location();
            String template = hologramTemplate(npc.action().getType());
            if (loc == null || loc.getWorld() == null || template == null) continue;
            seen.add(npc.key());
            Location base = loc.clone().add(0, npc.height() - BASE_BELOW_TOP + Properties.JoinNpcHologramOffset, 0);
            HashMap<String, String> replacements = NpcHologramText.replacements(npc.action(), ArenaRegistry.all());
            ShownHologram shown = holograms.get(npc.key());
            if (shown != null && shown.type() == npc.action().getType() && sameSpot(shown.base(), base) && isAlive(shown.hologram())) {
                shown.hologram().reload(replacements);
                continue;
            }
            if (shown != null) shown.hologram().remove();
            Hologram hologram = new Hologram(template, base.clone(), replacements);
            for (ArmorStand stand : hologram.getArmorStands()) {
                stand.setMarker(true);
                stand.setPersistent(false);
            }
            holograms.put(npc.key(), new ShownHologram(hologram, base, npc.action().getType()));
        }
        holograms.entrySet().removeIf(entry -> {
            if (seen.contains(entry.getKey())) return false;
            entry.getValue().hologram().remove();
            return true;
        });
    }

    private static String hologramTemplate(JoinNpcAction.Type type) {
        List<String> lines = Properties.JoinNpcHologram.get(type.name());
        return lines == null || lines.isEmpty() ? null : String.join("\n", lines);
    }

    private static boolean sameSpot(Location a, Location b) {
        return a.getWorld() == b.getWorld() && a.distanceSquared(b) < 0.01;
    }

    private static boolean isAlive(Hologram hologram) {
        for (ArmorStand stand : hologram.getArmorStands()) {
            if (!stand.isValid()) return false;
        }
        return true;
    }

    // ---- Clicks -------------------------------------------------------------------

    /** Runs the NPC's action, ignoring repeated clicks within a short time. */
    public void click(Player p, JoinNpc npc) {
        long now = System.currentTimeMillis();
        Long last = lastClick.get(p.getUniqueId());
        if (last != null && now - last < CLICK_COOLDOWN_MS) return;
        lastClick.put(p.getUniqueId(), now);
        JoinNpcExecutor.perform(p, npc.action());
    }

    private void clickFallback(Player p, Entity entity) {
        FallbackNpc npc = fallbackByEntity.get(entity.getUniqueId());
        JoinNpc view = npc == null ? null : view(npc);
        if (view != null) click(p, view);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!fallbackByEntity.containsKey(e.getRightClicked().getUniqueId())) return;
        e.setCancelled(true);
        if (e.getHand() == EquipmentSlot.HAND) clickFallback(e.getPlayer(), e.getRightClicked());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractAt(PlayerInteractAtEntityEvent e) {
        if (fallbackByEntity.containsKey(e.getRightClicked().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onArmorStand(PlayerArmorStandManipulateEvent e) {
        if (fallbackByEntity.containsKey(e.getRightClicked().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent e) {
        if (!fallbackByEntity.containsKey(e.getEntity().getUniqueId())) return;
        e.setCancelled(true);
        if (Properties.JoinNpcLeftClick && e instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player p) {
            clickFallback(p, e.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCombust(EntityCombustEvent e) {
        if (fallbackByEntity.containsKey(e.getEntity().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        lastClick.remove(e.getPlayer().getUniqueId());
    }

    // ---- Citizens -----------------------------------------------------------------

    @EventHandler
    public void onPluginEnable(PluginEnableEvent e) {
        if (CITIZENS.equals(e.getPlugin().getName()) && citizens == null) hookCitizens();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent e) {
        if (!CITIZENS.equals(e.getPlugin().getName()) || citizens == null) return;
        citizens.shutdown();
        citizens = null;
        refreshSoon();
    }

    private void hookCitizens() {
        try {
            citizens = new com.l299l.newbedwars.npc.citizens.CitizensHook(this);
            citizens.registerListeners();
            NewBedwars.plugin.getLogger().info("Citizens found: join NPCs can be attached with /bw npc set.");
        } catch (Throwable t) {
            citizens = null;
            NewBedwars.plugin.getLogger().warning("Could not hook into Citizens: " + t);
        }
    }
}
