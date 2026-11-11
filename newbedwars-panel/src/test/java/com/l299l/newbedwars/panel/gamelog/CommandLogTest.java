package com.l299l.newbedwars.panel.gamelog;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.panel.gamelog.GameLog.Entry;
import com.l299l.newbedwars.panel.gamelog.GameLog.Type;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandLogTest {
    private final CommandFilter filter = new CommandFilter(CommandFilter.DEFAULT_IGNORED);

    @Test
    void adminCommandsAreKeptPlayerCommandsAreNot() {
        assertEquals("/bw game forcestart", filter.loggable("/bw game forcestart"));
        assertEquals("/gamemode creative Alice", filter.loggable("/gamemode  creative Alice"));
        assertEquals("/gamemode creative", filter.loggable("/minecraft:gamemode creative"));
        assertEquals("/tp Bob", filter.loggable("/TP Bob"));
        assertNull(filter.loggable("/lobby"));
        assertNull(filter.loggable("/bw lobby"));
        assertNull(filter.loggable("/BW Lang pl"));
        assertNull(filter.loggable("/msg Bob hi"));
        assertNull(filter.loggable("/essentials:msg Bob hi"));
        assertNull(filter.loggable("/party invite Bob"));
        assertNull(filter.loggable("/p leave"));
        assertNull(filter.loggable("/"));
        assertNull(filter.loggable(null));
    }

    @Test
    void passwordCommandsAreNeverLogged() {
        CommandFilter none = new CommandFilter(List.of());
        assertNull(none.loggable("/login secret"));
        assertNull(none.loggable("/authme:register a a"));
        assertNull(none.loggable("/changepassword a b"));
        assertEquals("/lobby", none.loggable("/lobby"));
    }

    @Test
    void longCommandsAreCut() {
        String line = filter.loggable("/say " + "x".repeat(400));
        assertTrue(line.length() <= 260);
        assertTrue(line.endsWith("..."));
    }

    @Test
    void onlyCommandsInsideAnArenaWorldAreLogged() {
        GameLog log = new GameLog(() -> 1L);
        ArenaView arena = mock(ArenaView.class);
        when(arena.getName()).thenReturn("Castle");
        when(arena.getGamePlayer(any())).thenReturn(Optional.empty());
        World arenaWorld = mock(World.class);
        World lobby = mock(World.class);
        GameLogService service = new GameLogService(log, filter, w -> w == arenaWorld ? Optional.of(arena) : Optional.empty(),
                (p, label) -> !label.equals("op"));
        Player admin = mock(Player.class);
        when(admin.getName()).thenReturn("Admin");
        when(admin.getUniqueId()).thenReturn(UUID.randomUUID());
        when(admin.getWorld()).thenReturn(arenaWorld);
        service.onCommand(new PlayerCommandPreprocessEvent(admin, "/gamemode creative", new HashSet<>()));
        service.onCommand(new PlayerCommandPreprocessEvent(admin, "/bw lobby", new HashSet<>()));
        PlayerCommandPreprocessEvent cancelled = new PlayerCommandPreprocessEvent(admin, "/stop", new HashSet<>());
        cancelled.setCancelled(true);
        service.onCommand(cancelled);
        service.onCommand(new PlayerCommandPreprocessEvent(admin, "/op Admin", new HashSet<>()));
        when(admin.getWorld()).thenReturn(lobby);
        service.onCommand(new PlayerCommandPreprocessEvent(admin, "/gamemode survival", new HashSet<>()));

        List<Entry> entries = log.after("Castle", 0, 100).entries();
        assertEquals(3, entries.size());
        assertEquals("/op Admin (no permission)", entries.get(2).detail());
        assertEquals(Type.COMMAND, entries.get(0).type());
        assertEquals("Admin", entries.get(0).player());
        assertEquals("/gamemode creative", entries.get(0).detail());
        assertEquals("/stop (cancelled)", entries.get(1).detail());
    }
}
