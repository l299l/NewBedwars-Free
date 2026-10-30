package com.l299l.newbedwars.text;

import org.bukkit.ChatColor;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Produces the frames of a {@link TextEffect}. Call {@link #next(String)} once per tick of the caller's timer with the
 * current text ({@code &} or {@code §} color codes); the result is the colored frame.
 */
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

    /**
     * @param initialText   the first text, used to pick the pulsing color from its leading color code
     * @param pulseFallback pulsing color when the text has no leading color, or {@code null} for a random one
     */
    public TextAnimator(TextEffect effect, String initialText, ChatColor pulseFallback) {
        this.effect = effect == null ? TextEffect.NONE : effect;
        this.secondColor = randomColor();
        ChatColor leading = leadingColor(color(initialText));
        this.pulseColor = leading != null ? leading : pulseFallback != null ? pulseFallback : randomColor();
    }

    public TextEffect getEffect() {
        return effect;
    }

    /** Ticks between frames for the non-scrolling effects; scrolling ones run as fast as the caller likes. */
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
        ChatColor color = ChatColor.WHITE;
        for (int j = 0; j < text.length(); j++) {
            char c = text.charAt(j);
            if (c == ChatColor.COLOR_CHAR && j + 1 < text.length()) {
                ChatColor code = ChatColor.getByChar(text.charAt(j + 1));
                if (code != null) color = code;
                out.append(c).append(text.charAt(j + 1));
                j++;
                continue;
            }
            out.append(j == position ? randomColor() : color).append(c);
        }
        advance(text);
        return out.toString();
    }

    private String goingChanging(String text) {
        ChatColor leading = leadingColor(text);
        ChatColor color = leading != null ? leading : ChatColor.WHITE;
        StringBuilder out = new StringBuilder().append(secondColor);
        for (int j = leading != null ? 2 : 0; j < text.length(); j++) {
            out.append(j <= position ? secondColor : color).append(text.charAt(j));
        }
        advance(text);
        return out.toString();
    }

    private String pulsing(String text) {
        String frame = up ? secondColorOf(pulseColor) + text.replace(pulseColor.toString(), "") : text;
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

    /** Leading color of {@code text} (already translated), or {@code null}. */
    static ChatColor leadingColor(String text) {
        if (text.length() < 2 || text.charAt(0) != ChatColor.COLOR_CHAR) return null;
        ChatColor color = ChatColor.getByChar(text.charAt(1));
        return color != null && color.isColor() ? color : null;
    }

    /** A random color (no formats). */
    public static ChatColor randomColor() {
        return COLORS[ThreadLocalRandom.current().nextInt(COLORS.length)];
    }

    /** The light/dark partner of {@code color}, used by pulsing. */
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
