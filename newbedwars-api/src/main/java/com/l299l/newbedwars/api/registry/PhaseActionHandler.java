package com.l299l.newbedwars.api.registry;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.NewBedwarsAPI;

/** An action a game phase can run, registered with {@link NewBedwarsAPI#registerPhaseAction}. */
@FunctionalInterface
public interface PhaseActionHandler {
    /**
     * @param arena the arena whose phase runs the action
     * @param argument the text after the colon in {@code ACTION:argument} in config.yml, or "" when there is none
     */
    void execute(ArenaView arena, String argument);
}
