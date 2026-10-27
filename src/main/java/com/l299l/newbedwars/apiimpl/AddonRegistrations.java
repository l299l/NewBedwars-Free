package com.l299l.newbedwars.apiimpl;

import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Remembers how to undo what each addon registered, so it can be removed when the addon is disabled. */
final class AddonRegistrations {
    private final Map<Plugin, List<Runnable>> undo = new HashMap<>();

    synchronized void add(Plugin owner, Runnable undoAction) {
        undo.computeIfAbsent(owner, p -> new ArrayList<>()).add(undoAction);
    }

    synchronized void releaseAll(Plugin owner) {
        List<Runnable> actions = undo.remove(owner);
        if (actions == null) return;
        for (int i = actions.size() - 1; i >= 0; i--) {
            try {
                actions.get(i).run();
            } catch (RuntimeException ignored) {
                // already removed
            }
        }
    }
}
