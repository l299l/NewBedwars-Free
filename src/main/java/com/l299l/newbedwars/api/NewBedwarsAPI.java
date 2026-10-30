package com.l299l.newbedwars.api;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.ArenaGroupView;
import com.l299l.newbedwars.api.view.ArenaState;
import com.l299l.newbedwars.api.edit.ArenaEditor;
import com.l299l.newbedwars.api.command.AddonCommand;
import com.l299l.newbedwars.api.command.CommandContext;
import com.l299l.newbedwars.api.registry.UpgradeDefinition;
import com.l299l.newbedwars.api.registry.ArmorTierDefinition;
import com.l299l.newbedwars.api.registry.PhaseActionHandler;
import com.l299l.newbedwars.api.registry.ShopLogic;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Optional;

/**
 * Entry point for addons. Get it with {@link #get()} (or from Bukkit's {@code ServicesManager}) after
 * NewBedwars has enabled; add {@code NewBedwars} to {@code depend}/{@code softdepend} in your plugin.yml.
 * <p>
 * Everything in the {@code com.l299l.newbedwars.api} package is the stable contract; other packages
 * can change in any update.
 * <p>
 * Everything an addon registers is removed again when that addon is disabled; registering a name that
 * already exists throws {@link IllegalArgumentException}.
 */
public interface NewBedwarsAPI {
    /** @throws IllegalStateException when NewBedwars is not enabled */
    static NewBedwarsAPI get() {
        NewBedwarsAPI api = Bukkit.getServicesManager().load(NewBedwarsAPI.class);
        if (api == null) throw new IllegalStateException("NewBedwars is not enabled");
        return api;
    }

    /** Version of the NewBedwars plugin. */
    String getVersion();

    Collection<ArenaView> getArenas();

    Optional<ArenaView> getArena(String name);

    Optional<ArenaView> getArena(World world);

    /** The arena the player plays or spectates in. */
    Optional<ArenaView> getArena(Player player);

    /** All arena groups (maps that are voted between), read-only. */
    Collection<ArenaGroupView> getGroups();

    /** The group with that name (case-insensitive). */
    Optional<ArenaGroupView> getGroup(String name);

    /**
     * Changes the setup of an arena.
     * @throws IllegalStateException when the arena is not {@link ArenaState#WAITING}
     */
    ArenaEditor edit(ArenaView arena);

    /** Adds an action usable in {@code GamePhases} in config.yml as {@code NAME} or {@code NAME:argument}. */
    void registerPhaseAction(Plugin owner, String name, PhaseActionHandler handler);

    /** Adds a {@code /bw} subcommand. */
    void registerCommand(Plugin owner, AddonCommand command, CommandContext context);

    /** Adds shop item logic; use {@code key} as {@code itemLogic} in {@code items.yml}. */
    void registerShopLogic(Plugin owner, String key, ShopLogic logic);

    void registerArmorTier(Plugin owner, ArmorTierDefinition tier);

    /** Adds a team upgrade or trap, and its shop icon item (named like the upgrade) when it does not exist. */
    void registerUpgrade(Plugin owner, UpgradeDefinition upgrade);

    /** Removes everything {@code owner} registered. Done automatically when the plugin is disabled. */
    void unregisterAll(Plugin owner);
}
