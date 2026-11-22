package com.l299l.newbedwars.config.data;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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
        executor.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException | Error e) {
                log().log(java.util.logging.Level.SEVERE, "A background file task failed", e);
            }
        });
    }

    public static synchronized void flush() {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                int dropped = executor.shutdownNow().size();
                log().warning("File saving took longer than 15 seconds; "
                        + dropped + " pending write(s) were cancelled");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        executor = null;
    }

    private static Logger log() {
        return com.l299l.newbedwars.NewBedwars.plugin != null
                ? com.l299l.newbedwars.NewBedwars.plugin.getLogger() : Logger.getLogger("NewBedwars");
    }
}
