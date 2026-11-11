package com.l299l.newbedwars.commands.bedwars;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MainCommandRegistrationTest {
    private MainCommand main;

    @BeforeEach
    void setUp() {
        NewBedwars.plugin = mock(NewBedwars.class);
        Messages messages = mock(Messages.class);
        when(messages.getMsgToConsole(anyString())).thenReturn("msg /commands/");
        when(NewBedwars.plugin.getMessages()).thenReturn(messages);
        main = new MainCommand();
    }

    @AfterEach
    void tearDown() {
        ArenaRegistry.clear();
        NewBedwars.plugin = null;
    }

    private SubCommand command(String name) {
        SubCommand command = mock(SubCommand.class);
        when(command.getName()).thenReturn(name);
        return command;
    }

    @Test
    void addonCommandCanBeAddedToEveryContext() {
        for (MainCommand.Context context : MainCommand.Context.values()) {
            SubCommand command = command("addon-" + context.name().toLowerCase());
            assertDoesNotThrow(() -> main.register(command, context));
            assertTrue(main.unregister(command));
        }
    }

    @Test
    void nameMustBeFreeInThePlacesItIsUsed() {
        main.register(command("addoncmd"), MainCommand.Context.LOBBY);
        assertThrows(IllegalArgumentException.class, () -> main.register(command("ADDONCMD"), MainCommand.Context.LOBBY_ADMIN));
        assertDoesNotThrow(() -> main.register(command("ADDONCMD"), MainCommand.Context.GAME));
        assertThrows(IllegalArgumentException.class, () -> main.register(command("join"), MainCommand.Context.LOBBY_ADMIN));
        assertThrows(IllegalArgumentException.class, () -> main.register(command("help"), MainCommand.Context.GAME_ADMIN));
        assertThrows(IllegalArgumentException.class, () -> main.register(command("save"), MainCommand.Context.SETUP_AUTOMATIC));
        SubCommand partly = command("addoncmd");
        assertThrows(IllegalArgumentException.class, () -> main.register(partly, MainCommand.Context.CONSOLE, MainCommand.Context.GAME));
        assertTrue(main.contextsOf(partly).isEmpty());
        assertDoesNotThrow(() -> main.register(command("addoncmd"), MainCommand.Context.CONSOLE));
        assertThrows(IllegalArgumentException.class, () -> main.register(command("none")));
    }

    @Test
    void oneCommandInSeveralContexts() {
        SubCommand trim = command("trim");
        main.register(trim, MainCommand.Context.LOBBY, MainCommand.Context.GAME, MainCommand.Context.CONSOLE);
        assertEquals(List.of(MainCommand.Context.LOBBY, MainCommand.Context.GAME, MainCommand.Context.CONSOLE), main.contextsOf(trim));
        assertDoesNotThrow(() -> main.register(trim, MainCommand.Context.LOBBY, MainCommand.Context.GAME_ADMIN));
        assertEquals(List.of(MainCommand.Context.LOBBY, MainCommand.Context.GAME, MainCommand.Context.GAME_ADMIN, MainCommand.Context.CONSOLE), main.contextsOf(trim));
        assertTrue(main.unregister(trim));
        assertTrue(main.contextsOf(trim).isEmpty());
    }

    @Test
    void gameCommandGetsThePlayersArenaAndLobbyCommandGetsNone() {
        SubCommand info = command("info2");
        main.register(info, MainCommand.Context.LOBBY, MainCommand.Context.GAME);
        Command bw = mock(Command.class);
        World arenaWorld = mock(World.class);
        World lobbyWorld = mock(World.class);
        IArena arena = mock(IArena.class);
        UUID id = UUID.randomUUID();
        when(arena.getSetupMode()).thenReturn(Setup.READY);
        when(arena.getPlayers()).thenReturn(new HashSet<>(Set.of(id)));
        when(arena.status()).thenReturn(GameStatus.playing);
        ArenaRegistry.register("a1", arena);
        ArenaRegistry.bindWorld(arenaWorld, arena);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);

        when(player.getWorld()).thenReturn(arenaWorld);
        main.onCommand(player, bw, "bw", new String[]{"info2"});
        verify(info).perform(player, new String[]{"info2"}, arena);

        when(player.getWorld()).thenReturn(lobbyWorld);
        main.onCommand(player, bw, "bw", new String[]{"info2"});
        verify(info).perform(player, new String[]{"info2"}, null);
    }

    @Test
    void consoleRunsConsoleCommandsOnly() {
        SubCommand trim = command("trim");
        SubCommand lobbyOnly = command("lobbyonly");
        main.register(trim, MainCommand.Context.LOBBY, MainCommand.Context.CONSOLE);
        main.register(lobbyOnly, MainCommand.Context.LOBBY);
        when(trim.getConsoleArguments(any(), any())).thenReturn(List.of("all", "arena"));
        CommandSender console = mock(ConsoleCommandSender.class);
        Command bw = mock(Command.class);

        main.onCommand(console, bw, "bw", new String[]{"TRIM", "x"});
        verify(trim).performConsole(eq(console), eq(new String[]{"TRIM", "x"}));
        verify(trim, never()).perform(any(), any(), any());

        main.onCommand(console, bw, "bw", new String[]{"lobbyonly"});
        verify(lobbyOnly, never()).performConsole(any(), any());
        verify(console).sendMessage((String) any());

        assertEquals(List.of("trim"), main.onTabComplete(console, bw, "bw", new String[]{"t"}));
        assertEquals(List.of("arena"), main.onTabComplete(console, bw, "bw", new String[]{"trim", "ar"}));
    }

    @Test
    void unregisterOnlyRemovesThatCommand() {
        SubCommand command = command("temp");
        main.register(command, MainCommand.Context.LOBBY);
        assertTrue(main.unregister(command));
        assertFalse(main.unregister(command));
        assertDoesNotThrow(() -> main.register(command("temp"), MainCommand.Context.LOBBY));
    }
}
