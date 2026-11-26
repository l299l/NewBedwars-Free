package com.l299l.newbedwars.text;

import org.bukkit.ChatColor;

import java.util.concurrent.ThreadLocalRandom;

public final class TextAnimator {
    private static final ChatColor[] COLORS = {
            ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA, ChatColor.DARK_RED,
            ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY, ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN,
            ChatColor.AQUA, ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
    };

    private final TextEffect effect;
    private final ChatColor secondColor;
    private final ChatColor pulseColor;
    private int position;
    private boolean up = true;

    public TextAnimator(TextEffect effect, String initialText, ChatColor pulseFallback) {
        this.effect = effect == null ? TextEffect.NONE : effect;
        this.secondColor = randomColor();
        ChatColor leading = leadingColor(color(initialText));
        this.pulseColor = leading != null ? leading : pulseFallback != null ? pulseFallback : randomColor();
    }

    public TextEffect getEffect() {
        return effect;
    }

    public long period(long scrollTicks) {
        return switch (effect) {
            case RAINBOW, GOING_CHANGING -> scrollTicks;
            case PULSING -> 15;
            case RAINBOW_PULSING -> 5;
            case NONE -> 20;
        };
    }

    public String next(String rawText) {
        String text = color(rawText);
        return switch (effect) {
            case RAINBOW -> rainbow(text);
            case GOING_CHANGING -> goingChanging(text);
            case PULSING -> pulsing(text);
            case RAINBOW_PULSING -> randomColor() + text;
            case NONE -> text;
        };
    }

    private String rainbow(String text) {
        StringBuilder out = new StringBuilder();
        String color = ChatColor.WHITE.toString();
        int j = 0;
        while (j < text.length()) {
            int len = ColorCodes.tokenLength(text, j);
            String token = text.substring(j, j + len);
            if (ColorCodes.isCode(text, j)) {
                if (len == 14) {
                    color = token;
                } else if (len == 2 && ChatColor.getByChar(token.charAt(1)) != null) {
                    color = token;
                }
                out.append(token);
            } else {
                out.append(j == position ? randomColor().toString() : color).append(token);
            }
            j += len;
        }
        advance(text);
        return out.toString();
    }

    private String goingChanging(String text) {
        String leading = ColorCodes.leadingColor(text);
        String color = leading != null ? leading : ChatColor.WHITE.toString();
        StringBuilder out = new StringBuilder().append(secondColor);
        int j = leading != null ? leading.length() : 0;
        while (j < text.length()) {
            int len = ColorCodes.tokenLength(text, j);
            String token = text.substring(j, j + len);
            if (ColorCodes.isCode(text, j)) {
                out.append(token);
            } else {
                out.append(j <= position ? secondColor.toString() : color).append(token);
            }
            j += len;
        }
        advance(text);
        return out.toString();
    }

    private String pulsing(String text) {
        String frame = up ? secondColorOf(pulseColor) + ColorCodes.stripCode(text, pulseColor.toString()) : text;
        up = !up;
        return frame;
    }

    private void advance(String text) {
        position++;
        if (position >= text.length()) position = 0;
    }

    private static String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    static ChatColor leadingColor(String text) {
        if (text.length() < 2 || text.charAt(0) != ChatColor.COLOR_CHAR) return null;
        ChatColor color = ChatColor.getByChar(text.charAt(1));
        return color != null && color.isColor() ? color : null;
    }

    public static ChatColor randomColor() {
        return COLORS[ThreadLocalRandom.current().nextInt(COLORS.length)];
    }

    static ChatColor secondColorOf(ChatColor color) {
        return switch (color) {
            case GREEN -> ChatColor.DARK_GREEN;
            case DARK_GREEN -> ChatColor.GREEN;
            case RED -> ChatColor.DARK_RED;
            case DARK_RED -> ChatColor.RED;
            case BLUE -> ChatColor.DARK_BLUE;
            case DARK_BLUE -> ChatColor.BLUE;
            case AQUA -> ChatColor.DARK_AQUA;
            case DARK_AQUA -> ChatColor.AQUA;
            case YELLOW -> ChatColor.GOLD;
            case GOLD -> ChatColor.YELLOW;
            case WHITE -> ChatColor.GRAY;
            case DARK_GRAY -> ChatColor.BLACK;
            case BLACK -> ChatColor.DARK_GRAY;
            default -> ChatColor.WHITE;
        };
    }
}
