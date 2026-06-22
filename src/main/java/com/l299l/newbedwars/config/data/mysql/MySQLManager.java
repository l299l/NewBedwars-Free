package com.l299l.newbedwars.config.data.mysql;

import com.l299l.newbedwars.config.properties.Properties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLManager {
    private Connection connection;

    public MySQLManager() {
        setup();
    }

    private void setup() {
        String url = "jdbc:mysql://" + Properties.MySQLHost + ":" + Properties.MySQLPort
                + "/" + Properties.MySQLDatabase + "?useSSL=false&serverTimezone=UTC";
        try {
            connection = DriverManager.getConnection(url, Properties.MySQLUser, Properties.MySQLPassword);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to MySQL", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
        }
    }
}
