package com.l299l.newbedwars.panel.web;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

final class RequestGuard {
    static final long READ_MILLIS = 10_000;
    static final long IDLE_MILLIS = 30_000;

    enum State { READING, STREAMING, DONE, ABORTED }

    static final class Slot {
        private final Thread thread;
        private final LongSupplier clock;
        private final long readMillis;
        private State state = State.READING;
        private long deadline;
        private volatile long lastProgress;
        private volatile boolean eof;

        Slot(Thread thread, LongSupplier clock, long readMillis) {
            this.thread = thread;
            this.clock = clock;
            this.readMillis = readMillis;
            this.deadline = clock.getAsLong() + readMillis;
        }

        synchronized boolean done() {
            if (state == State.ABORTED) return false;
            state = State.DONE;
            return true;
        }

        synchronized void streaming() {
            if (state != State.READING) return;
            state = State.STREAMING;
            lastProgress = clock.getAsLong();
        }

        synchronized void finishing() {
            if (state != State.STREAMING) return;
            if (eof) {
                state = State.DONE;
                return;
            }
            state = State.READING;
            deadline = clock.getAsLong() + readMillis;
        }

        synchronized boolean aborted() {
            return state == State.ABORTED;
        }

        void progress() {
            lastProgress = clock.getAsLong();
        }

        void endOfBody() {
            eof = true;
            synchronized (this) {
                if (state == State.STREAMING) state = State.DONE;
            }
        }

        synchronized boolean check(long now, long idleMillis) {
            boolean late = state == State.READING && now >= deadline
                    || state == State.STREAMING && now - lastProgress >= idleMillis;
            if (!late) return false;
            state = State.ABORTED;
            thread.interrupt();
            return true;
        }

        synchronized void release() {
            if (state != State.ABORTED) state = State.DONE;
            Thread.interrupted();
        }
    }

    static Slot unguarded() {
        return new Slot(Thread.currentThread(), () -> 0L, Long.MAX_VALUE / 4);
    }

    private final Map<Thread, Slot> slots = new ConcurrentHashMap<>();
    private final long readMillis;
    private final long idleMillis;
    private final LongSupplier clock = System::currentTimeMillis;
    private ScheduledExecutorService timer;

    RequestGuard(long readMillis, long idleMillis) {
        this.readMillis = readMillis;
        this.idleMillis = idleMillis;
    }

    synchronized void start() {
        if (timer != null) return;
        timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NewBedwars-Panel-Guard");
            t.setDaemon(true);
            return t;
        });
        long tick = Math.max(20, Math.min(1000, readMillis / 4));
        timer.scheduleAtFixedRate(this::sweep, tick, tick, TimeUnit.MILLISECONDS);
    }

    synchronized void stop() {
        if (timer != null) timer.shutdownNow();
        timer = null;
        slots.clear();
    }

    Executor wrap(ExecutorService pool) {
        return task -> pool.execute(() -> run(task));
    }

    private void run(Runnable task) {
        Thread self = Thread.currentThread();
        Slot slot = new Slot(self, clock, readMillis);
        slots.put(self, slot);
        try {
            task.run();
        } finally {
            slots.remove(self, slot);
            slot.release();
        }
    }

    Slot current() {
        Slot slot = slots.get(Thread.currentThread());
        return slot == null ? unguarded() : slot;
    }

    private void sweep() {
        long now = clock.getAsLong();
        for (Slot slot : slots.values()) slot.check(now, idleMillis);
    }

    static InputStream track(InputStream in, Slot slot) {
        return new FilterInputStream(in) {
            @Override
            public int read() throws IOException {
                int b = super.read();
                if (b < 0) slot.endOfBody();
                else slot.progress();
                return b;
            }

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                int n = super.read(buffer, offset, length);
                if (n < 0) slot.endOfBody();
                else slot.progress();
                return n;
            }
        };
    }
}
