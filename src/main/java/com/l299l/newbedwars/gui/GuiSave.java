package com.l299l.newbedwars.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GuiSave {
    private final Class<?> guiClass;
    private final List<Object> items;
    private final HashMap<String, Object> guiData;
    private String guiId;
    private String name;
    private Integer size;
    private Boolean closeOnTransaction;

    public GuiSave(Class<?> guiClass, String id, String name, Integer size, Boolean closeOnTransaction) {
        this.guiClass = guiClass;
        this.guiId = id;
        this.name = name;
        this.size = size;
        this.closeOnTransaction = closeOnTransaction;
        guiData = new HashMap<>();
        items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(null);
        }
    }
    
    public void setGuiData(String key, Object value) {
        guiData.put(key, value);
    }
    
    public Object getGuiData(String key) {
        return guiData.get(key);
    }
    public void setItem(int slot, Object item) {
        items.set(slot, item);
    }

    public Object getItem(int slot) {
        return items.get(slot);
    }

    public List<Object> getItems() {
        return items;
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

    public Integer getSize() {
        return size;
    }

    public Boolean getCloseOnTransaction() {
        return closeOnTransaction;
    }

    public void setCloseOnTransaction(Boolean closeOnTransaction) {
        this.closeOnTransaction = closeOnTransaction;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
