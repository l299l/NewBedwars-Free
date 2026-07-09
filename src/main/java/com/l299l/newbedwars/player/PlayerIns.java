package com.l299l.newbedwars.player;

import com.l299l.newbedwars.config.Language;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public record PlayerIns(UUID id, String name, Language language,
                        Map<String, List<String>> fastBuyPerCategory) {

    public static Map<String, List<String>> defaultFastBuy() {
        return new HashMap<>();
    }
}
