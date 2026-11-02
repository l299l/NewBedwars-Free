package com.l299l.newbedwars.arena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.setup.DecoUtils;
import com.l299l.newbedwars.world.template.ArenaMaps;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ArenaResetCoordinatorTest {

    private static final int MAX_RETRIES = 6;

    private Arena arena;
    private BukkitScheduler scheduler;
    private ArenaResetCoordinator coordinator;

    @BeforeEach
    void setUp() {
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        NewBedwars.plugin = plugin;
        arena = mock(Arena.class);
        when(arena.getArenaName()).thenReturn("Castle");
        scheduler = mock(BukkitScheduler.class);
        coordinator = new ArenaResetCoordinator(arena);
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    @Test
    void failResetRetriesExactlyMaxTimesThenDisables() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ArenaMaps> maps = mockStatic(ArenaMaps.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            maps.when(() -> ArenaMaps.hasTemplate("Castle")).thenReturn(true);

            for (int i = 0; i < MAX_RETRIES; i++) coordinator.failReset("boom");

            verify(scheduler, times(MAX_RETRIES)).runTaskLater(any(), any(Runnable.class), anyLong());
            verify(arena, times(MAX_RETRIES)).setStatus(GameStatus.restarting);
            verify(arena, never()).disableBroken(any());

            coordinator.failReset("boom");

            verify(scheduler, times(MAX_RETRIES)).runTaskLater(any(), any(Runnable.class), anyLong());
            verify(arena).disableBroken("boom");
        }
    }

    @Test
    void failResetWithoutTemplateDisablesAtOnce() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ArenaMaps> maps = mockStatic(ArenaMaps.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            maps.when(() -> ArenaMaps.hasTemplate("Castle")).thenReturn(false);

            coordinator.failReset("no template");

            verify(scheduler, never()).runTaskLater(any(), any(Runnable.class), anyLong());
            verify(arena).disableBroken("no template");
        }
    }

    @Test
    void failResetFallsThroughToDisableWhenSchedulerRefuses() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ArenaMaps> maps = mockStatic(ArenaMaps.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            maps.when(() -> ArenaMaps.hasTemplate("Castle")).thenReturn(true);
            when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong())).thenThrow(new IllegalStateException("disabled"));

            coordinator.failReset("boom");

            verify(arena).disableBroken("boom");
        }
    }

    @Test
    void finishResetResetsTheRetryCounter() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ArenaMaps> maps = mockStatic(ArenaMaps.class);
             MockedStatic<ApiEvents> events = mockStatic(ApiEvents.class);
             MockedStatic<DecoUtils> deco = mockStatic(DecoUtils.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            maps.when(() -> ArenaMaps.hasTemplate("Castle")).thenReturn(true);

            for (int i = 0; i < MAX_RETRIES - 1; i++) coordinator.failReset("boom");
            coordinator.finishReset(mock(World.class));
            for (int i = 0; i < MAX_RETRIES; i++) coordinator.failReset("again");

            verify(scheduler, times(MAX_RETRIES * 2 - 1)).runTaskLater(any(), any(Runnable.class), anyLong());
            verify(arena, never()).disableBroken(any());
        }
    }

    @Test
    void finishResetOpensAnEnabledArenaAndKeepsADisabledOneClosed() {
        try (MockedStatic<ApiEvents> events = mockStatic(ApiEvents.class);
             MockedStatic<DecoUtils> deco = mockStatic(DecoUtils.class)) {
            World fresh = mock(World.class);
            when(arena.isEnabled()).thenReturn(true);
            coordinator.finishReset(fresh);
            verify(arena).rebindWorld(fresh);
            verify(arena).setStatus(GameStatus.waiting);

            when(arena.isEnabled()).thenReturn(false);
            coordinator.finishReset(fresh);
            verify(arena, times(1)).setStatus(GameStatus.restarting);
        }
    }

    @Test
    void retryTaskRunsThePipelineAgain() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ArenaMaps> maps = mockStatic(ArenaMaps.class);
             MockedStatic<com.l299l.newbedwars.world.template.ArenaResetPipeline> pipeline =
                     mockStatic(com.l299l.newbedwars.world.template.ArenaResetPipeline.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            maps.when(() -> ArenaMaps.hasTemplate("Castle")).thenReturn(true);
            when(scheduler.runTaskLater(any(), any(Runnable.class), eq(200L))).thenAnswer(inv -> {
                inv.<Runnable>getArgument(1).run();
                return null;
            });

            coordinator.failReset("boom");

            pipeline.verify(() -> com.l299l.newbedwars.world.template.ArenaResetPipeline.retry(arena));
        }
    }
}
