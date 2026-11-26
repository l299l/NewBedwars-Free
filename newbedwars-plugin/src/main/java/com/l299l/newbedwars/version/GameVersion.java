package com.l299l.newbedwars.version;

public final class GameVersion {

    private GameVersion() {}

    public static boolean isAtLeast(String bukkitVersion, int major, int minor, int patch) {
        int[] v = parse(bukkitVersion);
        if (v.length == 0) return false;
        int maj = v[0];
        int min = v.length > 1 ? v[1] : 0;
        int pat = v.length > 2 ? v[2] : 0;
        if (maj != major) return maj > major;
        if (min != minor) return min > minor;
        return pat >= patch;
    }

    public static int[] parse(String bukkitVersion) {
        if (bukkitVersion == null) return new int[0];
        String[] parts = bukkitVersion.trim().split("-", 2)[0].split("\\.");
        int count = 0;
        while (count < parts.length && isNumber(parts[count])) count++;
        int[] out = new int[count];
        for (int i = 0; i < count; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }

    private static boolean isNumber(String part) {
        if (part.isEmpty() || part.length() > 9) return false;
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }
}
