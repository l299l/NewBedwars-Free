package com.l299l.newbedwars.panel.gamelog;

import com.l299l.newbedwars.api.events.ArenaChatEvent;
import com.l299l.newbedwars.api.events.ArenaResetEvent;
import com.l299l.newbedwars.api.events.BedBreakEvent;
import com.l299l.newbedwars.api.events.GameEndEvent;
import com.l299l.newbedwars.api.events.GamePhaseChangeEvent;
import com.l299l.newbedwars.api.events.GameStartEvent;
import com.l299l.newbedwars.api.events.PlayerKillEvent;
import com.l299l.newbedwars.api.events.PlayerLeaveArenaEvent;
import com.l299l.newbedwars.api.events.LeaveReason;
import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.GamePlayerView;
import com.l299l.newbedwars.api.view.TeamView;
import com.l299l.newbedwars.panel.gamelog.GameLog.Entry;
import com.l299l.newbedwars.panel.gamelog.GameLog.Type;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameLogServiceTest {
    private final GameLog log = new GameLog(() -> 5L);
    private final GameLogService service = new GameLogService(log);
    private ArenaView arena;
    private TeamView red;
    private TeamView blue;
    private Player alice;
    private Player bob;

    private Player player(String name, TeamView team) {
        Player p = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(p.getName()).thenReturn(name);
        when(p.getUniqueId()).thenReturn(id);
        GamePlayerView gp = mock(GamePlayerView.class);
        when(gp.getTeam()).thenReturn(Optional.ofNullable(team));
        when(arena.getGamePlayer(id)).thenReturn(Optional.of(gp));
        return p;
    }

    private TeamView team(String name, ChatColor color) {
        TeamView t = mock(TeamView.class);
        when(t.getName()).thenReturn(name);
        when(t.getColor()).thenReturn(color);
        return t;
    }

    @BeforeEach
    void setUp() {
        arena = mock(ArenaView.class);
        when(arena.getName()).thenReturn("Castle");
        red = team("Red", ChatColor.RED);
        blue = team("Blue", ChatColor.BLUE);
        alice = player("Alice", red);
        bob = player("Bob", blue);
    }

    private List<Entry> entries() {
        return log.after("castle", 0, 100).entries();
    }

    @Test
    void killsDeathsAndBedsCarryTeamColours() {
        service.onKill(new PlayerKillEvent(arena, bob, alice, true, DamageCause.ENTITY_ATTACK));
        service.onKill(new PlayerKillEvent(arena, alice, null, false, DamageCause.VOID));
        service.onBed(new BedBreakEvent(arena, blue, alice));
        List<Entry> e = entries();
        assertEquals(Type.FINAL_KILL, e.get(0).type());
        assertEquals("Alice", e.get(0).player());
        assertEquals("#FF5555", e.get(0).teamColor());
        assertEquals("Bob", e.get(0).target());
        assertEquals("#5555FF", e.get(0).targetColor());
        assertEquals(Type.DEATH, e.get(1).type());
        assertEquals("VOID", e.get(1).detail());
        assertEquals(Type.BED, e.get(2).type());
        assertEquals("Blue", e.get(2).target());
    }

    @Test
    void chatPhaseLeaveAndEnd() {
        service.onChat(new ArenaChatEvent(arena, alice, "go mid", true, red));
        service.onPhase(new GamePhaseChangeEvent(arena, "One", "Two", true));
        service.onLeave(new PlayerLeaveArenaEvent(arena, bob, LeaveReason.DISCONNECT));
        service.onEnd(new GameEndEvent(arena, null));
        List<Entry> e = entries();
        assertEquals(Type.TEAM_CHAT, e.get(0).type());
        assertEquals("go mid", e.get(0).detail());
        assertEquals("Red", e.get(0).team());
        assertEquals("Two", e.get(1).target());
        assertEquals("forced", e.get(1).detail());
        assertEquals("DISCONNECT", e.get(2).detail());
        assertNull(e.get(3).target());
    }

    @Test
    void aResetKeepsTheOldGameUntilTheNextStart() {
        service.onEnd(new GameEndEvent(arena, red));
        service.onReset(new ArenaResetEvent(arena, ArenaResetEvent.Stage.PRE));
        service.onReset(new ArenaResetEvent(arena, ArenaResetEvent.Stage.POST));
        service.admin("Castle", "boss", "start", null);
        assertEquals(2, entries().size());
        when(arena.getPlayers()).thenReturn(List.of(alice, bob));
        service.onStart(new GameStartEvent(arena));
        List<Entry> e = entries();
        assertEquals(List.of(Type.ADMIN, Type.START), e.stream().map(Entry::type).toList());
        assertEquals("2", e.get(1).detail());
    }
}
