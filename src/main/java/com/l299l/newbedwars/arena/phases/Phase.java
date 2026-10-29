package com.l299l.newbedwars.arena.phases;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class Phase {
    private final String name;
    private final List<PhaseAction> actions;
    private final Integer initialDuration;
    private Integer duration;
    private BukkitTask task;

    public Phase(String name, Integer duration, List<PhaseAction> actions) {
        this.name = name;
        this.initialDuration = duration;
        this.duration = duration;
        this.actions = actions;
    }
    /** A fresh phase with the same settings; running state (duration, timer) is never shared between arenas. */
    public Phase copy() {
        return new Phase(name, initialDuration, actions);
    }

    public void start(IArena arena) {
        cancel();
        duration = initialDuration;
        if (duration > 0) {
            startTimer(arena);
        }
        // a failing action must not skip the others or stop the timer above
        for (PhaseAction action : actions) {
            try {
                action.execute(arena);
            } catch (RuntimeException e) {
                NewBedwars.plugin.getLogger().warning("Phase '" + name + "' action failed: " + e);
            }
        }
    }

    private void startTimer(IArena arena) {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (duration == 0) {
                    cancel();
                    arena.nextPhase();
                } else {
                    duration--;
                }
                arena.setPhaseTime(duration);
            }
        }.runTaskTimer(NewBedwars.plugin, 0, 20);
    }

    public void cancel() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public String getName() {
        return name;
    }

    public Integer getDuration() {
        return duration;
    }
}
