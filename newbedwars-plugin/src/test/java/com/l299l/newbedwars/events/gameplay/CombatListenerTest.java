package com.l299l.newbedwars.events.gameplay;

import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CombatListenerTest {

    private static EntityDamageEvent damage(DamageCause cause) {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getCause()).thenReturn(cause);
        return event;
    }

    @Test
    void quickVoidIsVoidWithoutAnAttacker() {
        assertEquals(DamageCause.VOID, CombatListener.deathCause(damage(DamageCause.CUSTOM), true));
    }

    @Test
    void quickVoidIsVoidWhenAPlayerKnockedTheVictimOff() {
        assertEquals(DamageCause.VOID, CombatListener.deathCause(damage(DamageCause.ENTITY_ATTACK), true));
    }

    @Test
    void otherDeathsKeepTheirCause() {
        assertEquals(DamageCause.FALL, CombatListener.deathCause(damage(DamageCause.FALL), false));
        assertEquals(DamageCause.VOID, CombatListener.deathCause(damage(DamageCause.VOID), false));
    }

    @Test
    void missingCauseIsCustom() {
        assertEquals(DamageCause.CUSTOM, CombatListener.deathCause(null, false));
    }
}
