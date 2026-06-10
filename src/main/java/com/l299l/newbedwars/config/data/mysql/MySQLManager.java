package com.l299l.newbedwars.config.data.mysql;

import com.l299l.newbedwars.config.properties.Properties;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class MySQLManager {
    private SessionFactory sessionFactory;

    public MySQLManager() {
        setup();
    }

    private void setup() {
        java.util.Properties prop = new java.util.Properties();

        prop.setProperty("hibernate.connection.url", "jdbc:mysql://" + Properties.MySQLHost + ":" + Properties.MySQLPort + "/" + Properties.MySQLDatabase);

        prop.setProperty("dialect", "org.hibernate.dialect.MySQL8Dialect");

        prop.setProperty("hibernate.connection.username", Properties.MySQLUser);
        prop.setProperty("hibernate.connection.password", Properties.MySQLPassword);
        prop.setProperty("connection.driver_class", "com.mysql.jdbc.Driver");
        prop.setProperty("show_sql", "true");
        prop.setProperty("hibernate.format_sql", "true");
        prop.setProperty("hibernate.hbm2ddl.auto", "create");

        sessionFactory = new Configuration().addProperties(prop).addAnnotatedClass(com.l299l.newbedwars.config.data.mysql.models.PlayerModel.class).buildSessionFactory();
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
