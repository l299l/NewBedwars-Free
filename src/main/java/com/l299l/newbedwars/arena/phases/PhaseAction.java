package com.l299l.newbedwars.arena.phases;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.GeneratorType;

public record PhaseAction(String action) {

    public void execute(IArena arena) {
       String[] args = formatAction(action);
       String action = args[0];
       String arg = args[1];
        switch (action) {
            case "DIAMOND_GENERATOR" -> arena.upgradeGenerators(GeneratorType.DIAMOND, Integer.parseInt(arg));
            case "EMERALD_GENERATOR" -> arena.upgradeGenerators(GeneratorType.EMERALD, Integer.parseInt(arg));
            case "BED_DESTRUCTION" -> arena.bedDestruction();
            case "END_GAME", "GAME_END" -> arena.endGame(arg);
            case "ENDER_DRAGON_END" -> arena.enderDragonEnd();
            case "ENDER_DRAGON_START" -> arena.enderDragonStart();
            case "DISABLE_REGENERATION" -> arena.disableRegeneration();
            case "DISABLE_TEAMS_FORGE" -> arena.disableTeamsForge();
        }
    }

    private String[] formatAction(String action) {
        action = action.replace(" ", "");
        String[] args = action.split(":");
        if (args.length == 1) {
            args = new String[]{args[0], ""};
        }
        return args;
    }
}
