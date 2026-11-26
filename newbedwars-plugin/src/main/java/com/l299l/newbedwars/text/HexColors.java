package com.l299l.newbedwars.text;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HexColors {
    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");

    private HexColors() {
    }

    public static String translate(String text) {
        if (text == null || !text.contains("&#")) return text;
        Matcher matcher = HEX.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            StringBuilder color = new StringBuilder().append(ChatColor.COLOR_CHAR).append('x');
            for (char digit : matcher.group(1).toCharArray()) {
                color.append(ChatColor.COLOR_CHAR).append(digit);
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(color.toString()));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
