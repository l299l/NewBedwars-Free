package com.l299l.newbedwars.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LanguageFill {
    private static final String SKIPPED = "CustomItemsNames";

    private LanguageFill() {
    }

    public static Map<String, String> plan(Map<String, String> english, Map<String, String> bundled, Map<String, String> server) {
        return plan(english, bundled, server, Map.of());
    }

    public static Map<String, String> plan(Map<String, String> english, Map<String, String> bundled,
                                           Map<String, String> server, Map<String, String> previous) {
        Map<String, String> changes = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : english.entrySet()) {
            String key = entry.getKey();
            if (key.equals(SKIPPED) || key.startsWith(SKIPPED + ".")) continue;
            String englishText = entry.getValue();
            String current = server.get(key);
            String translated = bundled.get(key);
            if (current == null) {
                String kept = previous.get(key);
                if (kept != null) changes.put(key, kept);
                else if (translated == null) changes.put(key, englishText);
            } else if (current.equals(englishText) && translated != null && !translated.equals(englishText)) {
                changes.put(key, translated);
            }
        }
        return changes;
    }

    public static String write(String text, Map<String, String> changes) {
        if (changes.isEmpty()) return text;
        List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) lines.remove(lines.size() - 1);
        for (Map.Entry<String, String> change : changes.entrySet()) {
            String line = change.getKey() + ": " + quote(change.getValue());
            int at = indexOf(lines, change.getKey());
            if (at < 0) {
                lines.add(line);
                continue;
            }
            int end = at + 1;
            while (end < lines.size() && isContinuation(lines.get(end))) end++;
            lines.subList(at, end).clear();
            lines.add(at, line);
        }
        return String.join("\n", lines) + "\n";
    }

    private static int indexOf(List<String> lines, String key) {
        String prefix = key + ":";
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) return i;
        }
        return -1;
    }

    private static boolean isContinuation(String line) {
        return !line.isEmpty() && (line.charAt(0) == ' ' || line.charAt(0) == '\t');
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.append('"').toString();
    }
}
