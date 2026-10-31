package com.l299l.newbedwars.common.protocol.payload;

import java.util.ArrayList;
import java.util.List;

/**
 * The arena groups of the network, defined on a lobby server. An arena entry is {@code arena} (that arena on
 * every game server) or {@code server:arena}. The newest {@code revision} (time of the last edit) wins.
 */
public class NetworkGroups {
    public long revision;
    public List<Group> groups = new ArrayList<>();

    public NetworkGroups() {}

    public NetworkGroups(long revision, List<Group> groups) {
        this.revision = revision;
        this.groups = groups;
    }

    public static class Group {
        public String name;
        public String displayItem;
        public List<String> arenas = new ArrayList<>();

        public Group() {}

        public Group(String name, String displayItem, List<String> arenas) {
            this.name = name;
            this.displayItem = displayItem;
            this.arenas = arenas;
        }
    }

    /** The group of {@code arena} on {@code server}: the first group with an entry for it, or null. */
    public String groupOf(String server, String arena) {
        if (groups == null) return null;
        for (Group g : groups) {
            if (g == null || g.arenas == null) continue;
            for (String entry : g.arenas) {
                if (covers(entry, server, arena)) return g.name;
            }
        }
        return null;
    }

    /** The server part of an entry, or null for {@code arena} (= every server). */
    public static String serverOf(String entry) {
        int i = entry == null ? -1 : entry.indexOf(':');
        return i <= 0 ? null : entry.substring(0, i);
    }

    /** The arena part of an entry. */
    public static String arenaOf(String entry) {
        int i = entry == null ? -1 : entry.indexOf(':');
        return i < 0 ? entry : entry.substring(i + 1);
    }

    /** Whether {@code entry} means {@code arena} on {@code server}. */
    public static boolean covers(String entry, String server, String arena) {
        if (entry == null || arena == null || !arenaOf(entry).equalsIgnoreCase(arena)) return false;
        String s = serverOf(entry);
        return s == null || s.equalsIgnoreCase(server);
    }

    /** Whether two entries can mean the same arena ({@code castle} and {@code game1:castle} do). */
    public static boolean overlaps(String a, String b) {
        if (a == null || b == null || !arenaOf(a).equalsIgnoreCase(arenaOf(b))) return false;
        String sa = serverOf(a);
        String sb = serverOf(b);
        return sa == null || sb == null || sa.equalsIgnoreCase(sb);
    }
}
