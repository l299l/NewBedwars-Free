package com.l299l.newbedwars.update;

import com.l299l.newbedwars.common.update.Channel;
import com.l299l.newbedwars.common.update.Fetcher;
import com.l299l.newbedwars.common.update.PluginVersion;
import com.l299l.newbedwars.common.update.ReleaseInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class UpdateDownloaderTest {

    @TempDir
    Path dir;

    private static byte[] jar(String pluginYml) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            if (pluginYml != null) {
                zip.putNextEntry(new ZipEntry("plugin.yml"));
                zip.write(pluginYml.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("com/example/A.class"));
            zip.write(new byte[]{1, 2, 3});
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static String sha512(byte[] data) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-512").digest(data)) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private static ReleaseInfo release(String version, String hash) {
        return new ReleaseInfo(PluginVersion.parse(version), Channel.BETA, "https://modrinth.com/x", "https://cdn.example/x.jar",
                "NewBedwars-" + version + ".jar", "SHA-512", hash, PluginVersion.parse("1.17"), "Modrinth");
    }

    private static Fetcher serving(byte[] data) {
        return new Fetcher() {
            @Override
            public String get(String url) {
                throw new UnsupportedOperationException();
            }

            @Override
            public InputStream open(String url) {
                return new ByteArrayInputStream(data);
            }
        };
    }

    private static final String YML = "name: NewBedwars\nversion: '2.7-beta'\nmain: com.l299l.newbedwars.NewBedwars\n";

    @Test
    void downloadsUnderTheCurrentJarName() throws Exception {
        byte[] data = jar(YML);
        Path target = new UpdateDownloader(serving(data)).download(release("2.7-beta", sha512(data)), dir.resolve("update"), "NewBedwars-2.6-beta.jar");
        assertEquals(dir.resolve("update").resolve("NewBedwars-2.6-beta.jar"), target);
        assertArrayEquals(data, Files.readAllBytes(target));
        assertEquals(List.of(target), Files.list(dir.resolve("update")).toList());
    }

    @Test
    void wrongChecksumLeavesNothing() throws Exception {
        byte[] data = jar(YML);
        UpdateDownloader downloader = new UpdateDownloader(serving(data));
        IOException e = assertThrows(IOException.class,
                () -> downloader.download(release("2.7-beta", "00"), dir, "NewBedwars.jar"));
        assertTrue(e.getMessage().contains("checksum"));
        assertEquals(0, Files.list(dir).count());
    }

    @Test
    void rejectsOtherPluginsAndVersions() throws Exception {
        for (Map.Entry<String, String> c : Map.of(
                "name: OtherPlugin\nversion: '2.7-beta'\n", "not NewBedwars",
                "name: NewBedwars\nversion: '2.8-beta'\n", "version 2.8-beta").entrySet()) {
            byte[] data = jar(c.getKey());
            IOException e = assertThrows(IOException.class,
                    () -> new UpdateDownloader(serving(data)).download(release("2.7-beta", sha512(data)), dir, "NewBedwars.jar"));
            assertTrue(e.getMessage().contains(c.getValue()), e.getMessage());
        }
        byte[] noYml = jar(null);
        assertThrows(IOException.class,
                () -> new UpdateDownloader(serving(noYml)).download(release("2.7-beta", sha512(noYml)), dir, "NewBedwars.jar"));
        byte[] notZip = "hello".getBytes(StandardCharsets.UTF_8);
        assertThrows(IOException.class,
                () -> new UpdateDownloader(serving(notZip)).download(release("2.7-beta", sha512(notZip)), dir, "NewBedwars.jar"));
        assertEquals(0, Files.list(dir).count());
    }

    @Test
    void needsAChecksumAndAPlainJarName() throws Exception {
        byte[] data = jar(YML);
        UpdateDownloader downloader = new UpdateDownloader(serving(data));
        ReleaseInfo noHash = new ReleaseInfo(PluginVersion.parse("2.7-beta"), Channel.BETA, null, "https://x/y.jar",
                "y.jar", null, null, null, "GitHub");
        assertThrows(IOException.class, () -> downloader.download(noHash, dir, "NewBedwars.jar"));
        assertThrows(IOException.class, () -> downloader.download(release("2.7-beta", sha512(data)), dir, "../NewBedwars.jar"));
        assertThrows(IOException.class, () -> downloader.download(release("2.7-beta", sha512(data)), dir, "NewBedwars.zip"));
    }

    @Test
    void releaseChecksTheLowestGameVersion() {
        ReleaseInfo r = release("2.7-beta", "x");
        assertTrue(r.supportsGameVersion(PluginVersion.parse("1.17.1")));
        assertTrue(r.supportsGameVersion(PluginVersion.parse("26.3")));
        assertFalse(r.supportsGameVersion(PluginVersion.parse("1.16.5")));
        assertTrue(r.supportsGameVersion(null));
    }

    @Test
    void settingsReadWithDefaults() {
        UpdateSettings s = UpdateSettings.read(Map.<String, Object>of(
                "Updates.Check", false,
                "Updates.Notify", "yes",
                "Updates.AutoDownload", true,
                "Updates.Channel", "alpha",
                "Updates.CheckIntervalHours", 0)::get);
        assertFalse(s.check());
        assertTrue(s.notifyAdmins());
        assertTrue(s.autoDownload());
        assertEquals(Channel.ALPHA, s.channel());
        assertEquals(12, s.intervalHours());
        assertEquals(UpdateSettings.DEFAULTS, UpdateSettings.read(key -> null));
    }
}
