package com.l299l.newbedwars.bossbar;

import com.l299l.newbedwars.bossbar.actions.EndGameBossBarAction;

import java.util.HashMap;

public class BarActionManager {
    private final HashMap<String, BarAction> actions;

    public BarActionManager() {
        actions = new HashMap<String, BarAction>();
        EndGameBossBarAction endGameBossBarAction = new EndGameBossBarAction();
        actions.put(endGameBossBarAction.getName(), endGameBossBarAction);
    }

    public BarAction getAction(String name) {
        if (!actions.containsKey(name)) {
            return null;
        }
        return actions.get(name);
    }
}
