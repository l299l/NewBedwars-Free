package com.l299l.newbedwars.bossbar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LobbyBossBarTest {

    @Test
    void lobbyPlaceholdersUseAllArenas() {
        assertEquals("Players: 7 of 32 []", BossBarManager.lobbyInfo("Players: /players/ of /max_players/ [/arena_name//phase/]", 7, 32));
    }
}
