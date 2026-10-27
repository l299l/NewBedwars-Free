package com.l299l.newbedwars.api;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * A team upgrade or trap added by an addon. After {@link NewBedwarsAPI#registerUpgrade} it can be used in the
 * upgrade shop GUI files as {@code upg:<name>}, and its prices can be set in config.yml under
 * {@code UpgradePrices.<NAME>}.
 */
public final class UpgradeDefinition {
    /** Applies an upgrade to a player. */
    @FunctionalInterface
    public interface Effect {
        void apply(Player player, TeamView team, int level);
    }

    /** Runs when an enemy enters a base protected by the trap. */
    @FunctionalInterface
    public interface Trap {
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
     * @param prices price per level (in the configured upgrade currency), used when config.yml has none
     */
    public static UpgradeDefinition upgrade(String name, int maxLevel, Material icon, List<Integer> prices, Effect effect) {
        return new UpgradeDefinition(name, maxLevel, icon, prices, effect, null);
    }

    /** A trap: it does nothing when bought and is used up when an enemy enters the base. */
    public static UpgradeDefinition trap(String name, int maxLevel, Material icon, List<Integer> prices, Trap trap) {
        return new UpgradeDefinition(name, maxLevel, icon, prices, null, trap);
    }

    /** Also apply the effect again when a team member respawns (for effects that do not survive death). */
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

    /** Shop icon material. */
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
