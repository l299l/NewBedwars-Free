package com.l299l.newbedwars.panel.auth;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public final class SessionManager {
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Session(String id, String user, String csrf, long created, long lastSeen) {}

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final long idleMillis;
    private final long maxMillis;
    private final LongSupplier clock;

    public SessionManager(long idleMillis, long maxMillis, LongSupplier clock) {
        this.idleMillis = idleMillis;
        this.maxMillis = maxMillis;
        this.clock = clock;
    }

    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public Session create(String user) {
        long now = clock.getAsLong();
        Session session = new Session(randomToken(), Account.key(user), randomToken(), now, now);
        sessions.put(session.id(), session);
        return session;
    }

    public Optional<Session> get(String id) {
        if (id == null) return Optional.empty();
        Session session = sessions.get(id);
        if (session == null) return Optional.empty();
        long now = clock.getAsLong();
        if (expired(session, now)) {
            sessions.remove(id);
            return Optional.empty();
        }
        Session touched = new Session(session.id(), session.user(), session.csrf(), session.created(), now);
        sessions.replace(id, session, touched);
        return Optional.of(touched);
    }

    private boolean expired(Session session, long now) {
        return now - session.lastSeen() > idleMillis || now - session.created() > maxMillis;
    }

    public void remove(String id) {
        if (id != null) sessions.remove(id);
    }

    public void removeUser(String user, String keepId) {
        String key = Account.key(user);
        sessions.values().removeIf(s -> s.user().equals(key) && !s.id().equals(keepId));
    }

    public int count() {
        cleanup();
        return sessions.size();
    }

    public void cleanup() {
        long now = clock.getAsLong();
        sessions.values().removeIf(s -> expired(s, now));
    }
}
