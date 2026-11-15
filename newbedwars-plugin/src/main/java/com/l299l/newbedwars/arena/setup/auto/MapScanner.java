package com.l299l.newbedwars.arena.setup.auto;

import com.l299l.newbedwars.NewBedwars;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.logging.Level;

public final class MapScanner {
    private static final int CHUNKS_PER_TICK = 3;
    private static final int PROGRESS_TICKS = 40;

    private final World world;
    private final List<long[]> queue = new ArrayList<>();
    private final ScanData data = new ScanData();
    private final AtomicInteger pending = new AtomicInteger();
    private final AtomicBoolean failed = new AtomicBoolean();
    private final int total;
    private final Consumer<ScanData> onDone;
    private final IntConsumer onProgress;
    private BukkitTask task;
    private int cursor;
    private int ticks;
    private boolean cancelled;

    public MapScanner(World world, int centerX, int centerZ, int radius, IntConsumer onProgress, Consumer<ScanData> onDone) {
        this.world = world;
        this.onProgress = onProgress;
        this.onDone = onDone;
        int chunkRadius = (radius >> 4) + 1;
        int cx = centerX >> 4;
        int cz = centerZ >> 4;
        for (int x = cx - chunkRadius; x <= cx + chunkRadius; x++) {
            for (int z = cz - chunkRadius; z <= cz + chunkRadius; z++) {
                long dx = (x * 16L + 8) - centerX;
                long dz = (z * 16L + 8) - centerZ;
                long limit = radius + 12L;
                if (dx * dx + dz * dz <= limit * limit) queue.add(new long[]{x, z, dx * dx + dz * dz});
            }
        }
        queue.sort(Comparator.comparingLong(c -> c[2]));
        total = queue.size();
    }

    public int chunks() {
        return total;
    }

    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(NewBedwars.plugin, 1L, 1L);
    }

    public void cancel() {
        cancelled = true;
        if (task != null) task.cancel();
    }

    private void tick() {
        if (cancelled) return;
        for (int i = 0; i < CHUNKS_PER_TICK && cursor < total; i++, cursor++) {
            long[] c = queue.get(cursor);
            scanChunk((int) c[0], (int) c[1]);
        }
        if (++ticks % PROGRESS_TICKS == 0 && cursor < total) onProgress.accept(cursor * 100 / total);
        if (cursor >= total && pending.get() == 0) {
            task.cancel();
            synchronized (data) {
                onDone.accept(data);
            }
        }
    }

    private void scanChunk(int x, int z) {
        if (!world.isChunkGenerated(x, z)) return;
        Chunk chunk = world.getChunkAt(x, z);
        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof Sign sign)) continue;
            SignMark mark = SignMark.parse(new Pos(sign.getX(), sign.getY(), sign.getZ()), sign.getLines());
            if (mark != null) {
                synchronized (data) {
                    data.signs().add(mark);
                }
            }
        }
        ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);
        int minY = world.getMinHeight();
        pending.incrementAndGet();
        NewBedwars.plugin.getServer().getScheduler().runTaskAsynchronously(NewBedwars.plugin, () -> {
            try {
                ScanData part = ChunkAnalyzer.analyze(new SnapshotSource(snapshot, minY), x, z);
                synchronized (data) {
                    data.merge(part);
                }
            } catch (RuntimeException e) {
                if (failed.compareAndSet(false, true)) {
                    NewBedwars.plugin.getLogger().log(Level.WARNING, "Automatic setup could not scan chunk " + x + " " + z, e);
                }
            } finally {
                pending.decrementAndGet();
            }
        });
    }

    private record SnapshotSource(ChunkSnapshot snapshot, int minY) implements BlockSource {
        @Override
        public int highestY(int x, int z) {
            return snapshot.getHighestBlockYAt(x, z);
        }

        @Override
        public Material type(int x, int y, int z) {
            return snapshot.getBlockType(x, y, z);
        }

        @Override
        public boolean isBedHead(int x, int y, int z) {
            BlockData blockData = snapshot.getBlockData(x, y, z);
            return blockData instanceof Bed bed && bed.getPart() == Bed.Part.HEAD;
        }
    }
}
