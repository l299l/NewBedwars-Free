package com.l299l.newbedwars.proxy.velocity;

import com.l299l.newbedwars.common.update.Channel;
import com.l299l.newbedwars.common.update.HttpFetcher;
import com.l299l.newbedwars.common.update.PluginVersion;
import com.l299l.newbedwars.common.update.Product;
import com.l299l.newbedwars.common.update.UpdateChecker;
import org.slf4j.Logger;

final class ProxyUpdateCheck {
    private final Logger logger;
    private final UpdateChecker checker;
    private final String currentText;
    private final PluginVersion current;
    private final Channel channel;
    private final boolean notify;
    private volatile String status;
    private volatile String loggedVersion;
    private volatile boolean loggedFailure;

    ProxyUpdateCheck(Logger logger, String currentVersion, Channel channel, boolean notify) {
        this.logger = logger;
        this.currentText = currentVersion;
        this.current = PluginVersion.parse(currentVersion);
        this.channel = channel;
        this.notify = notify;
        this.checker = new UpdateChecker(new HttpFetcher(UpdateChecker.userAgent("NewBedwars-Proxy", currentVersion)));
    }

    void run() {
        UpdateChecker.Result result = checker.check(Product.PROXY, current, channel);
        if (result.failed()) {
            status = "Update check failed: " + result.error();
            if (!loggedFailure) {
                loggedFailure = true;
                logger.warn("Could not check for NewBedwars-Proxy updates ({}). Will try again later.", result.error());
            }
            return;
        }
        if (!result.newer()) {
            status = "Up to date (" + currentText + ")";
            return;
        }
        String version = result.latest().version().toString();
        status = "Update available: " + version + " (this proxy has " + currentText + ") - " + result.latest().pageUrl();
        if (notify && !version.equals(loggedVersion)) {
            loggedVersion = version;
            logger.info("NewBedwars-Proxy {} is available (this proxy has {}): {}", version, currentText, result.latest().pageUrl());
        }
    }

    String status() {
        return status;
    }
}
