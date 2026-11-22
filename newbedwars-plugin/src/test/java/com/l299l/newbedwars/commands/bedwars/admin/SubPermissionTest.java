package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubPermissionTest {
    private Messages messages;

    @BeforeEach
    void setUp() {
        NewBedwars plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        messages = mock(Messages.class);
        when(plugin.getMessages()).thenReturn(messages);
        when(messages.getMsg(org.mockito.ArgumentMatchers.any(Player.class), anyString())).thenReturn("");
        new ArenaGroupManager();
    }

    @AfterEach
    void tearDown() {
        new ArenaGroupManager();
        NewBedwars.plugin = null;
    }

    private Player player(String... permissions) {
        Set<String> granted = Set.of(permissions);
        Player player = mock(Player.class);
        when(player.hasPermission(anyString())).thenAnswer(i -> granted.contains(i.<String>getArgument(0)));
        return player;
    }

    @Test
    void arenaSubPermissionIsEnoughForThatActionOnly() {
        Player p = player("newbedwars.bw.arena.list", "newbedwars.bw.arena.backups");
        assertTrue(ArenaCommand.hasAnyPermission(p));
        assertTrue(ArenaCommand.canUse(p, "list"));
        assertTrue(ArenaCommand.canUse(p, "backups"));
        assertFalse(ArenaCommand.canUse(p, "delete"));
        assertFalse(ArenaCommand.hasParentPermission(p));
    }

    @Test
    void parentPermissionsGrantEveryAction() {
        assertTrue(ArenaCommand.canUse(player("newbedwars.bw.arena"), "restore"));
        assertTrue(ArenaCommand.canUse(player("newbedwars.bw.admin"), "create"));
        assertFalse(ArenaCommand.hasAnyPermission(player("newbedwars.bw.setlobby")));
    }

    @Test
    void tabCompletionListsOnlyAllowedActions() {
        ArenaCommand cmd = new ArenaCommand();
        Player p = player("newbedwars.bw.arena.copy", "newbedwars.bw.arena.create");
        assertEquals(List.of("create", "copy"), cmd.getSubcommandArguments(p, new String[]{"arena", ""}));
        assertNull(cmd.getSubcommandArguments(player(), new String[]{"arena", ""}));
    }

    @Test
    void performRunsAllowedActionAndRefusesOthers() {
        ArenaCommand cmd = new ArenaCommand();
        Player p = player("newbedwars.bw.arena.backup");
        cmd.perform(p, new String[]{"arena", "backup"}, null);
        verify(messages, never()).send(p, "NoPermissions");
        cmd.perform(p, new String[]{"arena", "delete", "castle"}, null);
        verify(messages).send(p, "NoPermissions");
    }

    @Test
    void performRefusesPlayerWithoutAnyArenaPermission() {
        ArenaCommand cmd = new ArenaCommand();
        Player p = player("newbedwars.bw.group.list");
        cmd.perform(p, new String[]{"arena", "list"}, null);
        verify(messages).send(p, "NoPermissions");
    }

    @Test
    void groupSubPermissionsAreHonoured() {
        GroupCommand cmd = new GroupCommand();
        Player p = player("newbedwars.bw.group.list");
        assertTrue(GroupCommand.hasAnyPermission(p));
        assertFalse(GroupCommand.hasAnyPermission(player("newbedwars.bw.arena.list")));
        assertEquals(List.of("list"), cmd.getSubcommandArguments(p, new String[]{"group", ""}));
        assertNull(cmd.getSubcommandArguments(p, new String[]{"group", "delete", ""}));
        cmd.perform(p, new String[]{"group", "delete", "solo"}, null);
        verify(messages).send(p, "NoPermissions");
    }
}
