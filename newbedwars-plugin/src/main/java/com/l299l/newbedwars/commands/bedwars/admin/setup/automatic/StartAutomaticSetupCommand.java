package com.l299l.newbedwars.commands.bedwars.admin.setup.automatic;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.setup.auto.AutoSetupApplier;
import com.l299l.newbedwars.arena.setup.auto.AutoSetupSession;
import com.l299l.newbedwars.arena.setup.auto.MapScanner;
import com.l299l.newbedwars.arena.setup.auto.ScanInterpreter;
import com.l299l.newbedwars.arena.setup.auto.ScanResult;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class StartAutomaticSetupCommand extends BwCommand {
    @Override
    public String getName() {
        return "startAutomaticSetup";
    }

    @Override
    public String getDescription() {
        return "Scans the built map for beds, generator blocks and marker signs, then guides you through what is missing.";
    }

    @Override
    public String getSyntax() {
        return "/bw startAutomaticSetup [radius] | next | skip | cancel";
    }

    @Override
    public String getExample() {
        return "/bw startAutomaticSetup 120";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length >= 2 && !args[1].matches("\\d+")) {
            subCommand(player, args[1].toLowerCase(), arena);
            return;
        }
        int radius = Properties.AutoSetupDefaultRadius;
        if (args.length >= 2) {
            try {
                radius = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                radius = 0;
            }
            if (radius < 1 || radius > Properties.AutoSetupMaxRadius) {
                msg.send(player, "AutoSetup-BadRadius", Map.of("/max/", String.valueOf(Properties.AutoSetupMaxRadius)));
                return;
            }
        }
        startScan(player, arena, radius);
    }

    private void subCommand(Player player, String name, IArena arena) {
        switch (name) {
            case "cancel" -> msg.send(player, AutoSetupSession.cancel(player) ? "AutoSetup-Cancelled" : "AutoSetup-NoSession");
            case "skip", "next" -> {
                AutoSetupSession session = AutoSetupSession.of(player, arena);
                if (session == null) {
                    msg.send(player, "AutoSetup-NoSession");
                } else if (name.equals("skip")) {
                    session.skip(player);
                } else {
                    session.prompt(player);
                }
            }
            default -> usage(player);
        }
    }

    private void startScan(Player player, IArena arena, int radius) {
        if (AutoSetupSession.isScanning(player)) {
            msg.send(player, "AutoSetup-AlreadyRunning");
            return;
        }
        Location center = player.getLocation();
        UUID id = player.getUniqueId();
        AutoSetupSession session = AutoSetupSession.start(player, arena);
        MapScanner scanner = new MapScanner(arena.getArenaWorld(), center.getBlockX(), center.getBlockZ(), radius,
                percent -> withPlayer(id, p -> msg.send(p, "AutoSetup-Progress", Map.of("/percent/", String.valueOf(percent)))),
                data -> {
                    AutoSetupSession.scanEnded(id);
                    withPlayer(id, p -> finishScan(p, arena, session,
                            ScanInterpreter.interpret(data, new ScanInterpreter.Settings(
                                    Properties.AutoSetupGeneratorRadius, arena.getArenaWorld().getMinHeight()))));
                });
        AutoSetupSession.scanStarted(player, scanner);
        msg.send(player, "AutoSetup-Scanning", Map.of("/chunks/", String.valueOf(scanner.chunks()), "/radius/", String.valueOf(radius)));
        scanner.start();
    }

    private void finishScan(Player player, IArena arena, AutoSetupSession session, ScanResult result) {
        AutoSetupApplier.Outcome outcome = AutoSetupApplier.apply(arena, result, session.areas());
        msg.send(player, "AutoSetup-Done", Map.of(
                "/teams/", String.valueOf(outcome.teams()),
                "/diamond/", String.valueOf(outcome.diamondGenerators()),
                "/emerald/", String.valueOf(outcome.emeraldGenerators()),
                "/signs/", String.valueOf(outcome.signs())));
        for (Team team : arena.getTeams().values()) player.sendMessage(AutoSetupSession.summaryLine(player, team));
        for (ScanResult.Warning warning : outcome.warnings()) {
            msg.send(player, warning.key(), Map.of("/detail/", warning.detail()));
        }
        session.prompt(player);
    }

    private void withPlayer(UUID id, Consumer<Player> action) {
        Player player = Bukkit.getPlayer(id);
        if (player != null) action.accept(player);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) return List.of("next", "skip", "cancel", String.valueOf(Properties.AutoSetupDefaultRadius));
        return null;
    }
}
