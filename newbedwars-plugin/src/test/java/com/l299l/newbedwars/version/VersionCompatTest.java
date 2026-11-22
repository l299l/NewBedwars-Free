package com.l299l.newbedwars.version;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VersionCompatTest {

    private static EntityType resolve(String name) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.17.1-R0.1-SNAPSHOT");
            return VersionCompat.entityType(name);
        }
    }

    @Test
    void entityTypeResolvesCurrentNames() {
        assertEquals(EntityType.VILLAGER, resolve("VILLAGER"));
        assertEquals(EntityType.IRON_GOLEM, resolve(" iron_golem "));
    }

    @Test
    void entityTypeAcceptsTheRenamedNamesBothWays() {
        assertEquals(EntityType.MUSHROOM_COW, resolve("MUSHROOM_COW"));
        assertEquals(EntityType.MUSHROOM_COW, resolve("MOOSHROOM"));
        assertEquals(EntityType.SNOWMAN, resolve("snow_golem"));
        assertEquals(EntityType.SNOWMAN, resolve("SNOWMAN"));
    }

    @Test
    void entityTypeIsNullForUnknownNames() {
        assertNull(resolve(null));
        assertNull(resolve("null"));
        assertNull(resolve("NOT_A_MOB"));
    }
}
