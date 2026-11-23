package com.l299l.newbedwars.commands.bedwars.admin.setup.advanced;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import com.l299l.newbedwars.tablist.TablistConfig;
import com.l299l.newbedwars.tablist.TablistManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SetTablistCommandTest {
    private Messages messages;
    private Player player;
    private IArena arena;
    private SetTablistCommand command;

    @BeforeEach
    void setUp() {
        messages = mock(Messages.class);
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getMessages()).thenReturn(messages);
        when(messages.getMsg(any(Player.class), anyString())).thenReturn("usage:");
        TablistConfig config = TablistConfig.parse(YamlConfiguration.loadConfiguration(new java.io.StringReader(
                "Layouts:\n  Default: {}\n  Fancy: {}\n")), null);
        when(plugin.getTablistManager()).thenReturn(new TablistManager(config, null));
        NewBedwars.plugin = plugin;
        player = mock(Player.class);
        arena = mock(IArena.class);
        command = new SetTablistCommand();
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    @Test
    void knownLayoutIsSetAndSavedCaseInsensitively() {
        try (MockedConstruction<ArenaDataJson> json = org.mockito.Mockito.mockConstruction(ArenaDataJson.class)) {
            command.perform(player, new String[]{"setTablist", "fancy"}, arena);
            verify(arena).setTablistId("Fancy");
            verify(json.constructed().get(0)).save();
        }
        verify(messages).send(player, "TablistSet", Map.of("/id/", "Fancy"));
    }

    @Test
    void unknownLayoutIsRejected() {
        try (MockedConstruction<ArenaDataJson> json = org.mockito.Mockito.mockConstruction(ArenaDataJson.class)) {
            command.perform(player, new String[]{"setTablist", "nope"}, arena);
            assertEquals(0, json.constructed().size());
        }
        verify(arena, never()).setTablistId(any());
        verify(messages).send(player, "TablistInvalid", Map.of("/id/", "nope", "/available/", "Default, Fancy"));
    }

    @Test
    void tabCompletionListsLayouts() {
        assertEquals(List.of("Default", "Fancy"), command.getSubcommandArguments(player, new String[]{"setTablist", ""}));
    }
}
