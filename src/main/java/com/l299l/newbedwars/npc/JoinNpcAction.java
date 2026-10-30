package com.l299l.newbedwars.npc;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a join NPC does when clicked.
 * <ul>
 *     <li>{@code GROUP <group>} joins the best free arena of a group;</li>
 *     <li>{@code ARENA <arena>} joins that arena;</li>
 *     <li>{@code SELECTOR [group]} opens the arena selector, optionally filtered to a group;</li>
 *     <li>{@code NETWORK <group|arena>} sends the player to a game server (network mode).</li>
 * </ul>
 * A group is the name of an arena group ({@code /bw group}); otherwise it is an arena name pattern
 * ({@code *} = any characters, e.g. {@code solo_*}) or a team size {@code <teams>x<players>} (e.g. {@code 4x1}).
 */
public final class JoinNpcAction {
    public enum Type {
        GROUP(true), ARENA(true), SELECTOR(false), NETWORK(true);

        private final boolean targetRequired;

        Type(boolean targetRequired) {
            this.targetRequired = targetRequired;
        }

        public boolean isTargetRequired() {
            return targetRequired;
        }
    }

    private static final Pattern TEAM_SIZE = Pattern.compile("(\\d+)x(\\d+)", Pattern.CASE_INSENSITIVE);

    private final Type type;
    private final String target;
    private final Pattern namePattern;
    private final int teams;
    private final int maxInTeam;

    private JoinNpcAction(Type type, String target) {
        this.type = type;
        this.target = target == null || target.isBlank() ? null : target.trim();
        Matcher m = this.target == null ? null : TEAM_SIZE.matcher(this.target);
        if (m != null && m.matches()) {
            teams = Integer.parseInt(m.group(1));
            maxInTeam = Integer.parseInt(m.group(2));
            namePattern = null;
        } else {
            teams = -1;
            maxInTeam = -1;
            namePattern = this.target == null ? null : globToRegex(this.target);
        }
    }

    /**
     * @throws IllegalArgumentException with message {@code type} for an unknown type, or {@code target}
     *                                  when the type needs a target and none was given
     */
    public static JoinNpcAction parse(String type, String target) {
        Type t = parseType(type);
        if (t == null) throw new IllegalArgumentException("type");
        if (t.isTargetRequired() && (target == null || target.isBlank())) throw new IllegalArgumentException("target");
        return new JoinNpcAction(t, target);
    }

    /** Like {@link #parse} but returns null instead of throwing (for stored data). */
    public static JoinNpcAction tryParse(String type, String target) {
        try {
            return parse(type, target);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Type parseType(String type) {
        if (type == null) return null;
        try {
            return Type.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Type getType() {
        return type;
    }

    /** The group/arena name, or null (only {@code SELECTOR} may have none). */
    public String getTarget() {
        return target;
    }

    /** Whether {@code arena} belongs to this action's target (an exact name for ARENA, the group otherwise). */
    public boolean matches(IArena arena) {
        if (target == null) return true;
        if (type == Type.ARENA) return arena.getArenaName().equalsIgnoreCase(target);
        ArenaGroup group = ArenaGroupManager.lookup(target);
        if (group != null) return group.contains(arena.getArenaName());
        if (teams > 0) return arena.getTeams().size() == teams && arena.getMaxInTeam() == maxInTeam;
        return namePattern.matcher(arena.getArenaName()).matches();
    }

    private static Pattern globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        for (String part : glob.split("\\*", -1)) {
            if (sb.length() > 0) sb.append(".*");
            sb.append(Pattern.quote(part));
        }
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }

    @Override
    public String toString() {
        return target == null ? type.name() : type.name() + " " + target;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JoinNpcAction other && type == other.type && java.util.Objects.equals(target, other.target);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, target);
    }
}
