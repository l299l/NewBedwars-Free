package com.l299l.newbedwars.config.data.sql;

import com.l299l.newbedwars.common.sql.JdbcConnector;
import com.l299l.newbedwars.config.properties.Properties;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public final class Database {

    public interface Task {
        void run(Connection c) throws SQLException;
    }

    private static Database instance;

    private final Logger log;
    private final String settings;
    private final JdbcConnector connector;
    private static final int RETRIES = 5;
    private static final long RETRY_MILLIS = 30_000L;
    private static final long DRAIN_MILLIS = 15_000L;

    private final long retryMillis;
    private final long drainMillis;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, r -> {
        Thread t = new Thread(r, "NewBedwars-Database");
        t.setDaemon(true);
        return t;
    });
    private final Set<Retry> pending = new LinkedHashSet<>();
    private boolean closing;
    private final Map<String, Task> latest = new HashMap<>();
    private long lastErrorLog;
    private int suppressed;

    private static final class Retry {
        final String what;
        final Task task;
        final int retriesLeft;
        ScheduledFuture<?> future;

        Retry(String what, Task task, int retriesLeft) {
            this.what = what;
            this.task = task;
            this.retriesLeft = retriesLeft;
        }
    }

    private Database(Logger log, String settings) {
        this(log, settings, new JdbcConnector(Properties.MySQLHost, Properties.MySQLPort, Properties.MySQLDatabase,
                Properties.MySQLUser, Properties.MySQLPassword, Properties.MySQLSslMode,
                Properties.MySQLAllowPublicKeyRetrieval, Database.class.getClassLoader()), RETRY_MILLIS);
    }

    Database(Logger log, String settings, JdbcConnector connector, long retryMillis) {
        this(log, settings, connector, retryMillis, DRAIN_MILLIS);
    }

    Database(Logger log, String settings, JdbcConnector connector, long retryMillis, long drainMillis) {
        this.drainMillis = drainMillis;
        this.log = log;
        this.settings = settings;
        this.connector = connector;
        this.retryMillis = retryMillis;
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        executor.setRemoveOnCancelPolicy(true);
    }

    private static String currentSettings() {
        return Properties.MySQLHost + ":" + Properties.MySQLPort + "/" + Properties.MySQLDatabase + "@" + Properties.MySQLUser
                + "#" + (Properties.MySQLPassword == null ? 0 : Properties.MySQLPassword.hashCode())
                + "|" + Properties.MySQLSslMode + "|" + Properties.MySQLAllowPublicKeyRetrieval;
    }

    public static synchronized Database get(Logger log) {
        String s = currentSettings();
        if (instance != null && !instance.settings.equals(s)) {
            instance.close();
            instance = null;
        }
        if (instance == null) instance = new Database(log, s);
        return instance;
    }

    public static synchronized void shutdown() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    public void submit(String what, Task task) {
        submit(what, task, 0);
    }

    public void submitRetrying(String what, Task task) {
        submit(what, task, RETRIES);
    }

    private void submit(String what, Task task, int retriesLeft) {
        try {
            executor.execute(() -> run(what, task, retriesLeft));
        } catch (RejectedExecutionException e) {
            log.warning("[MySQL] " + what + " was not saved: the database connection is already closed");
        }
    }

    public void submitLatest(String key, String what, Task task) {
        boolean queue;
        synchronized (latest) {
            queue = latest.put(key, task) == null;
        }
        if (!queue) return;
        submit(what, c -> {
            Task newest;
            synchronized (latest) {
                newest = latest.remove(key);
            }
            if (newest != null) newest.run(c);
        });
    }

    private boolean run(String what, Task task, int retriesLeft) {
        boolean connected = false;
        try {
            synchronized (this) {
                Connection c = connector.get();
                connected = true;
                task.run(c);
            }
            return true;
        } catch (Exception e) {
            boolean safeToRetry = !connected || rolledBack(e);
            boolean retry = retriesLeft > 0 && safeToRetry && scheduleRetry(what, task, retriesLeft - 1);
            if (retry) {
                report(what + " failed (tried again in " + retryMillis / 1000 + "s)", e, false);
            } else if (retriesLeft > 0 && !safeToRetry) {
                report(what + " failed and was dropped (it may have been saved already, so it is not tried again to avoid counting it twice)", e, true);
            } else {
                report(what + " failed and was dropped", e, true);
            }
            return false;
        }
    }

    private static boolean rolledBack(Throwable e) {
        for (int i = 0; e != null && i < 8; i++, e = e.getCause()) {
            if (e instanceof SQLTransactionRollbackException) return true;
            if (e instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("40")) return true;
        }
        return false;
    }

    private boolean scheduleRetry(String what, Task task, int retriesLeft) {
        synchronized (pending) {
            if (closing) return false;
            Retry retry = new Retry(what, task, retriesLeft);
            try {
                retry.future = executor.schedule(() -> {
                    synchronized (pending) {
                        if (!pending.remove(retry)) return;
                    }
                    run(retry.what, retry.task, retry.retriesLeft);
                }, retryMillis, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException e) {
                return false;
            }
            pending.add(retry);
            return true;
        }
    }

    private void report(String message, Exception e, boolean always) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLog > 60_000L) {
            lastErrorLog = now;
            String others = suppressed > 0 ? " (" + suppressed + " other database errors in the last minute were not logged)" : "";
            suppressed = 0;
            log.log(java.util.logging.Level.WARNING, "[MySQL] " + message + others, e);
        } else if (always) {
            log.warning("[MySQL] " + message + ": " + e);
        } else {
            suppressed++;
        }
    }

    public synchronized void runNow(Task task) throws SQLException {
        task.run(connector.get());
    }

    void close() {
        List<Retry> drained;
        synchronized (pending) {
            closing = true;
            drained = new ArrayList<>(pending);
            pending.clear();
            for (Retry r : drained) r.future.cancel(false);
        }
        AtomicInteger skipped = new AtomicInteger();
        AtomicBoolean failed = new AtomicBoolean();
        for (Retry r : drained) {
            try {
                executor.execute(() -> {
                    if (failed.get()) {
                        skipped.incrementAndGet();
                    } else if (!run(r.what, r.task, 0)) {
                        failed.set(true);
                    }
                });
            } catch (RejectedExecutionException e) {
                skipped.incrementAndGet();
            }
        }
        executor.shutdown();
        boolean stuck = false;
        try {
            if (!executor.awaitTermination(drainMillis, TimeUnit.MILLISECONDS)) {
                int dropped = executor.shutdownNow().size() + skipped.get();
                log.warning("[MySQL] The database did not answer in time; " + dropped + " pending save(s) were not written");
                stuck = !executor.awaitTermination(Math.min(2000L, drainMillis), TimeUnit.MILLISECONDS);
            } else if (skipped.get() > 0) {
                log.warning("[MySQL] The database did not answer; " + skipped.get() + " pending save(s) were not written");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            stuck = true;
            Thread.currentThread().interrupt();
        }
        if (stuck) {
            Thread closer = new Thread(connector::close, "NewBedwars-Database-Close");
            closer.setDaemon(true);
            closer.start();
        } else {
            connector.close();
        }
    }
}
