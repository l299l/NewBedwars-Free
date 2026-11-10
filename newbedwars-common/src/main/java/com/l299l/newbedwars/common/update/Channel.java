package com.l299l.newbedwars.common.update;

import java.util.Locale;

public enum Channel {
    RELEASE, BETA, ALPHA;

    public boolean allows(Channel release) {
        return release.ordinal() <= ordinal();
    }

    public static Channel parse(String raw, Channel fallback) {
        if (raw == null) return fallback;
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public static Channel max(Channel a, Channel b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
