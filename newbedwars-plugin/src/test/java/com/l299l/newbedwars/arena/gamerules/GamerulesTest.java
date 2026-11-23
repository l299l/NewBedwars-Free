package com.l299l.newbedwars.arena.gamerules;

import com.l299l.newbedwars.config.properties.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class GamerulesTest {

    private HashMap<String, Boolean> saved;

    @BeforeEach
    void setUp() {
        saved = new HashMap<>(Properties.DefaultGamerules);
        Properties.DefaultGamerules.clear();
    }

    @AfterEach
    void tearDown() {
        Properties.DefaultGamerules.clear();
        Properties.DefaultGamerules.putAll(saved);
    }

    @Test
    void teamSelectionDefaultsWhenConfigLacksThem() {
        Gamerules g = new Gamerules();
        assertFalse(g.TeamSelection);
        assertTrue(g.TeamSelectionBalanced);
        assertTrue(g.TeamSelectionPartyLeader);
    }

    @Test
    void teamSelectionReadsConfiguredDefaults() {
        Properties.DefaultGamerules.put("TeamSelection", true);
        Properties.DefaultGamerules.put("TeamSelectionBalanced", false);
        Properties.DefaultGamerules.put("TeamSelectionPartyLeader", false);
        Gamerules g = new Gamerules();
        assertTrue(g.TeamSelection);
        assertFalse(g.TeamSelectionBalanced);
        assertFalse(g.TeamSelectionPartyLeader);
    }

    @Test
    void setGameruleUpdatesTeamSelectionRules() {
        Gamerules g = new Gamerules();
        g.setGamerule("TeamSelection", true);
        g.setGamerule("TeamSelectionBalanced", false);
        g.setGamerule("TeamSelectionPartyLeader", false);
        assertTrue(g.TeamSelection);
        assertFalse(g.TeamSelectionBalanced);
        assertFalse(g.TeamSelectionPartyLeader);
    }

    @Test
    void unknownGameruleIsIgnored() {
        Gamerules g = new Gamerules();
        g.setGamerule("Nope", true);
        assertFalse(g.TeamSelection);
    }

    @Test
    void toJsonContainsTeamSelectionRules() {
        Gamerules g = new Gamerules();
        g.setGamerule("TeamSelection", true);
        String json = g.toJson();
        assertTrue(json.contains("\"TeamSelection\":true"));
        assertTrue(json.contains("\"TeamSelectionBalanced\":true"));
        assertTrue(json.contains("\"TeamSelectionPartyLeader\":true"));
    }

    @Test
    void toJsonReflectsDisabledRules() {
        Gamerules g = new Gamerules();
        g.setGamerule("TeamSelectionBalanced", false);
        String json = g.toJson();
        assertTrue(json.contains("\"TeamSelection\":false"));
        assertTrue(json.contains("\"TeamSelectionBalanced\":false"));
    }
}
