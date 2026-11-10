package com.l299l.newbedwars.panel.auth;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public record Role(String name, Set<String> permissions, boolean builtIn) {
    public static final String ADMIN = "admin";
    public static final String STAFF = "staff";
    public static final Role ADMIN_ROLE = new Role(ADMIN, Set.copyOf(Permission.ALL), true);
    public static final Role STAFF_ROLE = new Role(STAFF, Set.of(Permission.DASHBOARD_VIEW), true);
    public static final List<Role> BUILT_IN = List.of(ADMIN_ROLE, STAFF_ROLE);
    private static final Pattern NAME = Pattern.compile("[a-z0-9_-]{2,32}");

    public Role {
        name = name.toLowerCase(Locale.ROOT);
        permissions = Set.copyOf(permissions);
    }

    public static Role custom(String name, Set<String> permissions) {
        String key = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (!NAME.matcher(key).matches()) throw new IllegalArgumentException("invalid_role_name");
        if (isBuiltIn(key)) throw new IllegalArgumentException("built_in_role");
        TreeSet<String> checked = new TreeSet<>();
        for (String p : permissions) {
            if (!Permission.isKnown(p)) throw new IllegalArgumentException("unknown_permission");
            checked.add(p);
        }
        return new Role(key, checked, false);
    }

    public static boolean isBuiltIn(String name) {
        return name != null && (ADMIN.equalsIgnoreCase(name) || STAFF.equalsIgnoreCase(name));
    }

    public boolean has(String permission) {
        return permissions.contains(permission);
    }
}
