package com.l299l.newbedwars.arena.setup.auto;

import com.l299l.newbedwars.utils.TeamColor;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ScanInterpreter {
    public static final int VOID_MARGIN = 10;
    public static final int MANY_GENERATORS = 16;

    public record Settings(int generatorRadius, int minY) {}

    private ScanInterpreter() {}

    public static ScanResult interpret(ScanData data, Settings settings) {
        ScanResult result = new ScanResult();
        addTeams(data, result);
        if (result.teams.isEmpty()) result.warnings.add(new ScanResult.Warning("AutoSetup-WarnNoBeds", ""));
        List<Pos> markedDiamond = new ArrayList<>();
        List<Pos> markedEmerald = new ArrayList<>();
        applySigns(data, result, markedDiamond, markedEmerald);
        assignTeamGenerators(data, result, settings.generatorRadius());
        addArenaGenerators(data, result, markedDiamond, markedEmerald);
        if (data.lowestSolidY() != null) {
            result.quickVoidY = Math.max(settings.minY(), data.lowestSolidY() - VOID_MARGIN);
        }
        int total = result.diamondGenerators.size() + result.emeraldGenerators.size();
        if (total > MANY_GENERATORS) {
            result.warnings.add(new ScanResult.Warning("AutoSetup-WarnManyGenerators", String.valueOf(total)));
        }
        return result;
    }

    private static void addTeams(ScanData data, ScanResult result) {
        List<ScanData.Bed> beds = new ArrayList<>(data.beds());
        beds.sort(Comparator.comparingInt((ScanData.Bed b) -> b.pos().x())
                .thenComparingInt(b -> b.pos().y()).thenComparingInt(b -> b.pos().z()));
        for (ScanData.Bed bed : beds) {
            if (result.team(bed.color()) != null) {
                result.warnings.add(new ScanResult.Warning("AutoSetup-WarnDuplicateBed", describe(bed.color(), bed.pos())));
                continue;
            }
            result.teams.add(new ScanResult.TeamPlan(bed.color(), TeamColor.displayName(bed.color()), bed.pos()));
        }
    }

    private static void applySigns(ScanData data, ScanResult result, List<Pos> diamond, List<Pos> emerald) {
        List<SignMark> signs = new ArrayList<>(data.signs());
        signs.sort(Comparator.comparingInt((SignMark s) -> s.pos().x())
                .thenComparingInt(s -> s.pos().y()).thenComparingInt(s -> s.pos().z()));
        for (SignMark sign : signs) {
            if (sign.kind() == SignMark.Kind.GENERATOR && sign.team().equalsIgnoreCase("DIAMOND")) {
                diamond.add(sign.pos());
                result.usedSigns.add(sign);
                continue;
            }
            if (sign.kind() == SignMark.Kind.GENERATOR && sign.team().equalsIgnoreCase("EMERALD")) {
                emerald.add(sign.pos());
                result.usedSigns.add(sign);
                continue;
            }
            ScanResult.TeamPlan team = teamFor(sign, result);
            if (team == null) continue;
            if (!assign(team, sign)) {
                result.warnings.add(new ScanResult.Warning("AutoSetup-WarnDuplicateSign",
                        team.name + " " + sign.kind().name().toLowerCase(Locale.ROOT) + " " + sign.pos().x() + " " + sign.pos().y() + " " + sign.pos().z()));
                continue;
            }
            result.usedSigns.add(sign);
        }
    }

    private static ScanResult.TeamPlan teamFor(SignMark sign, ScanResult result) {
        if (!sign.team().isEmpty()) {
            ChatColor color;
            try {
                color = TeamColor.parse(sign.team());
            } catch (IllegalArgumentException e) {
                result.warnings.add(new ScanResult.Warning("AutoSetup-WarnBadSignTeam", sign.team()));
                return null;
            }
            ScanResult.TeamPlan team = result.team(color);
            if (team == null) result.warnings.add(new ScanResult.Warning("AutoSetup-WarnSignTeamMissing", sign.team()));
            return team;
        }
        ScanResult.TeamPlan best = null;
        long bestDistance = Long.MAX_VALUE;
        for (ScanResult.TeamPlan team : result.teams) {
            long distance = team.bed.distanceSquared(sign.pos());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = team;
            }
        }
        if (best == null) {
            result.warnings.add(new ScanResult.Warning("AutoSetup-WarnSignNoTeam",
                    sign.pos().x() + " " + sign.pos().y() + " " + sign.pos().z()));
        }
        return best;
    }

    private static boolean assign(ScanResult.TeamPlan team, SignMark sign) {
        String entity = sign.extra().isEmpty() ? null : sign.extra().toUpperCase(Locale.ROOT);
        switch (sign.kind()) {
            case SPAWN -> {
                if (team.spawn != null) return false;
                team.spawn = sign.pos();
            }
            case SHOP -> {
                if (team.shop != null) return false;
                team.shop = sign.pos();
                team.shopEntity = entity;
            }
            case UPGRADES -> {
                if (team.upgrades != null) return false;
                team.upgrades = sign.pos();
                team.upgradesEntity = entity;
            }
            case GENERATOR -> {
                if (team.generator != null) return false;
                team.generator = sign.pos();
            }
        }
        return true;
    }

    private record Candidate(ScanResult.TeamPlan team, ScanData.GenBlock block, long distance) {}

    private static void assignTeamGenerators(ScanData data, ScanResult result, int radius) {
        long limit = (long) radius * radius;
        List<Candidate> candidates = new ArrayList<>();
        for (ScanResult.TeamPlan team : result.teams) {
            if (team.generator != null) continue;
            for (ScanData.GenBlock block : data.blocks()) {
                if (block.type() != ScanData.Block.IRON && block.type() != ScanData.Block.GOLD) continue;
                long distance = team.bed.distanceSquared(block.pos());
                if (distance <= limit) candidates.add(new Candidate(team, block, distance));
            }
        }
        candidates.sort(Comparator.comparingLong(Candidate::distance)
                .thenComparingInt(c -> c.block().pos().x()).thenComparingInt(c -> c.block().pos().y())
                .thenComparingInt(c -> c.block().pos().z()));
        Set<Pos> taken = new HashSet<>();
        for (Candidate c : candidates) {
            if (c.team().generator != null || !taken.add(c.block().pos())) continue;
            c.team().generator = c.block().pos().above();
        }
    }

    private static void addArenaGenerators(ScanData data, ScanResult result, List<Pos> markedDiamond, List<Pos> markedEmerald) {
        for (Pos pos : cluster(data, ScanData.Block.DIAMOND)) addUnlessNear(result.diamondGenerators, pos);
        for (Pos pos : markedDiamond) addUnlessNear(result.diamondGenerators, pos);
        for (Pos pos : cluster(data, ScanData.Block.EMERALD)) addUnlessNear(result.emeraldGenerators, pos);
        for (Pos pos : markedEmerald) addUnlessNear(result.emeraldGenerators, pos);
    }

    private static void addUnlessNear(List<Pos> list, Pos pos) {
        for (Pos existing : list) {
            if (existing.distanceSquared(pos) <= 4) return;
        }
        list.add(pos);
    }

    static List<Pos> cluster(ScanData data, ScanData.Block type) {
        List<Pos> remaining = new ArrayList<>();
        for (ScanData.GenBlock block : data.blocks()) {
            if (block.type() == type) remaining.add(block.pos());
        }
        remaining.sort(Comparator.comparingInt(Pos::x).thenComparingInt(Pos::y).thenComparingInt(Pos::z));
        List<Pos> out = new ArrayList<>();
        while (!remaining.isEmpty()) {
            List<Pos> group = new ArrayList<>();
            group.add(remaining.remove(0));
            for (int i = 0; i < group.size(); i++) {
                Pos current = group.get(i);
                for (int j = remaining.size() - 1; j >= 0; j--) {
                    Pos other = remaining.get(j);
                    if (Math.abs(other.x() - current.x()) <= 1 && Math.abs(other.y() - current.y()) <= 1
                            && Math.abs(other.z() - current.z()) <= 1) {
                        group.add(remaining.remove(j));
                    }
                }
            }
            long sx = 0;
            long sz = 0;
            int top = Integer.MIN_VALUE;
            for (Pos p : group) {
                sx += p.x();
                sz += p.z();
                top = Math.max(top, p.y());
            }
            out.add(new Pos(Math.round((float) sx / group.size()), top, Math.round((float) sz / group.size())).above());
        }
        return out;
    }

    private static String describe(ChatColor color, Pos pos) {
        return TeamColor.displayName(color) + " " + pos.x() + " " + pos.y() + " " + pos.z();
    }
}
