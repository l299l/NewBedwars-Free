package com.l299l.newbedwars.commands.bedwars;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Messages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MainCommandRegistrationTest {
    private MainCommand main;

    @BeforeEach
    void setUp() {
        NewBedwars.plugin = mock(NewBedwars.class);
        when(NewBedwars.plugin.getMessages()).thenReturn(mock(Messages.class));
        main = new MainCommand();
    }

    @AfterEach
    void tearDown() {
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
    void nameMustBeFreeInEveryContext() {
        main.register(command("addoncmd"), MainCommand.Context.LOBBY);
        assertThrows(IllegalArgumentException.class, () -> main.register(command("ADDONCMD"), MainCommand.Context.GAME));
        assertThrows(IllegalArgumentException.class, () -> main.register(command("join"), MainCommand.Context.LOBBY_ADMIN));
        assertThrows(IllegalArgumentException.class, () -> main.register(command("help"), MainCommand.Context.GAME));
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
