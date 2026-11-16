package com.l299l.newbedwars.api.registry;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.NewBedwarsAPI;

/** An action a game phase can run, registered with {@link NewBedwarsAPI#registerPhaseAction}. */
@FunctionalInterface
public interface PhaseActionHandler {
    /**
     * Runs the action on the server thread.
     *
     * @param arena    the arena whose phase ended
     * @param argument the text after the colon in {@code ACTION:argument}, or an empty string
     */
    void execute(ArenaView arena, String argument);
}
