package com.l299l.newbedwars.arena.shops.customitems;

import com.l299l.newbedwars.utils.TeamColor;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomItemManagerTest {
    private CustomItemManager manager;

    @BeforeEach
    void setUp() {
        manager = new CustomItemManager();
    }

    private CustomItem item(String name, Material material) {
        CustomItem item = mock(CustomItem.class);
        when(item.getUuid()).thenReturn(UUID.randomUUID());
        when(item.getName()).thenReturn(name);
        when(item.getMaterial()).thenReturn(material);
        manager.addCustomItem(item);
        return item;
    }

    @Test
    void findsTheItemSellingTheWhiteFormOfAFamily() {
        item("Sword", Material.IRON_SWORD);
        CustomItem concrete = item("Concrete", Material.WHITE_CONCRETE);
        assertSame(concrete, manager.getTeamBlockItem(TeamColor.Kind.CONCRETE));
    }

    @Test
    void woolFamilyPrefersTheDefaultWoolItem() {
        item("Other wool", Material.WHITE_WOOL);
        CustomItem wool = item("Wool", Material.WHITE_WOOL);
        assertSame(wool, manager.getTeamBlockItem(TeamColor.Kind.WOOL));
    }

    @Test
    void returnsNullWhenNothingSellsThatFamily() {
        item("Wool", Material.WHITE_WOOL);
        assertNull(manager.getTeamBlockItem(TeamColor.Kind.GLASS));
    }
}
