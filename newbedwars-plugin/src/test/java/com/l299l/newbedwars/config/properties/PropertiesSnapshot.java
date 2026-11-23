package com.l299l.newbedwars.config.properties;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

final class PropertiesSnapshot {
    private final Map<Field, Object> values = new HashMap<>();

    private PropertiesSnapshot() {
    }

    static PropertiesSnapshot take() {
        PropertiesSnapshot snapshot = new PropertiesSnapshot();
        for (Field field : Properties.class.getDeclaredFields()) {
            int mod = field.getModifiers();
            if (!Modifier.isStatic(mod) || Modifier.isFinal(mod)) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(null);
                snapshot.values.put(field, value);
                Object copy = copy(value);
                if (copy != value && field.getType().isInstance(copy)) field.set(null, copy);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return snapshot;
    }

    private static Object copy(Object value) {
        if (value instanceof java.util.LinkedHashMap) return new java.util.LinkedHashMap<>((Map<?, ?>) value);
        if (value instanceof HashMap) return new HashMap<>((Map<?, ?>) value);
        if (value instanceof java.util.ArrayList) return new java.util.ArrayList<>((java.util.List<?>) value);
        if (value instanceof java.util.HashSet) return new java.util.HashSet<>((java.util.Set<?>) value);
        return value;
    }

    void restore() {
        for (Map.Entry<Field, Object> e : values.entrySet()) {
            try {
                e.getKey().set(null, e.getValue());
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
