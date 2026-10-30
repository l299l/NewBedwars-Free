package com.l299l.newbedwars.commands.bedwars.admin.setup.basic;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.arena.setup.DecoUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

public class RemoveCommand extends BwCommand {

    private static final List<String> TEAM_PROPS =
            Arrays.asList("bed", "spawn", "shop", "upgrades", "buildprotpos1", "buildprotpos2", "pos1", "pos2");



    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Removes a team, generator, or a specific team property from the arena.";
    }

    @Override
    public String getSyntax() {
        return "/bw remove <team|generator> <teamName|index> [bed|spawn|shop|upgrades|buildprotpos1|buildprotpos2|pos1|pos2]";
    }

    @Override
    public String getExample() {
        return "/bw remove team red bed";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length < 3) {
            usage(player);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "team" -> handleTeam(player, args, arena);
            case "generator" -> handleGenerator(player, args, arena);
            default -> usage(player);
        }
    }

    private void handleTeam(Player player, String[] args, IArena arena) {
        String teamName = args[2];
        Team team = arena.getTeams().get(teamName);
        if (team == null) {
            msg.send(player, "TeamNotFound");
            return;
        }

        if (args.length == 3) {
            arena.removeTeam(teamName);
            msg.send(player, "TeamRemoved", Map.of("/team/", teamName));
            return;
        }

        String prop = args[3].toLowerCase();
        switch (prop) {
            case "bed" -> {
                if (!team.isBedSet()) { notSet(player, teamName); return; }
                DecoUtils.removeArmorStandAt(team.getTeamBed().getLocation());
                team.clearTeamBed();
            }
            case "spawn" -> {
                if (!team.isSpawnSet()) { notSet(player, teamName); return; }
                DecoUtils.removeArmorStandAt(team.getTeamSpawn());
                team.setTeamSpawn(null);
            }
            case "shop" -> {
                if (!team.isShopSet()) { notSet(player, teamName); return; }
                DecoUtils.removeArmorStandAt(team.getTeamShop().getLocation());
                team.setTeamShop(null);
            }
            case "upgrades" -> {
                if (!team.isUpgradesSet()) { notSet(player, teamName); return; }
                DecoUtils.removeArmorStandAt(team.getTeamUpgrades().getLocation());
                team.setTeamUpgrades(null);
            }
            case "buildprotpos1" -> {
                if (!team.isBuildProtAreaPos1Set()) { notSet(player, teamName); return; }
                team.setTeamBuildProtAreaPos1(null);
            }
            case "buildprotpos2" -> {
                if (!team.isBuildProtAreaPos2Set()) { notSet(player, teamName); return; }
                team.setTeamBuildProtAreaPos2(null);
            }
            case "pos1" -> {
                if (!team.isBasePos1Set()) { notSet(player, teamName); return; }
                team.setTeamBasePos1(null);
            }
            case "pos2" -> {
                if (!team.isBasePos2Set()) { notSet(player, teamName); return; }
                team.setTeamBasePos2(null);
            }
            default -> {
                usage(player);
                return;
            }
        }
        final String finalProp = prop;
        final String finalTeam = teamName;
        msg.send(player, "TeamPropertyCleared", Map.of(
                "/property/", finalProp,
                "/team/", finalTeam));
    }

    private void handleGenerator(Player player, String[] args, IArena arena) {
        try {
            int index = Integer.parseInt(args[2]) - 1;
            Generator removed = arena.removeGenerator(index);
            if (removed != null) {
                String typeName = removed.getType().name();
                String displayIndex = args[2];
                msg.send(player, "GeneratorRemoved", Map.of(
                        "/type/", typeName,
                        "/index/", displayIndex));
            } else {
                msg.send(player, "GeneratorNotFound");
            }
        } catch (NumberFormatException e) {
            usage(player);
        }
    }

    private void notSet(Player player, String teamName) {
        msg.send(player, "TeamPropertyNotSet", Map.of("/team/", teamName));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("team", "generator");
        }
        IArena arena = ArenaRegistry.byWorld(player.getWorld());
        if (arena == null) return null;

        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("team")) {
                return new ArrayList<>(arena.getTeams().keySet());
            } else if (args[1].equalsIgnoreCase("generator")) {
                ArrayList<String> indices = new ArrayList<>();
                int size = arena.getGenerators().size();
                for (int i = 1; i <= size; i++) indices.add(String.valueOf(i));
                return indices;
            }
        } else if (args.length == 4 && args[1].equalsIgnoreCase("team")) {
            return new ArrayList<>(TEAM_PROPS);
        }
        return null;
    }
}
