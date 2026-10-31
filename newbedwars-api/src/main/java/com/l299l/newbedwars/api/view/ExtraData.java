package com.l299l.newbedwars.api.view;

import java.util.Set;

/**
 * Settings an addon keeps on an arena or team. NewBedwars saves them in the arena file without touching them.
 * Each addon gets its own namespace (its plugin name), so addons never see each other's values. Changes are
 * written to disk shortly after (no need to save).
 */
public interface ExtraData {
    String getString(String key, String defaultValue);

    int getInt(String key, int defaultValue);

    double getDouble(String key, double defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    void set(String key, String value);

    void set(String key, int value);

    void set(String key, double value);

    void set(String key, boolean value);

    boolean has(String key);

    void remove(String key);

    Set<String> keys();
}
