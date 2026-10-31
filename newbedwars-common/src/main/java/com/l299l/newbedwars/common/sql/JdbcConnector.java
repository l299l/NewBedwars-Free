package com.l299l.newbedwars.common.sql;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 * One lazily (re)opened MySQL/MariaDB connection. The driver class is loaded through the given class loader,
 * so it works with a server-provided driver (Paper) as well as a shaded one (proxy). Not thread-safe:
 * use it from one thread.
 */
public final class JdbcConnector implements AutoCloseable {
    private static final String[] DRIVERS = {"com.mysql.cj.jdbc.Driver", "com.mysql.jdbc.Driver", "org.mariadb.jdbc.Driver"};

    private final String url;
    private final Properties props = new Properties();
    private final ClassLoader loader;
    private Driver driver;
    private Connection connection;

    public JdbcConnector(String host, int port, String database, String user, String password, ClassLoader loader) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000&socketTimeout=10000";
        props.setProperty("user", user == null ? "" : user);
        props.setProperty("password", password == null ? "" : password);
        this.loader = loader;
    }

    /** The open connection, reconnecting when it was closed or broke. */
    public Connection get() throws SQLException {
        if (connection != null) {
            try {
                if (!connection.isClosed() && connection.isValid(2)) return connection;
            } catch (SQLException ignored) {
                // reconnect below
            }
            closeQuietly();
        }
        Connection c = driver().connect(url, props);
        if (c == null) throw new SQLException("The JDBC driver does not accept " + url);
        connection = c;
        return c;
    }

    private Driver driver() throws SQLException {
        if (driver != null) return driver;
        for (String name : DRIVERS) {
            try {
                Class<?> cls = Class.forName(name, true, loader);
                driver = (Driver) cls.getDeclaredConstructor().newInstance();
                return driver;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // try the next one
            }
        }
        throw new SQLException("No MySQL/MariaDB JDBC driver found");
    }

    private void closeQuietly() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ignored) {
            // already broken
        }
        connection = null;
    }

    @Override
    public void close() {
        closeQuietly();
    }
}
