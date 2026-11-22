package com.l299l.newbedwars.arena.shops.customitems;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GamePermissions {
    private static final class Grant {
        final String arena;
        final Set<String> permissions = ConcurrentHashMap.newKeySet();
        PermissionAttachment attachment;

        Grant(String arena) {
            this.arena = arena;
        }

        void detach() {
            PermissionAttachment live = attachment;
            attachment = null;
            if (live != null) live.remove();
        }
    }

    private static final Map<UUID, Grant> GRANTS = new ConcurrentHashMap<>();

    private GamePermissions() {
    }

    public static void grant(Plugin plugin, Player player, String arena, String permission) {
        Grant grant = GRANTS.get(player.getUniqueId());
        if (grant != null && !grant.arena.equalsIgnoreCase(arena)) {
            revoke(player);
            grant = null;
        }
        if (grant == null) {
            grant = new Grant(arena);
            GRANTS.put(player.getUniqueId(), grant);
        }
        if (grant.attachment == null) grant.attachment = player.addAttachment(plugin);
        grant.permissions.add(permission);
        grant.attachment.setPermission(permission, true);
    }

    public static void suspend(Player player) {
        Grant grant = GRANTS.get(player.getUniqueId());
        if (grant != null) grant.detach();
    }

    public static void restore(Plugin plugin, Player player, String arena) {
        Grant grant = GRANTS.get(player.getUniqueId());
        if (grant == null || !grant.arena.equalsIgnoreCase(arena) || grant.permissions.isEmpty()) return;
        grant.detach();
        grant.attachment = player.addAttachment(plugin);
        for (String permission : grant.permissions) grant.attachment.setPermission(permission, true);
    }

    public static void revoke(Player player) {
        Grant grant = GRANTS.remove(player.getUniqueId());
        if (grant != null) grant.detach();
    }

    public static void forget(UUID player, String arena) {
        Grant grant = GRANTS.get(player);
        if (grant != null && grant.arena.equalsIgnoreCase(arena) && GRANTS.remove(player, grant)) grant.detach();
    }

    public static void revokeArena(String arena) {
        for (Map.Entry<UUID, Grant> e : new ArrayList<>(GRANTS.entrySet())) {
            if (e.getValue().arena.equalsIgnoreCase(arena) && GRANTS.remove(e.getKey(), e.getValue())) {
                e.getValue().detach();
            }
        }
    }

    public static void revokeAll() {
        for (Map.Entry<UUID, Grant> e : new ArrayList<>(GRANTS.entrySet())) {
            if (GRANTS.remove(e.getKey(), e.getValue())) {
                e.getValue().detach();
            }
        }
    }

    public static boolean has(UUID player, String permission) {
        Grant grant = GRANTS.get(player);
        return grant != null && grant.permissions.contains(permission);
    }
}
