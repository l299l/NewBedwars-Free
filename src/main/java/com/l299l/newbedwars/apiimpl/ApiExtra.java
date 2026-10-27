package com.l299l.newbedwars.apiimpl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.l299l.newbedwars.api.ExtraData;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/** One addon's namespace inside the {@code extra} JSON object of an arena or team. */
final class ApiExtra implements ExtraData {
    private final Supplier<JsonObject> root;
    private final String namespace;
    private final Runnable onChange;

    ApiExtra(Supplier<JsonObject> root, String namespace, Runnable onChange) {
        this.root = root;
        this.namespace = namespace;
        this.onChange = onChange;
    }

    /** The addon's object, or {@code null} while it has stored nothing. */
    private JsonObject read() {
        JsonElement element = root.get().get(namespace);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private JsonObject write() {
        JsonObject object = read();
        if (object == null) {
            object = new JsonObject();
            root.get().add(namespace, object);
        }
        return object;
    }

    private JsonPrimitive primitive(String key) {
        JsonObject object = read();
        if (object == null) return null;
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsJsonPrimitive() : null;
    }

    @Override
    public String getString(String key, String defaultValue) {
        JsonPrimitive p = primitive(key);
        return p == null ? defaultValue : p.getAsString();
    }

    @Override
    public int getInt(String key, int defaultValue) {
        JsonPrimitive p = primitive(key);
        if (p == null || !p.isNumber()) return defaultValue;
        return p.getAsInt();
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        JsonPrimitive p = primitive(key);
        if (p == null || !p.isNumber()) return defaultValue;
        return p.getAsDouble();
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        JsonPrimitive p = primitive(key);
        if (p == null || !p.isBoolean()) return defaultValue;
        return p.getAsBoolean();
    }

    @Override
    public void set(String key, String value) {
        if (value == null) {
            remove(key);
            return;
        }
        write().addProperty(key, value);
        onChange.run();
    }

    @Override
    public void set(String key, int value) {
        write().addProperty(key, value);
        onChange.run();
    }

    @Override
    public void set(String key, double value) {
        write().addProperty(key, value);
        onChange.run();
    }

    @Override
    public void set(String key, boolean value) {
        write().addProperty(key, value);
        onChange.run();
    }

    @Override
    public boolean has(String key) {
        JsonObject object = read();
        return object != null && object.has(key);
    }

    @Override
    public void remove(String key) {
        JsonObject object = read();
        if (object == null || !object.has(key)) return;
        object.remove(key);
        if (object.size() == 0) root.get().remove(namespace);
        onChange.run();
    }

    @Override
    public Set<String> keys() {
        JsonObject object = read();
        return object == null ? new LinkedHashSet<>() : new LinkedHashSet<>(object.keySet());
    }
}
