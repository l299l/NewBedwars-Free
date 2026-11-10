package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.events.gameplay.GameplayUtil.MobKey;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameplayUtilTest {

    @BeforeEach
    void setUp() {
        NewBedwars.plugin = mock(NewBedwars.class);
        when(NewBedwars.plugin.getName()).thenReturn("NewBedwars");
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    private static <T extends Entity> T tagged(Class<T> type, String key) {
        T entity = mock(type);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.get(any(), eq(PersistentDataType.STRING))).thenReturn(key);
        when(entity.getPersistentDataContainer()).thenReturn(pdc);
        return entity;
    }

    private static EntityDamageEvent hitBy(Entity damager) {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(damager);
        return event;
    }

    private static IArena arenaWithRed(Team red) {
        IArena arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn("Castle");
        when(arena.getTeam("RED")).thenReturn(red);
        return arena;
    }

    @Test
    void dragonTeamFromTheDragonItsFireballCloudAndParts() {
        Team red = mock(Team.class);
        IArena arena = arenaWithRed(red);
        EnderDragon dragon = tagged(EnderDragon.class, "Castle:RED");
        assertSame(red, GameplayUtil.dragonTeam(arena, hitBy(dragon)));

        DragonFireball fireball = tagged(DragonFireball.class, null);
        when(fireball.getShooter()).thenReturn(dragon);
        assertSame(red, GameplayUtil.dragonTeam(arena, hitBy(fireball)));

        AreaEffectCloud cloud = tagged(AreaEffectCloud.class, null);
        when(cloud.getSource()).thenReturn(dragon);
        assertSame(red, GameplayUtil.dragonTeam(arena, hitBy(cloud)));

        EnderDragonPart part = mock(EnderDragonPart.class);
        when(part.getParent()).thenReturn(dragon);
        assertSame(red, GameplayUtil.dragonTeam(arena, hitBy(part)));
    }

    @Test
    void noDragonTeamForOtherDamage() {
        IArena arena = arenaWithRed(mock(Team.class));
        assertNull(GameplayUtil.dragonTeam(arena, null));
        assertNull(GameplayUtil.dragonTeam(arena, mock(EntityDamageEvent.class)));
        assertNull(GameplayUtil.dragonTeam(arena, hitBy(tagged(EnderDragon.class, null))));
        assertNull(GameplayUtil.dragonTeam(arena, hitBy(tagged(EnderDragon.class, "OtherArena:RED"))));
        assertNull(GameplayUtil.dragonTeam(arena, hitBy(tagged(IronGolem.class, null))));
    }

    @Test
    void mobKeySplitsAtTheFirstColon() {
        MobKey key = MobKey.parse("Castle:Red:2");
        assertEquals("Castle", key.arenaName());
        assertEquals("Red:2", key.teamName());
    }

    @Test
    void invalidMobKeys() {
        assertNull(MobKey.parse(null));
        assertNull(MobKey.parse("NoColon"));
    }

    @Test
    void playerDamagerIsThePlayerOrTheShooter() {
        Player player = mock(Player.class);
        assertSame(player, GameplayUtil.playerDamager(player));

        Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(player);
        assertSame(player, GameplayUtil.playerDamager(arrow));

        Arrow dispenserArrow = mock(Arrow.class);
        assertNull(GameplayUtil.playerDamager(dispenserArrow));
        assertNull(GameplayUtil.playerDamager(mock(IronGolem.class)));
    }
}
