package com.l299l.newbedwars.panel;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PanelMessages {
    public static final String FALLBACK = "en";

    private final Map<String, Map<String, String>> languages = new HashMap<>();
    private final String defaultLanguage;

    public PanelMessages(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage == null ? FALLBACK : defaultLanguage.toLowerCase(Locale.ROOT);
    }

    public void put(String language, Map<String, String> messages) {
        languages.computeIfAbsent(language.toLowerCase(Locale.ROOT), k -> new HashMap<>()).putAll(messages);
    }

    public String language(String locale) {
        if (locale == null || locale.isEmpty()) return defaultLanguage;
        String lang = locale.toLowerCase(Locale.ROOT);
        int cut = lang.indexOf('_');
        if (cut < 0) cut = lang.indexOf('-');
        if (cut > 0) lang = lang.substring(0, cut);
        return languages.containsKey(lang) ? lang : defaultLanguage;
    }

    public String get(String language, String key, Object... args) {
        String text = lookup(language, key);
        if (text == null) text = lookup(defaultLanguage, key);
        if (text == null) text = lookup(FALLBACK, key);
        if (text == null) return key;
        for (int i = 0; i < args.length; i++) text = text.replace("{" + i + "}", String.valueOf(args[i]));
        return text;
    }

    private String lookup(String language, String key) {
        Map<String, String> map = language == null ? null : languages.get(language);
        String text = map == null ? null : map.get(key);
        return text == null || text.isEmpty() ? null : text;
    }
}
