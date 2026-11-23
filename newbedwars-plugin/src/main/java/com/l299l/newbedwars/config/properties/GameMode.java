package com.l299l.newbedwars.config.properties;

public record GameMode(String id, String generators, String gamePhases, String shopGui, String upgradeGui,
                       int maxInTeam, int teams) {
}
