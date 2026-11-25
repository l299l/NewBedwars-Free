package com.l299l.newbedwars;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.arena.group.MapVoteManager;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.extensions.PluginDetector;
import com.l299l.newbedwars.placeholder.NewBedwarsExpansion;
import com.l299l.newbedwars.arena.shops.customitems.CustomItemManager;
import com.l299l.newbedwars.bossbar.BossBarManager;
import com.l299l.newbedwars.commands.LangStandaloneCommand;
import com.l299l.newbedwars.commands.LobbyStandaloneCommand;
import com.l299l.newbedwars.commands.bedwars.MainCommand;
import com.l299l.newbedwars.commands.party.PartyCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.Updater;
import com.l299l.newbedwars.config.data.DataManager;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.world.template.ArenaMaps;
import com.l299l.newbedwars.config.data.LobbyData;
import com.l299l.newbedwars.config.properties.LangMessages;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.events.gameplay.*;
import com.l299l.newbedwars.events.lifecycle.ChatEvent;
import com.l299l.newbedwars.events.lifecycle.InitializationEvent;
import com.l299l.newbedwars.events.lifecycle.LanguageEvent;
import com.l299l.newbedwars.gui.GUIManager;
import com.l299l.newbedwars.npc.JoinNpcManager;
import com.l299l.newbedwars.parties.PartyManager;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.scoreboard.ScoreboardManager;
import com.l299l.newbedwars.world.chunkgenerators.VoidGenerator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public final class NewBedwars extends JavaPlugin {
    private MainCommand mainCommand;
    private com.l299l.newbedwars.apiimpl.NewBedwarsApiImpl apiImpl;
    public static NewBedwars plugin;
    private Location lobbyLocation;
    private final Updater updater;
    private final GUIManager guiManager;
    private final CustomItemManager customItemManager;
    private final PlayerManager playerManager;
    private final HashMap<String, GeneratorLeveling> generatorLeveling;
    private Messages messages;
    private Properties properties;
    private LangMessages langMessages;
    private DataManager dataManager;
    private PartyManager partyManager;
    private BossBarManager bossBarManager;
    private ScoreboardManager scoreboardManager;
    private com.l299l.newbedwars.tablist.TablistManager tablistManager;
    private com.l299l.newbedwars.bossbar.AddonBossBars addonBossBars;
    private final JoinNpcManager joinNpcManager;
    private final ArenaGroupManager arenaGroupManager;
    private final MapVoteManager mapVoteManager;
    private final com.l299l.newbedwars.arena.team.TeamSelectorManager teamSelectorManager;
    private com.l299l.newbedwars.update.UpdateService updateService;
    private org.bukkit.scheduler.BukkitTask safetyTask;

    public NewBedwars() {
        plugin = this;
        guiManager = new GUIManager();
        customItemManager = new CustomItemManager();
        updater = new Updater();
        playerManager = new PlayerManager();
        generatorLeveling = new HashMap<>();
        joinNpcManager = new JoinNpcManager();
        arenaGroupManager = new ArenaGroupManager();
        mapVoteManager = new MapVoteManager();
        teamSelectorManager = new com.l299l.newbedwars.arena.team.TeamSelectorManager();
    }

    @Override
    public void onEnable() {
        updateService = new com.l299l.newbedwars.update.UpdateService(this);
        reloadAll();
        mainCommand = new MainCommand();
        Objects.requireNonNull(getCommand("bw")).setExecutor(mainCommand);
        Objects.requireNonNull(getCommand("lobby")).setExecutor(new LobbyStandaloneCommand());
        LangStandaloneCommand langCmd = new LangStandaloneCommand();
        Objects.requireNonNull(getCommand("lang")).setExecutor(langCmd);
        Objects.requireNonNull(getCommand("lang")).setTabCompleter(langCmd);
        PartyCommand partyCmd = new PartyCommand();
        Objects.requireNonNull(getCommand("party")).setExecutor(partyCmd);
        Objects.requireNonNull(getCommand("party")).setTabCompleter(partyCmd);
        Objects.requireNonNull(getCommand("p")).setExecutor(partyCmd);
        Objects.requireNonNull(getCommand("p")).setTabCompleter(partyCmd);
        if (PluginDetector.isPlaceholderAPIPresent()) {
            new NewBedwarsExpansion(this).register();
            com.l299l.newbedwars.placeholder.PapiText.enable();
        }
        joinNpcManager.start();
        addonBossBars = new com.l299l.newbedwars.bossbar.AddonBossBars();
        addonBossBars.start();
        apiImpl = new com.l299l.newbedwars.apiimpl.NewBedwarsApiImpl();
        getServer().getPluginManager().registerEvents(apiImpl, this);
        getServer().getServicesManager().register(com.l299l.newbedwars.api.NewBedwarsAPI.class, apiImpl, this, org.bukkit.plugin.ServicePriority.Normal);
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NewBedwars]: NewBedwars v" + getDescription().getVersion() + " is " + ChatColor.DARK_GREEN + "Enabled");
    }

    @Override
    public void onDisable() {
        shutdownStep("tasks", () -> {
            cancelSafetyTask();
            com.l299l.newbedwars.placeholder.LeaderboardPlaceholders.stop();
            com.l299l.newbedwars.placeholder.PapiText.disable();
            com.l299l.newbedwars.arena.shops.customitems.GamePermissions.revokeAll();
            getServer().getServicesManager().unregisterAll(this);
            if (updateService != null) updateService.stop();
            if (bossBarManager != null) bossBarManager.stopAll();
            if (tablistManager != null) tablistManager.stop();
            if (addonBossBars != null) addonBossBars.stop();
        });
        shutdownStep("join NPCs", joinNpcManager::shutdown);
        int active = 0;
        for (IArena arena : new ArrayList<>(ArenaRegistry.all())) {
            if (!arena.getPlayers().isEmpty() || !arena.getSpectators().isEmpty()) active++;
            else if (!arena.getPlayersOnSetup().isEmpty()) {
                shutdownStep("setup players of " + arena.getArenaName(), () -> {
                    for (java.util.UUID p : new ArrayList<>(arena.getPlayersOnSetup())) {
                        Player pl = Bukkit.getPlayer(p);
                        if (pl == null) continue;
                        com.l299l.newbedwars.player.LobbyService.sendToLobby(pl, com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent.Reason.SERVER_SHUTDOWN);
                        pl.kickPlayer(messages.getMsg(pl, "ShutdownKick"));
                    }
                });
            }
        }
        if (active > 0) {
            getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "[NewBedwars]: " + active + " arena(s) still active — forcing shutdown...");
        }
        for (IArena arena : new ArrayList<>(ArenaRegistry.all())) {
            shutdownStep("arena " + arena.getArenaName(), arena::forceShutdown);
        }
        shutdownStep("lobby location", () -> LobbyData.save(lobbyLocation));
        if (com.l299l.newbedwars.world.template.BackgroundTasks.running() > 0) {
            getLogger().info("Waiting for arena maintenance (enable, copy, import, restore, delete) to finish...");
            if (!com.l299l.newbedwars.world.template.BackgroundTasks.await(30_000L)) {
                getLogger().warning("Arena maintenance did not finish in 30 seconds; check the affected arenas after the restart");
            }
        }
        IOExecutor.flush();
        shutdownStep("data", () -> {
            if (dataManager != null) dataManager.save();
        });
        if (ArenaMaps.isEnabled() && Properties.RollbackRestoreOnShutdown) {
            com.l299l.newbedwars.world.template.ArenaResetPipeline.awaitRunningCopies(30_000L);
            for (IArena arena : ArenaRegistry.all()) {
                String name = arena.getArenaName();
                if (!ArenaMaps.hasTemplate(name) || !ArenaMaps.isDirty(name)) continue;
                if (com.l299l.newbedwars.world.template.ArenaResetPipeline.isCopying(name)) continue;
                try {
                    ArenaMaps.restoreOffline(name, arena.getArenaWorld().getName());
                } catch (java.io.IOException | RuntimeException e) {
                    getLogger().warning("Could not restore map of arena " + name + " on shutdown: " + e.getMessage());
                }
            }
        }
        shutdownStep("network", com.l299l.newbedwars.network.NetworkManager::shutdown);
        shutdownStep("data", () -> {
            if (dataManager != null) dataManager.stop();
        });
        shutdownStep("database", com.l299l.newbedwars.config.data.sql.Database::shutdown);
        IOExecutor.flush();
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NewBedwars]: NewBedwars v" + getDescription().getVersion() + " is " + ChatColor.DARK_RED + "Disabled");
    }

    private void shutdownStep(String what, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException | LinkageError e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Error while stopping (" + what + "); continuing with the shutdown", e);
        }
    }

    public void reloadAll() {
        com.l299l.newbedwars.network.NetworkManager.shutdown();
        HandlerList.unregisterAll(this);
        cancelSafetyTask();
        for (IArena arena : new ArrayList<>(ArenaRegistry.all())) {
            if (!arena.getPlayers().isEmpty() || !arena.getSpectators().isEmpty()) {
                arena.stop();
            }
        }
        ArenaRegistry.clear();
        updater.reloadLanguages();
        updater.reloadBossBars();
        updater.reloadScoreboards();
        updater.reloadTablists();
        updater.reloadGeneratorsConfigurations();
        langMessages = new LangMessages(updater.getLangConfs());
        langMessages.reloadMessages();
        messages = new Messages(this, updater);
        updater.updateConf();
        properties = new Properties();
        if (dataManager != null) dataManager.stop();
        dataManager = new DataManager();
        dataManager.load(updater.takeLegacyUpgradePrices());
        arenaGroupManager.load();
        mapVoteManager.clear();
        partyManager = new PartyManager(this, messages);
        if (bossBarManager != null) bossBarManager.stopAll();
        bossBarManager = new BossBarManager(updater.getBossBarsConf());
        bossBarManager.loadBossBars();
        bossBarManager.startLobbyBossBar();
        scoreboardManager = new ScoreboardManager(updater.getScoreboardsConf());
        scoreboardManager.loadScoreboards();
        if (tablistManager != null) tablistManager.stop();
        com.l299l.newbedwars.tablist.TablistManager previousTablists = tablistManager;
        tablistManager = com.l299l.newbedwars.tablist.TablistManager.load(updater.getTablistsConf(), getLogger());
        if (previousTablists != null) tablistManager.inheritOverrides(previousTablists);
        tablistManager.start();
        reloadGenerators();
        reloadEvents();
        lobbyLocation = null;
        com.l299l.newbedwars.network.NetworkManager.restart(this);
        if (updateService != null) updateService.restart();
    }

    public Properties getProperties() {
        return properties;
    }
    public LangMessages getLangMessages() {
        return langMessages;
    }
    public Messages getMessages() {
        return messages;
    }
    public GUIManager getGuiManager() {
        return guiManager;
    }
    public PlayerManager getPlayerManager() {
        return playerManager;
    }
    public CustomItemManager getCustomItemManager() {
        return customItemManager;
    }
    public DataManager getDataManager() {
        return dataManager;
    }
    public PartyManager getPartyManager() {
        return partyManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public com.l299l.newbedwars.bossbar.AddonBossBars getAddonBossBars() {
        return addonBossBars;
    }

    public com.l299l.newbedwars.tablist.TablistManager getTablistManager() {
        return tablistManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public ArenaGroupManager getArenaGroupManager() {
        return arenaGroupManager;
    }

    public MapVoteManager getMapVoteManager() {
        return mapVoteManager;
    }

    public JoinNpcManager getJoinNpcManager() {
        return joinNpcManager;
    }

    public com.l299l.newbedwars.update.UpdateService getUpdateService() {
        return updateService;
    }

    public java.io.File getPluginFile() {
        return getFile();
    }

    public MainCommand getMainCommand() {
        return mainCommand;
    }

    public Location getLobbyLocation() {
        if (lobbyLocation == null) {
            lobbyLocation = LobbyData.load();
        }
        return lobbyLocation != null ? lobbyLocation : Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    public void setLobbyLocation(Location location) {
        this.lobbyLocation = location;
        LobbyData.save(location);
    }

    public HashMap<String, GeneratorLeveling> getGeneratorLeveling() {
        return generatorLeveling;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new VoidGenerator();
    }

    private void cancelSafetyTask() {
        if (safetyTask != null) {
            safetyTask.cancel();
            safetyTask = null;
        }
    }

    private void reloadEvents(){
        getServer().getPluginManager().registerEvents(new LanguageEvent(this), this);
        getServer().getPluginManager().registerEvents(new InitializationEvent(this), this);
        CombatTracker combat = new CombatTracker();
        CombatListener deaths = new CombatListener(combat);
        for (org.bukkit.event.Listener listener : List.of(new ConnectionListener(this), new SpectatorListener(this), new com.l299l.newbedwars.events.gameplay.SetupListener(),
                new InventoryListener(), deaths, new MobListener(),
                new BlockListener(this), new MovementListener(combat, deaths), new CustomItemListener(this))) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
        cancelSafetyTask();
        safetyTask = getServer().getScheduler().runTaskTimer(this, new ArenaSafetyTask(deaths),
                ArenaSafetyTask.PERIOD_TICKS, ArenaSafetyTask.PERIOD_TICKS);
        if (PluginDetector.isPlaceholderAPIPresent()) {
            com.l299l.newbedwars.placeholder.LeaderboardPlaceholders.start(this, playerManager, Properties.LeaderboardRefreshSeconds);
        }
        getServer().getPluginManager().registerEvents(new ChatEvent(this), this);
        getServer().getPluginManager().registerEvents(new com.l299l.newbedwars.arena.maintenance.MaintenanceEditGuard(), this);
        if (dataManager != null && dataManager.getPlayerDataSql() != null) {
            getServer().getPluginManager().registerEvents(dataManager.getPlayerDataSql(), this);
        }
        if (apiImpl != null) {
            getServer().getPluginManager().registerEvents(apiImpl, this);
        }
        joinNpcManager.registerListeners();
        getServer().getPluginManager().registerEvents(mapVoteManager, this);
        getServer().getPluginManager().registerEvents(teamSelectorManager, this);
        if (updateService != null) {
            getServer().getPluginManager().registerEvents(new com.l299l.newbedwars.update.UpdateNotifier(this, updateService), this);
        }
        guiManager.reloadGuis();
    }

    public void reloadGeneratorConfigs() {
        updater.reloadGeneratorsConfigurations();
        reloadGenerators();
    }

    private void reloadGenerators() {
        generatorLeveling.clear();
        for (int i = 0; i < updater.getGeneratorsConfigurations().size(); i++) {
            FileConfiguration configuration = updater.getGeneratorsConfigurations().get(i);
            generatorLeveling.put(configuration.getString("ConfigurationId"), new GeneratorLeveling(configuration));
        }
    }
}
