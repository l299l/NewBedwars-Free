package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerProfile;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LangCommandTest {
    private Messages messages;
    private PlayerManager players;
    private HashMap<String, PlayerProfile> profiles;
    private Player player;
    private LangCommand cmd;

    @BeforeEach
    void setUp() {
        messages = mock(Messages.class);
        players = mock(PlayerManager.class);
        profiles = new HashMap<>();
        when(players.getPlayers()).thenReturn(profiles);
        NewBedwars plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        when(plugin.getMessages()).thenReturn(messages);
        when(plugin.getPlayerManager()).thenReturn(players);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Leo");
        cmd = new LangCommand();
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    @Test
    void unchangedLanguageIsNotSaved() {
        profiles.put("Leo", new PlayerProfile(player.getUniqueId(), "Leo", Language.Polish, new HashMap<>()));
        cmd.perform(player, new String[]{"lang", "pl"}, null);
        verify(players, never()).updatePlayerLanguage(any(), any());
        verify(players, never()).save();
        verify(messages).send(eq(player), eq("LanguageChanged"), any());
    }

    @Test
    void changeSavesOnceAndSecondChangeWithinCooldownIsRefused() {
        profiles.put("Leo", new PlayerProfile(player.getUniqueId(), "Leo", Language.English, new HashMap<>()));
        cmd.perform(player, new String[]{"lang", "pl"}, null);
        verify(players).updatePlayerLanguage("Leo", Language.Polish);
        verify(players).save();

        cmd.perform(player, new String[]{"lang", "de"}, null);
        verify(messages).send(player, "LanguageCooldown");
        verify(players, times(1)).save();
    }

    @Test
    void cooldownExpiresAfterTwoSeconds() {
        UUID id = UUID.randomUUID();
        assertFalse(LangCommand.onCooldown(id, 1000));
        assertTrue(LangCommand.onCooldown(id, 2999));
        assertFalse(LangCommand.onCooldown(id, 3000));
    }
}
