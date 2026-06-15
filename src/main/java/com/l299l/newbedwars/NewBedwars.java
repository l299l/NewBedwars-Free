package com.l299l.newbedwars;

import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.shops.customitems.CustomItemManager;
import com.l299l.newbedwars.bossbar.BossBarManager;
import com.l299l.newbedwars.commands.LangStandaloneCommand;
import com.l299l.newbedwars.commands.LobbyStandaloneCommand;
import com.l299l.newbedwars.commands.bedwars.MainCommand;
import com.l299l.newbedwars.commands.party.PartyCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.Updater;
import com.l299l.newbedwars.config.data.DataManager;
import com.l299l.newbedwars.config.data.LobbyData;
import com.l299l.newbedwars.config.properties.LangMessages;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.events.ArenaGameplayEvents;
import com.l299l.newbedwars.events.ChatEvent;
import com.l299l.newbedwars.events.InitializationEvent;
import com.l299l.newbedwars.events.LanguageEvent;
import com.l299l.newbedwars.gui.GuiManager;
import com.l299l.newbedwars.parties.PartyManager;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Objects;

public final class NewBedwars extends JavaPlugin {
    public static NewBedwars plugin;
    private Location lobbyLocation;
    private final Updater updater;
    private final GuiManager guiManager;
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

    public NewBedwars() {
        plugin = this;
        guiManager = new GuiManager();
        customItemManager = new CustomItemManager();
        updater = new Updater();
        playerManager = new PlayerManager();
        generatorLeveling = new HashMap<>();
    }

    @Override
    public void onEnable() {
        reloadAll();
        Objects.requireNonNull(getCommand("bw")).setExecutor(new MainCommand());
        Objects.requireNonNull(getCommand("lobby")).setExecutor(new LobbyStandaloneCommand());
        LangStandaloneCommand langCmd = new LangStandaloneCommand();
        Objects.requireNonNull(getCommand("lang")).setExecutor(langCmd);
        Objects.requireNonNull(getCommand("lang")).setTabCompleter(langCmd);
        PartyCommand partyCmd = new PartyCommand();
        Objects.requireNonNull(getCommand("party")).setExecutor(partyCmd);
        Objects.requireNonNull(getCommand("party")).setTabCompleter(partyCmd);
        Objects.requireNonNull(getCommand("p")).setExecutor(partyCmd);
        Objects.requireNonNull(getCommand("p")).setTabCompleter(partyCmd);
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NewBedwars]: NewBedwars v" + getDescription().getVersion() + " is " + ChatColor.DARK_GREEN + "Enabled");
    }

    @Override
    public void onDisable() {
        LobbyData.save(lobbyLocation);
        if (dataManager != null) dataManager.save();
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NewBedwars]: NewBedwars v" + getDescription().getVersion() + " is " + ChatColor.DARK_RED + "Disabled");
    }



    public void reloadAll() {
        HandlerList.unregisterAll(this);
        Arena.arenaByName.clear();
        Arena.arenaByWorld.clear();
        updater.reloadEnglish();
        updater.reloadPolish();
        updater.reloadBossBars();
        updater.reloadScoreboards();
        updater.reloadGeneratorsConfigurations();
        langMessages = new LangMessages(updater.getPlPLConf(), updater.getEnConf());
        langMessages.reloadMessages();
        messages = new Messages(this, updater);
        updater.updateConf();
        properties = new Properties();
        dataManager = new DataManager();
        dataManager.load();
        partyManager = new PartyManager(this, messages);
        bossBarManager = new BossBarManager(updater.getBossBarsConf());
        bossBarManager.loadBossBars();
        scoreboardManager = new ScoreboardManager(updater.getScoreboardsConf());
        scoreboardManager.loadScoreboards();
        reloadGenerators();
        reloadEvents();
        lobbyLocation = LobbyData.load();
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
    public GuiManager getGuiManager() {
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

    public Location getLobbyLocation() {
        return lobbyLocation != null ? lobbyLocation : Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    public void setLobbyLocation(Location location) {
        this.lobbyLocation = location;
        LobbyData.save(location);
    }

    public HashMap<String, GeneratorLeveling> getGeneratorLeveling() {
        return generatorLeveling;
    }

    private void reloadEvents(){
        getServer().getPluginManager().registerEvents(new LanguageEvent(this), this);
        getServer().getPluginManager().registerEvents(new InitializationEvent(this), this);
        getServer().getPluginManager().registerEvents(new ArenaGameplayEvents(this), this);
        getServer().getPluginManager().registerEvents(new ChatEvent(this), this);
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
