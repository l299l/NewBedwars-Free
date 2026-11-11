package com.l299l.newbedwars.config.data.yaml.guis;

import java.util.List;
import java.util.Map;

public record GuiDefinition(String id, String guiClass, String title, int size, boolean closeOnTransaction,
                            String fill, String empty, int headerSize, int footerSize,
                            List<String> slots, List<Category> categories, Map<String, Object> extra) {

    public GuiDefinition {
        slots = List.copyOf(slots);
        categories = List.copyOf(categories);
        extra = extra == null ? Map.of() : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(extra));
    }

    public record Category(String id, String name, String icon, String description, List<String> items) {
        public Category {
            items = List.copyOf(items);
        }
    }

    public Category category(String categoryId) {
        for (Category c : categories) if (c.id().equals(categoryId)) return c;
        return null;
    }
}
