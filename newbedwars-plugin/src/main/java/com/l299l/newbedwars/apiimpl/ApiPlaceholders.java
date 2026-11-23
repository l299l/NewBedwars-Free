package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.api.text.TextPlaceholder;
import com.l299l.newbedwars.apiimpl.adapter.ApiRegistrations;
import com.l299l.newbedwars.placeholder.AddonPlaceholders;
import org.bukkit.plugin.Plugin;

public final class ApiPlaceholders {
    private final ApiRegistrations registrations;

    public ApiPlaceholders(ApiRegistrations registrations) {
        this.registrations = registrations;
    }

    public void register(Plugin owner, String name, TextPlaceholder placeholder) {
        if (owner == null) throw new IllegalArgumentException("owner is null");
        AddonPlaceholders.register(name, placeholder);
        registrations.add(owner, () -> AddonPlaceholders.unregister(name, placeholder));
    }
}
