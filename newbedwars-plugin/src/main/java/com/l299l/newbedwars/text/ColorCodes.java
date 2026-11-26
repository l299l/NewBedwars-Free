package com.l299l.newbedwars.text;

import org.bukkit.ChatColor;

public final class ColorCodes {
    private ColorCodes() {
    }

    public static boolean hasHex(String text) {
        return text != null && (text.contains("§x") || text.contains("§X"));
    }

    public static int tokenLength(String text, int index) {
        char c = text.charAt(index);
        int n = text.length();
        if (c == ChatColor.COLOR_CHAR) {
            if (index + 1 >= n) return 1;
            char next = text.charAt(index + 1);
            if ((next == 'x' || next == 'X') && isHexSequence(text, index)) return 14;
            return 2;
        }
        if (Character.isHighSurrogate(c) && index + 1 < n && Character.isLowSurrogate(text.charAt(index + 1))) return 2;
        return 1;
    }

    public static boolean isCode(String text, int index) {
        return text.charAt(index) == ChatColor.COLOR_CHAR;
    }

    private static boolean isHexSequence(String text, int index) {
        if (index + 14 > text.length()) return false;
        for (int k = 1; k < 7; k++) {
            int at = index + 2 * k;
            if (text.charAt(at) != ChatColor.COLOR_CHAR || Character.digit(text.charAt(at + 1), 16) < 0) return false;
        }
        return true;
    }

    public static String stripCode(String text, String code) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int len = tokenLength(text, i);
            String token = text.substring(i, i + len);
            if (!token.equals(code)) out.append(token);
            i += len;
        }
        return out.toString();
    }

    public static String leadingColor(String text) {
        if (text.length() < 2 || text.charAt(0) != ChatColor.COLOR_CHAR) return null;
        int len = tokenLength(text, 0);
        if (len == 14) return text.substring(0, 14);
        ChatColor color = ChatColor.getByChar(text.charAt(1));
        return color != null && color.isColor() ? text.substring(0, 2) : null;
    }
}
