package com.l299l.newbedwars.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class YamlGuard {
    private YamlGuard() {
    }

    public static boolean ensureValid(File file, Logger logger) {
        if (file == null || !file.isFile()) return true;
        try {
            new YamlConfiguration().load(file);
            return true;
        } catch (InvalidConfigurationException e) {
            File target = new File(file.getParentFile(), file.getName() + ".corrupt-" + System.currentTimeMillis());
            if (file.renameTo(target)) {
                logger.severe(file.getPath() + " is not valid YAML and was moved to " + target.getName()
                        + ". A default file was created; fix the old one and copy your changes back. Error: " + firstLine(e));
            } else {
                logger.severe(file.getPath() + " is not valid YAML and could not be moved aside. Error: " + firstLine(e));
            }
            return false;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not read " + file.getPath(), e);
            return true;
        }
    }

    private static String firstLine(Exception e) {
        String message = String.valueOf(e.getMessage());
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
