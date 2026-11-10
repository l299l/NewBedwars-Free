package com.l299l.newbedwars.update;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.common.update.HttpFetcher;
import com.l299l.newbedwars.common.update.PluginVersion;
import com.l299l.newbedwars.common.update.Product;
import com.l299l.newbedwars.common.update.ReleaseInfo;
import com.l299l.newbedwars.common.update.UpdateChecker;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UpdateService {
    private static final long FIRST_CHECK_TICKS = 100L;
    private static final long TICKS_PER_HOUR = 72000L;

    private final NewBedwars plugin;
    private final UpdateChecker checker;
    private final UpdateDownloader downloader;
    private final AtomicBoolean downloading = new AtomicBoolean();
    private final Set<String> notified = ConcurrentHashMap.newKeySet();
    private BukkitTask task;
    private volatile ReleaseInfo available;
    private volatile String downloadedVersion;
    private volatile String loggedVersion;
    private volatile boolean loggedFailure;
    private volatile long lastCheck;

    public UpdateService(NewBedwars plugin) {
        this.plugin = plugin;
        HttpFetcher fetcher = new HttpFetcher(UpdateChecker.userAgent("NewBedwars", plugin.getDescription().getVersion()));
        this.checker = new UpdateChecker(fetcher);
        this.downloader = new UpdateDownloader(fetcher);
    }

    public void restart() {
        stop();
        UpdateSettings settings = Properties.Updates;
        if (!settings.check()) {
            available = null;
            return;
        }
        long period = settings.intervalHours() * TICKS_PER_HOUR;
        long delay = FIRST_CHECK_TICKS;
        if (lastCheck > 0) {
            long elapsedTicks = (System.currentTimeMillis() - lastCheck) / 50L;
            delay = Math.max(FIRST_CHECK_TICKS, period - elapsedTicks);
        }
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> check(true), delay, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public CompletableFuture<UpdateChecker.Result> checkNow() {
        CompletableFuture<UpdateChecker.Result> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(check(false));
            } catch (RuntimeException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Path> downloadNow(ReleaseInfo release) {
        CompletableFuture<Path> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(download(release));
            } catch (IOException | RuntimeException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private UpdateChecker.Result check(boolean automatic) {
        UpdateSettings settings = Properties.Updates;
        UpdateChecker.Result result = checker.check(Product.PLUGIN, currentVersion(), settings.channel());
        lastCheck = System.currentTimeMillis();
        if (result.failed()) {
            if (automatic && !loggedFailure) {
                loggedFailure = true;
                plugin.getLogger().warning("Could not check for updates (" + result.error() + "). Will try again later.");
            }
            return result;
        }
        available = result.newer() ? result.latest() : null;
        ReleaseInfo release = available;
        if (release == null) return result;
        String version = release.version().toString();
        if (automatic && settings.notifyAdmins() && !version.equals(loggedVersion)) {
            loggedVersion = version;
            plugin.getLogger().info("NewBedwars " + version + " is available (this server has "
                    + plugin.getDescription().getVersion() + "): " + release.pageUrl());
            if (!settings.autoDownload()) {
                plugin.getLogger().info("Install it with /bw update download, or set Updates.AutoDownload: true in config.yml.");
            }
        }
        if (automatic && settings.autoDownload() && !version.equals(downloadedVersion)) {
            try {
                download(release);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not download NewBedwars " + version + ": " + e.getMessage());
            }
        }
        return result;
    }

    private Path download(ReleaseInfo release) throws IOException {
        if (!release.supportsGameVersion(serverVersion())) {
            throw new IncompatibleException(release);
        }
        if (!downloading.compareAndSet(false, true)) throw new IOException("a download is already running");
        try {
            Path target = downloader.download(release, Bukkit.getUpdateFolderFile().toPath(), plugin.getPluginFile().getName());
            downloadedVersion = release.version().toString();
            plugin.getLogger().info("Downloaded NewBedwars " + downloadedVersion + " to " + target
                    + "; it will be installed on the next server restart.");
            return target;
        } finally {
            downloading.set(false);
        }
    }

    public ReleaseInfo available() {
        return Properties.Updates.check() ? available : null;
    }

    boolean wasNotified(String key) {
        return notified.contains(key);
    }

    boolean markNotified(String key) {
        return notified.add(key);
    }

    public boolean isDownloaded(ReleaseInfo release) {
        return release != null && release.version().toString().equals(downloadedVersion);
    }

    public PluginVersion currentVersion() {
        return PluginVersion.parse(plugin.getDescription().getVersion());
    }

    static PluginVersion serverVersion() {
        String bukkit = Bukkit.getBukkitVersion();
        int dash = bukkit.indexOf('-');
        return PluginVersion.parse(dash < 0 ? bukkit : bukkit.substring(0, dash));
    }

    public static final class IncompatibleException extends IOException {
        private final transient ReleaseInfo release;

        IncompatibleException(ReleaseInfo release) {
            super("NewBedwars " + release.version() + " needs Minecraft " + release.minGameVersion() + " or newer");
            this.release = release;
        }

        public ReleaseInfo release() {
            return release;
        }
    }
}
