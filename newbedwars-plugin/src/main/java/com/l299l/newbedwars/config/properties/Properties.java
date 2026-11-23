package com.l299l.newbedwars.config.properties;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.arena.phases.Phase;
import com.l299l.newbedwars.arena.phases.PhaseAction;
import com.l299l.newbedwars.common.sql.JdbcConnector;
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
    public static com.l299l.newbedwars.arena.dragon.DragonSettings Dragon = com.l299l.newbedwars.arena.dragon.DragonSettings.DEFAULTS;
    public static com.l299l.newbedwars.update.UpdateSettings Updates = com.l299l.newbedwars.update.UpdateSettings.DEFAULTS;
    public static Set<Material> DragonIndestructibleBlocks = new HashSet<>();
    public static HashMap<String, Boolean> DefaultGamerules = new HashMap<>();
    public static HashMap<String, GamePhases> GamePhases = new HashMap<>();
    public static Map<String, GameMode> Modes = new java.util.LinkedHashMap<>();
    public static String DefaultLanguage;
    public static StorageType StorageType;
    public static Boolean ArenaChatEnabled;
    public static Boolean TablistIsolationEnabled;
    public static boolean TablistEnabled = true;
    public static String DefaultTablist = "Default";
    public static String MySQLHost;
    public static String MySQLUser;
    public static String MySQLPassword;
    public static String MySQLDatabase;
    public static Integer MySQLPort;
    public static String MySQLSslMode = "PREFERRED";
    public static Boolean MySQLAllowPublicKeyRetrieval = false;
    public static Integer IronGolemLifetime;
    public static Boolean RollbackLegacySystem = false;
    public static Boolean RollbackAsyncCopy = true;
    public static Boolean RollbackRestoreOnShutdown = false;
    public static Integer RollbackUnloadRetryTicks = 10;
    public static List<String> RollbackTemplateExcludes = new ArrayList<>();
    public static Integer BackupsKeep = 5;
    public static Integer AutoSetupDefaultRadius = 150;
    public static Integer AutoSetupMaxRadius = 400;
    public static Integer AutoSetupGeneratorRadius = 20;
    public static Integer AutoSetupBasePadding = 8;
    public static Integer AutoSetupBaseBelow = 5;
    public static Integer AutoSetupBaseAbove = 15;
    public static Integer LeaderboardRefreshSeconds = 60;
    public static Integer MapVoteItemSlot = 2;
    public static String MapVoteItemMaterial = "PAPER";
    public static Integer TeamSelectorItemSlot = 0;
    public static String TeamSelectorItemMaterial = "WHITE_WOOL";
    public static Integer JoinNpcUpdateSeconds = 2;
    public static Boolean JoinNpcLeftClick = false;
    public static Double JoinNpcHologramOffset = 0.0;
    public static Map<String, List<String>> JoinNpcHologram = new HashMap<>();
    public static com.l299l.newbedwars.api.view.NetworkMode NetworkMode = com.l299l.newbedwars.api.view.NetworkMode.NONE;
    public static String NetworkServerId = "";
    public static String NetworkLobbyServer = "";
    public static Boolean NetworkLocalLobby = false;
    public static Boolean NetworkLobbyJoinMessages = true;
    public static Integer NetworkRestartAfterGames = 0;
    public static Integer NetworkTicketTimeoutSeconds = 10;
    public static Integer NetworkStatePublishSeconds = 5;
    public static Boolean NetworkMySQLEnabled = false;
    public static Integer NetworkMySQLHeartbeatSeconds = 5;

    public Properties() {
        NewBedwars.plugin.reloadConfig();
        ServerName = text("ServerName");
        StorageType = com.l299l.newbedwars.config.StorageType.JSON;
        if (get("Storage.Type") instanceof String st && st.trim().equalsIgnoreCase("MYSQL")) {
            StorageType = com.l299l.newbedwars.config.StorageType.MYSQL;
        }
        MySQLHost = get("MySQL.Host") instanceof String str ? str : "localhost";
        MySQLPort = get("MySQL.Port") instanceof Integer i ? i : 3306;
        MySQLDatabase = get("MySQL.Database") instanceof String str ? str : "newbedwars";
        MySQLUser = get("MySQL.User") instanceof String str ? str : "root";
        MySQLPassword = get("MySQL.Password") != null ? String.valueOf(get("MySQL.Password")) : "";
        MySQLSslMode = JdbcConnector.normalizeSslMode(get("MySQL.SslMode") instanceof String str ? str : null);
        MySQLAllowPublicKeyRetrieval = get("MySQL.AllowPublicKeyRetrieval") instanceof Boolean b && b;
        Object ace = get("ArenaChatEnabled");
        ArenaChatEnabled = ace instanceof Boolean b ? b : true;
        Object tie = get("TablistIsolationEnabled");
        TablistIsolationEnabled = tie instanceof Boolean b ? b : true;
        TablistEnabled = !(get("Tablist.Enabled") instanceof Boolean te) || te;
        DefaultTablist = get("DefaultTablist") instanceof String dt && !dt.isBlank() ? dt.trim() : "Default";

        DefaultMinPlayers = typed("DefaultMinPlayers", Integer.class);
        DefaultMaxInTeam = typed("DefaultMaxInTeam", Integer.class);
        DefaultWaitingTime = typed("DefaultWaitingTime", Integer.class);
        DefaultTeamShopGui = text("DefaultTeamShopGui");
        DefaultUpgradeShopGui = text("DefaultTeamUpgradeGui");
        DefaultGeneratorConfig = text("DefaultGeneratorConfig");
        DefaultGamePhase = text("DefaultGamePhase");
        DefaultRespawnTime = typed("DefaultRespawnTime", Integer.class);

        DefaultGamerules.put("RandomTeams", typed("DefaultGamerules.RandomTeams", Boolean.class));
        DefaultGamerules.put("AllowParties", typed("DefaultGamerules.AllowParties", Boolean.class));
        DefaultGamerules.put("AllowSpectators", typed("DefaultGamerules.AllowSpectators", Boolean.class));
        DefaultGamerules.put("AllowTeamChat", typed("DefaultGamerules.AllowTeamChat", Boolean.class));
        DefaultGamerules.put("AllowGlobalChat", typed("DefaultGamerules.AllowGlobalChat", Boolean.class));
        DefaultGamerules.put("AllowPrivateChat", typed("DefaultGamerules.AllowPrivateChat", Boolean.class));
        DefaultGamerules.put("AllowTeamDamage", typed("DefaultGamerules.AllowTeamDamage", Boolean.class));
        DefaultGamerules.put("MakeSwordsPermanent", typed("DefaultGamerules.MakeSwordsPermanent", Boolean.class));
        Object dmt = get("DefaultGamerules.MakeToolsPermanent");
        DefaultGamerules.put("MakeToolsPermanent", dmt instanceof Boolean b ? b : true);
        DefaultGamerules.put("AllowMultipleGolems", typed("DefaultGamerules.AllowMultipleGolems", Boolean.class));
        DefaultGamerules.put("SplitPartiesToStart", typed("DefaultGamerules.SplitPartiesToStart", Boolean.class));
        Object dmv = get("DefaultGamerules.MapVote");
        DefaultGamerules.put("MapVote", dmv instanceof Boolean b ? b : true);
        Object dts = get("DefaultGamerules.TeamSelection");
        DefaultGamerules.put("TeamSelection", dts instanceof Boolean b ? b : false);
        Object dtsb = get("DefaultGamerules.TeamSelectionBalanced");
        DefaultGamerules.put("TeamSelectionBalanced", dtsb instanceof Boolean b ? b : true);
        Object dtsp = get("DefaultGamerules.TeamSelectionPartyLeader");
        DefaultGamerules.put("TeamSelectionPartyLeader", dtsp instanceof Boolean b ? b : true);
        DefaultLanguage = text("DefaultLanguage");

        RejoinTimeEnabled = typed("RejoinTimeLimitEnabled", Boolean.class);
        RejoinTime = typed("RejoinTime", Integer.class);
        BasicSword = text("BasicSword");
        FullArmor = typed("FullArmor", Boolean.class);
        Object blce = get("BedLeaveConfirmEnabled");
        BedLeaveConfirmEnabled = blce instanceof Boolean b ? b : true;
        Object blcd = get("BedLeaveConfirmDelay");
        BedLeaveConfirmDelay = blcd instanceof Integer i ? i : 3;
        Object agwt = get("AfterGameWatchTime");
        AfterGameWatchTime = agwt instanceof Integer i ? i : 30;
        Object tpe = get("TntParticlesEnabled");
        TntParticlesEnabled = tpe instanceof Boolean b ? b : true;
        Object dsh = get("DragonSpawnHeight");
        DragonSpawnHeight = dsh instanceof Integer i ? i : 20;
        Dragon = com.l299l.newbedwars.arena.dragon.DragonSettings.read(this::get);
        Updates = com.l299l.newbedwars.update.UpdateSettings.read(this::get);

        Object igl = get("IronGolemLifetime");
        IronGolemLifetime = igl instanceof Integer i ? i : 120;

        Object rls = get("Rollback.UseLegacySystem");
        RollbackLegacySystem = rls instanceof Boolean b ? b : false;
        Object rac = get("Rollback.AsyncCopy");
        RollbackAsyncCopy = rac instanceof Boolean b ? b : true;
        Object rros = get("Rollback.RestoreOnShutdown");
        RollbackRestoreOnShutdown = rros instanceof Boolean b ? b : false;
        Object rurt = get("Rollback.UnloadRetryTicks");
        RollbackUnloadRetryTicks = rurt instanceof Integer i && i > 0 ? i : 10;
        RollbackTemplateExcludes = new ArrayList<>();
        Object rte = get("Rollback.TemplateExcludes");
        if (rte instanceof List<?> excludes) {
            for (Object entry : excludes) RollbackTemplateExcludes.add(String.valueOf(entry));
        } else {
            RollbackTemplateExcludes.addAll(List.of("session.lock", "uid.dat", "playerdata", "stats", "advancements"));
        }
        Object bk = get("Backups.Keep");
        BackupsKeep = bk instanceof Integer i && i >= 0 ? i : 5;

        AutoSetupMaxRadius = positive(get("AutomaticSetup.MaxRadius"), 400);
        AutoSetupDefaultRadius = Math.min(positive(get("AutomaticSetup.DefaultRadius"), 150), AutoSetupMaxRadius);
        AutoSetupGeneratorRadius = positive(get("AutomaticSetup.GeneratorSearchRadius"), 20);
        AutoSetupBasePadding = positive(get("AutomaticSetup.BasePadding"), 8);
        AutoSetupBaseBelow = positive(get("AutomaticSetup.BaseBelow"), 5);
        AutoSetupBaseAbove = positive(get("AutomaticSetup.BaseAbove"), 15);
        LeaderboardRefreshSeconds = positive(get("Leaderboards.RefreshSeconds"), 60);

        Object mvs = get("MapVote.Item.Slot");
        MapVoteItemSlot = mvs instanceof Integer i && i >= 0 && i <= 8 ? i : 2;
        Object mvm = get("MapVote.Item.Material");
        MapVoteItemMaterial = mvm instanceof String str ? str : "PAPER";
        Object tss = get("TeamSelector.Item.Slot");
        TeamSelectorItemSlot = tss instanceof Integer i && i >= 0 && i <= 7 ? i : 0;
        Object tsm = get("TeamSelector.Item.Material");
        TeamSelectorItemMaterial = tsm instanceof String str ? str : "WHITE_WOOL";

        Object jnu = get("JoinNpc.UpdateSeconds");
        JoinNpcUpdateSeconds = jnu instanceof Integer i && i > 0 ? i : 2;
        Object jnl = get("JoinNpc.LeftClick");
        JoinNpcLeftClick = jnl instanceof Boolean b ? b : false;
        Object jno = get("JoinNpc.Hologram.Offset");
        JoinNpcHologramOffset = jno instanceof Number n ? n.doubleValue() : 0.0;
        JoinNpcHologram = new HashMap<>();
        for (String type : new String[]{"GROUP", "ARENA", "SELECTOR"}) {
            Object lines = get("JoinNpc.Hologram." + type);
            List<String> list = new ArrayList<>();
            if (lines instanceof List<?> raw) {
                for (Object line : raw) list.add(String.valueOf(line));
            }
            JoinNpcHologram.put(type, list);
        }
        loadNetwork();
        DragonIndestructibleBlocks.clear();
        Object dibRaw = get("DragonIndestructibleBlocks");
        if (dibRaw instanceof List<?> dibList) {
            for (Object entry : dibList) {
                Material mat = Material.matchMaterial(entry.toString());
                if (mat != null) DragonIndestructibleBlocks.add(mat);
            }
        }

        reloadModes();
    }

    public static void reloadModes() {
        GamePhases = new HashMap<>();
        Map<String, Object> own = com.l299l.newbedwars.config.ModesFile.current();
        Map<String, Object> builtIn = com.l299l.newbedwars.config.ModesFile.bundled();
        Set<String> configured = new HashSet<>();
        Object ownPhases = own.get("GamePhases");
        if (ownPhases instanceof List<?> phaseLists) {
            loadGamePhases(phaseLists, configured, "modes.yml");
        } else if (ownPhases != null) {
            NewBedwars.plugin.getLogger().warning("modes.yml: GamePhases is not a list; only the built-in game phases are loaded");
        }
        if (builtIn.get("GamePhases") instanceof List<?> lists) {
            loadGamePhases(lists, configured, null);
        }
        loadModes(own, builtIn);
    }

    private static void loadModes(Map<String, Object> own, Map<String, Object> builtIn) {
        Map<String, GameMode> modes = new java.util.LinkedHashMap<>();
        Object section = own.get("Modes");
        if (section instanceof Map<?, ?> map) {
            readModes(map, modes, true);
        } else if (section != null) {
            NewBedwars.plugin.getLogger().warning("modes.yml: Modes is not a section; only the built-in modes are loaded");
        }
        if (builtIn.get("Modes") instanceof Map<?, ?> map) {
            readModes(map, modes, false);
        }
        Modes = modes;
    }

    private static void readModes(Map<?, ?> section, Map<String, GameMode> modes, boolean warn) {
        for (Map.Entry<?, ?> e : section.entrySet()) {
            String id = String.valueOf(e.getKey());
            if (modes.containsKey(id) || e.getValue() == null) continue;
            if (!(e.getValue() instanceof Map<?, ?> mode)) {
                if (warn) NewBedwars.plugin.getLogger().warning("modes.yml: mode '" + id + "' is not a section and was skipped");
                continue;
            }
            modes.put(id, new GameMode(id, modeText(mode, "Generators"), modeText(mode, "GamePhases"),
                    modeText(mode, "ShopGui"), modeText(mode, "UpgradeGui"),
                    mode.get("MaxInTeam") instanceof Integer i && i > 0 ? i : 0,
                    mode.get("Teams") instanceof Integer i && i > 0 ? i : 0, modeText(mode, "Tablist")));
        }
    }

    private static String modeText(Map<?, ?> mode, String key) {
        return mode.get(key) instanceof String str && !str.isBlank() ? str.trim() : null;
    }

    private static void loadGamePhases(List<?> phaseLists, Set<String> configured, String source) {
        for (Object entry : phaseLists) {
            if (!(entry instanceof Map<?, ?> gamePhase) || !(gamePhase.get("ID") instanceof String ID)
                    || !(gamePhase.get("Phases") instanceof List<?> phases)) {
                if (source != null) NewBedwars.plugin.getLogger().warning(source + ": a GamePhases entry without ID or Phases list was skipped");
                continue;
            }
            if (source == null && configured.contains(ID)) continue;
            configured.add(ID);
            List<Phase> phaseList = new ArrayList<>();
            for (int i = 0; i < phases.size(); i++) {
                if (!(phases.get(i) instanceof Map<?, ?> phase)) {
                    NewBedwars.plugin.getLogger().warning("Game phases '" + ID + "': phase " + (i + 1) + " is not a section and was skipped");
                    continue;
                }
                String name = String.valueOf(phase.get("Name"));
                int duration = phase.get("Duration") instanceof Number n ? n.intValue() : 0;
                if (duration <= 0 && i < phases.size() - 1) {
                    NewBedwars.plugin.getLogger().warning("Game phases '" + ID + "': phase '" + name
                            + "' has no valid Duration, using 1 second so the game can continue");
                    duration = 1;
                }
                List<?> actions = phase.get("Actions") instanceof List<?> l ? l : List.of();
                List<PhaseAction> phaseActions = new ArrayList<>();
                for (Object action : actions) {
                    phaseActions.add(new PhaseAction(String.valueOf(action)));
                }
                phaseList.add(new Phase(name, duration, phaseActions));
            }
            GamePhases.put(ID, new GamePhases(ID, phaseList));
        }
    }

    private void loadNetwork() {
        Object mode = get("Network.Mode");
        NetworkMode = com.l299l.newbedwars.api.view.NetworkMode.NONE;
        if (mode instanceof String str) {
            try {
                NetworkMode = com.l299l.newbedwars.api.view.NetworkMode.valueOf(str.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                NewBedwars.plugin.getLogger().warning("Unknown Network.Mode '" + str + "', using NONE");
            }
        }
        NetworkServerId = get("Network.ServerId") instanceof String str ? str.trim() : "";
        NetworkLobbyServer = get("Network.LobbyServer") instanceof String str ? str.trim() : "";
        NetworkLocalLobby = get("Network.LocalLobby") instanceof Boolean b ? b : false;
        NetworkLobbyJoinMessages = get("Network.LobbyJoinMessages") instanceof Boolean b ? b : true;
        NetworkRestartAfterGames = get("Network.RestartAfterGames") instanceof Integer i && i >= 0 ? i : 0;
        NetworkTicketTimeoutSeconds = get("Network.TicketTimeoutSeconds") instanceof Integer i && i > 0 ? i : 10;
        NetworkStatePublishSeconds = get("Network.StatePublishSeconds") instanceof Integer i && i > 0 ? i : 5;
        NetworkMySQLEnabled = get("Network.MySQL.Enabled") instanceof Boolean b ? b : false;
        NetworkMySQLHeartbeatSeconds = get("Network.MySQL.HeartbeatSeconds") instanceof Integer i && i > 0 ? i : 5;
    }

    private static int positive(Object value, int fallback) {
        return value instanceof Integer i && i > 0 ? i : fallback;
    }

    private Object get(String text) {
        return NewBedwars.plugin.getConfig().get(text);
    }

    private <T> T typed(String key, Class<T> type) {
        Object value = get(key);
        if (type.isInstance(value)) return type.cast(value);
        org.bukkit.configuration.Configuration defaults = NewBedwars.plugin.getConfig().getDefaults();
        Object fallback = defaults == null ? null : defaults.get(key);
        if (value != null) {
            NewBedwars.plugin.getLogger().warning("config.yml: " + key + " should be " + (type == Boolean.class ? "true or false" : "a whole number")
                    + ", not '" + value + "'; using " + fallback);
        }
        return type.isInstance(fallback) ? type.cast(fallback) : null;
    }

    private String text(String key) {
        Object value = get(key);
        return value == null ? null : String.valueOf(value);
    }
}
