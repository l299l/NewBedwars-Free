package com.l299l.newbedwars.config.data.yaml.items;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.arena.shops.customitems.CustomItemManager;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.data.yaml.SafeYaml;
import com.l299l.newbedwars.config.properties.LangMessages;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemsDataManagerTest {
    @TempDir
    Path dir;
    private CustomItemManager items;
    private ItemsDataManager data;

    static final String FILE = """
            Wool:
              description: default
              material: WHITE_WOOL
              price: 4
              priceType: IRON
              amount: 16
              iconDesc: default
              permission: ''
              itemLogic: NONE
              onBuyEvent: GIVE_ITEM
              isPermanent: false
              enchantments: {}
              itemFlags: []
            FutureBlock:
              material: SOME_BLOCK_FROM_2030
              price: 1
              extraField: kept
            Speed:
              material: POTION
              price: 4
              priceType: EMERALD
              amount: 1
              itemLogic: POTION
              onBuyEvent: GIVE_ITEM
              enchantments: {}
              itemFlags:
              - HIDE_ENCHANTS
            """;

    @BeforeEach
    void setUp() throws IOException {
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getMessages()).thenReturn(mock(Messages.class));
        when(plugin.getLangMessages()).thenReturn(mock(LangMessages.class));
        NewBedwars.plugin = plugin;
        Files.createDirectories(dir.resolve("data"));
        Files.writeString(dir.resolve("data/items.yml"), FILE);
        items = new CustomItemManager();
        data = new ItemsDataManager(dir, items, Runnable::run, Logger.getLogger("test"), 3);
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    @Test
    void oneBrokenItemDoesNotDropTheOthers() {
        data.load();
        assertEquals(2, items.getCustomItems().size());
        assertNotNull(items.getCustomItem("wool"));
        assertTrue(data.isBroken("futureblock"));
        assertEquals(List.of("FutureBlock"), data.getBrokenNames());
    }

    @Test
    void potionLogicIsKept() {
        data.load();
        assertEquals("POTION", items.getCustomItem("Speed").getEvent().getKey());
    }

    @SuppressWarnings("unchecked")
    @Test
    void savingKeepsOrderAndBrokenItemsUnchanged() throws IOException {
        data.load();
        items.getCustomItem("Wool").setPrice(8);
        data.save();
        Map<String, Object> saved = SafeYaml.create().load(Files.readString(dir.resolve("data/items.yml")));
        assertEquals(List.of("Wool", "FutureBlock", "Speed"), new ArrayList<>(saved.keySet()));
        assertEquals(8, ((Map<String, Object>) saved.get("Wool")).get("price"));
        assertEquals("kept", ((Map<String, Object>) saved.get("FutureBlock")).get("extraField"));
        assertEquals("POTION", ((Map<String, Object>) saved.get("Speed")).get("itemLogic"));
        assertEquals(List.of("HIDE_ENCHANTS"), ((Map<String, Object>) saved.get("Speed")).get("itemFlags"));
        assertTrue(Files.isDirectory(dir.resolve("data/backups/shop/items")));
    }

    @Test
    void reloadingDoesNotDuplicateItems() {
        data.load();
        data.load();
        assertEquals(2, items.getCustomItems().size());
    }

    @Test
    void removedAndBrokenItemsCanBeDeleted() throws IOException {
        data.load();
        items.remove("Wool");
        data.remove("Wool");
        data.remove("futureblock");
        data.save();
        Map<String, Object> saved = SafeYaml.create().load(Files.readString(dir.resolve("data/items.yml")));
        assertEquals(List.of("Speed"), new ArrayList<>(saved.keySet()));
    }

    @Test
    void unreadableFileIsNotOverwritten() throws IOException {
        Files.writeString(dir.resolve("data/items.yml"), "Wool: [broken");
        data.load();
        assertTrue(items.getCustomItems().isEmpty());
        assertEquals("Wool: [broken", Files.readString(dir.resolve("data/items.yml")));
    }

    @Test
    void newItemsAreAppended() throws IOException {
        data.load();
        items.addCustomItem(new CustomItem("Extra", "default", Material.STONE, 1,
                com.l299l.newbedwars.arena.shops.customitems.PriceType.IRON, 1, "default", "",
                new com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.NoneLogic(),
                com.l299l.newbedwars.arena.shops.customitems.customitemlogic.OnBuyEvent.GIVE_ITEM, false,
                new java.util.HashMap<>(), new ArrayList<>(), true));
        data.save();
        Map<String, Object> saved = SafeYaml.create().load(Files.readString(dir.resolve("data/items.yml")));
        assertEquals(List.of("Wool", "FutureBlock", "Speed", "Extra"), new ArrayList<>(saved.keySet()));
    }
}
