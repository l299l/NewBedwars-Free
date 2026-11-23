package com.l299l.newbedwars.commands.bedwars.admin.setup.advanced;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import com.l299l.newbedwars.gui.GUISave;
import com.l299l.newbedwars.config.properties.GameMode;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GUIManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SetModeCommandTest {
    private Messages messages;
    private GUIManager guiManager;
    private Player player;
    private IArena arena;
    private HashMap<String, GeneratorLeveling> levelings;
    private Map<String, GameMode> savedModes;
    private HashMap<String, GamePhases> savedPhases;
    private GeneratorLeveling leveling;
    private GamePhases phases;
    private GUISave shop;
    private GUISave upgrade;
    private SetModeCommand command;

    @BeforeEach
    void setUp() {
        messages = mock(Messages.class);
        guiManager = mock(GUIManager.class);
        levelings = new HashMap<>();
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getMessages()).thenReturn(messages);
        when(plugin.getGuiManager()).thenReturn(guiManager);
        when(plugin.getGeneratorLeveling()).thenReturn(levelings);
        when(messages.getMsg(any(Player.class), anyString())).thenReturn("usage:");
        NewBedwars.plugin = plugin;
        player = mock(Player.class);
        arena = mock(IArena.class);
        savedModes = Properties.Modes;
        savedPhases = Properties.GamePhases;
        Properties.Modes = new LinkedHashMap<>();
        Properties.GamePhases = new HashMap<>();
        leveling = mock(GeneratorLeveling.class);
        phases = new GamePhases("Squads", List.of());
        shop = mock(GUISave.class);
        upgrade = mock(GUISave.class);
        levelings.put("Squads", leveling);
        Properties.GamePhases.put("Squads", phases);
        when(guiManager.getGui("shopX")).thenReturn(shop);
        when(guiManager.getGui("upgradeSquads")).thenReturn(upgrade);
        when(arena.getTeams()).thenReturn(teams(4));
        command = new SetModeCommand();
    }

    @AfterEach
    void tearDown() {
        Properties.Modes = savedModes;
        Properties.GamePhases = savedPhases;
        NewBedwars.plugin = null;
    }

    private static Map<String, Team> teams(int count) {
        Map<String, Team> teams = new HashMap<>();
        for (int i = 0; i < count; i++) teams.put("t" + i, mock(Team.class));
        return teams;
    }

    private void mode(GameMode mode) {
        Properties.Modes.put(mode.id(), mode);
    }

    private void run(String... args) {
        try (MockedConstruction<ArenaDataJson> json = mockConstruction(ArenaDataJson.class)) {
            command.perform(player, args, arena);
            if (json.constructed().isEmpty()) return;
            verify(json.constructed().get(0)).save();
        }
    }

    @Test
    void appliesEveryPartSavesAndConfirms() {
        mode(new GameMode("Squads", "Squads", "Squads", "shopX", "upgradeSquads", 4, 4));
        try (MockedConstruction<ArenaDataJson> json = mockConstruction(ArenaDataJson.class)) {
            command.perform(player, new String[]{"setMode", "Squads"}, arena);
            assertEquals(1, json.constructed().size());
            InOrder order = inOrder(arena, json.constructed().get(0), messages);
            order.verify(arena).setGeneratorsLeveling(leveling);
            order.verify(arena).setGamePhases(phases);
            order.verify(arena).setShopGuiId("shopX");
            order.verify(arena).setUpgradeGuiId("upgradeSquads");
            order.verify(arena).setMaxInTeam(4);
            order.verify(json.constructed().get(0)).save();
            order.verify(messages).send(player, "ModeSet", Map.of("/mode/", "Squads"));
        }
        verify(messages, never()).send(any(Player.class), org.mockito.ArgumentMatchers.eq("ModeTeamsDiffer"), any(Map.class));
    }

    @Test
    void partialModeLeavesOtherSettingsUntouched() {
        mode(new GameMode("Squads", "Squads", null, null, null, 0, 0));
        run("setMode", "Squads");
        verify(arena).setGeneratorsLeveling(leveling);
        verify(arena, never()).setGamePhases(any());
        verify(arena, never()).setShopGuiId(anyString());
        verify(arena, never()).setUpgradeGuiId(anyString());
        verify(arena, never()).setMaxInTeam(anyInt());
        verify(messages).send(player, "ModeSet", Map.of("/mode/", "Squads"));
    }

    @Test
    void missingPartsChangeNothingAndAreListed() {
        mode(new GameMode("Broken2", "NoGen", "NoPhases", "noShop", "noUpgrade", 2, 0));
        try (MockedConstruction<ArenaDataJson> json = mockConstruction(ArenaDataJson.class)) {
            command.perform(player, new String[]{"setMode", "Broken2"}, arena);
            assertTrue(json.constructed().isEmpty());
        }
        verify(arena, never()).setGeneratorsLeveling(any());
        verify(arena, never()).setGamePhases(any());
        verify(arena, never()).setShopGuiId(anyString());
        verify(arena, never()).setUpgradeGuiId(anyString());
        verify(arena, never()).setMaxInTeam(anyInt());
        verify(messages).send(player, "ModeMissingParts", Map.of("/mode/", "Broken2", "/missing/", "NoGen, NoPhases, noShop, noUpgrade"));
        verify(messages, never()).send(any(Player.class), org.mockito.ArgumentMatchers.eq("ModeSet"), any(Map.class));
    }

    @Test
    void onlyTheMissingPartIsReportedWhenOthersExist() {
        mode(new GameMode("Half", "Squads", "Squads", null, "gone", 0, 0));
        try (MockedConstruction<ArenaDataJson> json = mockConstruction(ArenaDataJson.class)) {
            command.perform(player, new String[]{"setMode", "Half"}, arena);
            assertTrue(json.constructed().isEmpty());
        }
        verify(arena, never()).setGeneratorsLeveling(any());
        verify(messages).send(player, "ModeMissingParts", Map.of("/mode/", "Half", "/missing/", "gone"));
    }

    @Test
    void unknownModeListsAvailable() {
        mode(new GameMode("Solo", null, null, null, null, 0, 0));
        mode(new GameMode("Squads", null, null, null, null, 0, 0));
        try (MockedConstruction<ArenaDataJson> json = mockConstruction(ArenaDataJson.class)) {
            command.perform(player, new String[]{"setMode", "Nope"}, arena);
            assertTrue(json.constructed().isEmpty());
        }
        verify(messages).send(player, "ModeInvalid", Map.of("/mode/", "Nope", "/available/", "Solo, Squads"));
        verify(arena, never()).setMaxInTeam(anyInt());
    }

    @Test
    void modeLookupIsCaseInsensitive() {
        mode(new GameMode("Squads", "Squads", null, null, null, 0, 0));
        run("setMode", "sQuAdS");
        verify(arena).setGeneratorsLeveling(leveling);
        verify(messages).send(player, "ModeSet", Map.of("/mode/", "Squads"));
    }

    @Test
    void teamCountMismatchWarnsAfterModeSet() {
        mode(new GameMode("Duels", null, null, null, null, 1, 2));
        try (MockedConstruction<ArenaDataJson> json = mockConstruction(ArenaDataJson.class)) {
            command.perform(player, new String[]{"setMode", "Duels"}, arena);
        }
        InOrder order = inOrder(messages);
        order.verify(messages).send(player, "ModeSet", Map.of("/mode/", "Duels"));
        order.verify(messages).send(player, "ModeTeamsDiffer", Map.of("/mode/", "Duels", "/teams/", "2", "/arenaTeams/", "4"));
    }

    @Test
    void matchingTeamCountDoesNotWarn() {
        mode(new GameMode("Squads", null, null, null, null, 4, 4));
        run("setMode", "Squads");
        verify(messages, never()).send(any(Player.class), org.mockito.ArgumentMatchers.eq("ModeTeamsDiffer"), any(Map.class));
    }

    @Test
    void zeroTeamsDoesNotWarn() {
        mode(new GameMode("Free", null, null, null, null, 0, 0));
        run("setMode", "Free");
        verify(messages).send(player, "ModeSet", Map.of("/mode/", "Free"));
        verify(messages, never()).send(any(Player.class), org.mockito.ArgumentMatchers.eq("ModeTeamsDiffer"), any(Map.class));
    }

    @Test
    void wrongArgumentCountShowsUsage() {
        mode(new GameMode("Squads", "Squads", null, null, null, 0, 0));
        command.perform(player, new String[]{"setMode"}, arena);
        command.perform(player, new String[]{"setMode", "Squads", "x"}, arena);
        verify(arena, never()).setGeneratorsLeveling(any());
        verify(player, times(2)).sendMessage("usage:" + command.getSyntax());
    }

    @Test
    void tabCompletionListsModeKeys() {
        mode(new GameMode("Solo", null, null, null, null, 0, 0));
        mode(new GameMode("Custom", null, null, null, null, 0, 0));
        assertEquals(List.of("Solo", "Custom"), command.getSubcommandArguments(player, new String[]{"setMode", ""}));
        assertTrue(command.getSubcommandArguments(player, new String[]{"setMode", "Solo", ""}).isEmpty());
    }
}
