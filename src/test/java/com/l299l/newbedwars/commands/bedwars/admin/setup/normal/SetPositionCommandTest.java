package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.admin.setup.normal.SetPositionCommand.Point;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SetPositionCommandTest {
    private Messages messages;
    private Player player;
    private World world;
    private IArena arena;
    private Team team;

    @BeforeEach
    void setUp() {
        messages = mock(Messages.class);
        NewBedwars plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        when(plugin.getMessages()).thenReturn(messages);
        when(messages.getMsg(any(Player.class), any(String.class))).thenReturn("");

        world = mock(World.class);
        player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 1.5, 2, 3.5));

        arena = mock(IArena.class);
        team = mock(Team.class);
        when(arena.getTeams()).thenReturn(Map.of("Red", team));
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    @Test
    void arenaPointUsesPlayerLocationWithoutCoordinates() {
        when(arena.getWaitingPos1()).thenReturn(new Location(world, 1, 2, 3));
        new SetPositionCommand(Point.WAITING_POS1).perform(player, new String[]{"setWaitingPos1"}, arena);
        verify(arena).setWaitingPos1(player.getLocation());
        verify(player).sendMessage("1 2 3");
    }

    @Test
    void arenaPointParsesCoordinates() {
        when(arena.getWaitingSpawn()).thenReturn(new Location(world, 10, 20, 30));
        new SetPositionCommand(Point.WAITING_SPAWN).perform(player, new String[]{"setWaitingSpawn", "10", "20", "30"}, arena);
        ArgumentCaptor<Location> loc = ArgumentCaptor.forClass(Location.class);
        verify(arena).setWaitingSpawn(loc.capture());
        assertEquals(10, loc.getValue().getBlockX());
        assertEquals(30, loc.getValue().getBlockZ());
    }

    @Test
    void badCoordinatesShowUsage() {
        SetPositionCommand cmd = new SetPositionCommand(Point.WAITING_POS2);
        cmd.perform(player, new String[]{"setWaitingPos2", "a", "2", "3"}, arena);
        cmd.perform(player, new String[]{"setWaitingPos2", "1", "2"}, arena);
        verify(arena, never()).setWaitingPos2(any());
        verify(player, times(2)).sendMessage(cmd.getSyntax());
    }

    @Test
    void teamPointSetsTheNamedTeam() {
        when(team.getTeamBasePos1()).thenReturn(new Location(world, 5, 6, 7));
        new SetPositionCommand(Point.TEAM_BASE_POS1).perform(player, new String[]{"setTeamBasePos1", "Red", "5", "6", "7"}, arena);
        verify(team).setTeamBasePos1(any(Location.class));
        verify(player).sendMessage("5 6 7");
    }

    @Test
    void unknownTeamShowsUsage() {
        SetPositionCommand cmd = new SetPositionCommand(Point.TEAM_BUILD_PROT_POS2);
        cmd.perform(player, new String[]{"setTeamBuildProtAreaPos2", "Blue"}, arena);
        cmd.perform(player, new String[]{"setTeamBuildProtAreaPos2"}, arena);
        verify(team, never()).setTeamBuildProtAreaPos2(any());
        verify(player, times(2)).sendMessage(cmd.getSyntax());
    }

    @Test
    void commandNamesAndSyntax() {
        assertEquals("setTeamSpawn", new SetPositionCommand(Point.TEAM_SPAWN).getName());
        assertEquals("/bw setTeamSpawn <teamName> (<x> <y> <z>)", new SetPositionCommand(Point.TEAM_SPAWN).getSyntax());
        assertEquals("/bw setWaitingPos1 (<x> <y> <z>)", new SetPositionCommand(Point.WAITING_POS1).getSyntax());
    }
}
