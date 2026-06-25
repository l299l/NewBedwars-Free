package com.l299l.newbedwars.player;

import com.l299l.newbedwars.config.Language;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * fastBuyPerCategory maps category ID → ordered list of item names (max 3 per category).
 * An empty map means "use the YAML home-page default."
 */
public record PlayerIns(UUID id, String name, Language language,
                        Map<String, List<String>> fastBuyPerCategory) {

    public static Map<String, List<String>> defaultFastBuy() {
        return new HashMap<>();
    }
}
