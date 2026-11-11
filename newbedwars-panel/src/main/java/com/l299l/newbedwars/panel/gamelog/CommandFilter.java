package com.l299l.newbedwars.panel.gamelog;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CommandFilter {
    public static final List<String> DEFAULT_IGNORED = List.of("lobby", "hub", "leave", "lang", "party", "p", "msg", "tell",
            "w", "whisper", "m", "r", "reply", "me", "list", "help", "?", "rejoin", "bw join", "bw rejoin", "bw lobby",
            "bw lang", "bw spectate", "bw stats", "bw profile", "bw leaderboard", "bw help");
    private static final Set<String> SECRET = Set.of("login", "l", "log", "register", "reg", "changepassword", "changepass",
            "cp", "unregister", "unreg", "2fa", "totp", "email", "premium");
    private static final int MAX_LENGTH = 256;

    private final Set<String> ignored = new HashSet<>();

    public CommandFilter(List<String> ignored) {
        for (String s : ignored) {
            String n = normalize(s);
            if (!n.isEmpty()) this.ignored.add(n);
        }
    }

    static String normalize(String s) {
        return String.join(" ", s.trim().toLowerCase(Locale.ROOT).replaceFirst("^/", "").split("\\s+"));
    }

    public String loggable(String message) {
        if (message == null) return null;
        String line = message.trim();
        if (line.startsWith("/")) line = line.substring(1);
        if (line.isEmpty()) return null;
        String[] parts = line.split("\\s+");
        String label = parts[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);
        if (label.isEmpty() || SECRET.contains(label) || ignored.contains(label)) return null;
        if (parts.length > 1 && ignored.contains(label + " " + parts[1].toLowerCase(Locale.ROOT))) return null;
        parts[0] = label;
        String cleaned = "/" + String.join(" ", parts);
        return cleaned.length() > MAX_LENGTH ? cleaned.substring(0, MAX_LENGTH) + "..." : cleaned;
    }
}
