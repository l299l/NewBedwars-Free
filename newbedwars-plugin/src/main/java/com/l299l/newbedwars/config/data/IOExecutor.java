package com.l299l.newbedwars.config.data;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Single background thread for file writes, so saves never block the server thread or overlap. */
public final class IOExecutor {
    private static ExecutorService executor;

    private IOExecutor() {}

    public static synchronized void submit(Runnable task) {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "NewBedwars-IO");
                t.setDaemon(true);
                return t;
            });
        }
        executor.execute(task);
    }

    /** Waits for queued writes to finish. Called when the plugin is disabled. */
    public static synchronized void flush() {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        executor = null;
    }
}
