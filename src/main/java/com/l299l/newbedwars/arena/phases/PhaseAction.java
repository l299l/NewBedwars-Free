package com.l299l.newbedwars.arena.phases;

import com.l299l.newbedwars.arena.IArena;

import java.util.function.BiConsumer;

public record PhaseAction(String action) {

    public void execute(IArena arena) {
        String[] args = formatAction(action);
        BiConsumer<IArena, String> handler = PhaseActionRegistry.get(args[0]);
        if (handler != null) handler.accept(arena, args[1]);
    }

    private String[] formatAction(String action) {
        action = action.replace(" ", "");
        String[] args = action.split(":", 2);
        if (args.length == 1) {
            args = new String[]{args[0], ""};
        }
        return args;
    }
}
