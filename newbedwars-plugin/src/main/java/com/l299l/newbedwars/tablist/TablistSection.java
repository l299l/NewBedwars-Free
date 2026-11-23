package com.l299l.newbedwars.tablist;

import java.util.List;

public record TablistSection(List<String> header, List<String> footer, String playerFormat) {
    public static final TablistSection EMPTY = new TablistSection(null, null, null);

    public TablistSection merge(TablistSection fallback) {
        if (fallback == null) return this;
        return new TablistSection(header != null ? header : fallback.header,
                footer != null ? footer : fallback.footer,
                playerFormat != null ? playerFormat : fallback.playerFormat);
    }
}
