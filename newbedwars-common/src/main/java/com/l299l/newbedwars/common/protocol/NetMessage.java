package com.l299l.newbedwars.common.protocol;

import java.util.Objects;
import java.util.UUID;

/** A protocol envelope: version, type, sender server id, unique id and the typed payload. */
public final class NetMessage {
    /** Current protocol version. Messages with another version are dropped. */
    public static final int VERSION = 1;

    private final int v;
    private final MessageType type;
    private final String from;
    private final String id;
    private final Object payload;

    public NetMessage(MessageType type, String from, Object payload) {
        this(VERSION, type, from, UUID.randomUUID().toString(), payload);
    }

    NetMessage(int v, MessageType type, String from, String id, Object payload) {
        this.v = v;
        this.type = Objects.requireNonNull(type, "type");
        this.from = from;
        this.id = id;
        this.payload = Objects.requireNonNull(payload, "payload");
        if (!type.payloadClass().isInstance(payload)) {
            throw new IllegalArgumentException(type + " needs a " + type.payloadClass().getSimpleName());
        }
    }

    public int version() { return v; }
    public MessageType type() { return type; }
    /** Server id of the sender ("proxy" for the proxy). */
    public String from() { return from; }
    public String id() { return id; }

    public Object payload() { return payload; }

    /** The payload cast to {@code cls}; throws when the type does not match. */
    public <T> T payload(Class<T> cls) {
        return cls.cast(payload);
    }

    public static NetMessage of(String from, Object payload) {
        for (MessageType t : MessageType.values()) {
            if (t.payloadClass() == payload.getClass()) return new NetMessage(t, from, payload);
        }
        throw new IllegalArgumentException("Not a protocol payload: " + payload.getClass());
    }
}
