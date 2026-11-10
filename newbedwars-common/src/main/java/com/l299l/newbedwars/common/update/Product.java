package com.l299l.newbedwars.common.update;

public enum Product {
    PLUGIN("paper", "NewBedwars-"),
    PROXY("velocity", "NewBedwars-Proxy-");

    private static final String PANEL_PREFIX = "NewBedwars-Panel-";

    private final String loader;
    private final String jarPrefix;

    Product(String loader, String jarPrefix) {
        this.loader = loader;
        this.jarPrefix = jarPrefix;
    }

    public String loader() {
        return loader;
    }

    public boolean isJar(String fileName) {
        if (fileName == null || !fileName.endsWith(".jar") || fileName.endsWith("-sources.jar")) return false;
        if (!fileName.startsWith(jarPrefix)) return false;
        return this == PROXY || (!fileName.startsWith(PROXY.jarPrefix) && !fileName.startsWith(PANEL_PREFIX));
    }
}
