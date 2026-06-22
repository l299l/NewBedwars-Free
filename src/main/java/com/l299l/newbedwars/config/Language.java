package com.l299l.newbedwars.config;

public enum Language {
    English("en"),
    Polish("pl"),
    German("de"),
    Spanish("es"),
    French("fr"),
    Russian("ru");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Language fromCode(String code) {
        for (Language lang : values()) {
            if (lang.code.equalsIgnoreCase(code)) return lang;
        }
        return null;
    }
}
