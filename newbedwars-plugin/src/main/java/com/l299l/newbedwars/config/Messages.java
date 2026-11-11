package com.l299l.newbedwars.config;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.config.properties.LangMessages;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import com.l299l.newbedwars.player.PlayerProfile;

public class Messages {
    private final LangMessages langMessages;
    private final Updater updater;

    public Messages(NewBedwars plugin, Updater updater) {
        langMessages = plugin.getLangMessages();
        this.updater = updater;
    }

    public void send(Player p, String text) {
        Language lang = playerLanguage(p);
        p.sendMessage(langMessages.getMsg(lang, text));
    }

    public void send(Player p, String text, Map<String, String> replace) {
        Language lang = playerLanguage(p);
        String message = langMessages.getMsg(lang, text);
        for (String key : replace.keySet()) {
            message = message.replace(key, replace.get(key));
        }
        p.sendMessage(message);
    }

    public void sendToConsole(String text) {
        Bukkit.getConsoleSender().sendMessage(getMsgToConsole(text));
    }

    public String getMsg(Player p, String text) {
        return langMessages.getMsg(playerLanguage(p), text);
    }

    public String getMsg(Language language, String text) {
        return langMessages.getMsg(language, text);
    }

    public String getMsgToConsole(String text) {
        Language lang = Language.fromCode(Properties.DefaultLanguage);
        if (lang == null) lang = Language.English;
        return langMessages.getMsg(lang, text);
    }

    public String getYes(Player p, Boolean yes) {
        Language lang = playerLanguage(p);
        String yes_text, no_text;
        switch (lang) {
            case Polish  -> { yes_text = "&a&lTak!";  no_text = "&c&lNie!"; }
            case German  -> { yes_text = "&a&lJa!";   no_text = "&c&lNein!"; }
            case Spanish -> { yes_text = "&a&lSí!";   no_text = "&c&lNo!"; }
            case French  -> { yes_text = "&a&lOui!";  no_text = "&c&lNon!"; }
            case Russian -> { yes_text = "&a&lДа!";   no_text = "&c&lНет!"; }
            default      -> { yes_text = "&a&lYes!";  no_text = "&c&lNo!"; }
        }
        return ChatColor.translateAlternateColorCodes('&', yes ? yes_text : no_text);
    }

    public void sendConf(Player player, Setup setup) {
        switch (setup) {
            case NORMAL_SETUP   -> player.sendMessage(getMsg(player, "NormalSetupText").replaceAll(";", "\n"));
            case ADVANCED_SETUP -> player.sendMessage(getMsg(player, "AdvancedSetupText").replaceAll(";", "\n"));
            case AUTOMATIC_SETUP  -> player.sendMessage(getMsg(player, "AutomaticModeText").replaceAll(";", "\n"));
        }
    }

    public void addCustomItemProperty(String name, String suffix) {
        langMessages.addCustomItemProperty(name, suffix);
        String key = "CustomItemsNames." + name + suffix;
        for (Language lang : Language.values()) {
            org.bukkit.configuration.file.FileConfiguration conf = langConf(lang);
            if (conf != null && !conf.contains(key)) {
                conf.set(key, name);
                saveLater(lang);
            }
        }
    }

    private final java.util.Set<Language> pendingSaves = java.util.EnumSet.noneOf(Language.class);

    private void saveLater(Language lang) {
        boolean first = pendingSaves.isEmpty();
        pendingSaves.add(lang);
        if (!first) return;
        try {
            Bukkit.getScheduler().runTask(NewBedwars.plugin, this::flushPendingSaves);
        } catch (RuntimeException e) {
            flushPendingSaves();
        }
    }

    private void flushPendingSaves() {
        for (Language lang : java.util.List.copyOf(pendingSaves)) saveLangFile(lang, false);
        pendingSaves.clear();
    }

    public org.bukkit.configuration.file.FileConfiguration langConf(Language lang) {
        return updater.getLangConfs().get(lang);
    }

    public void saveLangFile(Language lang, boolean backup) {
        org.bukkit.configuration.file.FileConfiguration conf = langConf(lang);
        java.io.File file = updater.getLangFiles().get(lang);
        if (conf == null || file == null || !file.exists()) return;
        String content = conf.saveToString();
        java.nio.file.Path backups = backup ? NewBedwars.plugin.getDataFolder().toPath()
                .resolve("data/backups/shop/language/" + lang.getCode()) : null;
        int keep = backup ? Math.max(1, com.l299l.newbedwars.config.properties.Properties.BackupsKeep) : 0;
        com.l299l.newbedwars.config.data.IOExecutor.submit(() -> {
            try {
                com.l299l.newbedwars.config.data.json.arenas.AtomicFileWriter.write(file.toPath(), content, backups, keep, ".yml");
            } catch (IOException e) {
                NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not save " + file.getPath(), e);
            }
        });
    }

    private Language playerLanguage(Player p) {
        PlayerProfile profile = NewBedwars.plugin.getPlayerManager().getPlayer(p.getName());
        return profile == null || profile.language() == null ? Language.English : profile.language();
    }
}
