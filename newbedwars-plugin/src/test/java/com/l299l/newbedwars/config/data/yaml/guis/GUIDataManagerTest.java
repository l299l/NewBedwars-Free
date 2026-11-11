package com.l299l.newbedwars.config.data.yaml.guis;

import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GUIDataManagerTest {
    @TempDir
    Path dir;
    private CustomItem wool;

    private GUIDataManager manager() {
        wool = mock(CustomItem.class);
        return new GUIDataManager(dir, name -> name.equalsIgnoreCase("wool") ? wool : null, Runnable::run,
                Logger.getLogger("test"), 3);
    }

    private Path guis() throws IOException {
        Path guis = dir.resolve("data/guis");
        Files.createDirectories(guis);
        return guis;
    }

    @Test
    void copiesTheExamplesOnFirstStart() {
        GUIDataManager m = manager();
        m.load();
        assertNotNull(m.get("exampleShop"));
        assertNotNull(m.get("exampleUpgrade"));
        assertTrue(Files.isRegularFile(dir.resolve("data/guis/exampleShop.yml")));
    }

    @Test
    void oneBrokenFileDoesNotStopTheOthers() throws IOException {
        Path guis = guis();
        Files.writeString(guis.resolve("a.yml"), "GuiId: a\nSize: 9\nSlots:\n  1: ci:Wool\n");
        Files.writeString(guis.resolve("b.yml"), "GuiId: [broken\n");
        Files.writeString(guis.resolve("c.yml"), "GuiId: a\nSize: 9\n");
        Files.writeString(guis.resolve("notes.txt"), "ignored");
        GUIDataManager m = manager();
        m.load();
        assertEquals(1, m.getGuiSaves().size());
        assertSame(wool, m.get("a").getItem(0));
        assertEquals(2, m.getBrokenFiles().size());
        assertTrue(m.getBrokenFiles().get("c.yml").contains("already used"));
    }

    @Test
    void savingWritesTheOwnFileAndKeepsABackup() throws IOException {
        Path guis = guis();
        Path file = guis.resolve("myFile.yml");
        Files.writeString(file, "# hand comment\nGuiId: shop\nSize: 9\n");
        GUIDataManager m = manager();
        m.load();
        GuiDefinition d = m.get("shop").getDefinition();
        List<String> slots = new ArrayList<>(d.slots());
        slots.set(0, "ci:Wool");
        m.save(new GuiDefinition("shop", d.guiClass(), "&aNew", 9, false, "empty", "empty", 0, 0, slots, List.of(), Map.of()));
        assertSame(wool, m.get("shop").getItem(0));
        assertFalse(Files.exists(guis.resolve("shop.yml")));
        assertEquals("&aNew", GuiCodec.parse(Files.readString(file)).title());
        try (Stream<Path> backups = Files.list(dir.resolve("data/backups/shop/guis/shop"))) {
            assertEquals(1, backups.count());
        }
        GUIDataManager reloaded = manager();
        reloaded.load();
        assertEquals("&aNew", reloaded.get("shop").getName());
    }

    @Test
    void newGuisGetAFileNamedAfterTheIdAndDeleteKeepsACopy() throws IOException {
        guis();
        GUIDataManager m = manager();
        m.load();
        m.save(new GuiDefinition("fresh", "ShopGUI", "F", 9, false, "empty", "empty", 0, 0,
                Collections.nCopies(9, "empty"), List.of(), Map.of()));
        Path file = dir.resolve("data/guis/fresh.yml");
        assertTrue(Files.isRegularFile(file));
        assertTrue(m.delete("fresh"));
        assertFalse(Files.exists(file));
        assertNull(m.get("fresh"));
        try (Stream<Path> backups = Files.list(dir.resolve("data/backups/shop/guis/fresh"))) {
            assertEquals(1, backups.count());
        }
        assertFalse(m.delete("fresh"));
    }

    @Test
    void fileTakenSeesForeignFiles() throws IOException {
        Path guis = guis();
        Files.writeString(guis.resolve("other.yml"), "GuiId: different\nSize: 9\n");
        GUIDataManager m = manager();
        m.load();
        assertTrue(m.fileTaken("other"));
        assertFalse(m.fileTaken("different"));
        assertFalse(m.fileTaken("new"));
    }
}
