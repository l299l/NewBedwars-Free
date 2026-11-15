package com.l299l.newbedwars.panel.network;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public record NetworkConfig(NetworkRole role, String id, String secret, boolean nodeLogin, List<Peer> peers,
                            List<String> rejectedPeers) {
    public static final int MIN_SECRET = 32;
    public static final String SYSTEM_CERTIFICATES = "system";
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final Pattern FINGERPRINT = Pattern.compile("[0-9A-F]{64}");

    public record Peer(String id, String url, String fingerprint) {
        public boolean systemCertificates() {
            return SYSTEM_CERTIFICATES.equals(fingerprint);
        }

        public boolean https() {
            return url.startsWith("https://");
        }
    }

    public static final NetworkConfig DEFAULTS = new NetworkConfig(NetworkRole.STANDALONE, "", "", false, List.of(), List.of());

    public static NetworkConfig read(Function<String, Object> get) {
        NetworkRole role = NetworkRole.parse(get.apply("Network.Role"));
        String id = text(get.apply("Network.Id"));
        if (!id.isEmpty() && !isValidId(id)) id = "";
        String secret = text(get.apply("Network.Secret"));
        boolean nodeLogin = get.apply("Network.NodeLogin") instanceof Boolean b && b;
        List<Peer> peers = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        if (get.apply("Network.Peers") instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String peerId = String.valueOf(entry.getKey());
                Peer peer = entry.getValue() instanceof Map<?, ?> values ? peer(peerId, values) : null;
                if (peer == null) rejected.add(peerId);
                else peers.add(peer);
            }
        }
        return new NetworkConfig(role, id, secret, nodeLogin, List.copyOf(peers), List.copyOf(rejected));
    }

    private static Peer peer(String id, Map<?, ?> values) {
        if (!isValidId(id)) return null;
        String url = text(values.get("Url"));
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null
                    || uri.getRawPath() != null && !uri.getRawPath().isEmpty()) return null;
            url = scheme + url.substring(scheme.length());
        } catch (IllegalArgumentException e) {
            return null;
        }
        String fingerprint = normalizeFingerprint(text(values.get("Fingerprint")));
        if (fingerprint == null) return null;
        return new Peer(id, url, fingerprint);
    }

    public static String normalizeFingerprint(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return "";
        if (value.equalsIgnoreCase(SYSTEM_CERTIFICATES)) return SYSTEM_CERTIFICATES;
        String hex = value.replace(":", "").replace(" ", "").toUpperCase(Locale.ROOT);
        return FINGERPRINT.matcher(hex).matches() ? hex : null;
    }

    public static boolean isValidId(String id) {
        return id != null && ID.matcher(id).matches();
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    public boolean hasUsableSecret() {
        return secret.length() >= MIN_SECRET;
    }
}
