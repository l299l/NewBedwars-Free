package com.l299l.newbedwars.arena.phases;

import com.l299l.newbedwars.arena.IArena;

import java.util.List;

public class GamePhases {
    private final String ID;
    private final List<Phase> phases;
    private Integer currentPhase = 0;

    public GamePhases(String ID, List<Phase> phases) {
        this.ID = ID;
        this.phases = phases;
    }

    /** Creates an independent copy so each arena has its own phase state. */
    public GamePhases(GamePhases source) {
        this.ID = source.ID;
        this.phases = source.phases;
        this.currentPhase = 0;
    }

    public void start(IArena arena) {
        phases.get(currentPhase).start(arena);
    }

    public void stop() {
        if (currentPhase < phases.size()) {
            phases.get(currentPhase).cancel();
        }
        currentPhase = 0;
    }

    public String getID() {
        return ID;
    }

    public Phase getCurrentPhase() {
        return phases.get(currentPhase);
    }

    public void nextPhase(IArena arena) {
        if (isLastPhase()) return;
        currentPhase++;
        phases.get(currentPhase).start(arena);
    }

    public Phase getNextPhase() {
        if (!hasNextPhase()) {
            return null;
        }
        return phases.get(currentPhase + 1);
    }

    public boolean isLastPhase() {
        return currentPhase >= phases.size() - 1;
    }

    private boolean hasNextPhase() {
        return currentPhase + 1 < phases.size();
    }
}
