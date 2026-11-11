package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.game.GameControl;
import com.l299l.newbedwars.api.maintenance.MaintenanceResult;
import com.l299l.newbedwars.arena.game.GameControlService;
import com.l299l.newbedwars.arena.game.GameControlService.Action;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class ApiGameControl implements GameControl {

    static MaintenanceResult toResult(GameControlService.Outcome outcome) {
        String text = NewBedwars.plugin.getMessages().getMsgToConsole(outcome.key());
        for (Map.Entry<String, String> e : outcome.replacements().entrySet()) text = text.replace(e.getKey(), e.getValue());
        return new MaintenanceResult(outcome.code(), outcome.arena(), ChatColor.stripColor(text), null);
    }

    private static CompletableFuture<MaintenanceResult> run(Supplier<GameControlService.Outcome> action) {
        return ApiMaintenance.onMain(() -> CompletableFuture.completedFuture(toResult(action.get())));
    }

    @Override
    public CompletableFuture<MaintenanceResult> start(String arena, String actor) {
        return run(() -> GameControlService.run(arena, Action.START, actor));
    }

    @Override
    public CompletableFuture<MaintenanceResult> forceStart(String arena, String actor) {
        return run(() -> GameControlService.run(arena, Action.FORCE_START, actor));
    }

    @Override
    public CompletableFuture<MaintenanceResult> stop(String arena, String actor) {
        return run(() -> GameControlService.run(arena, Action.STOP, actor));
    }

    @Override
    public CompletableFuture<MaintenanceResult> forceStop(String arena, String actor) {
        return run(() -> GameControlService.run(arena, Action.FORCE_STOP, actor));
    }

    @Override
    public CompletableFuture<MaintenanceResult> nextPhase(String arena, String actor) {
        return run(() -> GameControlService.run(arena, Action.NEXT_PHASE, actor));
    }

    @Override
    public CompletableFuture<MaintenanceResult> broadcast(String arena, String actor, String message) {
        return run(() -> GameControlService.broadcast(arena, actor, message));
    }
}
