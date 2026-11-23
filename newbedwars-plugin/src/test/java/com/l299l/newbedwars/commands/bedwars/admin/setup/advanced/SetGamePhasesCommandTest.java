package com.l299l.newbedwars.commands.bedwars.admin.setup.advanced;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SetGamePhasesCommandTest {
    private Messages messages;
    private Player player;
    private IArena arena;
    private HashMap<String, GamePhases> saved;
    private GamePhases solo;
    private GamePhases squads;
    private SetGamePhasesCommand command;

    @BeforeEach
    void setUp() {
        messages = mock(Messages.class);
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getMessages()).thenReturn(messages);
        when(messages.getMsg(any(Player.class), anyString())).thenReturn("usage:");
        NewBedwars.plugin = plugin;
        player = mock(Player.class);
        arena = mock(IArena.class);
        saved = Properties.GamePhases;
        Properties.GamePhases = new HashMap<>();
        solo = new GamePhases("Solo", List.of());
        squads = new GamePhases("Squads", List.of());
        Properties.GamePhases.put("Solo", solo);
        Properties.GamePhases.put("Squads", squads);
        command = new SetGamePhasesCommand();
    }

    @AfterEach
    void tearDown() {
        Properties.GamePhases = saved;
        NewBedwars.plugin = null;
    }

    @Test
    void exactIdSetsPhasesAndSaves() {
        try (MockedConstruction<ArenaDataJson> json = org.mockito.Mockito.mockConstruction(ArenaDataJson.class)) {
            command.perform(player, new String[]{"setGamePhases", "Squads"}, arena);
            verify(arena).setGamePhases(squads);
            assertEquals(1, json.constructed().size());
            verify(json.constructed().get(0)).save();
        }
        verify(messages).send(player, "GamePhasesSet", Map.of("/id/", "Squads"));
    }

    @Test
    void idIsCaseInsensitive() {
        try (MockedConstruction<ArenaDataJson> json = org.mockito.Mockito.mockConstruction(ArenaDataJson.class)) {
            command.perform(player, new String[]{"setGamePhases", "sOlO"}, arena);
            verify(arena).setGamePhases(solo);
        }
        verify(messages).send(player, "GamePhasesSet", Map.of("/id/", "Solo"));
    }

    @Test
    void unknownIdListsAvailableSorted() {
        try (MockedConstruction<ArenaDataJson> json = org.mockito.Mockito.mockConstruction(ArenaDataJson.class)) {
            command.perform(player, new String[]{"setGamePhases", "Nope"}, arena);
            assertTrue(json.constructed().isEmpty());
        }
        verify(arena, never()).setGamePhases(any());
        verify(messages).send(player, "GamePhasesInvalid", Map.of("/id/", "Nope", "/available/", "Solo, Squads"));
    }

    @Test
    void wrongArgumentCountShowsUsage() {
        command.perform(player, new String[]{"setGamePhases"}, arena);
        command.perform(player, new String[]{"setGamePhases", "Solo", "x"}, arena);
        verify(arena, never()).setGamePhases(any());
        verify(player, org.mockito.Mockito.times(2)).sendMessage("usage:" + command.getSyntax());
    }

    @Test
    void tabCompletionListsSortedKeys() {
        assertEquals(List.of("Solo", "Squads"), command.getSubcommandArguments(player, new String[]{"setGamePhases", ""}));
        Properties.GamePhases.put("Duels", new GamePhases("Duels", List.of()));
        assertEquals(List.of("Duels", "Solo", "Squads"), command.getSubcommandArguments(player, new String[]{"setGamePhases", "S"}));
        assertTrue(command.getSubcommandArguments(player, new String[]{"setGamePhases", "Solo", ""}).isEmpty());
    }
}
