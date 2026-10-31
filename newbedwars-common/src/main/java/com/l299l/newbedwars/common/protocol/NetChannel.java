package com.l299l.newbedwars.common.protocol;

/** Plugin messaging channel names used by the network mode. */
public final class NetChannel {
    private NetChannel() {}

    /** Channel between the NewBedwars plugin on backend servers and the NewBedwars proxy plugin. */
    public static final String MAIN = "newbedwars:main";
    /** BungeeCord-compatible channel (also understood by Velocity), used for {@code Connect}. */
    public static final String BUNGEE = "BungeeCord";
}
