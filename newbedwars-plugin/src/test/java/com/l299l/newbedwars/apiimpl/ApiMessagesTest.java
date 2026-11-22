package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.api.view.StatType;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.player.PlayerStats;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ApiMessagesTest {
    private final ApiMessages messages = new ApiMessages();
    private final Plugin addon = mock(Plugin.class);

    @Test
    void theChosenLanguageWinsAndFallsBackToEnglish() {
        messages.register(addon, Language.English, Map.of("Hello", "&aHello", "Bye", "Bye"));
        messages.register(addon, Language.Polish, Map.of("Hello", "Cześć"));

        assertEquals("Cześć", messages.find(Language.Polish, "Hello"));
        assertEquals("Bye", messages.find(Language.Polish, "Bye"));
        assertEquals("§aHello", messages.find(Language.German, "Hello"));
        assertNull(messages.find(Language.English, "Unknown"));
    }

    @Test
    void registeringALanguageAgainReplacesIt() {
        messages.register(addon, Language.English, Map.of("A", "1"));
        messages.register(addon, Language.English, Map.of("B", "2"));
        assertNull(messages.find(Language.English, "A"));
        assertEquals("2", messages.find(Language.English, "B"));
    }

    @Test
    void releasedMessagesAreGone() {
        messages.register(addon, Language.English, Map.of("A", "1"));
        messages.release(addon);
        assertNull(messages.find(Language.English, "A"));
    }

    @Test
    void unknownLanguageIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> messages.register(addon, null, Map.of()));
    }

    @Test
    void statDeltasHitOneStat() {
        assertEquals(new PlayerStats(0, 0, 0, 0, 0, 3, 0), ApiStats.delta(StatType.BEDS_BROKEN, 3));
        assertEquals(new PlayerStats(1, 0, 0, 0, 0, 0, 0), ApiStats.delta(StatType.WINS, 1));
        assertThrows(IllegalArgumentException.class, () -> ApiStats.delta(StatType.WINS, 0));
        assertThrows(IllegalArgumentException.class, () -> ApiStats.delta(null, 1));
    }
}
