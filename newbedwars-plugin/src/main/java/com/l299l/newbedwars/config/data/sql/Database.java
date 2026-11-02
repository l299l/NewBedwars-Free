package com.l299l.newbedwars.config.data.sql;

import com.l299l.newbedwars.common.sql.JdbcConnector;
import com.l299l.newbedwars.config.properties.Properties;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * The one MySQL/MariaDB connection of the plugin (section {@code MySQL} in config.yml), used by
 * {@code Storage.Type: MYSQL} and the network status table. Every query runs on its single worker thread.
 */
public final class Database {
    /** Work done on the database thread. */
    public interface Task {
        void run(Connection c) throws SQLException;
    }

    private static Database instance;

    private final Logger log;
    private final String settings;
    private final JdbcConnector connector;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NewBedwars-Database");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, Task> latest = new HashMap<>();
    private long lastErrorLog;

    private Database(Logger log, String settings) {
        this.log = log;
        this.settings = settings;
        this.connector = new JdbcConnector(Properties.MySQLHost, Properties.MySQLPort, Properties.MySQLDatabase,
                Properties.MySQLUser, Properties.MySQLPassword, Properties.MySQLSslMode,
                Properties.MySQLAllowPublicKeyRetrieval, Database.class.getClassLoader());
    }

    private static String currentSettings() {
        return Properties.MySQLHost + ":" + Properties.MySQLPort + "/" + Properties.MySQLDatabase + "@" + Properties.MySQLUser
                + "#" + (Properties.MySQLPassword == null ? 0 : Properties.MySQLPassword.hashCode())
                + "|" + Properties.MySQLSslMode + "|" + Properties.MySQLAllowPublicKeyRetrieval;
    }

    /** The database for the current config (reopened when the connection settings changed). */
    public static synchronized Database get(Logger log) {
        String s = currentSettings();
        if (instance != null && !instance.settings.equals(s)) {
            instance.close();
            instance = null;
        }
        if (instance == null) instance = new Database(log, s);
        return instance;
    }

    /** Waits for queued work (at most 5 seconds) and closes the connection. */
    public static synchronized void shutdown() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    /** Runs {@code task} on the database thread; errors are logged (at most once a minute). */
    public void submit(String what, Task task) {
        try {
            executor.execute(() -> run(what, task));
        } catch (RejectedExecutionException ignored) {
            // closing
        }
    }

    /**
     * Like {@link #submit}, but a task of the same {@code key} that did not start yet is replaced by this one,
     * so a slow database never collects a backlog of outdated status writes.
     */
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

    private void run(String what, Task task) {
        try {
            task.run(connector.get());
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            if (now - lastErrorLog > 60_000L) {
                lastErrorLog = now;
                log.warning("[MySQL] " + what + " failed: " + e.getMessage());
            }
        }
    }

    /** Runs {@code task} now on the calling thread (start-up only); throws on failure. */
    public synchronized void runNow(Task task) throws SQLException {
        task.run(connector.get());
    }

    private void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                executor.awaitTermination(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        connector.close();
    }
}
