package com.l299l.newbedwars.arena.setup;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What the setup info shows ({@code /bw arena} in chat and in its GUIs): one source for both. */
public final class ArenaInfo {
    private ArenaInfo() {
    }

    /**
     * One team setting: a single point ({@code pos1}) or, when {@code area}, the region {@code pos1}-{@code pos2}.
     * {@code labelKey} is a language key, see {@link #label(Player)}.
     */
    public record TeamSetting(String labelKey, Material icon, boolean area, boolean set, Location pos1, Location pos2) {
        public String label(Player viewer) {
            return text(viewer, labelKey);
        }

        static TeamSetting point(String label, Material icon, boolean set, Location pos) {
            return new TeamSetting(label, icon, false, set, pos, null);
        }

        static TeamSetting area(String label, Material icon, boolean set, Location pos1, Location pos2) {
            return new TeamSetting(label, icon, true, set, pos1, pos2);
        }
    }

    public static List<TeamSetting> teamSettings(Team team, boolean advanced) {
        List<TeamSetting> list = new ArrayList<>();
        list.add(TeamSetting.point("Info-Bed", Material.RED_BED, team.isBedSet(),
                team.isBedSet() ? team.getTeamBed().getLocation() : null));
        list.add(TeamSetting.point("Info-Spawn", Material.BEDROCK, team.isSpawnSet(), team.getTeamSpawn()));
        list.add(TeamSetting.point("Info-Shop", Material.CHEST, team.isShopSet(),
                team.isShopSet() ? team.getTeamShop().getLocation() : null));
        list.add(TeamSetting.point("Info-Upgrades", Material.ANVIL, team.isUpgradesSet(),
                team.isUpgradesSet() ? team.getTeamUpgrades().getLocation() : null));
        list.add(TeamSetting.point("Info-Generator", Material.DROPPER, team.isGeneratorSet(),
                team.isGeneratorSet() ? team.getGenerator().getLocation() : null));
        list.add(TeamSetting.area("Info-BuildProt", Material.RED_CONCRETE, team.isBuildProtAreaPosSet(),
                team.getTeamBuildProtAreaPos1(), team.getTeamBuildProtAreaPos2()));
        list.add(TeamSetting.area("Info-BaseArea", Material.RED_STAINED_GLASS, team.isBasePosSet(),
                team.getTeamBasePos1(), team.getTeamBasePos2()));
        if (advanced) {
            list.add(TeamSetting.point("Info-Accessories", Material.DIAMOND_HORSE_ARMOR, team.isAccessoriesSet(),
                    team.isAccessoriesSet() ? team.getTeamAccessories() : null));
        }
        return list;
    }

    /** The boolean gamerules shown in advanced setup, by name. */
    public static Map<String, Boolean> gamerules(Gamerules g) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put("RandomTeams", g.RandomTeams);
        map.put("AllowParties", g.AllowParties);
        map.put("AllowSpectators", g.AllowSpectators);
        map.put("AllowTeamChat", g.AllowTeamChat);
        map.put("AllowGlobalChat", g.AllowGlobalChat);
        map.put("AllowPrivateChat", g.AllowPrivateChat);
        map.put("AllowTeamDamage", g.AllowTeamDamage);
        map.put("MakeSwordsPermanent", g.MakeSwordsPermanent);
        map.put("AllowMultipleGolems", g.AllowMultipleGolems);
        map.put("SplitPartiesToStart", g.SplitPartiesToStart);
        map.put("MapVote", g.MapVote);
        return map;
    }

    public static int generatorCount(IArena arena, GeneratorType type) {
        int count = 0;
        for (Generator g : arena.getGenerators()) {
            if (g.getType() == type) count++;
        }
        return count;
    }

    /** A setup info text in the viewer's language. */
    public static String text(Player viewer, String key) {
        return NewBedwars.plugin.getMessages().getMsg(viewer, key);
    }

    private static String notSet(Player viewer) {
        return ChatColor.RED + text(viewer, "Info-NotSet");
    }

    /** White block coordinates, or red "not set". */
    public static String loc(Player viewer, Location loc) {
        if (loc == null) return notSet(viewer);
        return ChatColor.WHITE + String.valueOf(loc.getBlockX()) + " " + loc.getBlockY() + " " + loc.getBlockZ();
    }

    public static String tick(boolean set) {
        return set ? ChatColor.GREEN + "✔" : ChatColor.RED + "✘";
    }

    /** Green true / red false / red "not set". */
    public static String bool(Player viewer, Boolean value) {
        if (value == null) return notSet(viewer);
        return value ? ChatColor.GREEN + "true" : ChatColor.RED + "false";
    }

    /** White value, or red "not set" when {@code null}. */
    public static String value(Player viewer, Object value) {
        return value != null ? ChatColor.WHITE + String.valueOf(value) : notSet(viewer);
    }
}
