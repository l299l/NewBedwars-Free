package com.l299l.newbedwars.world;

public interface ArenaWorldCreator {

    boolean createWorld(String worldName);
    boolean createWorldFromSchematic(String worldName, String schematicName);
}
