package com.l299l.newbedwars.config.data.mysql.repos;

import  com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.data.mysql.MySQLManager;
import com.l299l.newbedwars.config.data.mysql.models.PlayerModel;
import jakarta.persistence.EntityManager;

import java.util.List;

public class PlayersRepo {
    private final EntityManager em;

    public PlayersRepo(MySQLManager mySQLManager) {
        this.em = mySQLManager.getSessionFactory().createEntityManager();
    }

    public void updatePlayerLanguage(String name, Language lang) {
        em.getTransaction().begin();
        em.createNativeQuery("INSERT INTO Players (name, lang) VALUES ('" + name + "', '" + lang.toString() + "') ON DUPLICATE KEY UPDATE lang='" + lang + "';").executeUpdate();
        em.getTransaction().commit();
    }

    public void updatePlayerShopGui(String name, String shopGui) {
        em.getTransaction().begin();
        em.createNativeQuery("INSERT INTO Players (name, shopGui) VALUES ('" + name + "', '" + shopGui + "') ON DUPLICATE KEY UPDATE shopGui='" + shopGui + "';").executeUpdate();
        em.getTransaction().commit();
    }

    public void updatePlayerUpgrades(String name, String upgradeGui) {
        em.getTransaction().begin();
        em.createNativeQuery("INSERT INTO Players (name, upgradeGui) VALUES ('" + name + "', '" + upgradeGui + "') ON DUPLICATE KEY UPDATE upgradeGui='" + upgradeGui + "';").executeUpdate();
        em.getTransaction().commit();
    }

    public List<PlayerModel> findAll() {
        em.getTransaction().begin();
        List<PlayerModel> players = em.createQuery("SELECT p FROM PlayerModel p", PlayerModel.class).getResultList();
        em.getTransaction().commit();
        return players;
    }

    public void close() {
        em.close();
    }
}
