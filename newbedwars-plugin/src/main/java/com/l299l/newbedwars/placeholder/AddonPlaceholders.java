package com.l299l.newbedwars.placeholder;

import com.l299l.newbedwars.api.text.TextPlaceholder;
import com.l299l.newbedwars.apiimpl.view.ApiArena;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public final class AddonPlaceholders {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,32}");
    private static final Set<String> BUILT_IN = builtIn();
    private static final Logger LOG = Logger.getLogger("NewBedwars");
    private static final Map<String, TextPlaceholder> REGISTERED = new ConcurrentHashMap<>();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    private AddonPlaceholders() {
    }

    private static Set<String> builtIn() {
        Set<String> names = new HashSet<>();
        for (String n : new String[]{"server", "online", "player", "teamColor", "teamName", "teamShort", "arena", "players",
                "maxPlayers", "minPlayers", "phase", "phaseTime", "gameTime", "kills", "finalKills", "bedsBroken",
                "datetime", "date", "servername", "map", "status", "teams", "bed", "yourteam", "time",
                "server_name", "arena_name", "max_players"}) {
            names.add(n.toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(names);
    }

    public static void register(String name, TextPlaceholder placeholder) {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid placeholder name (use 1-32 letters, digits or _): " + name);
        }
        if (placeholder == null) throw new IllegalArgumentException("placeholder is null");
        if (BUILT_IN.contains(name.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(name + " is a built-in NewBedwars placeholder");
        }
        if (REGISTERED.putIfAbsent(name, placeholder) != null) {
            throw new IllegalArgumentException("placeholder " + name + " is already registered");
        }
    }

    public static void unregister(String name, TextPlaceholder placeholder) {
        if (REGISTERED.remove(name, placeholder)) FAILED.remove(name);
    }

    public static void clear() {
        REGISTERED.clear();
        FAILED.clear();
    }

    public static boolean isEmpty() {
        return REGISTERED.isEmpty();
    }

    public static String apply(String text, char open, char close, Player viewer, IArena arena) {
        if (REGISTERED.isEmpty() || text == null || text.indexOf(open) < 0) return text;
        for (Map.Entry<String, TextPlaceholder> e : REGISTERED.entrySet()) {
            String token = open + e.getKey() + close;
            if (text.contains(token)) text = text.replace(token, resolve(e.getKey(), e.getValue(), viewer, arena));
        }
        return text;
    }

    private static String resolve(String name, TextPlaceholder placeholder, Player viewer, IArena arena) {
        try {
            String value = placeholder.resolve(viewer, arena == null ? null : new ApiArena(arena));
            return value == null ? "" : value;
        } catch (Exception e) {
            if (FAILED.add(name)) LOG.log(Level.WARNING, "Placeholder " + name + " threw an exception; it renders as empty", e);
            return "";
        }
    }
}
