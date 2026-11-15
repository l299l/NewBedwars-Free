package com.l299l.newbedwars.panel.network;

public final class PeerException extends Exception {
    public enum Kind { UNREACHABLE, UNTRUSTED, TIMEOUT }

    private final Kind kind;

    public PeerException(Kind kind, Throwable cause) {
        super(kind.name(), cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
