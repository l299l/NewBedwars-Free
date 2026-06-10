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

public class Messages {
    private final LangMessages langMessages;
    private final Updater updater;

    public Messages(NewBedwars plugin, Updater updater) {
        langMessages = plugin.getLangMessages();
        this.updater = updater;
    }

    public void send(Player p, String text) {
        Language plLanguage = NewBedwars.plugin.getPlayerManager().getPlayer(p.getName()).language();
        switch (plLanguage) {
            case Polish:
                p.sendMessage(langMessages.getMsgPolish(text));
                break;
            case English:
                p.sendMessage((langMessages.getMsgEnglish(text)));
                break;
        }
    }
    public void send(Player p, String text, HashMap<String, String> replace) {
        Language plLanguage = NewBedwars.plugin.getPlayerManager().getPlayer(p.getName()).language();
        String message = switch (plLanguage) {
            case Polish -> langMessages.getMsgPolish(text);
            case English -> (langMessages.getMsgEnglish(text));
        };
        for (String key : replace.keySet()) {
            message = message.replaceAll(key, replace.get(key));
        }
        p.sendMessage(message);
    }

    public void sendToConsole(String text) {
        Bukkit.getConsoleSender().sendMessage(getMsgToConsole(text));
    }

    public String getMsg(Player p, String text) {
        Language plLanguage = NewBedwars.plugin.getPlayerManager().getPlayer(p.getName()).language();
        return getMsg(plLanguage, text);
    }

    public String getMsg(Language language, String text) {
        switch (language) {
            case Polish:
                return (langMessages.getMsgPolish(text));
            case English:
                return (langMessages.getMsgEnglish(text));
        }
        return null;
    }

    public String getMsgToConsole(String text) {
        String defult = Properties.DefaultLanguage;
        if(defult.equalsIgnoreCase("pl_PL")) {
            return (langMessages.getMsgPolish(text));
        }else if(defult.equalsIgnoreCase("en")) {
            return (langMessages.getMsgEnglish(text));
        }else {
            return (langMessages.getMsgEnglish(text));
        }
    }


    public String getYes(Player p, Boolean yes) {
        Language plLanguage = NewBedwars.plugin.getPlayerManager().getPlayer(p.getName()).language();
        switch (plLanguage) {
            case Polish:
                if (yes) {
                    return ChatColor.translateAlternateColorCodes('&', "&a&lTak!");
                }else {
                    return ChatColor.translateAlternateColorCodes('&', "&c&lNie!");
                }
            case English:
                if (yes) {
                    return ChatColor.translateAlternateColorCodes('&', "&a&lYes!");
                }else {
                    return ChatColor.translateAlternateColorCodes('&', "&c&lNo!");
                }
        }
        return null;
    }

    public void sendConf(Player player, Setup setup) {
        switch (setup) {
            case NORMAL_SETUP:  player.sendMessage(getMsg(player, "NormalSetupText").replaceAll(";", "\n"));return;
            case BUILDING_MODE: player.sendMessage(getMsg(player, "AutomaticModeText").replaceAll(";", "\n"));
        }
    }

    public void addCustomItemProperty(String name, String suffix) {
        langMessages.addCustomItemProperty(name, suffix);
        File enFile = updater.getEnFile();
        File plFile = updater.getPlPLFile();
        if (enFile.exists() && plFile.exists()) {
            addToFile(name, enFile, suffix);
            addToFile(name, plFile, suffix);
        }else {
            NewBedwars.plugin.getLogger().warning("Can't add custom item name to lang files!");
        }
    }

    private void addToFile(String name, File file, String suffix) {
        try {
            BufferedWriter output;
            output = new BufferedWriter(new FileWriter(file, true));
            output.append("  ").append(name).append(suffix).append(": ").append(name);
            output.newLine();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

