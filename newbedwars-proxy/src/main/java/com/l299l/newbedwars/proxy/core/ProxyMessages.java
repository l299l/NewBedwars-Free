package com.l299l.newbedwars.proxy.core;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ProxyMessages {
    public static final String DEFAULT = "en";
    private final Map<String, Map<String, String>> languages = new HashMap<>();

    public void put(String language, Map<String, String> texts) {
        languages.put(language.toLowerCase(Locale.ROOT), new HashMap<>(texts));
    }

    public String get(Locale locale, String key) {
        String lang = locale == null ? DEFAULT : locale.getLanguage().toLowerCase(Locale.ROOT);
        Map<String, String> map = languages.get(lang);
        String value = map == null ? null : map.get(key);
        if (value == null || value.isEmpty()) {
            Map<String, String> en = languages.get(DEFAULT);
            value = en == null ? null : en.get(key);
        }
        return value == null ? key : value;
    }

    public String format(Locale locale, String key, Map<String, String> replacements) {
        String text = get(locale, key);
        for (Map.Entry<String, String> r : replacements.entrySet()) {
            String v = r.getValue() == null ? "" : r.getValue();
            if (v.length() > 2 && v.startsWith("%") && v.endsWith("%")) v = get(locale, v.substring(1, v.length() - 1));
            text = text.replace(r.getKey(), v);
        }
        return text;
    }
}
