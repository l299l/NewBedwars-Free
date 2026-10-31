package com.l299l.newbedwars.common.protocol.payload;

/** One arena of a game server, as seen by lobbies and the proxy. */
public class ArenaInfo {
    /** Arena states on the wire (independent of the plugin's internal enum). */
    public enum State { WAITING, STARTING, PLAYING, ENDING, RESTARTING, DISABLED }

    public String name;
    /** Arena group name, or null. */
    public String group;
    public State state;
    public int players;
    public int maxPlayers;
    public int maxInTeam;
    public int teams;
    public boolean allowParties;
    /** Players who left this running game and may still rejoin it. */
    public java.util.List<RejoinSlot> rejoin = new java.util.ArrayList<>();

    public ArenaInfo() {}

    public ArenaInfo(String name, String group, State state, int players, int maxPlayers,
                     int maxInTeam, int teams, boolean allowParties) {
        this.name = name;
        this.group = group;
        this.state = state;
        this.players = players;
        this.maxPlayers = maxPlayers;
        this.maxInTeam = maxInTeam;
        this.teams = teams;
        this.allowParties = allowParties;
    }

    /** The rejoin slot of {@code player} that is not over at {@code now}, or null. */
    public RejoinSlot rejoinSlot(java.util.UUID player, long now) {
        if (rejoin == null || player == null) return null;
        for (RejoinSlot s : rejoin) {
            if (s != null && player.equals(s.player) && (s.expiresAt <= 0 || s.expiresAt > now)) return s;
        }
        return null;
    }

    /** True when the arena takes new players (waiting or starting). */
    public boolean isJoinable() {
        return state == State.WAITING || state == State.STARTING;
    }

    public int freeSlots() {
        return Math.max(0, maxPlayers - players);
    }

    /** Whether {@code needed} players (a party when more than 1) can join together. */
    public boolean fits(int needed) {
        if (!isJoinable() || freeSlots() < needed) return false;
        return needed <= 1 || (allowParties && needed <= maxInTeam);
    }
}
