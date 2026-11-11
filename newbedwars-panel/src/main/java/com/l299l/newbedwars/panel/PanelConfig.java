package com.l299l.newbedwars.panel;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public record PanelConfig(String bind, int port, boolean https, String keystore, String keystorePassword,
                          List<String> hostnames, boolean trustProxyHeaders, int sessionIdleMinutes,
                          int sessionMaxHours, int loginMaxAttempts, int loginLockMinutes, int uploadMaxMegabytes,
                          String language, List<String> ignoredCommands) {

    public static final PanelConfig DEFAULTS = new PanelConfig("0.0.0.0", 8150, true, "", "", List.of(), false,
            30, 12, 5, 10, 512, "en", com.l299l.newbedwars.panel.gamelog.CommandFilter.DEFAULT_IGNORED);

    public static PanelConfig read(Function<String, Object> get) {
        return new PanelConfig(
                string(get.apply("Bind"), DEFAULTS.bind),
                range(get.apply("Port"), 1, 65535, DEFAULTS.port),
                bool(get.apply("Https.Enabled"), DEFAULTS.https),
                string(get.apply("Https.Keystore"), ""),
                string(get.apply("Https.KeystorePassword"), ""),
                strings(get.apply("Https.Hostnames")),
                bool(get.apply("TrustProxyHeaders"), DEFAULTS.trustProxyHeaders),
                range(get.apply("Session.IdleMinutes"), 1, 24 * 60, DEFAULTS.sessionIdleMinutes),
                range(get.apply("Session.MaxHours"), 1, 24 * 30, DEFAULTS.sessionMaxHours),
                range(get.apply("Login.MaxAttempts"), 1, 100, DEFAULTS.loginMaxAttempts),
                range(get.apply("Login.LockMinutes"), 1, 24 * 60, DEFAULTS.loginLockMinutes),
                range(get.apply("Upload.MaxMegabytes"), 1, 8192, DEFAULTS.uploadMaxMegabytes),
                string(get.apply("DefaultLanguage"), DEFAULTS.language).toLowerCase(Locale.ROOT),
                get.apply("GameLog.IgnoredCommands") instanceof List<?> ? strings(get.apply("GameLog.IgnoredCommands")) : DEFAULTS.ignoredCommands);
    }

    private static String string(Object value, String fallback) {
        return value == null ? fallback : value.toString().trim();
    }

    private static boolean bool(Object value, boolean fallback) {
        return value instanceof Boolean b ? b : fallback;
    }

    private static int range(Object value, int min, int max, int fallback) {
        if (!(value instanceof Number n)) return fallback;
        int v = n.intValue();
        return v < min || v > max ? fallback : v;
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(o -> o != null).map(o -> o.toString().trim()).filter(s -> !s.isEmpty()).toList();
    }
}
