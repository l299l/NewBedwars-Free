package com.l299l.newbedwars.config.properties;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.arena.phases.Phase;
import com.l299l.newbedwars.arena.phases.PhaseAction;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.PriceType;
import com.l299l.newbedwars.config.StorageType;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Properties{
    public static String ServerName;
    public static Integer DefaultMinPlayers;
    public static Integer DefaultMaxTeams;
    public static Integer DefaultMaxInTeam;
    public static Integer DefaultWaitingTime;
    public static String DefaultTeamShopGui;
    public static String DefaultUpgradeShopGui;
    public static String DefaultGeneratorConfig;
    public static String DefaultGamePhase;
    public static Integer DefaultRespawnTime;
    public static Boolean RejoinTimeEnabled;
    public static Integer RejoinTime;
    public static String BasicSword;
    public static Boolean FullArmor;
    public static Boolean BedLeaveConfirmEnabled;
    public static Integer BedLeaveConfirmDelay;
    public static Integer AfterGameWatchTime;
    public static Boolean TntParticlesEnabled;
    public static Integer DragonSpawnHeight;
    public static Set<Material> DragonIndestructibleBlocks = new HashSet<>();
    public static HashMap<String, Boolean> DefaultGamerules = new HashMap<>();
    public static HashMap<String, GamePhases> GamePhases = new HashMap<>();
    public static HashMap<String, List<Integer>> UpgradePrices = new HashMap<>();
    public static PriceType UpgradePriceType = PriceType.DIAMOND;
    public static String DefaultLanguage;
    public static StorageType StorageType;
    public static Boolean ArenaChatEnabled;
    public static Boolean TablistIsolationEnabled;
    public static Boolean RequireJoinPermission;
    public static Boolean RequireLobbyPermission;
    public static String MySQLHost;
    public static String MySQLUser;
    public static String MySQLPassword;
    public static String MySQLDatabase;
    public static Integer MySQLPort;

    public Properties() {
        NewBedwars.plugin.reloadConfig();
        ServerName = (String) get("ServerName");
        StorageType = com.l299l.newbedwars.config.StorageType.JSON;
        Object ace = get("ArenaChatEnabled");
        ArenaChatEnabled = ace instanceof Boolean b ? b : true;
        Object tie = get("TablistIsolationEnabled");
        TablistIsolationEnabled = tie instanceof Boolean b ? b : true;
        Object rjp = get("RequireJoinPermission");
        RequireJoinPermission = rjp instanceof Boolean b ? b : false;
        Object rlp = get("RequireLobbyPermission");
        RequireLobbyPermission = rlp instanceof Boolean b ? b : false;
        //Default Settings
        DefaultMinPlayers = (Integer) get("DefaultMinPlayers");
        DefaultMaxTeams = (Integer) get("DefaultMaxTeams");
        DefaultMaxInTeam = (Integer) get("DefaultMaxInTeam");
        DefaultWaitingTime = (Integer) get("DefaultWaitingTime");
        DefaultTeamShopGui = (String) get("DefaultTeamShopGui");
        DefaultUpgradeShopGui = (String) get("DefaultTeamUpgradeGui");
        DefaultGeneratorConfig = (String) get("DefaultGeneratorConfig");
        DefaultGamePhase = (String) get("DefaultGamePhase");
        DefaultRespawnTime = (Integer) get("DefaultRespawnTime");
        //Gamerules
        DefaultGamerules.put("RandomTeams", (Boolean) get("DefaultGamerules.RandomTeams"));
        DefaultGamerules.put("AllowParties", (Boolean) get("DefaultGamerules.AllowParties"));
        DefaultGamerules.put("AllowSpectators", (Boolean) get("DefaultGamerules.AllowSpectators"));
        DefaultGamerules.put("AllowTeamChat", (Boolean) get("DefaultGamerules.AllowTeamChat"));
        DefaultGamerules.put("AllowGlobalChat", (Boolean) get("DefaultGamerules.AllowGlobalChat"));
        DefaultGamerules.put("AllowPrivateChat", (Boolean) get("DefaultGamerules.AllowPrivateChat"));
        DefaultGamerules.put("AllowTeamDamage", (Boolean) get("DefaultGamerules.AllowTeamDamage"));
        DefaultGamerules.put("MakeSwordsPermanent", (Boolean) get("DefaultGamerules.MakeSwordsPermanent"));
        DefaultLanguage = (String) get("DefaultLanguage");
        //Game
        RejoinTimeEnabled = (Boolean) get("RejoinTimeLimitEnabled");
        RejoinTime = (Integer) get("RejoinTime");
        BasicSword = (String) get("BasicSword");
        FullArmor = (Boolean) get("FullArmor");
        Object blce = get("BedLeaveConfirmEnabled");
        BedLeaveConfirmEnabled = blce instanceof Boolean b ? b : true;
        Object blcd = get("BedLeaveConfirmDelay");
        BedLeaveConfirmDelay = blcd instanceof Integer i ? i : 3;
        Object agwt = get("AfterGameWatchTime");
        AfterGameWatchTime = agwt instanceof Integer i ? i : 20;
        Object tpe = get("TntParticlesEnabled");
        TntParticlesEnabled = tpe instanceof Boolean b ? b : true;
        Object dsh = get("DragonSpawnHeight");
        DragonSpawnHeight = dsh instanceof Integer i ? i : 20;
        DragonIndestructibleBlocks.clear();
        Object dibRaw = get("DragonIndestructibleBlocks");
        if (dibRaw instanceof List<?> dibList) {
            for (Object entry : dibList) {
                Material mat = Material.matchMaterial(entry.toString());
                if (mat != null) DragonIndestructibleBlocks.add(mat);
            }
        }
        //Phases
        List<Map<String, Object>> allGamePhases = (List<Map<String, Object>>) get("GamePhases");
        for (Map<String, Object> gamePhase : allGamePhases) {
            String ID = (String) gamePhase.get("ID");
            List<Map<String, Object>> phases = (List<Map<String, Object>>) gamePhase.get("Phases");
            List<Phase> phaseList = new ArrayList<>();
            for (Map<String, Object> phase : phases) {
                String name = (String) phase.get("Name");
                Integer duration = (Integer) phase.get("Duration");
                List<String> actions = (List<String>) phase.get("Actions");
                List<PhaseAction> phaseActions = new ArrayList<>();
                for (String action : actions) {
                    phaseActions.add(new PhaseAction(action));
                }
                phaseList.add(new Phase(name, duration, phaseActions));
            }
            GamePhases.put(ID, new GamePhases(ID, phaseList));
        }
        //Upgrade prices
        try {
            UpgradePriceType = PriceType.valueOf(((String) get("UpgradePrices.PriceType")).toUpperCase());
        } catch (Exception ignored) {
            UpgradePriceType = PriceType.DIAMOND;
        }
        UpgradePrices.clear();
        for (Upgrade upgrade : Upgrade.values()) {
            Object pricesObj = get("UpgradePrices." + upgrade.name());
            if (pricesObj instanceof List<?> list) {
                List<Integer> prices = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof Integer i) prices.add(i);
                }
                UpgradePrices.put(upgrade.name(), prices);
            }
        }
    }

    public static int getUpgradePrice(Upgrade upgrade, int currentLevel) {
        List<Integer> prices = UpgradePrices.get(upgrade.name());
        if (prices == null || currentLevel >= prices.size()) return 999;
        return prices.get(currentLevel);
    }

    public static Material getMaterialForPriceType(PriceType priceType) {
        return switch (priceType) {
            case EMERALD -> Material.EMERALD;
            case GOLD -> Material.GOLD_INGOT;
            case IRON -> Material.IRON_INGOT;
            default -> Material.DIAMOND;
        };
    }

    private Object get(String text) {
        return NewBedwars.plugin.getConfig().get(text);
    }
}
