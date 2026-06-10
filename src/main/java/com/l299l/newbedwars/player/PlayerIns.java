package com.l299l.newbedwars.player;

import com.l299l.newbedwars.config.Language;

import java.util.UUID;

public record PlayerIns(UUID id, String name, Language language, String shopGui, String upgradeGui) {
}
