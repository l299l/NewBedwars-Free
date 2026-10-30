package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.events.gameplay.GameplayUtil.MobKey;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameplayUtilTest {

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
