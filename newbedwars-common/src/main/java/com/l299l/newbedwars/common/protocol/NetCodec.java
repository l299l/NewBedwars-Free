package com.l299l.newbedwars.common.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.l299l.newbedwars.common.protocol.payload.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class NetCodec {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private NetCodec() {}

    public static byte[] encode(NetMessage msg) {
        return toJson(msg).getBytes(StandardCharsets.UTF_8);
    }

    public static String toJson(NetMessage msg) {
        JsonObject o = new JsonObject();
        o.addProperty("v", msg.version());
        o.addProperty("type", msg.type().name());
        if (msg.from() != null) o.addProperty("from", msg.from());
        if (msg.id() != null) o.addProperty("id", msg.id());
        o.add("payload", GSON.toJsonTree(msg.payload()));
        return GSON.toJson(o);
    }

    public static NetMessage decode(byte[] data) {
        if (data == null) return null;
        return fromJson(new String(data, StandardCharsets.UTF_8));
    }

    public static NetMessage fromJson(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return null;
            JsonObject o = root.getAsJsonObject();
            if (!o.has("v") || o.get("v").getAsInt() != NetMessage.VERSION) return null;
            MessageType type = MessageType.valueOf(o.get("type").getAsString());
            JsonElement payloadJson = o.get("payload");
            if (payloadJson == null || !payloadJson.isJsonObject()) return null;
            Object payload = GSON.fromJson(payloadJson, type.payloadClass());
            if (payload == null) return null;
            normalize(payload);
            String from = o.has("from") ? o.get("from").getAsString() : null;
            String id = o.has("id") ? o.get("id").getAsString() : null;
            return new NetMessage(NetMessage.VERSION, type, from, id, payload);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String payloadToJson(Object payload) {
        return GSON.toJson(payload);
    }

    public static <T> T payloadFromJson(String json, Class<T> cls) {
        try {
            T payload = GSON.fromJson(json, cls);
            if (payload != null) normalize(payload);
            return payload;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void normalize(Object payload) {
        if (payload instanceof ServerState s) {
            normalizeState(s);
        } else if (payload instanceof NetworkState n) {
            n.servers = clean(n.servers, s -> s != null && validName(s.serverId), Limits.MAX_SERVERS);
            n.servers.forEach(NetCodec::normalizeState);
        } else if (payload instanceof PartySync p) {
            p.members = clean(p.members, m -> m != null && m.uuid != null, Limits.MAX_PLAYERS_PER_MESSAGE);
            p.removed = clean(p.removed, u -> u != null, Limits.MAX_PLAYERS_PER_MESSAGE);
        } else if (payload instanceof JoinRequest r) {
            r.players = clean(r.players, u -> u != null, Limits.MAX_PLAYERS_PER_MESSAGE);
        } else if (payload instanceof SendToLobby l) {
            l.players = clean(l.players, u -> u != null, Limits.MAX_PLAYERS_PER_MESSAGE);
        } else if (payload instanceof JoinTicket t) {
            t.party = clean(t.party, u -> u != null, Limits.MAX_PLAYERS_PER_MESSAGE);
        } else if (payload instanceof NetworkGroups g) {
            g.groups = clean(g.groups, x -> x != null && validName(x.name), Limits.MAX_GROUPS);
            for (NetworkGroups.Group x : g.groups) {
                x.arenas = clean(x.arenas, a -> a != null && a.length() <= Limits.MAX_ENTRY_LENGTH, Limits.MAX_GROUP_ENTRIES);
                if (x.displayItem != null && x.displayItem.length() > Limits.MAX_NAME_LENGTH) x.displayItem = null;
            }
        }
    }

    private static void normalizeState(ServerState s) {
        s.arenas = clean(s.arenas, a -> a != null && validName(a.name), Limits.MAX_ARENAS_PER_SERVER);
        if (s.serverId != null && s.serverId.length() > Limits.MAX_NAME_LENGTH) s.serverId = null;
        if (s.mode != null && s.mode.length() > Limits.MAX_NAME_LENGTH) s.mode = null;
        int rejoinLeft = Limits.MAX_REJOIN_PER_SERVER;
        for (ArenaInfo a : s.arenas) {
            if (a.group != null && a.group.length() > Limits.MAX_NAME_LENGTH) a.group = null;
            a.rejoin = clean(a.rejoin, r -> r != null, Math.min(Limits.MAX_REJOIN_PER_ARENA, rejoinLeft));
            rejoinLeft -= a.rejoin.size();
        }
    }

    private static boolean validName(String name) {
        return name != null && name.length() <= Limits.MAX_NAME_LENGTH;
    }

    private static <T> List<T> clean(List<T> list, Predicate<T> keep, int max) {
        List<T> out = new ArrayList<>();
        if (list != null) {
            for (T t : list) {
                if (out.size() >= max) break;
                if (keep.test(t)) out.add(t);
            }
        }
        return out;
    }
}
