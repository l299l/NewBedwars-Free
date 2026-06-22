package com.l299l.newbedwars.config;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.config.properties.LangMessages;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.HashMap;

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

    public void send(Player p, String text, HashMap<String, String> replace) {
        Language lang = playerLanguage(p);
        String message = langMessages.getMsg(lang, text);
        for (String key : replace.keySet()) {
            message = message.replaceAll(key, replace.get(key));
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
            case NORMAL_SETUP  -> player.sendMessage(getMsg(player, "NormalSetupText").replaceAll(";", "\n"));
            case BUILDING_MODE -> player.sendMessage(getMsg(player, "AutomaticModeText").replaceAll(";", "\n"));
        }
    }

    public void addCustomItemProperty(String name, String suffix) {
        langMessages.addCustomItemProperty(name, suffix);
        for (java.io.File file : updater.getLangFiles().values()) {
            if (file.exists()) {
                addToFile(name, file, suffix);
            }
        }
    }

    private Language playerLanguage(Player p) {
        try {
            return NewBedwars.plugin.getPlayerManager().getPlayer(p.getName()).language();
        } catch (Exception e) {
            return Language.English;
        }
    }

    private void addToFile(String name, java.io.File file, String suffix) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String key = "CustomItemsNames." + name + suffix;
            if (!config.contains(key)) {
                config.set(key, name);
                config.save(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
