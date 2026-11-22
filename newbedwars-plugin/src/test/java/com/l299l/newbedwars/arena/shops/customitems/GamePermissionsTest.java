package com.l299l.newbedwars.arena.shops.customitems;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GamePermissionsTest {
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        when(plugin.isEnabled()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        GamePermissions.revokeAll();
    }

    private Player player() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.addAttachment(plugin)).thenAnswer(i -> new PermissionAttachment(plugin, player));
        return player;
    }

    @Test
    void grantedUntilRevoked() {
        Player player = player();
        GamePermissions.grant(plugin, player, "Arena1", "fly.use");
        GamePermissions.grant(plugin, player, "Arena1", "kit.vip");
        assertTrue(GamePermissions.has(player.getUniqueId(), "fly.use"));
        assertTrue(GamePermissions.has(player.getUniqueId(), "kit.vip"));
        verify(player, times(1)).addAttachment(plugin);
        GamePermissions.revoke(player);
        assertFalse(GamePermissions.has(player.getUniqueId(), "fly.use"));
        verify(player).removeAttachment(org.mockito.ArgumentMatchers.any(PermissionAttachment.class));
    }

    @Test
    void gameEndRevokesOnlyThatArena() {
        Player a = player();
        Player b = player();
        GamePermissions.grant(plugin, a, "Arena1", "fly.use");
        GamePermissions.grant(plugin, b, "Arena2", "fly.use");
        GamePermissions.revokeArena("arena1");
        assertFalse(GamePermissions.has(a.getUniqueId(), "fly.use"));
        assertTrue(GamePermissions.has(b.getUniqueId(), "fly.use"));
    }

    @Test
    void joiningAnotherArenaDropsTheOldGrants() {
        Player player = player();
        GamePermissions.grant(plugin, player, "Arena1", "fly.use");
        GamePermissions.grant(plugin, player, "Arena2", "kit.vip");
        assertFalse(GamePermissions.has(player.getUniqueId(), "fly.use"));
        assertTrue(GamePermissions.has(player.getUniqueId(), "kit.vip"));
    }

    @Test
    void suspendedGrantsSurviveARejoinAndAreReappliedToTheNewPlayerObject() {
        Player before = player();
        GamePermissions.grant(plugin, before, "Arena1", "fly.use");
        GamePermissions.suspend(before);
        verify(before).removeAttachment(org.mockito.ArgumentMatchers.any(PermissionAttachment.class));
        assertTrue(GamePermissions.has(before.getUniqueId(), "fly.use"));

        Player after = mock(Player.class);
        java.util.UUID id = before.getUniqueId();
        when(after.getUniqueId()).thenReturn(id);
        PermissionAttachment[] created = new PermissionAttachment[1];
        when(after.addAttachment(plugin)).thenAnswer(i -> created[0] = new PermissionAttachment(plugin, after));
        GamePermissions.restore(plugin, after, "arena1");

        assertTrue(GamePermissions.has(after.getUniqueId(), "fly.use"));
        assertTrue(created[0].getPermissions().get("fly.use"));
    }

    @Test
    void forgettingDropsASuspendedGrantForGood() {
        Player player = player();
        GamePermissions.grant(plugin, player, "Arena1", "fly.use");
        GamePermissions.suspend(player);
        GamePermissions.forget(player.getUniqueId(), "Arena1");
        assertFalse(GamePermissions.has(player.getUniqueId(), "fly.use"));
        GamePermissions.restore(plugin, player, "Arena1");
        verify(player, times(1)).addAttachment(plugin);
    }

    @Test
    void forgettingAnotherArenaKeepsTheGrant() {
        Player player = player();
        GamePermissions.grant(plugin, player, "Arena2", "fly.use");
        GamePermissions.forget(player.getUniqueId(), "Arena1");
        assertTrue(GamePermissions.has(player.getUniqueId(), "fly.use"));
    }

    @Test
    void restoreIgnoresAGrantFromAnotherArena() {
        Player player = player();
        GamePermissions.grant(plugin, player, "Arena1", "fly.use");
        GamePermissions.suspend(player);
        GamePermissions.restore(plugin, player, "Arena2");
        verify(player, times(1)).addAttachment(plugin);
    }

    @Test
    void gameEndDropsSuspendedGrantsToo() {
        Player player = player();
        GamePermissions.grant(plugin, player, "Arena1", "fly.use");
        GamePermissions.suspend(player);
        GamePermissions.revokeArena("Arena1");
        assertFalse(GamePermissions.has(player.getUniqueId(), "fly.use"));
    }
}
