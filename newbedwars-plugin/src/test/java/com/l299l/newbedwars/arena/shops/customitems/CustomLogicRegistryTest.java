package com.l299l.newbedwars.arena.shops.customitems;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogicRegistry;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.RegisteredLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.TntLogic;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CustomLogicRegistryTest {
    private final List<String> added = new ArrayList<>();

    @AfterEach
    void tearDown() {
        added.forEach(CustomLogicRegistry::unregister);
    }

    private CustomLogic logic(boolean result, AtomicInteger calls) {
        return new CustomLogic() {
            @Override
            public boolean perform(Player player, IArena arena) {
                calls.incrementAndGet();
                return result;
            }

            @Override
            public LogicType getType() {
                return LogicType.NONE;
            }
        };
    }

    private void register(String key, CustomLogic logic) {
        CustomLogicRegistry.register(key, () -> logic);
        added.add(key);
    }

    @Test
    void builtInKeysAreRecognisedCaseInsensitively() {
        assertTrue(CustomLogicRegistry.isBuiltIn("TNT"));
        assertTrue(CustomLogicRegistry.isBuiltIn("bridge_egg"));
        assertFalse(CustomLogicRegistry.isBuiltIn("MY_LOGIC"));
    }

    @Test
    void builtInLogicDefaultsKeyToTheTypeName() {
        assertEquals("TNT", new TntLogic().getKey());
    }

    @Test
    void registeredLogicIsCreatedByKey() {
        AtomicInteger calls = new AtomicInteger();
        register("TEST_LOGIC", logic(true, calls));
        assertNotNull(CustomLogicRegistry.create("test_logic"));
        assertNull(CustomLogicRegistry.create("OTHER"));
    }

    @Test
    void builtInAndDuplicateKeysAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> CustomLogicRegistry.register("TNT", () -> null));
        register("TEST_DUP", logic(true, new AtomicInteger()));
        assertThrows(IllegalArgumentException.class, () -> CustomLogicRegistry.register("test_dup", () -> null));
    }

    @Test
    void registeredLogicStandInResolvesWhenUsed() {
        AtomicInteger calls = new AtomicInteger();
        RegisteredLogic standIn = new RegisteredLogic("test_late");
        assertEquals("TEST_LATE", standIn.getKey());
        assertEquals(LogicType.NONE, standIn.getType());
        // addon not registered yet: nothing happens and the item is not consumed
        assertFalse(standIn.perform(mock(Player.class), mock(IArena.class)));
        register("TEST_LATE", logic(true, calls));
        assertTrue(standIn.perform(mock(Player.class), mock(IArena.class)));
        assertEquals(1, calls.get());
        CustomLogicRegistry.unregister("TEST_LATE");
        assertFalse(standIn.perform(mock(Player.class), mock(IArena.class)));
        assertEquals(1, calls.get());
    }

    @Test
    void standInKeepsReturnedConsumeFlag() {
        register("TEST_KEEP", logic(false, new AtomicInteger()));
        assertFalse(new RegisteredLogic("TEST_KEEP").perform(mock(Player.class), mock(IArena.class)));
    }

    @Test
    void managerPicksBuiltInLogicOrStandIn() {
        CustomItemManager manager = new CustomItemManager();
        assertInstanceOf(TntLogic.class, manager.matchLogicKey("TNT"));
        CustomLogic addon = manager.matchLogicKey("someAddonKey");
        assertInstanceOf(RegisteredLogic.class, addon);
        assertEquals("SOMEADDONKEY", addon.getKey());
    }
}
