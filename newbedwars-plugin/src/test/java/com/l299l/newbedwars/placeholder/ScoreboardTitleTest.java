package com.l299l.newbedwars.placeholder;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.scoreboard.ScoreboardManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScoreboardTitleTest {
    private ScoreboardManager manager;

    @BeforeEach
    void setUp() {
        NewBedwars plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        String yaml = """
                Scoreboards:
                  Hex:
                    Enabled: true
                    Title: "&#0ED6AAHello %player_name% &7!"
                    Lines: []
                """;
        manager = new ScoreboardManager(YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml)));
        manager.loadScoreboards();
    }

    @AfterEach
    void tearDown() {
        PapiText.disable();
        NewBedwars.plugin = null;
    }

    @Test
    void titleIsResolvedPerPlayerWithHexAndPlaceholders() {
        PapiText.use((p, text) -> text.replace("%player_name%", p.getName()));
        Player a = mock(Player.class);
        Player b = mock(Player.class);
        when(a.getName()).thenReturn("Alice");
        when(b.getName()).thenReturn("Bob");
        assertEquals("§x§0§E§D§6§A§AHello Alice §7!", manager.titleOf("Hex", a));
        assertEquals("§x§0§E§D§6§A§AHello Bob §7!", manager.titleOf("Hex", b));
    }

    @Test
    void titleWithoutPlaceholderApiStillTranslatesHex() {
        assertEquals("§x§0§E§D§6§A§AHello %player_name% §7!", manager.titleOf("Hex"));
    }
}
