package com.l299l.newbedwars.placeholder;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record MessageContext(boolean aboutViewer, Player subject, Map<String, Player> targets) {
    private static final MessageContext VIEWER = new MessageContext(true, null, Map.of());

    public static MessageContext viewer() {
        return VIEWER;
    }

    public static MessageContext about(Player subject) {
        return new MessageContext(false, subject, Map.of());
    }

    public static MessageContext named(String name) {
        boolean lookUp = PapiText.isEnabled() && name != null && !name.isEmpty();
        return about(lookUp ? Bukkit.getPlayerExact(name) : null);
    }

    public static MessageContext kill(Player victim, Player killer) {
        Map<String, Player> targets = new HashMap<>();
        targets.put("victim", victim);
        targets.put("killer", killer);
        return new MessageContext(false, victim, Collections.unmodifiableMap(targets));
    }
}
