package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.common.protocol.payload.NetworkGroups;
import com.l299l.newbedwars.network.NetworkManager;
import com.l299l.newbedwars.network.RemoteArena;
import com.l299l.newbedwars.network.RemoteArenaRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class GroupCommand extends BwCommand {
    private static final List<String> SUBCOMMANDS = List.of("create", "delete", "add", "remove", "list", "info", "setitem");

    @Override
    public String getName() {
        return "group";
    }

    @Override
    public String getDescription() {
        return "Manage arena groups: players waiting in a grouped arena vote for one of the group's maps.";
    }

    @Override
    public String getSyntax() {
        return "/bw group <create|delete|add|remove|list|info|setitem> ...";
    }

    @Override
    public String getExample() {
        return "/bw group add solo castle";
    }

    @Override
    public void perform(Player p, String[] args, IArena arena) {
        if (args.length < 2 || !SUBCOMMANDS.contains(args[1].toLowerCase(Locale.ROOT))) {
            if (!hasPermission(p, "list")) {
                msg.send(p, "NoPermissions");
                return;
            }
            msg.send(p, "GroupUsage");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        if (!hasPermission(p, sub)) {
            msg.send(p, "NoPermissions");
            return;
        }
        ArenaGroupManager groups = ArenaGroupManager.get();
        if (NetworkManager.isGame() && !sub.equals("list") && !sub.equals("info")) {
            msg.send(p, "NetworkGroupOnLobby");
            return;
        }
        switch (sub) {
            case "create" -> create(p, args, groups);
            case "delete" -> delete(p, args, groups);
            case "add" -> add(p, args, groups);
            case "remove" -> remove(p, args, groups);
            case "list" -> list(p, groups);
            case "info" -> info(p, args, groups);
            case "setitem" -> setItem(p, args, groups);
        }
    }

    private static boolean hasPermission(Player p, String sub) {
        return p.isOp() || p.hasPermission("newbedwars.bw.admin") || p.hasPermission("newbedwars.bw.group")
                || p.hasPermission("newbedwars.bw.group." + sub);
    }

    private void create(Player p, String[] args, ArenaGroupManager groups) {
        if (args.length < 3) {
            msg.send(p, "GroupUsage");
            return;
        }
        String name = args[2];
        RemoteArenaRegistry remote = remote();
        if (ArenaRegistry.contains(name) || ArenaRegistry.find(name) != null || (remote != null && remote.byName(name) != null)) {
            msg.send(p, "GroupNameIsArena", r("/group/", name));
            return;
        }
        String item = ArenaGroup.DEFAULT_ITEM;
        if (args.length > 3) {
            item = material(p, args[3]);
            if (item == null) return;
        }
        if (!groups.create(name, item)) {
            msg.send(p, "GroupExists", r("/group/", name));
            return;
        }
        msg.send(p, "GroupCreated", r("/group/", name));
    }

    private void delete(Player p, String[] args, ArenaGroupManager groups) {
        ArenaGroup group = group(p, args, groups);
        if (group == null) return;
        groups.delete(group.getName());
        msg.send(p, "GroupDeleted", r("/group/", group.getName()));
    }

    private void add(Player p, String[] args, ArenaGroupManager groups) {
        if (args.length < 4) {
            msg.send(p, "GroupUsage");
            return;
        }
        ArenaGroup group = group(p, args, groups);
        if (group == null) return;
        RemoteArenaRegistry remote = remote();
        if (remote != null) {
            addNetwork(p, args[3], group, groups, remote);
            return;
        }
        IArena arena = ArenaRegistry.find(args[3]);
        if (arena == null) {
            msg.send(p, "ArenaNotExists");
            return;
        }
        ArenaGroup current = groups.groupOf(arena);
        if (current == group) {
            msg.send(p, "GroupArenaAlreadyIn", r2(group, arena.getArenaName()));
            return;
        }
        if (current != null) {
            msg.send(p, "GroupArenaInOtherGroup", r2(current, arena.getArenaName()));
            return;
        }
        String reason = group.incompatibility(arena, ArenaRegistry::find);
        if (reason != null) {
            HashMap<String, String> r = r2(group, arena.getArenaName());
            r.put("/reason/", reason);
            msg.send(p, "GroupArenaIncompatible", r);
            return;
        }
        group.add(arena.getArenaName());
        groups.changed();
        msg.send(p, "GroupArenaAdded", r2(group, arena.getArenaName()));
    }

    private void addNetwork(Player p, String entry, ArenaGroup group, ArenaGroupManager groups, RemoteArenaRegistry remote) {
        List<RemoteArena> found = remote.resolveEntry(entry);
        if (found.isEmpty()) {
            msg.send(p, "ArenaNotExists");
            return;
        }

        RemoteArena first = found.get(0);
        entry = NetworkGroups.serverOf(entry) == null ? first.getName() : first.getServer() + ":" + first.getName();
        ArenaGroup current = groupWith(groups, entry);
        if (current == group) {
            msg.send(p, "GroupArenaAlreadyIn", r2(group, entry));
            return;
        }
        if (current != null) {
            msg.send(p, "GroupArenaInOtherGroup", r2(current, entry));
            return;
        }
        String reason = group.incompatibility(remote.members(entry), remote::members);
        if (reason != null) {
            HashMap<String, String> r = r2(group, entry);
            r.put("/reason/", reason);
            msg.send(p, "GroupArenaIncompatible", r);
            return;
        }
        group.add(entry);
        groups.changed();
        msg.send(p, "GroupArenaAdded", r2(group, entry));
    }

    private static ArenaGroup groupWith(ArenaGroupManager groups, String entry) {
        for (ArenaGroup g : groups.getGroups()) {
            for (String other : g.getArenas()) {
                if (NetworkGroups.overlaps(entry, other)) return g;
            }
        }
        return null;
    }

    private static RemoteArenaRegistry remote() {
        NetworkManager network = NetworkManager.get();
        return network != null && NetworkManager.isLobby() ? network.getRemoteArenas() : null;
    }

    private static List<String> problems(ArenaGroup group) {
        RemoteArenaRegistry remote = remote();
        if (remote != null) return group.problemsOf(remote::members);
        return group.problems(ArenaRegistry::find);
    }

    private void remove(Player p, String[] args, ArenaGroupManager groups) {
        if (args.length < 4) {
            msg.send(p, "GroupUsage");
            return;
        }
        ArenaGroup group = group(p, args, groups);
        if (group == null) return;
        if (!group.remove(args[3])) {
            msg.send(p, "GroupArenaNotIn", r2(group, args[3]));
            return;
        }
        groups.changed();
        msg.send(p, "GroupArenaRemoved", r2(group, args[3]));
    }

    private void list(Player p, ArenaGroupManager groups) {
        if (groups.getGroups().isEmpty()) {
            msg.send(p, "GroupListEmpty");
            return;
        }
        msg.send(p, "GroupListHeader", r("/count/", String.valueOf(groups.getGroups().size())));
        for (ArenaGroup group : groups.getGroups()) {
            HashMap<String, String> r = r("/group/", group.getName());
            r.put("/arenas/", group.getArenas().isEmpty() ? "-" : String.join(", ", group.getArenas()));
            r.put("/problems/", String.valueOf(problems(group).size()));
            msg.send(p, "GroupListEntry", r);
        }
    }

    private void info(Player p, String[] args, ArenaGroupManager groups) {
        ArenaGroup group = group(p, args, groups);
        if (group == null) return;
        HashMap<String, String> r = r("/group/", group.getName());
        r.put("/arenas/", group.getArenas().isEmpty() ? "-" : String.join(", ", group.getArenas()));
        r.put("/item/", group.getDisplayItem());
        msg.send(p, "GroupInfo", r);
        for (String problem : problems(group)) {
            msg.send(p, "GroupProblem", r("/problem/", problem));
        }
    }

    private void setItem(Player p, String[] args, ArenaGroupManager groups) {
        if (args.length < 4) {
            msg.send(p, "GroupUsage");
            return;
        }
        ArenaGroup group = group(p, args, groups);
        if (group == null) return;
        String item = material(p, args[3]);
        if (item == null) return;
        group.setDisplayItem(item);
        groups.changed();
        HashMap<String, String> r = r("/group/", group.getName());
        r.put("/item/", item);
        msg.send(p, "GroupItemSet", r);
    }

    private ArenaGroup group(Player p, String[] args, ArenaGroupManager groups) {
        if (args.length < 3) {
            msg.send(p, "GroupUsage");
            return null;
        }
        ArenaGroup group = groups.group(args[2]);
        if (group == null) msg.send(p, "GroupNotFound", r("/group/", args[2]));
        return group;
    }

    private String material(Player p, String name) {
        Material m = Material.matchMaterial(name);
        if (m == null || m.isAir() || !m.isItem()) {
            msg.send(p, "GroupInvalidItem", r("/item/", name));
            return null;
        }
        return m.name();
    }

    private static HashMap<String, String> r(String key, String value) {
        HashMap<String, String> r = new HashMap<>();
        r.put(key, value);
        return r;
    }

    private static HashMap<String, String> r2(ArenaGroup group, String arena) {
        HashMap<String, String> r = r("/group/", group.getName());
        r.put("/arena/", arena);
        return r;
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) return new ArrayList<>(SUBCOMMANDS);
        ArenaGroupManager groups = ArenaGroupManager.get();
        String sub = args[1].toLowerCase(Locale.ROOT);
        if (args.length == 3 && !sub.equals("create") && !sub.equals("list") && groups != null) {
            return groups.getGroups().stream().map(ArenaGroup::getName).toList();
        }
        if (args.length == 4 && sub.equals("add") && groups != null) {
            List<String> names = new ArrayList<>();
            RemoteArenaRegistry remote = remote();
            if (remote != null) {
                for (String n : NpcCommand.networkArenaNames(NetworkManager.get())) {
                    if (groupWith(groups, n) == null) names.add(n);
                }
                return names;
            }
            for (IArena a : ArenaRegistry.all()) {
                if (groups.groupOf(a) == null) names.add(a.getArenaName());
            }
            return names;
        }
        if (args.length == 4 && sub.equals("remove") && groups != null) {
            ArenaGroup group = groups.group(args[2]);
            return group == null ? List.of() : new ArrayList<>(group.getArenas());
        }
        return null;
    }
}
