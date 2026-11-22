package com.l299l.newbedwars.proxy.core;

import com.l299l.newbedwars.common.protocol.NetMessage;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public interface ProxyPlatform {
    boolean isOnline(UUID player);

    String nameOf(UUID player);

    UUID findOnline(String name);

    String currentServer(UUID player);

    void message(UUID player, String key, Map<String, String> replacements);

    void inviteButtons(UUID player);

    boolean sendToPlayerServer(UUID player, NetMessage msg);

    boolean sendToServer(String server, NetMessage msg);

    void connect(UUID player, String server, Consumer<Boolean> done);

    int playerCount(String server);

    boolean serverExists(String server);
}
