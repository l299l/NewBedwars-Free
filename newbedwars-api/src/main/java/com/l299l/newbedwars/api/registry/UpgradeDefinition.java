package com.l299l.newbedwars.api.registry;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import com.l299l.newbedwars.api.NewBedwarsAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * A team upgrade or trap added by an addon. After {@link NewBedwarsAPI#registerUpgrade} it can be put into
 * upgrade GUIs as {@code upg:<name>}. Its prices are saved on the first registration; server owners can change
 * them afterwards.
 */
public final class UpgradeDefinition {
    /**
     * Applies the upgrade to one player. Runs on the server thread for every team member when a level is
     * bought, and on respawn when {@link #reapplyOnRespawn()} is set.
     */
    @FunctionalInterface
    public interface Effect {
        /** @param level the team's level of this upgrade, 1 or higher */
        void apply(Player player, TeamView team, int level);
    }

    /**
     * Runs on the server thread when an enemy walks into the base of a team that owns the trap. Every bought
     * level is one use.
     */
    @FunctionalInterface
    public interface Trap {
        /** @param team the team that owns the trap */
        void trigger(Player enemy, TeamView team, ArenaView arena);
    }

    private final String name;
    private final int maxLevel;
    private final Material icon;
    private final List<Integer> prices;
    private final Effect effect;
    private final Trap trap;
    private boolean reapplyOnRespawn;

    private UpgradeDefinition(String name, int maxLevel, Material icon, List<Integer> prices, Effect effect, Trap trap) {
        this.name = name;
        this.maxLevel = maxLevel;
        this.icon = icon;
        this.prices = List.copyOf(prices);
        this.effect = effect;
        this.trap = trap;
    }

    /**
     * An upgrade whose {@code effect} is applied to every team member when bought. Use
     * {@link TeamView#getUpgradeLevel} to read the level later.
     *
     * @param prices starting price of each level, in diamonds unless the server owner changed the default
     *               currency
     */
    public static UpgradeDefinition upgrade(String name, int maxLevel, Material icon, List<Integer> prices, Effect effect) {
        return new UpgradeDefinition(name, maxLevel, icon, prices, effect, null);
    }

    /**
     * A trap. Buying it does nothing yet; it fires when an enemy enters the base.
     *
     * @param prices starting price of each level, as in {@link #upgrade}
     */
    public static UpgradeDefinition trap(String name, int maxLevel, Material icon, List<Integer> prices, Trap trap) {
        return new UpgradeDefinition(name, maxLevel, icon, prices, null, trap);
    }

    /** Applies the effect again when a team member respawns, for effects that don't survive death. */
    public UpgradeDefinition reapplyOnRespawn() {
        this.reapplyOnRespawn = true;
        return this;
    }

    public String getName() {
        return name;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public Material getIcon() {
        return icon;
    }

    public List<Integer> getPrices() {
        return prices;
    }

    /** {@code null} for traps. */
    public Effect getEffect() {
        return effect;
    }

    /** {@code null} for plain upgrades. */
    public Trap getTrap() {
        return trap;
    }

    public boolean isReapplyOnRespawn() {
        return reapplyOnRespawn;
    }
}
