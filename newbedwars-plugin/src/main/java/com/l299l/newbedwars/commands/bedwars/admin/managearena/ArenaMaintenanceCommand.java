package com.l299l.newbedwars.commands.bedwars.admin.managearena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.maintenance.ArenaMaintenanceService;
import com.l299l.newbedwars.arena.maintenance.ArenaMaintenanceService.Outcome;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ArenaMaintenanceCommand extends BwCommand {
    public enum Action {
        BACKUP("backup", "Back up the arena file and map now.", "<arenaName>", "castle", 3, 3),
        BACKUPS("backups", "List the backups of an arena.", "<arenaName>", "castle", 3, 3),
        RESTORE("restore", "Restore an arena (file and map) from a backup.", "<arenaName> [backupId]", "castle", 3, 4),
        EXPORT("export", "Export an arena (file and map) to a zip.", "<arenaName>", "castle", 3, 3),
        IMPORT("import", "Import an arena from a zip in data/exports (loaded disabled).", "<fileName>", "castle-20261027.zip", 3, 3),
        COPY("copy", "Copy an arena (settings and map) to a new arena; the copy joins the same group.", "<arenaName> <newArenaName>", "castle castle2", 4, 4),
        RESETMAP("resetmap", "Put the arena map back to its saved state now.", "<arenaName>", "castle", 3, 3);

        final String command;
        final String description;
        final String arguments;
        final String example;
        final int minArgs;
        final int maxArgs;

        Action(String command, String description, String arguments, String example, int minArgs, int maxArgs) {
            this.command = command;
            this.description = description;
            this.arguments = arguments;
            this.example = example;
            this.minArgs = minArgs;
            this.maxArgs = maxArgs;
        }
    }

    private final Action action;

    public ArenaMaintenanceCommand(Action action) {
        this.action = action;
    }

    @Override
    public String getName() {
        return action.command;
    }

    @Override
    public String getDescription() {
        return action.description;
    }

    @Override
    public String getSyntax() {
        return "/bw arena " + action.command + " " + action.arguments;
    }

    @Override
    public String getExample() {
        return "/bw arena " + action.command + " " + action.example;
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        if (!player.hasPermission("newbedwars.bw.arena." + action.command)) {
            msg.send(player, "NoPermissions");
            return;
        }
        if (args.length < action.minArgs || args.length > action.maxArgs) {
            usage(player);
            return;
        }
        String target = args[2];
        switch (action) {
            case IMPORT -> reply(player, ArenaMaintenanceService.importArena(target));
            case BACKUP -> reply(player, ArenaMaintenanceService.backup(target));
            case BACKUPS -> list(player, target);
            case RESTORE -> reply(player, ArenaMaintenanceService.restore(target, args.length == 4 ? args[3] : null));
            case EXPORT -> reply(player, ArenaMaintenanceService.export(target));
            case COPY -> reply(player, ArenaMaintenanceService.copy(target, args[3]));
            case RESETMAP -> reply(player, ArenaMaintenanceService.resetMap(target));
        }
    }

    private void reply(Player player, CompletableFuture<Outcome> result) {
        result.thenAccept(outcome -> reply(player, outcome));
    }

    private void reply(Player player, Outcome outcome) {
        msg.send(player, outcome.key(), outcome.replacements());
    }

    private void list(Player player, String arenaName) {
        IArena arena = ArenaRegistry.byName(arenaName);
        if (arena == null) {
            msg.send(player, "ArenaNotExists");
            return;
        }
        String name = arena.getArenaName();
        try {
            List<ArenaMaintenanceService.Backup> backups = ArenaMaintenanceService.backups(name);
            if (backups.isEmpty()) {
                msg.send(player, "BackupListEmpty", Map.of("/arenaname/", name));
                return;
            }
            msg.send(player, "BackupListHeader", Map.of("/arenaname/", name));
            for (ArenaMaintenanceService.Backup b : backups) {
                player.sendMessage(msg.getMsg(player, "BackupListEntry").replace("/id/", b.id())
                        .replace("/parts/", (b.hasArenaFile() ? "arena" : "") + (b.hasArenaFile() && b.hasMap() ? " + " : "")
                                + (b.hasMap() ? "map" : "")));
            }
        } catch (IOException e) {
            msg.send(player, "BackupFailed", Map.of("/arenaname/", name, "/info/", String.valueOf(e.getMessage())));
        }
    }

    void doImport(Player player, String fileName) {
        reply(player, ArenaMaintenanceService.importArena(fileName));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 3) {
            if (action == Action.IMPORT) return ArenaMaintenanceService.exportFiles();
            return new ArrayList<>(ArenaRegistry.names());
        }
        if (args.length == 4 && action == Action.COPY) {
            return List.of(args[2] + "2");
        }
        if (args.length == 4 && action == Action.RESTORE && ArenaRegistry.contains(args[2])) {
            try {
                return new ArrayList<>(ArenaMaintenanceService.backupIds(args[2]));
            } catch (IOException e) {
                NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not list the backups of " + args[2], e);
            }
        }
        return null;
    }
}
