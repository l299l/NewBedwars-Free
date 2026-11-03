package com.l299l.newbedwars.config.data;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
