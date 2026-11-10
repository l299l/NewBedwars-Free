package com.l299l.newbedwars.update;

import com.l299l.newbedwars.common.update.Fetcher;
import com.l299l.newbedwars.common.update.PluginVersion;
import com.l299l.newbedwars.common.update.ReleaseInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class UpdateDownloader {
    static final long MAX_BYTES = 64L * 1024 * 1024;
    static final String PLUGIN_NAME = "NewBedwars";

    private final Fetcher fetcher;

    public UpdateDownloader(Fetcher fetcher) {
        this.fetcher = fetcher;
    }

    public Path download(ReleaseInfo release, Path updateDir, String jarName) throws IOException {
        if (!release.hasChecksum()) throw new IOException("the release has no published checksum");
        if (jarName == null || !jarName.endsWith(".jar") || jarName.contains("/") || jarName.contains("\\")) {
            throw new IOException("unexpected plugin file name " + jarName);
        }
        Files.createDirectories(updateDir);
        Path part = updateDir.resolve(jarName + ".part");
        Path target = updateDir.resolve(jarName);
        try {
            String actual = copy(release, part);
            if (!actual.equalsIgnoreCase(release.hash())) throw new IOException("checksum mismatch");
            checkPluginYml(part, release.version());
            try {
                Files.move(part, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } finally {
            Files.deleteIfExists(part);
        }
    }

    private String copy(ReleaseInfo release, Path part) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(release.hashAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("unsupported checksum " + release.hashAlgorithm());
        }
        long total = 0;
        byte[] buffer = new byte[16384];
        try (InputStream in = fetcher.open(release.downloadUrl()); OutputStream out = Files.newOutputStream(part)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BYTES) throw new IOException("the download is larger than " + (MAX_BYTES >> 20) + " MB");
                digest.update(buffer, 0, read);
                out.write(buffer, 0, read);
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) hex.append(String.format(Locale.ROOT, "%02x", b));
        return hex.toString();
    }

    static void checkPluginYml(Path jar, PluginVersion expected) throws IOException {
        String name = null;
        String version = null;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("plugin.yml");
            if (entry == null) throw new IOException("the download is not a plugin");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("name:")) name = value(line);
                    else if (line.startsWith("version:")) version = value(line);
                }
            }
        } catch (java.util.zip.ZipException e) {
            throw new IOException("the download is not a jar file");
        }
        if (!PLUGIN_NAME.equals(name)) throw new IOException("the download is not NewBedwars");
        PluginVersion actual = PluginVersion.parse(version);
        if (actual == null || !actual.equals(expected)) throw new IOException("the download has version " + version);
    }

    private static String value(String line) {
        String v = line.substring(line.indexOf(':') + 1).trim();
        if (v.length() >= 2 && (v.startsWith("'") && v.endsWith("'") || v.startsWith("\"") && v.endsWith("\""))) {
            v = v.substring(1, v.length() - 1);
        }
        return v;
    }
}
