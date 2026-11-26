package com.l299l.newbedwars.scoreboard;

import com.l299l.newbedwars.text.ColorCodes;

public final class LinePacker {
    public static final int PART_LIMIT = 64;

    public record Packed(String prefix, String suffix) {
    }

    private LinePacker() {
    }

    public static Packed pack(String line) {
        StringBuilder prefix = new StringBuilder();
        StringBuilder suffix = new StringBuilder();
        String color = "";
        StringBuilder formats = new StringBuilder();
        boolean inSuffix = false;
        int i = 0;
        while (i < line.length()) {
            int len = ColorCodes.tokenLength(line, i);
            String token = line.substring(i, i + len);
            i += len;
            StringBuilder target = inSuffix ? suffix : prefix;
            if (target.length() + len > PART_LIMIT) {
                if (inSuffix) break;
                inSuffix = true;
                target = suffix;
                target.append(color).append(formats);
                if (target.length() + len > PART_LIMIT) break;
            }
            target.append(token);
            if (ColorCodes.isCode(token, 0) && len >= 2) {
                char code = Character.toLowerCase(token.charAt(1));
                if (len == 14) {
                    color = token;
                    formats.setLength(0);
                } else if (code == 'r') {
                    color = "";
                    formats.setLength(0);
                } else if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    color = token;
                    formats.setLength(0);
                } else if (code >= 'k' && code <= 'o') {
                    if (formats.indexOf(token) < 0) formats.append(token);
                }
            }
        }
        return new Packed(prefix.toString(), suffix.toString());
    }
}
