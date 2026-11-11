package com.l299l.newbedwars.config.data.yaml.guis;

import java.util.Locale;

public final class GuiRefs {
    public enum Kind { FILL, EMPTY, ITEM, CATEGORY, UPGRADE, INVALID }

    public record Ref(Kind kind, String target) {}

    private GuiRefs() {}

    public static Ref slot(String token) {
        if (token == null) return new Ref(Kind.EMPTY, null);
        String t = token.trim();
        if (t.equalsIgnoreCase("fill")) return new Ref(Kind.FILL, null);
        if (t.isEmpty() || t.equalsIgnoreCase("empty")) return new Ref(Kind.EMPTY, null);
        return typed(t, Kind.INVALID);
    }

    public static Ref categoryItem(String token) {
        if (token == null || token.isBlank()) return new Ref(Kind.INVALID, null);
        return typed(token.trim(), Kind.ITEM);
    }

    public static Ref fillOrEmpty(String token) {
        if (token == null || token.isBlank() || token.trim().equalsIgnoreCase("empty")) return new Ref(Kind.EMPTY, null);
        Ref ref = typed(token.trim(), Kind.INVALID);
        return ref.kind() == Kind.ITEM ? ref : new Ref(Kind.INVALID, ref.target());
    }

    private static Ref typed(String t, Kind plain) {
        int colon = t.indexOf(':');
        if (colon < 0) return new Ref(plain, t);
        String type = t.substring(0, colon).toLowerCase(Locale.ROOT);
        String target = t.substring(colon + 1).trim();
        if (target.isEmpty()) return new Ref(Kind.INVALID, null);
        return switch (type) {
            case "ci", "customitem" -> new Ref(Kind.ITEM, target);
            case "cat", "category" -> new Ref(Kind.CATEGORY, target);
            case "upg", "upgrade" -> new Ref(Kind.UPGRADE, target);
            default -> new Ref(Kind.INVALID, target);
        };
    }

    public static boolean refersToItem(String token, String itemName, boolean categoryItem) {
        Ref ref = categoryItem ? categoryItem(token) : slot(token);
        return (ref.kind() == Kind.ITEM || ref.kind() == Kind.UPGRADE) && ref.target() != null
                && ref.target().equalsIgnoreCase(itemName);
    }
}
