package com.l299l.newbedwars.api.view;

import java.util.Set;

/**
 * Data an addon keeps on an arena or team, saved in the arena file. Each addon has its own namespace, so
 * addons never see each other's keys. Changes are written to disk shortly after, there is no save method.
 * <p>
 * The getters return {@code defaultValue} when the key is missing or holds another type, e.g. {@code getInt}
 * on a text value.
 */
public interface ExtraData {
    String getString(String key, String defaultValue);

    int getInt(String key, int defaultValue);

    double getDouble(String key, double defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    /** A {@code null} value removes the key. */
    void set(String key, String value);

    void set(String key, int value);

    void set(String key, double value);

    void set(String key, boolean value);

    boolean has(String key);

    void remove(String key);

    /** A copy; changing it doesn't change the data. */
    Set<String> keys();
}
