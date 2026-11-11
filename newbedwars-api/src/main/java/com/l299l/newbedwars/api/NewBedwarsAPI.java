package com.l299l.newbedwars.api;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.NetworkArenaView;
import com.l299l.newbedwars.api.view.NetworkMode;
import com.l299l.newbedwars.api.view.PlayerStatsView;
import com.l299l.newbedwars.api.view.StatType;
import com.l299l.newbedwars.api.view.LeaderboardEntry;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import com.l299l.newbedwars.api.events.NetworkJoinRequestEvent;
import com.l299l.newbedwars.api.view.ArenaGroupView;
import com.l299l.newbedwars.api.view.ArenaState;
import com.l299l.newbedwars.api.edit.ArenaEditor;
import com.l299l.newbedwars.api.maintenance.ArenaMaintenance;
import com.l299l.newbedwars.api.game.GameControl;
import com.l299l.newbedwars.api.shop.ShopEditor;
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

    /** How this server takes part in a proxy network ({@code Network.Mode}). */
    NetworkMode getNetworkMode();

    /** Arenas of all game servers, as last reported by the proxy (lobby and game servers). Empty in {@link NetworkMode#NONE}. */
    Collection<NetworkArenaView> getNetworkArenas();

    /** This server's name in the proxy ({@code Network.ServerId}); empty in {@link NetworkMode#NONE}. */
    Optional<String> getServerId();

    /**
     * LOBBY servers: asks the proxy to send {@code player} (and their party, if they are its admin) to a game.
     * {@code target} is an arena name, a group (or name pattern / team size), or null for any arena; the same
     * checks as {@code /bw join} apply and the player gets the same messages.
     *
     * @return false when this is not a lobby server, a check failed, a {@link NetworkJoinRequestEvent} listener
     *         cancelled it, or the proxy can not be reached
     */
    boolean requestNetworkJoin(Player player, String target);

    /** LOBBY servers: sends {@code player} back to the game they left, like {@code /bw rejoin}. */
    boolean requestNetworkRejoin(Player player);

    /** LOBBY servers: the network arena {@code player} can still rejoin, if any. */
    Optional<NetworkArenaView> getNetworkRejoin(Player player);

    /**
     * A player's stats (all zeros when there are none). With {@code Storage.Type: MYSQL} these are the stats of
     * the whole network; only players who are online (or were loaded since the start) are known then.
     */
    PlayerStatsView getStats(String playerName);

    /**
     * The best players by one statistic, best first (ties by name). Works with JSON and MySQL storage; with MySQL
     * the database is asked, so offline players are included. Can be called from any thread. Available from 2.7-beta.
     *
     * @param limit 1 to 100
     */
    CompletableFuture<List<LeaderboardEntry>> getLeaderboard(StatType stat, int limit);

    /**
     * Statistics of any player who ever played, also offline (with MySQL they are read from the database).
     * Empty when the player has no statistics. Can be called from any thread. Available from 2.7-beta.
     */
    CompletableFuture<Optional<PlayerStatsView>> loadStats(String playerName);

    /** Names of players with statistics that start with {@code prefix} (case-insensitive), sorted. Available from 2.7-beta. */
    CompletableFuture<List<String>> findPlayers(String prefix, int limit);

    /**
     * Changes the setup of an arena.
     * @throws IllegalStateException when the arena is not {@link ArenaState#WAITING}
     */
    ArenaEditor edit(ArenaView arena);

    /** Arena actions of {@code /bw arena}: enable, disable, backups, export/import, copy, map reset, delete. */
    ArenaMaintenance getMaintenance();

    /** Start, stop, next phase and broadcast for running games, like {@code /bw game}. Available from 2.7-beta. */
    GameControl getGameControl();

    /** Shop items, their texts and shop GUIs: read and change them like the web panel's shop editor. Available from 2.7-beta. */
    ShopEditor getShop();

    /** Adds an action usable in {@code GamePhases} in config.yml as {@code NAME} or {@code NAME:argument}. */
    void registerPhaseAction(Plugin owner, String name, PhaseActionHandler handler);

    /** Adds a {@code /bw} subcommand in one context. */
    default void registerCommand(Plugin owner, AddonCommand command, CommandContext context) {
        registerCommand(owner, command, context, new CommandContext[0]);
    }

    /**
     * Adds one {@code /bw} subcommand in several contexts, e.g. {@code LOBBY, GAME, CONSOLE}. It is the same
     * command object everywhere; {@code arena} in {@link AddonCommand#perform} is {@code null} outside arenas.
     * Available from 2.7-beta.
     *
     * @throws IllegalArgumentException when another subcommand with the same name is available in one of these
     *                                  contexts (lobby and lobby admin count as one place, so do game and game admin,
     *                                  and all setup modes)
     */
    void registerCommand(Plugin owner, AddonCommand command, CommandContext context, CommandContext... more);

    /** Adds shop item logic; use {@code key} as {@code itemLogic} in {@code items.yml}. */
    void registerShopLogic(Plugin owner, String key, ShopLogic logic);

    void registerArmorTier(Plugin owner, ArmorTierDefinition tier);

    /** Adds a team upgrade or trap, and its shop icon item (named like the upgrade) when it does not exist. */
    void registerUpgrade(Plugin owner, UpgradeDefinition upgrade);

    /** Removes everything {@code owner} registered. Done automatically when the plugin is disabled. */
    void unregisterAll(Plugin owner);
}
