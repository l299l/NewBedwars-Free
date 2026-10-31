package com.l299l.newbedwars.config.data.json.arenas;

import com.l299l.newbedwars.world.template.MapTemplateManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Writes files so a crash mid-write can never leave a half-written (corrupt) file behind. */
public final class AtomicFileWriter {
    private AtomicFileWriter() {}

    /**
     * Writes {@code content} to {@code target} via a temp file. If {@code target} already exists and
     * {@code backupDir} is not null, the old file is first copied to {@code backupDir/<timestamp>.json}
     * and only the newest {@code keep} backups are kept.
     */
    public static void write(Path target, String content, Path backupDir, int keep) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path tmp = parent.resolve(target.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        if (backupDir != null && keep > 0 && Files.isRegularFile(target) && !sameContent(target, content)) {
            Files.createDirectories(backupDir);
            Files.copy(target, backupDir.resolve(MapTemplateManager.timestamp() + ".json"), StandardCopyOption.REPLACE_EXISTING);
            MapTemplateManager.rotate(backupDir, ".json", keep);
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
            return false;
        }
    }
}
