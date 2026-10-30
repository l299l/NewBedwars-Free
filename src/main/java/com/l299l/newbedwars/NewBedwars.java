package com.l299l.newbedwars;

import com.l299l.newbedwars.arena.Arena;
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
import com.l299l.newbedwars.events.ArenaGameplayEvents;
import com.l299l.newbedwars.events.ChatEvent;
import com.l299l.newbedwars.events.InitializationEvent;
import com.l299l.newbedwars.events.LanguageEvent;
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
    private final JoinNpcManager joinNpcManager;
    private final ArenaGroupManager arenaGroupManager;
    private final MapVoteManager mapVoteManager;

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
    }

    @Override
    public void onEnable() {
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
        }
        joinNpcManager.start();
        apiImpl = new com.l299l.newbedwars.apiimpl.NewBedwarsApiImpl();
        getServer().getPluginManager().registerEvents(apiImpl, this);
        getServer().getServicesManager().register(com.l299l.newbedwars.api.NewBedwarsAPI.class, apiImpl, this, org.bukkit.plugin.ServicePriority.Normal);
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NewBedwars]: NewBedwars v" + getDescription().getVersion() + " is " + ChatColor.DARK_GREEN + "Enabled");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        joinNpcManager.shutdown();
        int active = 0;
        for (IArena arena : Arena.arenaByName.values()) {
            if (!arena.getPlayers().isEmpty() || !arena.getSpectators().isEmpty()) active++;
            else if (!arena.getPlayersOnSetup().isEmpty()) {
                for (java.util.UUID p : new ArrayList<>(arena.getPlayersOnSetup())) {
                    Player pl = Bukkit.getPlayer(p);
                    if (pl == null) continue;
                    com.l299l.newbedwars.player.LobbyService.sendToLobby(pl, com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent.Reason.SERVER_SHUTDOWN);
                    pl.kickPlayer(ChatColor.RED + "Server went down!");
                }
            }
        }
        if (active > 0) {
            getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "[NewBedwars]: " + active + " arena(s) still active — forcing shutdown...");
        }
        for (IArena arena : new ArrayList<>(Arena.arenaByName.values())) {
            arena.forceShutdown();
        }
        LobbyData.save(lobbyLocation);
        if (dataManager != null) dataManager.save();
        if (ArenaMaps.isEnabled() && Properties.RollbackRestoreOnShutdown) {
            // dataManager.save() unloaded the worlds without saving, so the folders can be replaced now.
            for (IArena arena : Arena.arenaByName.values()) {
                String name = arena.getArenaName();
                if (!ArenaMaps.hasTemplate(name) || !ArenaMaps.isDirty(name)) continue;
                try {
                    ArenaMaps.restoreOffline(name, arena.getArenaWorld().getName());
                } catch (java.io.IOException e) {
                    getLogger().warning("Could not restore map of arena " + name + " on shutdown: " + e.getMessage());
                }
            }
        }
        IOExecutor.flush();
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NewBedwars]: NewBedwars v" + getDescription().getVersion() + " is " + ChatColor.DARK_RED + "Disabled");
    }



    public void reloadAll() {
        HandlerList.unregisterAll(this);
        for (IArena arena : new ArrayList<>(Arena.arenaByName.values())) {
            if (!arena.getPlayers().isEmpty() || !arena.getSpectators().isEmpty()) {
                arena.stop();
            }
        }
        Arena.arenaByName.clear();
        Arena.arenaByWorld.clear();
        updater.reloadLanguages();
        updater.reloadBossBars();
        updater.reloadScoreboards();
        updater.reloadGeneratorsConfigurations();
        langMessages = new LangMessages(updater.getLangConfs());
        langMessages.reloadMessages();
        messages = new Messages(this, updater);
        updater.updateConf();
        properties = new Properties();
        dataManager = new DataManager();
        dataManager.load();
        arenaGroupManager.load();
        mapVoteManager.clear();
        partyManager = new PartyManager(this, messages);
        bossBarManager = new BossBarManager(updater.getBossBarsConf());
        bossBarManager.loadBossBars();
        scoreboardManager = new ScoreboardManager(updater.getScoreboardsConf());
        scoreboardManager.loadScoreboards();
        reloadGenerators();
        reloadEvents();
        lobbyLocation = null;
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

    private void reloadEvents(){
        getServer().getPluginManager().registerEvents(new LanguageEvent(this), this);
        getServer().getPluginManager().registerEvents(new InitializationEvent(this), this);
        getServer().getPluginManager().registerEvents(new ArenaGameplayEvents(this), this);
        getServer().getPluginManager().registerEvents(new ChatEvent(this), this);
        if (apiImpl != null) {
            getServer().getPluginManager().registerEvents(apiImpl, this);
        }
        joinNpcManager.registerListeners();
        getServer().getPluginManager().registerEvents(mapVoteManager, this);
        guiManager.reloadGuis();
    }

    private void reloadGenerators() {
        generatorLeveling.clear();
        for (int i = 0; i < updater.getGeneratorsConfigurations().size(); i++) {
            FileConfiguration configuration = updater.getGeneratorsConfigurations().get(i);
            generatorLeveling.put(configuration.getString("ConfigurationId"), new GeneratorLeveling(configuration));
        }
    }
}
