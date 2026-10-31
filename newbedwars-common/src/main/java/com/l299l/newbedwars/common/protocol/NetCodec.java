package com.l299l.newbedwars.common.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;

/**
 * JSON encoding of {@link NetMessage}s: {@code {"v":1,"type":"...","from":"...","id":"...","payload":{...}}}.
 * Decoding never throws: malformed input, unknown types and other protocol versions give {@code null}.
 */
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
            String from = o.has("from") ? o.get("from").getAsString() : null;
            String id = o.has("id") ? o.get("id").getAsString() : null;
            return new NetMessage(NetMessage.VERSION, type, from, id, payload);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Plain Gson for payload objects stored outside an envelope (e.g. the MySQL status table). */
    public static String payloadToJson(Object payload) {
        return GSON.toJson(payload);
    }

    public static <T> T payloadFromJson(String json, Class<T> cls) {
        try {
            return GSON.fromJson(json, cls);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
