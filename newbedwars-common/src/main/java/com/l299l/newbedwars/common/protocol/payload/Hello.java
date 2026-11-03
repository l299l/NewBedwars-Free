package com.l299l.newbedwars.common.protocol.payload;

public class Hello {
    public String serverId;
    public String mode;
    public String pluginVersion;

    public Hello() {}

    public Hello(String serverId, String mode, String pluginVersion) {
        this.serverId = serverId;
        this.mode = mode;
        this.pluginVersion = pluginVersion;
    }
}
