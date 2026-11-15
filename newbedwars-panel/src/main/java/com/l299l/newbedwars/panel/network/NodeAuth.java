package com.l299l.newbedwars.panel.network;

import com.l299l.newbedwars.panel.auth.Permission;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongSupplier;

public final class NodeAuth {
    public static final String TIME = "X-NBW-Time";
    public static final String NONCE = "X-NBW-Nonce";
    public static final String SIGNATURE = "X-NBW-Sig";
    public static final String ACTOR = "X-NBW-Actor";
    public static final String PERMISSIONS = "X-NBW-Perms";
    public static final String IP = "X-NBW-Ip";
    public static final String GATEWAY = "X-NBW-Gateway";
    public static final String BODY = "X-NBW-Body";
    public static final String TARGET = "X-NBW-Target";
    public static final String UNSIGNED = "UNSIGNED";
    public static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_NONCES = 50_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Identity(String actor, Set<String> permissions, String ip, String gateway) {
        public String account() {
            return actor + "@" + gateway;
        }
    }

    public record Call(String method, String path, String query, long time, String nonce, Identity identity, String body,
                       String target) {}

    public enum Failure { MALFORMED, CLOCK, SIGNATURE, REPLAY, TARGET }

    public record Verdict(Identity identity, Failure failure, String body) {
        public boolean ok() {
            return failure == null;
        }
    }

    private NodeAuth() {}

    public static Set<String> forwardable(Set<String> permissions) {
        Set<String> result = new LinkedHashSet<>();
        for (String p : Permission.ALL) {
            if (permissions.contains(p) && !Permission.LOCAL_ONLY.contains(p)) result.add(p);
        }
        return result;
    }

    private static String canonical(Set<String> permissions) {
        return String.join(",", forwardable(permissions));
    }

    public static String nonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return hex(bytes);
    }

    public static String sha256(byte[] data) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sign(String secret, Call call) {
        return sign(secret, call, canonical(call.identity().permissions()));
    }

    static String sign(String secret, Call call, String permissions) {
        String text = String.join("\n", "NBW2", call.method(), call.path(), call.query() == null ? "" : call.query(),
                Long.toString(call.time()), call.nonce(), call.identity().actor(), permissions,
                call.identity().ip() == null ? "" : call.identity().ip(), call.identity().gateway(), call.body(), call.target());
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return hex(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Map<String, String> headers(String secret, Call call) {
        Map<String, String> headers = new java.util.LinkedHashMap<>();
        headers.put(TIME, Long.toString(call.time()));
        headers.put(NONCE, call.nonce());
        headers.put(ACTOR, call.identity().actor());
        headers.put(PERMISSIONS, canonical(call.identity().permissions()));
        headers.put(IP, call.identity().ip() == null ? "" : call.identity().ip());
        headers.put(GATEWAY, call.identity().gateway());
        headers.put(BODY, call.body());
        headers.put(TARGET, call.target());
        headers.put(SIGNATURE, sign(secret, call));
        return headers;
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format("%02x", b));
        return out.toString();
    }

    public static final class Verifier {
        private final String secret;
        private final String selfId;
        private final LongSupplier clock;
        private final Map<String, Long> nonces = new ConcurrentHashMap<>();

        public Verifier(String secret, String selfId, LongSupplier clock) {
            this.secret = secret;
            this.selfId = selfId;
            this.clock = clock;
        }

        public String selfId() {
            return selfId;
        }

        public Verdict verify(String method, String path, String query, Function<String, String> header) {
            String signature = header.apply(SIGNATURE);
            String nonce = header.apply(NONCE);
            String actor = header.apply(ACTOR);
            String gateway = header.apply(GATEWAY);
            String body = header.apply(BODY);
            String perms = header.apply(PERMISSIONS);
            String ip = header.apply(IP);
            String target = header.apply(TARGET);
            long time;
            try {
                time = Long.parseLong(String.valueOf(header.apply(TIME)).trim());
            } catch (NumberFormatException e) {
                return fail(Failure.MALFORMED);
            }
            if (signature == null || nonce == null || actor == null || gateway == null || body == null || perms == null
                    || ip == null || target == null || !clean(actor) || !clean(gateway) || !clean(nonce) || !clean(perms)
                    || !clean(ip) || !clean(body) || !clean(target) || nonce.length() < 16 || nonce.length() > 64
                    || actor.isEmpty() || gateway.isEmpty()) {
                return fail(Failure.MALFORMED);
            }
            Set<String> permissions = new HashSet<>();
            for (String p : perms.split(",")) {
                if (!p.isBlank() && Permission.isKnown(p.trim())) permissions.add(p.trim());
            }
            Identity identity = new Identity(actor, forwardable(permissions), ip, gateway);
            Call call = new Call(method, path, query, time, nonce, identity, body, target);
            String expected = sign(secret, call, perms);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
                return fail(Failure.SIGNATURE);
            }
            long now = clock.getAsLong();
            if (Math.abs(now - time) > WINDOW_MILLIS) return fail(Failure.CLOCK);
            if (!target.equals(selfId)) return new Verdict(null, Failure.TARGET, target);
            purge(now);
            if (nonces.size() >= MAX_NONCES || nonces.putIfAbsent(nonce, now) != null) return fail(Failure.REPLAY);
            return new Verdict(identity, null, body);
        }

        private void purge(long now) {
            if (nonces.size() < 1000) return;
            nonces.values().removeIf(seen -> now - seen > 2 * WINDOW_MILLIS);
        }

        private static boolean clean(String value) {
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c < 0x20 || c == 0x7f) return false;
            }
            return true;
        }

        private static Verdict fail(Failure failure) {
            return new Verdict(null, failure, null);
        }
    }
}
