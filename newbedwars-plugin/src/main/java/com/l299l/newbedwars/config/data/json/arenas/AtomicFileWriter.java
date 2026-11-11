package com.l299l.newbedwars.config.data.json.arenas;

import com.l299l.newbedwars.world.template.MapTemplateManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AtomicFileWriter {
    private static final Logger LOG = Logger.getLogger("NewBedwars");

    private AtomicFileWriter() {}

    public static void write(Path target, String content, Path backupDir, int keep) throws IOException {
        write(target, content, backupDir, keep, ".json");
    }

    public static void write(Path target, String content, Path backupDir, int keep, String backupExtension) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path tmp = parent.resolve(target.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        if (backupDir != null && keep > 0 && Files.isRegularFile(target) && !sameContent(target, content)) {
            Files.createDirectories(backupDir);
            Files.copy(target, backupDir.resolve(MapTemplateManager.timestamp() + backupExtension), StandardCopyOption.REPLACE_EXISTING);
            MapTemplateManager.rotate(backupDir, backupExtension, keep);
        }
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean sameContent(Path target, String content) {
        try {
            return Files.readString(target, StandardCharsets.UTF_8).equals(content);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not compare " + target + " with the new content", e);
            return false;
        }
    }
}
