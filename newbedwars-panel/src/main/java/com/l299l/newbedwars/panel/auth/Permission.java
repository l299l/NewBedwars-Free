package com.l299l.newbedwars.panel.auth;

import java.util.List;

public final class Permission {
    public static final String DASHBOARD_VIEW = "dashboard.view";
    public static final String ARENA_ENABLE = "arena.enable";
    public static final String ARENA_DISABLE = "arena.disable";
    public static final String ARENA_BACKUP = "arena.backup";
    public static final String ARENA_RESTORE = "arena.restore";
    public static final String ARENA_RESETMAP = "arena.resetmap";
    public static final String ARENA_EXPORT = "arena.export";
    public static final String ARENA_IMPORT = "arena.import";
    public static final String ARENA_COPY = "arena.copy";
    public static final String ARENA_DELETE = "arena.delete";
    public static final String USERS_MANAGE = "users.manage";
    public static final String ROLES_MANAGE = "roles.manage";
    public static final String AUDIT_VIEW = "audit.view";
    public static final String STATS_VIEW = "stats.view";
    public static final String GAME_MANAGE = "game.manage";
    public static final String GAME_LOG = "game.log";
    public static final String SHOP_EDIT = "shop.edit";

    public static final List<String> ALL = List.of(
            DASHBOARD_VIEW, STATS_VIEW, GAME_MANAGE, GAME_LOG, SHOP_EDIT, ARENA_ENABLE, ARENA_DISABLE, ARENA_BACKUP, ARENA_RESTORE, ARENA_RESETMAP,
            ARENA_EXPORT, ARENA_IMPORT, ARENA_COPY, ARENA_DELETE, USERS_MANAGE, ROLES_MANAGE, AUDIT_VIEW);

    private Permission() {}

    public static boolean isKnown(String permission) {
        return ALL.contains(permission);
    }
}
