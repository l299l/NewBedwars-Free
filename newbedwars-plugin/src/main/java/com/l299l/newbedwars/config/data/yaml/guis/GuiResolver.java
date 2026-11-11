package com.l299l.newbedwars.config.data.yaml.guis;

import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.gui.GUISave;
import com.l299l.newbedwars.gui.configuration.game.guis.ShopGUI;
import com.l299l.newbedwars.gui.model.GUICategory;
import com.l299l.newbedwars.gui.model.GUIEntry;
import com.l299l.newbedwars.gui.model.GUIUpgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class GuiResolver {
    private final Function<String, CustomItem> items;

    public GuiResolver(Function<String, CustomItem> items) {
        this.items = items;
    }

    public GUISave resolve(GuiDefinition d) {
        List<String> problems = new ArrayList<>();
        Class<?> guiClass = d.guiClass().equalsIgnoreCase("ShopGui") ? ShopGUI.class : null;
        GUISave save = new GUISave(guiClass, d.id(), d.title(), d.size(), d.closeOnTransaction());
        save.setDefinition(d);
        save.setHeaderSize(Math.max(0, Math.min(d.headerSize(), d.size())));
        save.setFooterSize(Math.max(0, Math.min(d.footerSize(), d.size() - save.getHeaderSize())));

        for (GuiDefinition.Category c : d.categories()) {
            CustomItem icon = items.apply(c.icon());
            if (icon == null) {
                problems.add("category " + c.id() + ": unknown icon item " + c.icon());
                continue;
            }
            List<GUIEntry> entries = new ArrayList<>();
            for (String token : c.items()) {
                GUIEntry entry = categoryEntry(token, "category " + c.id(), problems);
                if (entry != null) entries.add(entry);
            }
            save.addCategory(new GUICategory(c.id(), c.name(), icon, entries, c.description()));
        }

        CustomItem fill = fillOrEmpty(d.fill(), "Fill", problems);
        CustomItem empty = fillOrEmpty(d.empty(), "Empty", problems);
        for (int i = 0; i < d.slots().size(); i++) {
            String token = d.slots().get(i);
            GuiRefs.Ref ref = GuiRefs.slot(token);
            String where = "slot " + (i + 1);
            GUIEntry entry = switch (ref.kind()) {
                case FILL -> fill;
                case EMPTY -> empty;
                case ITEM -> item(ref.target(), where, problems);
                case UPGRADE -> upgrade(ref.target(), where, problems);
                case CATEGORY -> {
                    GUICategory cat = save.getCategories().get(ref.target());
                    if (cat == null) problems.add(where + ": unknown category " + ref.target());
                    yield cat;
                }
                case INVALID -> {
                    problems.add(where + ": invalid value " + token);
                    yield null;
                }
            };
            save.setItem(i, entry);
        }
        save.setProblems(problems);
        return save;
    }

    private GUIEntry categoryEntry(String token, String where, List<String> problems) {
        GuiRefs.Ref ref = GuiRefs.categoryItem(token);
        return switch (ref.kind()) {
            case ITEM -> item(ref.target(), where, problems);
            case UPGRADE -> upgrade(ref.target(), where, problems);
            default -> {
                problems.add(where + ": invalid value " + token);
                yield null;
            }
        };
    }

    private CustomItem fillOrEmpty(String token, String where, List<String> problems) {
        GuiRefs.Ref ref = GuiRefs.fillOrEmpty(token);
        if (ref.kind() == GuiRefs.Kind.EMPTY) return null;
        if (ref.kind() == GuiRefs.Kind.INVALID) {
            problems.add(where + ": invalid value " + token);
            return null;
        }
        return item(ref.target(), where, problems);
    }

    private CustomItem item(String name, String where, List<String> problems) {
        CustomItem item = items.apply(name);
        if (item == null) problems.add(where + ": unknown item " + name);
        return item;
    }

    private GUIUpgrade upgrade(String name, String where, List<String> problems) {
        CustomItem icon = items.apply(name);
        if (icon == null) {
            problems.add(where + ": no icon item for upgrade " + name);
            return null;
        }
        return new GUIUpgrade(name, icon);
    }
}
