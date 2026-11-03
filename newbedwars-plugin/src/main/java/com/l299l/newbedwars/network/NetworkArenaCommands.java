package com.l299l.newbedwars.network;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class NetworkArenaCommands {

    public static final List<String> ALLOWED = List.of("list", "info");

    private NetworkArenaCommands() {}

    public static void perform(Player p, String sub) {
        Messages msg = NewBedwars.plugin.getMessages();
        if ("list".equalsIgnoreCase(sub)) {
            list(p);
        } else if ("info".equalsIgnoreCase(sub)) {
            for (String line : msg.getMsg(p, "NetworkArenaInfo").split("\n")) p.sendMessage(line);
        } else if (sub == null) {
            msg.send(p, "NetworkArenaUsage");
        } else {
            msg.send(p, "NetworkArenaOnGameServer", Map.of("/sub/", sub));
        }
    }

    private static void list(Player p) {
        Messages msg = NewBedwars.plugin.getMessages();
        NetworkManager network = NetworkManager.get();
        List<RemoteArena> arenas = network == null ? List.of() : network.getRemoteArenas().all();
        if (arenas.isEmpty()) {
            msg.send(p, "NetworkArenaListEmpty");
            return;
        }
        Map<String, List<RemoteArena>> byServer = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (RemoteArena a : arenas) byServer.computeIfAbsent(a.getServer(), k -> new java.util.ArrayList<>()).add(a);
        msg.send(p, "NetworkArenaListHeader", Map.of("/arenas/", String.valueOf(arenas.size()),
                "/servers/", String.valueOf(byServer.size())));
        for (Map.Entry<String, List<RemoteArena>> e : byServer.entrySet()) {
            msg.send(p, "NetworkArenaListServer", Map.of("/server/", e.getKey()));
            for (RemoteArena a : e.getValue()) {
                ArenaInfo i = a.info();
                msg.send(p, "NetworkArenaListEntry", Map.of(
                        "/arena/", a.getName(),
                        "/group/", i.group == null ? "-" : i.group,
                        "/state/", i.state == null ? "?" : i.state.name(),
                        "/players/", String.valueOf(i.players),
                        "/max/", String.valueOf(i.maxPlayers),
                        "/teams/", i.teams + "x" + i.maxInTeam));
            }
        }
    }
}
