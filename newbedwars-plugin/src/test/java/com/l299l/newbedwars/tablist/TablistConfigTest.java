package com.l299l.newbedwars.tablist;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TablistConfigTest {
    private static TablistConfig parse(String yaml) {
        return TablistConfig.parse(YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml)), Logger.getAnonymousLogger());
    }

    @Test
    void bundledFileParsesWithDefaultLayoutAndLobbyOff() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("tablists.yml"), StandardCharsets.UTF_8));
        TablistConfig config = TablistConfig.parse(yaml, Logger.getAnonymousLogger());
        assertFalse(config.isLobbyEnabled());
        TablistLayout layout = config.find("Default");
        assertNotNull(layout);
        assertEquals("{teamColor}{player}", layout.waiting().playerFormat());
        assertEquals("{teamColor}[{teamShort}] {player}", layout.playing().playerFormat());
        assertEquals("&7{player}", layout.spectator().playerFormat());
    }

    @Test
    void spectatorMissingKeysFallBackToPlaying() {
        TablistConfig config = parse("""
                Layouts:
                  L:
                    Playing:
                      Header: [a, b]
                      Footer: [f]
                      PlayerFormat: "{player}!"
                    Spectator:
                      PlayerFormat: "&7{player}"
                """);
        TablistSection spec = config.find("L").resolve(TablistLayout.State.SPECTATOR);
        assertEquals(List.of("a", "b"), spec.header());
        assertEquals(List.of("f"), spec.footer());
        assertEquals("&7{player}", spec.playerFormat());
    }

    @Test
    void waitingFallsBackToPlayingAndPlayingToWaiting() {
        TablistConfig config = parse("""
                Layouts:
                  OnlyPlaying:
                    Playing:
                      PlayerFormat: p
                  OnlyWaiting:
                    Waiting:
                      PlayerFormat: w
                """);
        assertEquals("p", config.find("OnlyPlaying").resolve(TablistLayout.State.WAITING).playerFormat());
        assertEquals("w", config.find("OnlyWaiting").resolve(TablistLayout.State.PLAYING).playerFormat());
        assertNull(config.find("OnlyWaiting").resolve(TablistLayout.State.WAITING).header());
    }

    @Test
    void lobbyEnabledAndLayoutIdsAreCaseInsensitive() {
        TablistConfig config = parse("""
                Lobby:
                  Enabled: true
                  PlayerFormat: "&7{player}"
                Layouts:
                  Fancy:
                    Playing:
                      Header: single line
                """);
        assertTrue(config.isLobbyEnabled());
        assertEquals("&7{player}", config.lobby().playerFormat());
        assertNotNull(config.find("fancy"));
        assertEquals(List.of("single line"), config.find("FANCY").playing().header());
        assertNull(config.find("missing"));
    }

    @Test
    void emptyConfigHasNothing() {
        TablistConfig config = TablistConfig.parse(null, null);
        assertFalse(config.isLobbyEnabled());
        assertTrue(config.layouts().isEmpty());
    }
}
