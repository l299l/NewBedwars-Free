package com.l299l.newbedwars.config.data.yaml.guis;

import com.l299l.newbedwars.config.data.yaml.SafeYaml;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GuiCodec {
    static final Set<String> KNOWN = Set.of("GuiId", "GuiClass", "GuiName", "Size", "CloseOnTransaction", "Fill", "Empty",
            "HeaderSize", "FooterSize", "Slots", "Categories");

    private GuiCodec() {}

    public static GuiDefinition parse(String yamlText) {
        Object root = SafeYaml.create().load(yamlText);
        if (!(root instanceof Map<?, ?> map)) throw new IllegalArgumentException("the file is not a YAML map");
        return parse(map);
    }

    public static GuiDefinition parse(Map<?, ?> map) {
        Object idObj = map.get("GuiId");
        if (idObj == null || idObj.toString().isBlank()) throw new IllegalArgumentException("GuiId is missing");
        String id = idObj.toString();
        String guiClass = map.get("GuiClass") == null ? "ShopGUI" : map.get("GuiClass").toString();
        String title = map.get("GuiName") == null ? id : map.get("GuiName").toString();
        int size = intValue(map.get("Size"), -1);
        if (size < 9 || size > 54 || size % 9 != 0) throw new IllegalArgumentException("Size must be 9, 18, 27, 36, 45 or 54");
        boolean close = Boolean.TRUE.equals(map.get("CloseOnTransaction")) || "true".equalsIgnoreCase(String.valueOf(map.get("CloseOnTransaction")));
        String fill = map.get("Fill") == null ? "empty" : map.get("Fill").toString();
        String empty = map.get("Empty") == null ? "empty" : map.get("Empty").toString();
        int header = intValue(map.get("HeaderSize"), 0);
        int footer = intValue(map.get("FooterSize"), 0);

        List<String> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) slots.add("empty");
        if (map.get("Slots") instanceof Map<?, ?> slotMap) {
            for (Map.Entry<?, ?> e : slotMap.entrySet()) {
                int key = intValue(e.getKey(), -1);
                if (key < 1 || key > size) continue;
                slots.set(key - 1, e.getValue() == null ? "empty" : e.getValue().toString());
            }
        }

        List<GuiDefinition.Category> categories = new ArrayList<>();
        if (map.get("Categories") instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> c) || c.get("ID") == null) continue;
                String catId = c.get("ID").toString();
                String name = c.get("Name") == null ? defaultName(catId) : c.get("Name").toString();
                String icon = c.get("Icon") == null ? "" : c.get("Icon").toString();
                String description = c.get("Description") == null ? "" : c.get("Description").toString();
                List<String> items = new ArrayList<>();
                if (c.get("Items") instanceof List<?> itemList) {
                    for (Object item : itemList) if (item != null) items.add(item.toString());
                }
                categories.add(new GuiDefinition.Category(catId, name, icon, description, items));
            }
        }

        Map<String, Object> extra = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (!KNOWN.contains(key)) extra.put(key, e.getValue());
        }
        return new GuiDefinition(id, guiClass, title, size, close, fill, empty, header, footer, slots, categories, extra);
    }

    public static String toYaml(GuiDefinition d) {
        StringBuilder sb = new StringBuilder();
        sb.append("GuiId: ").append(quote(d.id())).append('\n');
        sb.append("GuiClass: ").append(quote(d.guiClass())).append('\n');
        sb.append("GuiName: ").append(quote(d.title())).append('\n');
        sb.append("Size: ").append(d.size()).append('\n');
        sb.append("CloseOnTransaction: ").append(d.closeOnTransaction()).append('\n');
        sb.append('\n');
        sb.append("Fill: ").append(quote(d.fill())).append('\n');
        sb.append("Empty: ").append(quote(d.empty())).append('\n');
        sb.append("HeaderSize: ").append(d.headerSize()).append('\n');
        sb.append("FooterSize: ").append(d.footerSize()).append('\n');
        sb.append('\n');
        sb.append("Slots:\n");
        for (int i = 0; i < d.slots().size(); i++) {
            sb.append("  ").append(i + 1).append(": ").append(quote(d.slots().get(i))).append('\n');
        }
        sb.append('\n');
        if (d.categories().isEmpty()) {
            sb.append("Categories: []\n");
        } else {
            sb.append("Categories:\n");
            for (GuiDefinition.Category c : d.categories()) {
                sb.append("  - ID: ").append(quote(c.id())).append('\n');
                sb.append("    Name: ").append(quote(c.name())).append('\n');
                sb.append("    Icon: ").append(quote(c.icon())).append('\n');
                sb.append("    Description: ").append(quote(c.description())).append('\n');
                if (c.items().isEmpty()) {
                    sb.append("    Items: []\n");
                } else {
                    sb.append("    Items:\n");
                    for (String item : c.items()) sb.append("      - ").append(quote(item)).append('\n');
                }
            }
        }
        if (!d.extra().isEmpty()) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            sb.append('\n').append(new Yaml(options).dump(new LinkedHashMap<>(d.extra())));
        }
        return sb.toString();
    }

    static String quote(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) sb.append(String.format("\\x%02x", (int) ch));
                    else sb.append(ch);
                }
            }
        }
        return sb.append('"').toString();
    }

    private static int intValue(Object o, int fallback) {
        if (o instanceof Number n) return n.intValue();
        if (o == null) return fallback;
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String defaultName(String id) {
        return id.isEmpty() ? id : id.substring(0, 1).toUpperCase() + id.substring(1).toLowerCase();
    }
}
