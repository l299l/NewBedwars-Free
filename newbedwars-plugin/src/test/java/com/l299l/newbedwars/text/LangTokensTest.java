package com.l299l.newbedwars.text;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.properties.LangMessages;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.placeholder.PapiText;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerProfile;
import com.l299l.newbedwars.scoreboard.ScoreboardManager;
import com.l299l.newbedwars.tablist.TablistRenderer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class LangTokensTest {
    private String savedDefault;
    private Player english;
    private Player polish;

    private Player player(String name, Language language) {
        Player p = mock(Player.class);
        when(p.getName()).thenReturn(name);
        PlayerProfile profile = mock(PlayerProfile.class);
        when(profile.language()).thenReturn(language);
        when(NewBedwars.plugin.getPlayerManager().getPlayer(name)).thenReturn(profile);
        return p;
    }

    @BeforeEach
    void setUp() {
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        PlayerManager players = mock(PlayerManager.class);
        when(plugin.getPlayerManager()).thenReturn(players);
        LangMessages lang = mock(LangMessages.class);
        when(lang.getMsg(any(Language.class), any(String.class))).thenAnswer(inv -> {
            Language l = inv.getArgument(0);
            String key = inv.getArgument(1);
            if (!key.equals("Greeting")) return "";
            return l == Language.Polish ? "&cCzesc /players/ {arena}" : "&aHello /players/ {arena}";
        });
        when(plugin.getLangMessages()).thenReturn(lang);
        Messages messages = new Messages(plugin, null);
        when(plugin.getMessages()).thenReturn(messages);
        NewBedwars.plugin = plugin;
        savedDefault = Properties.DefaultLanguage;
        Properties.DefaultLanguage = "pl";
        english = player("Eng", Language.English);
        polish = player("Pol", Language.Polish);
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
        Properties.DefaultLanguage = savedDefault;
    }

    @Test
    void replacesBothSyntaxesPerViewerLanguage() {
        assertEquals("[&aHello /players/ {arena}] [&aHello /players/ {arena}]",
                LangTokens.apply("[{lang:Greeting}] [/lang:Greeting/]", english));
        assertEquals("&cCzesc /players/ {arena}", LangTokens.apply("{lang:Greeting}", polish));
    }

    @Test
    void nullViewerUsesDefaultLanguage() {
        assertEquals("&cCzesc /players/ {arena}", LangTokens.apply("{lang:Greeting}", null));
        Properties.DefaultLanguage = "xx";
        assertEquals("&aHello /players/ {arena}", LangTokens.apply("{lang:Greeting}", null));
    }

    @Test
    void unknownKeyIsEmptyAndPlainTextIsUntouched() {
        assertEquals("a  b", LangTokens.apply("a {lang:Nope} b", english));
        assertEquals("/phase/ in {x} /lang:/", LangTokens.apply("/phase/ in {x} /lang:/", english));
        assertTrue(LangTokens.contains("x {lang:A}"));
        assertFalse(LangTokens.contains("x {lang }"));
    }

    @Test
    void scoreboardTitleUsesViewerLanguage() {
        YamlConfiguration conf = new YamlConfiguration();
        conf.set("Scoreboards.S.Enabled", true);
        conf.set("Scoreboards.S.Title", "{lang:Greeting}");
        conf.set("Scoreboards.S.Lines", java.util.List.of("{lang:Greeting}", "", "/server/"));
        ScoreboardManager manager = new ScoreboardManager(conf);
        manager.loadScoreboards();
        try (MockedStatic<PapiText> papi = mockStatic(PapiText.class)) {
            papi.when(() -> PapiText.apply(any(), any(String.class))).thenAnswer(inv -> inv.getArgument(1));
            assertEquals("§aHello /players/ {arena}", manager.titleOf("S", english));
            assertEquals("§cCzesc /players/ {arena}", manager.titleOf("S", polish));
            assertEquals("§cCzesc /players/ {arena}", manager.titleOf("S"));
        }
    }

    @Test
    void tablistResolvesTokenBeforePlaceholders() {
        try (MockedStatic<org.bukkit.Bukkit> bukkit = mockStatic(org.bukkit.Bukkit.class);
             MockedStatic<PapiText> papi = mockStatic(PapiText.class)) {
            papi.when(() -> PapiText.apply(any(), any(String.class))).thenAnswer(inv -> inv.getArgument(1));
            com.l299l.newbedwars.arena.IArena arena = mock(com.l299l.newbedwars.arena.IArena.class);
            when(arena.getArenaName()).thenReturn("castle");
            when(arena.getActivePlayerCount()).thenReturn(3);
            assertEquals("\u00a7cCzesc 3 castle", TablistRenderer.render("{lang:Greeting}", polish, arena, polish));
            assertEquals("\u00a7aHello 3 castle", TablistRenderer.render("{lang:Greeting}", english, arena, english));
        }
    }
}
