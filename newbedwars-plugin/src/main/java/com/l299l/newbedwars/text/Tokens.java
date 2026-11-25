package com.l299l.newbedwars.text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Tokens {
    private Tokens() {
    }

    public static String apply(String text, Map<String, String> values) {
        if (text == null || values.isEmpty()) return text;
        if (text.indexOf('/') < 0 && text.indexOf('{') < 0) return text;
        Map<String, String> byName = new HashMap<>();
        for (Map.Entry<String, String> e : values.entrySet()) byName.put(canonical(e.getKey()), e.getValue());
        int n = text.length();
        StringBuilder out = new StringBuilder(n + 16);
        int i = 0;
        while (i < n) {
            char c = text.charAt(i);
            if (c == '/' || c == '{') {
                char close = c == '/' ? '/' : '}';
                int j = i + 1;
                while (j < n && isNameChar(text.charAt(j))) j++;
                if (j > i + 1 && j < n && text.charAt(j) == close) {
                    String value = byName.get(canonical(text.substring(i + 1, j)));
                    if (value != null) {
                        out.append(value);
                        i = j + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    public static String replaceKey(String text, String key, String value) {
        String out = text.replace(key, value);
        if (key.length() > 2 && key.charAt(0) == '/' && key.charAt(key.length() - 1) == '/') {
            out = out.replace("{" + key.substring(1, key.length() - 1) + "}", value);
        }
        return out;
    }

    private static boolean isNameChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '.';
    }

    private static String canonical(String name) {
        return name.replace("_", "").toLowerCase(Locale.ROOT);
    }
}
