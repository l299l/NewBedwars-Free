package com.l299l.newbedwars.proxy.core;

import com.l299l.newbedwars.common.protocol.NetMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class FakePlatform implements ProxyPlatform {
    public final Map<UUID, String> names = new HashMap<>();
    public final Map<UUID, String> servers = new HashMap<>();
    public final Set<String> knownServers = new HashSet<>();
    public final List<String> messages = new ArrayList<>();
    public final List<NetMessage> toPlayerServer = new ArrayList<>();
    public final List<UUID> toPlayerServerVia = new ArrayList<>();
    public final List<String> toServer = new ArrayList<>();
    public final List<String> connects = new ArrayList<>();
    public final Set<String> failing = new HashSet<>();
    public boolean completeConnects = true;

    public UUID add(String name, String server) {
        UUID id = UUID.nameUUIDFromBytes(name.getBytes());
        names.put(id, name);
        servers.put(id, server);
        knownServers.add(server);
        return id;
    }

    @Override public boolean isOnline(UUID p) { return names.containsKey(p); }
    @Override public String nameOf(UUID p) { return names.get(p); }

    @Override
    public UUID findOnline(String name) {
        for (Map.Entry<UUID, String> e : names.entrySet()) if (e.getValue().equalsIgnoreCase(name)) return e.getKey();
        return null;
    }

    @Override public String currentServer(UUID p) { return servers.get(p); }

    @Override
    public void message(UUID p, String key, Map<String, String> r) {
        messages.add(names.get(p) + ":" + key + (r.isEmpty() ? "" : new java.util.TreeMap<>(r).toString()));
    }

    @Override public void inviteButtons(UUID p) { messages.add(names.get(p) + ":buttons"); }

    @Override
    public boolean sendToPlayerServer(UUID p, NetMessage msg) {
        toPlayerServer.add(msg);
        toPlayerServerVia.add(p);
        return true;
    }

    @Override
    public boolean sendToServer(String server, NetMessage msg) {
        toServer.add(server + ":" + msg.type());
        return true;
    }

    @Override
    public void connect(UUID p, String server, Consumer<Boolean> done) {
        connects.add(names.get(p) + "->" + server);
        if (failing.contains(server)) {
            done.accept(false);
            return;
        }
        if (completeConnects) {
            servers.put(p, server);
            done.accept(true);
        }
    }

    @Override
    public int playerCount(String server) {
        int n = 0;
        for (String s : servers.values()) if (server.equals(s)) n++;
        return n;
    }

    @Override public boolean serverExists(String server) { return knownServers.contains(server); }

    public boolean has(String message) {
        return messages.contains(message);
    }
}
