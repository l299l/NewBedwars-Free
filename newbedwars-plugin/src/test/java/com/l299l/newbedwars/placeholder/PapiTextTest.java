package com.l299l.newbedwars.placeholder;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PapiTextTest {
    private final Player player = mock(Player.class);

    @AfterEach
    void tearDown() {
        PapiText.disable();
    }

    @Test
    void textIsUnchangedWithoutPlaceholderApi() {
        assertEquals("Wins: %newbedwars_stat_wins%", PapiText.apply(player, "Wins: %newbedwars_stat_wins%"));
    }

    @Test
    void placeholdersAreFilledInForTheViewer() {
        PapiText.use((p, text) -> p == player ? text.replace("%newbedwars_stat_wins%", "&a12") : text);
        assertEquals("Wins: §a12", PapiText.apply(player, "Wins: %newbedwars_stat_wins%"));
    }

    @Test
    void textWithoutPercentOrViewerIsNotSentToPlaceholderApi() {
        AtomicInteger calls = new AtomicInteger();
        PapiText.use((p, text) -> {
            calls.incrementAndGet();
            return "changed";
        });
        assertEquals("Wool", PapiText.apply(player, "Wool"));
        assertEquals("%player_name%", PapiText.apply(null, "%player_name%"));
        assertNull(PapiText.apply(player, null));
        assertEquals(0, calls.get());
    }

    @Test
    void aFailingPlaceholderKeepsTheText() {
        PapiText.use((p, text) -> {
            throw new IllegalStateException("broken expansion");
        });
        assertEquals("%broken%", PapiText.apply(player, "%broken%"));
        PapiText.use((p, text) -> {
            throw new NoClassDefFoundError("me/clip/placeholderapi/PlaceholderAPI");
        });
        assertEquals("%broken%", PapiText.apply(player, "%broken%"));
    }
}
