package com.l299l.newbedwars.proxy.core;

import com.l299l.newbedwars.common.protocol.NetMessage;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** What the proxy core needs from the proxy software (Velocity now, BungeeCord later). */
public interface ProxyPlatform {
    boolean isOnline(UUID player);

    /** Name of an online player, or null. */
    String nameOf(UUID player);

    /** An online player by name (case-insensitive), or null. */
    UUID findOnline(String name);

    /** The server the player is on, or null. */
    String currentServer(UUID player);

    /** Sends a translated message ({@code key} from the proxy language files) to a player. */
    void message(UUID player, String key, Map<String, String> replacements);

    /** Clickable accept/deny buttons for a party invite. */
    void inviteButtons(UUID player);

    /** Sends to the backend server the player is connected to, through that connection. */
    boolean sendToPlayerServer(UUID player, NetMessage msg);

    /** Sends to a backend server through any player on it; false when nobody is there. */
    boolean sendToServer(String server, NetMessage msg);

    /** Moves a player to a server; {@code done} gets whether it worked. */
    void connect(UUID player, String server, Consumer<Boolean> done);

    /** Players connected to a server (0 for unknown servers). */
    int playerCount(String server);

    /** Whether a server with this name is registered on the proxy. */
    boolean serverExists(String server);
}
