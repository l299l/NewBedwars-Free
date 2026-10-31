package com.l299l.newbedwars.arena.phases;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PhaseActionRegistryTest {
    private final List<String> added = new ArrayList<>();

    @AfterEach
    void tearDown() {
        added.forEach(PhaseActionRegistry::unregister);
    }

    private void register(String name, java.util.function.BiConsumer<IArena, String> action) {
        PhaseActionRegistry.register(name, action);
        added.add(name);
    }

    @Test
    void builtInActionsStillWork() {
        IArena arena = mock(IArena.class);
        new PhaseAction("DIAMOND_GENERATOR:2").execute(arena);
        verify(arena).upgradeGenerators(GeneratorType.DIAMOND, 2);
        new PhaseAction("EMERALD_GENERATOR: 3").execute(arena);
        verify(arena).upgradeGenerators(GeneratorType.EMERALD, 3);
        new PhaseAction("BED_DESTRUCTION").execute(arena);
        verify(arena).bedDestruction();
        new PhaseAction("GAME_END:draw").execute(arena);
        new PhaseAction("END_GAME:draw").execute(arena);
        verify(arena, times(2)).endGame("draw");
        new PhaseAction("ENDER_DRAGON_START").execute(arena);
        new PhaseAction("ENDER_DRAGON_END").execute(arena);
        new PhaseAction("DISABLE_REGENERATION").execute(arena);
        new PhaseAction("DISABLE_TEAMS_FORGE").execute(arena);
        verify(arena).enderDragonStart();
        verify(arena).enderDragonEnd();
        verify(arena).disableRegeneration();
        verify(arena).disableTeamsForge();
    }

    @Test
    void registeredActionRunsWithItsArgument() {
        List<String> calls = new ArrayList<>();
        register("MY_ACTION", (arena, arg) -> calls.add(arg));
        new PhaseAction("MY_ACTION:hello").execute(mock(IArena.class));
        new PhaseAction("my_action").execute(mock(IArena.class));
        assertEquals(List.of("hello", ""), calls);
    }

    @Test
    void argumentMayContainColons() {
        List<String> calls = new ArrayList<>();
        register("COLON_ACTION", (arena, arg) -> calls.add(arg));
        new PhaseAction("COLON_ACTION:a:b").execute(mock(IArena.class));
        assertEquals(List.of("a:b"), calls);
    }

    @Test
    void unknownActionIsIgnored() {
        IArena arena = mock(IArena.class);
        assertDoesNotThrow(() -> new PhaseAction("NOT_AN_ACTION:1").execute(arena));
        verifyNoInteractions(arena);
    }

    @Test
    void duplicateNamesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PhaseActionRegistry.register("bed_destruction", (a, s) -> {}));
        register("ONCE", (a, s) -> {});
        assertThrows(IllegalArgumentException.class, () -> PhaseActionRegistry.register("once", (a, s) -> {}));
    }

    @Test
    void unregisteredActionStopsRunning() {
        List<String> calls = new ArrayList<>();
        register("TEMP_ACTION", (arena, arg) -> calls.add(arg));
        assertTrue(PhaseActionRegistry.unregister("TEMP_ACTION"));
        new PhaseAction("TEMP_ACTION").execute(mock(IArena.class));
        assertTrue(calls.isEmpty());
        assertFalse(PhaseActionRegistry.unregister("TEMP_ACTION"));
    }
}
