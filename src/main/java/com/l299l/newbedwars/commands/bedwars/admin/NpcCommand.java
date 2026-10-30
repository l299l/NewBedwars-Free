package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.npc.CitizensBridge;
import com.l299l.newbedwars.npc.JoinNpc;
import com.l299l.newbedwars.npc.JoinNpcAction;
import com.l299l.newbedwars.npc.JoinNpcManager;
import com.l299l.newbedwars.npc.fallback.FallbackNpc;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/** {@code /bw npc set|clear|info|list|create|remove}: lobby join NPCs (Citizens or fallback). */
public class NpcCommand extends SubCommand {
    private static final List<String> SUBCOMMANDS = List.of("set", "clear", "info", "list", "create", "remove");
    private final Messages msg;

    public NpcCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public String getName() {
        return "npc";
    }

    @Override
    public String getDescription() {
        return "Manage lobby join NPCs (Citizens NPCs or simple fallback NPCs).";
    }

    @Override
    public String getSyntax() {
        return "/bw npc <set|clear|info|list|create|remove> ...";
    }

    @Override
    public String getExample() {
        return "/bw npc set GROUP solo_*";
    }

    @Override
    public void perform(Player p, String[] args, IArena arena) {
        if (!p.hasPermission("newbedwars.bw.npc") && !p.isOp()) {
            msg.send(p, "NoPermissions");
            return;
        }
        JoinNpcManager manager = NewBedwars.plugin.getJoinNpcManager();
        if (args.length < 2) {
            msg.send(p, "NpcUsage");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "set" -> set(p, args, manager);
            case "clear" -> clear(p, manager);
            case "info" -> info(p, args, manager);
            case "list" -> list(p, manager);
            case "create" -> create(p, args, manager);
            case "remove" -> remove(p, args, manager);
            default -> msg.send(p, "NpcUsage");
        }
    }

    private void set(Player p, String[] args, JoinNpcManager manager) {
        CitizensBridge citizens = requireCitizens(p, manager);
        if (citizens == null) return;
        if (args.length < 3) {
            msg.send(p, "NpcUsage");
            return;
        }
        JoinNpcAction action = parseAction(p, args[2], args.length > 3 ? args[3] : null);
        if (action == null) return;
        String name = citizens.selectedName(p);
        if (name == null || !citizens.attach(p, action)) {
            msg.send(p, "NpcNoneSelected");
            return;
        }
        msg.send(p, "NpcSet", replace(name, action));
    }

    private void clear(Player p, JoinNpcManager manager) {
        CitizensBridge citizens = requireCitizens(p, manager);
        if (citizens == null) return;
        String name = citizens.selectedName(p);
        if (name == null) {
            msg.send(p, "NpcNoneSelected");
            return;
        }
        if (!citizens.detach(p)) {
            msg.send(p, "NpcNotAttached");
            return;
        }
        msg.send(p, "NpcCleared", replace(name, null));
    }

    private void info(Player p, String[] args, JoinNpcManager manager) {
        if (args.length > 2) {
            FallbackNpc npc = fallback(p, args[2], manager);
            if (npc == null) return;
            JoinNpcAction action = npc.joinAction();
            msg.send(p, "NpcInfo", replace("#" + npc.id + " " + npc.entityType, action));
            return;
        }
        CitizensBridge citizens = requireCitizens(p, manager);
        if (citizens == null) return;
        String name = citizens.selectedName(p);
        if (name == null) {
            msg.send(p, "NpcNoneSelected");
            return;
        }
        JoinNpc npc = citizens.selected(p);
        if (npc == null) {
            msg.send(p, "NpcNotAttached");
            return;
        }
        msg.send(p, "NpcInfo", replace(name, npc.action()));
    }

    private void list(Player p, JoinNpcManager manager) {
        List<JoinNpc> npcs = manager.allNpcs();
        if (npcs.isEmpty()) {
            msg.send(p, "NpcListEmpty");
            return;
        }
        msg.send(p, "NpcListHeader", new HashMap<>() {{ put("/count/", String.valueOf(npcs.size())); }});
        for (JoinNpc npc : npcs) {
            HashMap<String, String> r = replace(npc.displayName(), npc.action());
            Location loc = npc.location();
            r.put("/location/", loc == null ? "-" : loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
            msg.send(p, "NpcListEntry", r);
        }
    }

    private void create(Player p, String[] args, JoinNpcManager manager) {
        if (args.length < 4) {
            msg.send(p, "NpcUsage");
            return;
        }
        EntityType type;
        try {
            type = EntityType.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            type = null;
        }
        if (!JoinNpcManager.isUsableEntityType(type)) {
            msg.send(p, "NpcInvalidEntity");
            return;
        }
        if (Arena.arenaByWorld.get(p.getWorld()) != null) {
            msg.send(p, "NpcNotInArenaWorld");
            return;
        }
        JoinNpcAction action = parseAction(p, args[3], args.length > 4 ? args[4] : null);
        if (action == null) return;
        FallbackNpc npc = manager.createFallback(p.getLocation(), type, action);
        HashMap<String, String> r = replace("#" + npc.id + " " + npc.entityType, action);
        r.put("/id/", String.valueOf(npc.id));
        msg.send(p, "NpcCreated", r);
    }

    private void remove(Player p, String[] args, JoinNpcManager manager) {
        if (args.length < 3) {
            msg.send(p, "NpcUsage");
            return;
        }
        FallbackNpc npc = fallback(p, args[2], manager);
        if (npc == null) return;
        manager.removeFallback(npc.id);
        msg.send(p, "NpcRemoved", new HashMap<>() {{ put("/id/", String.valueOf(npc.id)); }});
    }

    private CitizensBridge requireCitizens(Player p, JoinNpcManager manager) {
        CitizensBridge citizens = manager.getCitizens();
        if (citizens == null) msg.send(p, "NpcCitizensMissing");
        return citizens;
    }

    private FallbackNpc fallback(Player p, String id, JoinNpcManager manager) {
        FallbackNpc npc = null;
        try {
            npc = manager.fallback(Integer.parseInt(id.replace("#", "")));
        } catch (NumberFormatException ignored) {
        }
        if (npc == null) msg.send(p, "NpcNotFound", new HashMap<>() {{ put("/id/", id); }});
        return npc;
    }

    /** Parses and validates an action; sends the reason and returns null when it is invalid. */
    private JoinNpcAction parseAction(Player p, String type, String target) {
        JoinNpcAction action;
        try {
            action = JoinNpcAction.parse(type, target);
        } catch (IllegalArgumentException e) {
            if ("target".equals(e.getMessage())) {
                msg.send(p, "NpcTargetRequired", new HashMap<>() {{ put("/action/", type.toUpperCase(Locale.ROOT)); }});
            } else {
                msg.send(p, "NpcInvalidAction");
            }
            return null;
        }
        if (action.getType() == JoinNpcAction.Type.ARENA && ArenaJoinService.arenaByName(action.getTarget()) == null) {
            msg.send(p, "ArenaNotExists");
            return null;
        }
        return action;
    }

    private static HashMap<String, String> replace(String npc, JoinNpcAction action) {
        HashMap<String, String> r = new HashMap<>();
        r.put("/npc/", npc);
        r.put("/action/", action == null ? "-" : action.toString());
        return r;
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) return new ArrayList<>(SUBCOMMANDS);
        String sub = args[1].toLowerCase(Locale.ROOT);
        int actionIndex = sub.equals("set") ? 2 : sub.equals("create") ? 3 : -1;
        if (sub.equals("create") && args.length == 3) {
            return Arrays.stream(EntityType.values()).filter(JoinNpcManager::isUsableEntityType)
                    .map(t -> t.name().toLowerCase(Locale.ROOT)).toList();
        }
        if (actionIndex > 0 && args.length == actionIndex + 1) {
            return Arrays.stream(JoinNpcAction.Type.values()).map(Enum::name).toList();
        }
        if (actionIndex > 0 && args.length == actionIndex + 2) {
            JoinNpcAction.Type type = JoinNpcAction.parseType(args[actionIndex]);
            if (type == null) return List.of();
            List<String> names = new ArrayList<>();
            if (type != JoinNpcAction.Type.ARENA) {
                com.l299l.newbedwars.arena.group.ArenaGroupManager groups = com.l299l.newbedwars.arena.group.ArenaGroupManager.get();
                if (groups != null) {
                    for (com.l299l.newbedwars.arena.group.ArenaGroup g : groups.getGroups()) names.add(g.getName());
                }
            }
            if (type == JoinNpcAction.Type.ARENA || type == JoinNpcAction.Type.NETWORK) {
                for (IArena a : Arena.arenaByName.values()) names.add(a.getArenaName());
            }
            return names;
        }
        if ((sub.equals("remove") || sub.equals("info")) && args.length == 3) {
            return NewBedwars.plugin.getJoinNpcManager().getFallbackNpcs().stream().map(n -> String.valueOf(n.id)).toList();
        }
        return null;
    }
}
