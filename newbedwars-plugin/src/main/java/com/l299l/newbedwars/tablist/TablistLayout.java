package com.l299l.newbedwars.tablist;

public record TablistLayout(String id, TablistSection waiting, TablistSection playing, TablistSection spectator) {

    public enum State {
        WAITING,
        PLAYING,
        SPECTATOR
    }

    public TablistSection resolve(State state) {
        return switch (state) {
            case WAITING -> waiting.merge(playing);
            case PLAYING -> playing.merge(waiting);
            case SPECTATOR -> spectator.merge(playing).merge(waiting);
        };
    }
}
