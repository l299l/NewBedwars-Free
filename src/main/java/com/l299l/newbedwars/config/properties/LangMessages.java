package com.l299l.newbedwars.config.properties;


import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class LangMessages {
    private final HashMap<String, String> polish;
    private final FileConfiguration polishConf;
    private final HashMap<String, String> english;
    private final FileConfiguration englishConf;


    public LangMessages(FileConfiguration polishConf, FileConfiguration englishConf) {
        this.polishConf = polishConf;
        polish = new HashMap<String, String>();
        this.englishConf = englishConf;
        english = new HashMap<String, String>();
    }

    public void reloadMessages() {
        set("CorrectUsage");
        set("ErrorOnCreate");
        set("OnlyPlayer");
        set("NotInArena");
        set("ArenaNotExists");
        set("AlreadyCreated");
        set("LoadingSchematicError");
        set("ArenaCreated");
        set("ConfigurationGuiName");
        set("NormalSetup");
        set("AdvancedSetup");
        set("NoPermissions");
        set("AutomaticSetup");
        set("NormalSetupLore");
        set("StartArena");
        set("AdvancedSetupLore");
        set("AutomaticSetupLore");
        set("ConfirmLeaveGuiName");
        set("YesLore");
        set("LeaveInfoLore");
        set("LeaveInfo");
        set("NoLore");
        set("SuccessfullyLeave");
        set("KickedByOtherAdmin");
        set("AddedToSetupMode");
        set("AdminHaveBypass");
        set("ArenaReady");
        set("NormalSetupText");
        set("AutomaticModeText");
        set("placeBedFirst");
        set("PageDoesntExists");
        set("CommandDoesntExists");
        set("setMaxInTeamSuccess");
        set("setMaxTeamsSuccess");
        set("createTeamSuccess");
        set("setMinPlayersSuccess");
        set("setTeamBedSuccess");
        set("setTeamShopSuccess");
        set("setTeamSpawnSuccess");
        set("setTeamUpgradesSuccess");
        set("TeamBuildProtAreaPos1Set");
        set("TeamBuildProtAreaPos2Set");
        set("TeamBasePos1Set");
        set("TeamBasePos2Set");
        set("QuickVoidYSet");
        set("WaitingSpawnSet");
        set("WaitingPos1Set");
        set("WaitingPos2Set");
        set("setWaitingTimeSuccess");
        set("setGeneratorSuccess");
        set("TeamNotFound");
        set("TeamRemoved");
        set("TeamPropertyCleared");
        set("TeamPropertyNotSet");
        set("GeneratorRemoved");
        set("GeneratorNotFound");
        set("ArenaDeleted");
        set("ArenaCannotBeDeleted");
        set("ConfirmDeleteGuiName");
        set("DeleteInfo");
        set("DeleteInfoLore");
        set("DeleteYesLore");
        set("DeleteNoLore");
        set("saveSuccess");
        set("ArenaNotEnabled");
        set("NotEnoughPlayers");
        set("ArenaIsFullError");
        set("ArenaJoinError");
        set("ArenaRunningError");
        set("ArenaRestartingError");
        set("PlayerJoinedArena");
        set("PlayerLeftArena");
        set("RejoinTimeOver");
        set("AlreadyInArena");
        set("NoArenaToRejoin");
        set("Rejoined");
        set("RejoinFailed");
        set("GameStarted");
        set("StartCanceled");
        set("ArenaEnabled");
        set("ArenaCantBeEnabled");
        set("ArenaDisabled");
        set("ArenaCantBeDisabled");
        set("AlreadyDisabled");
        set("AlreadyEnabled");
        set("LeaveItemName");
        set("LeaveItemLore");
        set("BedLeaveConfirmTitle");
        set("BedLeaveConfirmSubtitle");
        set("BedLeaveCancelled");
        set("SpectatorItemName");
        set("SpectatorItemLore");
        set("SpectatorEffectsItemName");
        set("SpectatorEffectsItemLore");
        set("SpectatorPlayersGuiName");
        set("SpectatorPlayerLore");
        set("SpectatorEffectsGuiName");
        set("SpectatorNightVisionName");
        set("SpectatorNightVisionLore");
        set("SpectatorSpeedName");
        set("SpectatorSpeedLore");
        set("SpectatorEffectEnabled");
        set("SpectatorEffectDisabled");
        set("KickedFromArena");
        set("DiamondGenerator");
        set("EmeraldGenerator");
        set("shop-itemIcon-description");
        set("shop-item-description");
        set("PlayerDeath");
        set("PlayerDeathByVoid");
        set("BedDestroyed");
        set("BoughtItem");
        set("NotEnoughMoney");
        set("GameEnded");
        set("GameDraw");
        set("BedsDestroyed");
        set("EnderDragonSpawned");
        set("RegenerationDisabled");
        set("ForgeDisabled");
        set("AlarmTrapTriggered");
        set("ArenaChatDisabled");
        set("TeamChatFormat");
        set("ArenaChatFormat");
        set("AdminForcedStart");
        set("AdminStoppedGame");
        set("LanguageChanged");
        set("LanguageInvalid");
        set("LobbySet");
        set("TeleportedToLobby");
        set("NoPermission");
        set("BedDestroyedTitle");
        set("BedDestroyedSubtitle");
        setFromCustomItemsNames();
    }

    public void addCustomItemProperty(String name, String suffix) {
        polish.putIfAbsent(name + suffix, name);
        english.putIfAbsent(name + suffix, name);
    }
    public String getMsgPolish(String text) {
        return polish.get(text);
    }

    public String getMsgEnglish(String text) {
        return english.get(text);
    }

    private String getString(String text, FileConfiguration configuration) {
        String value = configuration.getString(text);
        if (value == null) return "";
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private void setFromCustomItemsNames() {
        ConfigurationSection configurationSectionPl = polishConf.getConfigurationSection("CustomItemsNames");
        ConfigurationSection configurationSectionEn = englishConf.getConfigurationSection("CustomItemsNames");
        if (configurationSectionPl != null) {
            configurationSectionPl.getValues(false).forEach((key, value) -> {
                polish.put(key, ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(configurationSectionPl.getString(key))));
            });
        }
        if (configurationSectionEn != null) {
            configurationSectionEn.getValues(false).forEach((key, value) -> {
                english.put(key, ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(configurationSectionEn.getString(key))));
            });
        }
    }

    private void set(String text) {
        polish.put(text, getString(text, polishConf));
        english.put(text, getString(text, englishConf));
    }
}
