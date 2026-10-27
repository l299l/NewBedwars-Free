package com.l299l.newbedwars.api;

/** Holds the running API instance; set by the plugin on enable and cleared on disable. */
public final class ApiHolder {
    static volatile NewBedwarsAPI api;

    private ApiHolder() {}

    public static void set(NewBedwarsAPI instance) {
        api = instance;
    }
}
