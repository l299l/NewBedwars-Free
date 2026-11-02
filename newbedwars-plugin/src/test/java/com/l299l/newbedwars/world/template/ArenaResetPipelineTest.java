package com.l299l.newbedwars.world.template;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ArenaResetPipelineTest {

    private Arena arena;
    private World world;
    private BukkitScheduler scheduler;
    private final List<String> order = new ArrayList<>();
    private boolean oldAsync;
    private int oldRetryTicks;

    @BeforeEach
    void setUp() {
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        NewBedwars.plugin = plugin;
        oldAsync = Properties.RollbackAsyncCopy;
        oldRetryTicks = Properties.RollbackUnloadRetryTicks;
        Properties.RollbackAsyncCopy = false;
        Properties.RollbackUnloadRetryTicks = 10;

        arena = mock(Arena.class);
        world = mock(World.class);
        when(world.getName()).thenReturn("castle_world");
        when(world.getWorldFolder()).thenReturn(new File("castle_world"));
        when(world.getPlayers()).thenReturn(List.of());
        when(arena.getArenaName()).thenReturn("Castle");
        when(arena.getArenaWorld()).thenReturn(world);
        scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong())).thenAnswer(inv -> {
            inv.<Runnable>getArgument(1).run();
            return null;
        });

        doAnswer(inv -> order.add("unbind")).when(arena).unbindWorld();
        doAnswer(inv -> order.add("finish")).when(arena).finishReset(any());
    }

    @AfterEach
    void tearDown() {
        Properties.RollbackAsyncCopy = oldAsync;
        Properties.RollbackUnloadRetryTicks = oldRetryTicks;
        NewBedwars.plugin = null;
    }

    @Test
    void nullWorldFailsTheReset() {
        when(arena.getArenaWorld()).thenReturn(null);

        assertFalse(ArenaResetPipeline.start(arena));

        verify(arena).failReset(contains("not loaded"));
    }

    @Test
    void unloadIsRetriedThenFails() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.unloadWorld(world, false)).thenReturn(false);

            assertTrue(ArenaResetPipeline.start(arena));

            bukkit.verify(() -> Bukkit.unloadWorld(world, false), times(50));
            verify(scheduler, times(49)).runTaskLater(any(), any(Runnable.class), eq(10L));
            verify(arena).failReset(contains("50 attempts"));
            verify(arena, never()).finishReset(any());
        }
    }

    @Test
    void successRunsEveryStepInOrder() {
        World fresh = mock(World.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ArenaMaps> maps = mockStatic(ArenaMaps.class);
             MockedStatic<MapTemplateManager> templates = mockStatic(MapTemplateManager.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.unloadWorld(world, false)).thenReturn(true);
            bukkit.when(() -> Bukkit.createWorld(any(WorldCreator.class))).thenAnswer(inv -> {
                order.add("create");
                return fresh;
            });
            templates.when(() -> MapTemplateManager.restoreTemplate(any(), eq("Castle"), any(), any()))
                    .thenAnswer(inv -> order.add("restore"));
            maps.when(() -> ArenaMaps.applyWorldRules(fresh)).thenAnswer(inv -> order.add("rules"));
            maps.when(() -> ArenaMaps.clearDirty("Castle")).thenAnswer(inv -> order.add("clearDirty"));

            assertTrue(ArenaResetPipeline.start(arena));

            assertEquals(List.of("unbind", "restore", "create", "rules", "clearDirty", "finish"), order);
            verify(arena, never()).failReset(any());
            verify(world).setAutoSave(false);
        }
    }

    @Test
    void restoreFailureFailsWithoutClearingDirty() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ArenaMaps> maps = mockStatic(ArenaMaps.class);
             MockedStatic<MapTemplateManager> templates = mockStatic(MapTemplateManager.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.unloadWorld(world, false)).thenReturn(true);
            templates.when(() -> MapTemplateManager.restoreTemplate(any(), any(), any(), any()))
                    .thenThrow(new IOException("corrupt"));

            ArenaResetPipeline.start(arena);

            verify(arena).failReset(contains("corrupt"));
            maps.verify(() -> ArenaMaps.clearDirty(any()), never());
            bukkit.verify(() -> Bukkit.createWorld(any(WorldCreator.class)), never());
            verify(arena, never()).finishReset(any());
        }
    }

    @Test
    void worldCreationFailureFailsWithoutClearingDirty() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ArenaMaps> maps = mockStatic(ArenaMaps.class);
             MockedStatic<MapTemplateManager> templates = mockStatic(MapTemplateManager.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.unloadWorld(world, false)).thenReturn(true);
            bukkit.when(() -> Bukkit.createWorld(any(WorldCreator.class))).thenReturn(null);

            ArenaResetPipeline.start(arena);

            verify(arena).failReset(contains("recreated"));
            maps.verify(() -> ArenaMaps.clearDirty(any()), never());
            verify(arena, never()).finishReset(any());
        }
    }

    @Test
    void retryWithUnknownWorldFails() {
        when(arena.getArenaWorld()).thenReturn(null);

        ArenaResetPipeline.retry(arena);

        verify(arena).failReset(contains("not known"));
    }

    @Test
    void retryWhileWorldIsStillLoadedUnloadsAgain() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getWorld("castle_world")).thenReturn(world);
            bukkit.when(() -> Bukkit.unloadWorld(world, false)).thenReturn(false);

            ArenaResetPipeline.retry(arena);

            bukkit.verify(() -> Bukkit.unloadWorld(world, false), atLeastOnce());
            verify(arena).failReset(contains("could not be unloaded"));
        }
    }

    @Test
    void retryWithUnloadedWorldRestoresFromTheTemplate() {
        World fresh = mock(World.class);
        Path folder = new File("castle_world").toPath();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ArenaMaps> maps = mockStatic(ArenaMaps.class);
             MockedStatic<MapTemplateManager> templates = mockStatic(MapTemplateManager.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getWorld("castle_world")).thenReturn(null);
            bukkit.when(() -> Bukkit.createWorld(any(WorldCreator.class))).thenReturn(fresh);
            maps.when(() -> ArenaMaps.worldFolder("castle_world")).thenReturn(folder);

            ArenaResetPipeline.retry(arena);

            bukkit.verify(() -> Bukkit.unloadWorld(any(World.class), anyBoolean()), never());
            templates.verify(() -> MapTemplateManager.restoreTemplate(any(), eq("Castle"), eq(folder), any()));
            verify(arena).unbindWorld();
            verify(arena).finishReset(fresh);
        }
    }
}
