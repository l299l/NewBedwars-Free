package com.l299l.newbedwars.common.protocol.payload;

/** A backend server announces itself to the proxy. */
public class Hello {
    public String serverId;
    /** {@code GAME} or {@code LOBBY}. */
    public String mode;
    public String pluginVersion;

    public Hello() {}

    public Hello(String serverId, String mode, String pluginVersion) {
        this.serverId = serverId;
        this.mode = mode;
        this.pluginVersion = pluginVersion;
    }
}
