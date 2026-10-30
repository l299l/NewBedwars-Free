package com.l299l.newbedwars.gui;

import com.l299l.newbedwars.gui.model.GUICategory;
import com.l299l.newbedwars.gui.model.GUIEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A shop GUI loaded from {@code data/guis/*.yml}: slot entries, categories and the header/footer layout. */
public class GUISave {
    private final Class<?> guiClass;
    private final List<GUIEntry> items;
    private final Map<String, GUICategory> categories = new LinkedHashMap<>();
    private String guiId;
    private String name;
    private int size;
    private boolean closeOnTransaction;
    private int headerSize;
    private int footerSize;

    public GUISave(Class<?> guiClass, String id, String name, int size, boolean closeOnTransaction) {
        this.guiClass = guiClass;
        this.guiId = id;
        this.name = name;
        this.size = size;
        this.closeOnTransaction = closeOnTransaction;
        items = new ArrayList<>(Collections.nCopies(size, (GUIEntry) null));
    }

    public void setItem(int slot, GUIEntry item) {
        items.set(slot, item);
    }

    /** The entry on a slot, or null when it is empty. */
    public GUIEntry getItem(int slot) {
        return items.get(slot);
    }

    public List<GUIEntry> getItems() {
        return items;
    }

    /** Categories by id, in file order. */
    public Map<String, GUICategory> getCategories() {
        return categories;
    }

    public void addCategory(GUICategory category) {
        categories.put(category.id(), category);
    }

    public int getHeaderSize() {
        return headerSize;
    }

    public void setHeaderSize(int headerSize) {
        this.headerSize = headerSize;
    }

    public int getFooterSize() {
        return footerSize;
    }

    public void setFooterSize(int footerSize) {
        this.footerSize = footerSize;
    }

    public Class<?> getGuiClass() {
        return guiClass;
    }

    public String getGuiId() {
        return guiId;
    }

    public void setGuiId(String guiId) {
        this.guiId = guiId;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public boolean getCloseOnTransaction() {
        return closeOnTransaction;
    }

    public void setCloseOnTransaction(boolean closeOnTransaction) {
        this.closeOnTransaction = closeOnTransaction;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
