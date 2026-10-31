package com.l299l.newbedwars.network;

import com.l299l.newbedwars.common.protocol.NetMessage;
import org.bukkit.entity.Player;

/** Sends protocol messages to the proxy (plugin messages always travel through a connected player). */
public interface Messenger {
    /** Sends through any online player; false when nobody is online. */
    boolean send(NetMessage msg);

    /** Sends through {@code via}; false when that is not possible. */
    boolean send(Player via, NetMessage msg);

    /** Moves {@code player} to another server of the proxy ({@code BungeeCord Connect}). */
    void connect(Player player, String server);
}
