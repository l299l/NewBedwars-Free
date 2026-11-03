package com.l299l.newbedwars.common.protocol;

public final class Limits {
    public static final int MAX_NAME_LENGTH = 64;
    public static final int MAX_ENTRY_LENGTH = 2 * MAX_NAME_LENGTH + 1;
    public static final int MAX_ARENAS_PER_SERVER = 64;
    public static final int MAX_REJOIN_PER_ARENA = 64;
    public static final int MAX_REJOIN_PER_SERVER = 256;
    public static final int MAX_SERVERS = 128;
    public static final int MAX_GROUPS = 64;
    public static final int MAX_GROUP_ENTRIES = 256;
    public static final int MAX_PLAYERS_PER_MESSAGE = 64;
    public static final int MAX_DB_ROWS = 256;
    public static final int MAX_STATE_JSON_LENGTH = 65536;
    public static final int MAX_PAYLOAD_BYTES = 30000;

    private Limits() {}
}
