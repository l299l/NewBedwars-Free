package com.l299l.newbedwars.panel.network;

import java.util.Locale;

public enum NetworkRole {
    STANDALONE, GATEWAY, NODE;

    public static NetworkRole parse(Object value) {
        if (value == null) return STANDALONE;
        try {
            return valueOf(value.toString().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return STANDALONE;
        }
    }
}
