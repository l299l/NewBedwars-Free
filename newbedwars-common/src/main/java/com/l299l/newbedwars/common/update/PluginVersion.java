package com.l299l.newbedwars.common.update;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginVersion implements Comparable<PluginVersion> {
    private static final Pattern NUMBERS = Pattern.compile("\\d+(\\.\\d+)*");
    private static final Pattern STAGE = Pattern.compile("(alpha|beta|rc|pre)[.\\-]?(\\d*)");
    private static final int ALPHA = 0, BETA = 1, RC = 2, RELEASE = 3;

    private final String text;
    private final int[] numbers;
    private final int stage;
    private final int stageNumber;

    private PluginVersion(String text, int[] numbers, int stage, int stageNumber) {
        this.text = text;
        this.numbers = numbers;
        this.stage = stage;
        this.stageNumber = stageNumber;
    }

    public static PluginVersion parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        if (s.toLowerCase(Locale.ROOT).endsWith("-proxy")) s = s.substring(0, s.length() - "-proxy".length());
        int dash = s.indexOf('-');
        String head = dash < 0 ? s : s.substring(0, dash);
        String tail = dash < 0 ? "" : s.substring(dash + 1).toLowerCase(Locale.ROOT);
        if (!NUMBERS.matcher(head).matches()) return null;
        String[] parts = head.split("\\.");
        int[] numbers = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) numbers[i] = Integer.parseInt(parts[i]);
        } catch (NumberFormatException e) {
            return null;
        }
        int stage = RELEASE;
        int stageNumber = 0;
        if (!tail.isEmpty()) {
            Matcher m = STAGE.matcher(tail);
            if (!m.matches()) return null;
            stage = switch (m.group(1)) {
                case "alpha" -> ALPHA;
                case "beta" -> BETA;
                default -> RC;
            };
            stageNumber = m.group(2).isEmpty() ? 0 : Integer.parseInt(m.group(2));
        }
        return new PluginVersion(s, numbers, stage, stageNumber);
    }

    public Channel channel() {
        return switch (stage) {
            case ALPHA -> Channel.ALPHA;
            case BETA, RC -> Channel.BETA;
            default -> Channel.RELEASE;
        };
    }

    public boolean isNewerThan(PluginVersion other) {
        return other != null && compareTo(other) > 0;
    }

    @Override
    public int compareTo(PluginVersion o) {
        int length = Math.max(numbers.length, o.numbers.length);
        for (int i = 0; i < length; i++) {
            int a = i < numbers.length ? numbers[i] : 0;
            int b = i < o.numbers.length ? o.numbers[i] : 0;
            if (a != b) return Integer.compare(a, b);
        }
        if (stage != o.stage) return Integer.compare(stage, o.stage);
        return Integer.compare(stageNumber, o.stageNumber);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PluginVersion v && compareTo(v) == 0;
    }

    @Override
    public int hashCode() {
        int length = numbers.length;
        while (length > 1 && numbers[length - 1] == 0) length--;
        return 31 * (31 * Arrays.hashCode(Arrays.copyOf(numbers, length)) + stage) + stageNumber;
    }

    @Override
    public String toString() {
        return text;
    }
}
