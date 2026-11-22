package com.l299l.newbedwars.world.template;

public final class BackgroundTasks {
    private static final Object LOCK = new Object();
    private static int running;

    private BackgroundTasks() {}

    public static void started() {
        synchronized (LOCK) {
            running++;
        }
    }

    public static void finished() {
        synchronized (LOCK) {
            running = Math.max(0, running - 1);
            LOCK.notifyAll();
        }
    }

    public static int running() {
        synchronized (LOCK) {
            return running;
        }
    }

    public static boolean await(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        synchronized (LOCK) {
            while (running > 0) {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) return false;
                try {
                    LOCK.wait(left);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return running == 0;
                }
            }
            return true;
        }
    }
}
