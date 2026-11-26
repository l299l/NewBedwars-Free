package com.l299l.newbedwars.text;

import com.l299l.newbedwars.NewBedwars;
import org.bukkit.entity.Player;

public final class LangTokens {
    private static final String PREFIX = "lang:";

    private LangTokens() {
    }

    public static boolean contains(String text) {
        return text != null && (text.contains("{lang:") || text.contains("/lang:"));
    }

    public static String apply(String text, Player viewer) {
        if (!contains(text)) return text;
        int n = text.length();
        StringBuilder out = new StringBuilder(n + 32);
        int i = 0;
        while (i < n) {
            char c = text.charAt(i);
            if ((c == '{' || c == '/') && text.startsWith(PREFIX, i + 1)) {
                char close = c == '{' ? '}' : '/';
                int start = i + 1 + PREFIX.length();
                int j = start;
                while (j < n && isKeyChar(text.charAt(j))) j++;
                if (j > start && j < n && text.charAt(j) == close) {
                    out.append(lookup(viewer, text.substring(start, j)));
                    i = j + 1;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    public static String lookup(Player viewer, String key) {
        if (NewBedwars.plugin == null || NewBedwars.plugin.getMessages() == null) return "";
        String value = NewBedwars.plugin.getMessages().getRaw(viewer, key);
        return value == null ? "" : value;
    }

    private static boolean isKeyChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.';
    }
}
