package com.l299l.newbedwars.config.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CorruptFiles {
    private CorruptFiles() {
    }

    public static boolean moveAside(File file, Logger log, String whatIsDisabled) {
        if (!file.exists()) return true;
        File target = new File(file.getParentFile(), file.getName() + ".corrupt-" + System.currentTimeMillis());
        try {
            Files.move(file.toPath(), target.toPath());
            log.severe("Moved the unreadable file " + file.getName() + " to " + target.getName());
            return true;
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not move " + file.getPath() + " aside; " + whatIsDisabled, e);
            return false;
        }
    }
}
