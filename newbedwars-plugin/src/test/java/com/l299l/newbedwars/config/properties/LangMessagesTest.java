package com.l299l.newbedwars.config.properties;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class LangMessagesTest {

    private static YamlConfiguration yaml(String content) {
        return YamlConfiguration.loadConfiguration(new StringReader(content));
    }

    @Test
    void reloadMessages_loadsAllFlatKeysFromEnglish() {
        LangMessages lm = new LangMessages(yaml(""), yaml("Hello: 'Hi'\nBye: 'Goodbye'"));
        lm.reloadMessages();
        assertEquals("Hi", lm.getMsgEnglish("Hello"));
        assertEquals("Goodbye", lm.getMsgEnglish("Bye"));
    }

    @Test
    void reloadMessages_skipsCustomItemsNamesSection() {
        LangMessages lm = new LangMessages(yaml(""), yaml("Key: value\nCustomItemsNames:\n  Item: ItemName"));
        lm.reloadMessages();
        assertNotNull(lm.getMsgEnglish("Key"));
        assertNull(lm.getMsgEnglish("CustomItemsNames"));
    }

    @Test
    void reloadMessages_translatesColorCodes() {
        LangMessages lm = new LangMessages(yaml(""), yaml("Colored: '&aGreen'"));
        lm.reloadMessages();
        assertEquals("§aGreen", lm.getMsgEnglish("Colored"));
    }

    @Test
    void reloadMessages_loadPolishKeyNotInEnglish() {
        LangMessages lm = new LangMessages(yaml("OnlyPolish: 'Tak'"), yaml("Hello: 'Hi'"));
        lm.reloadMessages();
        assertEquals("Tak", lm.getMsgPolish("OnlyPolish"));
    }

    @Test
    void setFromCustomItemsNames_loadsCustomItemNamesIntoEnglish() {
        LangMessages lm = new LangMessages(yaml(""), yaml("CustomItemsNames:\n  MyItem: 'My Item'"));
        lm.reloadMessages();
        assertEquals("My Item", lm.getMsgEnglish("MyItem"));
    }

    @Test
    void setFromCustomItemsNames_loadsCustomItemNamesIntoPolish() {
        LangMessages lm = new LangMessages(yaml("CustomItemsNames:\n  MojPrzedmiot: 'Moj Przedmiot'"), yaml(""));
        lm.reloadMessages();
        assertEquals("Moj Przedmiot", lm.getMsgPolish("MojPrzedmiot"));
    }

    @Test
    void addCustomItemProperty_addsNameWhenKeyAbsent() {
        LangMessages lm = new LangMessages(yaml(""), yaml(""));
        lm.reloadMessages();
        lm.addCustomItemProperty("Sword", "-name");
        assertEquals("Sword", lm.getMsgEnglish("Sword-name"));
        assertEquals("Sword", lm.getMsgPolish("Sword-name"));
    }

    @Test
    void addCustomItemProperty_doesNotOverwriteExistingKey() {
        LangMessages lm = new LangMessages(yaml(""), yaml("CustomItemsNames:\n  Sword-name: 'Iron Sword'"));
        lm.reloadMessages();
        lm.addCustomItemProperty("Sword", "-name");
        assertEquals("Iron Sword", lm.getMsgEnglish("Sword-name"));
    }

    @Test
    void getMsgEnglish_returnsNullForUnknownKey() {
        LangMessages lm = new LangMessages(yaml(""), yaml(""));
        lm.reloadMessages();
        assertNull(lm.getMsgEnglish("nonexistent"));
    }

    @Test
    void getMsgPolish_returnsNullForUnknownKey() {
        LangMessages lm = new LangMessages(yaml(""), yaml(""));
        lm.reloadMessages();
        assertNull(lm.getMsgPolish("nonexistent"));
    }
}
