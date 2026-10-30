package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcCommandTabTest {
    private NpcCommand cmd;
    private final Player player = mock(Player.class);

    @BeforeEach
    void setUp() {
        NewBedwars plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        when(plugin.getMessages()).thenReturn(mock(Messages.class));
        ArenaRegistry.clear();
        IArena castle = mock(IArena.class);
        when(castle.getArenaName()).thenReturn("castle");
        ArenaRegistry.register("castle", castle);
        ArenaGroupManager groups = new ArenaGroupManager();
        groups.register(new ArenaGroup("solo", null));
        cmd = new NpcCommand();
    }

    @AfterEach
    void tearDown() {
        new ArenaGroupManager();
        ArenaRegistry.clear();
        NewBedwars.plugin = null;
    }

    @Test
    void setTargetDependsOnAction() {
        assertEquals(List.of("solo"), cmd.getSubcommandArguments(player, new String[]{"npc", "set", "GROUP", ""}));
        assertEquals(List.of("solo"), cmd.getSubcommandArguments(player, new String[]{"npc", "set", "selector", ""}));
        assertEquals(List.of("castle"), cmd.getSubcommandArguments(player, new String[]{"npc", "set", "ARENA", ""}));
        assertEquals(List.of("solo", "castle"), cmd.getSubcommandArguments(player, new String[]{"npc", "set", "NETWORK", ""}));
        assertEquals(List.of(), cmd.getSubcommandArguments(player, new String[]{"npc", "set", "FLY", ""}));
    }

    @Test
    void createTargetDependsOnAction() {
        assertEquals(List.of("solo"), cmd.getSubcommandArguments(player, new String[]{"npc", "create", "villager", "GROUP", ""}));
        assertEquals(List.of("castle"), cmd.getSubcommandArguments(player, new String[]{"npc", "create", "villager", "ARENA", ""}));
    }
}
