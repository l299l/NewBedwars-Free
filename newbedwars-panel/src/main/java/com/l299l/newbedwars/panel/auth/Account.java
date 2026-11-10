package com.l299l.newbedwars.panel.auth;

import java.util.Locale;
import java.util.regex.Pattern;

public record Account(String name, String passwordHash, String role, boolean mustChangePassword, long created) {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_.-]{3,32}");

    public static boolean isValidName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    public String key() {
        return key(name);
    }

    public static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public Account withPassword(String hash, boolean mustChange) {
        return new Account(name, hash, role, mustChange, created);
    }

    public Account withRole(String newRole) {
        return new Account(name, passwordHash, newRole, mustChangePassword, created);
    }
}
