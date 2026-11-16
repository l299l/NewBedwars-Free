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
import com.l299l.newbedwars.api.shop.ItemTexts;
import com.l299l.newbedwars.api.shop.ShopEditor;
import com.l299l.newbedwars.api.shop.ShopItem;
import com.l299l.newbedwars.api.shop.ShopOptions;
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
import java.util.Map;
import java.util.Optional;

/**
 * Entry point for addons. Get it with {@link #get()} (or from Bukkit's {@code ServicesManager}) after
 * NewBedwars has enabled; add {@code NewBedwars} to {@code depend}/{@code softdepend} in your plugin.yml.
 * <p>
 * The {@code com.l299l.newbedwars.api} package and its subpackages are the stable contract. The plugin's
 * other classes can change in any update.
 * <p>
 * Everything an addon registers is removed again when that addon is disabled; registering a name that
 * already exists throws {@link IllegalArgumentException}.
 */
public interface NewBedwarsAPI {
    /**
     * Returns the API instance.
     *
     * @throws IllegalStateException when NewBedwars is not enabled
     */
    static NewBedwarsAPI get() {
        NewBedwarsAPI api = Bukkit.getServicesManager().load(NewBedwarsAPI.class);
        if (api == null) throw new IllegalStateException("NewBedwars is not enabled");
        return api;
    }

    /** For example {@code 2.7-beta}. */
    String getVersion();

    Collection<ArenaView> getArenas();

    Optional<ArenaView> getArena(String name);

    Optional<ArenaView> getArena(World world);

    /** The arena the player plays or spectates in. */
    Optional<ArenaView> getArena(Player player);

    /** All arena groups (maps that are voted between), read-only. */
    Collection<ArenaGroupView> getGroups();

    /** Case-insensitive. */
    Optional<ArenaGroupView> getGroup(String name);

    /** How this server takes part in a Velocity network. */
    NetworkMode getNetworkMode();

    /**
     * Arenas of all game servers, as last reported by the proxy. Filled on {@code LOBBY} and {@code GAME}
     * servers, empty on {@link NetworkMode#NONE}.
     */
    Collection<NetworkArenaView> getNetworkArenas();

    /** This server's name in the proxy, empty on {@link NetworkMode#NONE}. */
    Optional<String> getServerId();

    /**
     * LOBBY servers: asks the proxy to send {@code player} (and their party, if they are its admin) to a game.
     * {@code target} is an arena name, a group (or name pattern / team size), or null for any arena; the same
     * checks as {@code /bw join} apply and the player gets the same messages.
     *
     * @return false when this is not a lobby server, a check failed, a {@link NetworkJoinRequestEvent} listener
     *         cancelled it, or the proxy can't be reached
     */
    boolean requestNetworkJoin(Player player, String target);

    /** LOBBY servers: sends {@code player} back to the game they left, like {@code /bw rejoin}. */
    boolean requestNetworkRejoin(Player player);

    /** LOBBY servers: the network arena {@code player} can still rejoin, if any. */
    Optional<NetworkArenaView> getNetworkRejoin(Player player);

    /**
     * Stats of a player this server has loaded, i.e. who is online or joined since the server started. Other
     * players get all zeros; use {@link #loadStats(String)} for them. With MySQL storage the values are
     * network-wide.
     */
    PlayerStatsView getStats(String playerName);

    /**
     * The best players by one statistic, best first, ties sorted by name. Offline players are included.
     * <p>
     * Can be called from any thread. With MySQL storage the future completes on a database thread, so switch to
     * the server thread before using Bukkit.
     *
     * @param limit clamped to 1..100
     * @since 2.7-beta
     */
    CompletableFuture<List<LeaderboardEntry>> getLeaderboard(StatType stat, int limit);

    /**
     * Stats of any player who ever played, online or not. Empty when the player has none. Threading as in
     * {@link #getLeaderboard(StatType, int)}.
     *
     * @since 2.7-beta
     */
    CompletableFuture<Optional<PlayerStatsView>> loadStats(String playerName);

    /**
     * Names of players with stats that start with {@code prefix}, case-insensitive and sorted. Threading as in
     * {@link #getLeaderboard(StatType, int)}.
     *
     * @param limit clamped to 1..100
     * @since 2.7-beta
     */
    CompletableFuture<List<String>> findPlayers(String prefix, int limit);

    /**
     * Changes the setup of an arena.
     *
     * @throws IllegalStateException    when the arena is not {@link ArenaState#WAITING}
     * @throws IllegalArgumentException when the arena no longer exists
     */
    ArenaEditor edit(ArenaView arena);

    /** Enables, disables, backs up, restores, exports, imports, copies, resets and deletes arenas. */
    ArenaMaintenance getMaintenance();

    /**
     * Start, stop, skip a phase or broadcast in a game, like {@code /bw game}.
     *
     * @since 2.7-beta
     */
    GameControl getGameControl();

    /**
     * Reads and changes shop items, their texts and shop GUIs, like the web panel's shop editor.
     *
     * @since 2.7-beta
     */
    ShopEditor getShop();

    /**
     * The arena {@code player} is setting up in setup mode.
     *
     * @since 2.7-beta
     */
    Optional<ArenaView> getSetupArena(Player player);

    /**
     * Everyone in setup mode right now, with the arena they are setting up.
     *
     * @since 2.7-beta
     */
    Map<Player, ArenaView> getSetupPlayers();

    /**
     * Adds an action that admins can use in the {@code GamePhases} of config.yml, as {@code NAME} or
     * {@code NAME:argument}. The handler runs on the server thread when the phase's time is over.
     */
    void registerPhaseAction(Plugin owner, String name, PhaseActionHandler handler);

    /** Adds a {@code /bw} subcommand in one context. */
    default void registerCommand(Plugin owner, AddonCommand command, CommandContext context) {
        registerCommand(owner, command, context, new CommandContext[0]);
    }

    /**
     * Adds one {@code /bw} subcommand in several contexts, e.g. {@code LOBBY, GAME, CONSOLE}. It is the same
     * command object everywhere; {@code arena} in {@link AddonCommand#perform} is {@code null} outside arenas.
     *
     * @throws IllegalArgumentException when a subcommand with the same name already exists in one of these
     *                                  places. {@code LOBBY} and {@code LOBBY_ADMIN} count as one place, so do
     *                                  {@code GAME} and {@code GAME_ADMIN}, and all {@code SETUP_*} contexts.
     * @since 2.7-beta
     */
    void registerCommand(Plugin owner, AddonCommand command, CommandContext context, CommandContext... more);

    /** Adds shop item logic. Admins use {@code key} as {@code itemLogic} in items.yml. */
    void registerShopLogic(Plugin owner, String key, ShopLogic logic);

    /** Adds an armor tier that shop items with {@code onBuyEvent: GIVE_ARMOR} can give. */
    void registerArmorTier(Plugin owner, ArmorTierDefinition tier);

    /**
     * Adds a team upgrade or trap. Its shop icon item, named like the upgrade, is added to items.yml when it
     * doesn't exist yet.
     */
    void registerUpgrade(Plugin owner, UpgradeDefinition upgrade);

    /**
     * Adds a shop item to items.yml unless an item with that name already exists. An existing item is never
     * changed, since the server owner may have edited it, and the item stays when the addon is disabled.
     * <p>
     * Register the {@link ShopLogic} it uses first. To be sold, the item still has to be put into a shop GUI.
     * Call it on the server thread, e.g. in {@code onEnable}.
     *
     * @param texts display texts per language code (see {@link ShopOptions#languages()}), or {@code null} to use
     *              the item name
     * @return {@code true} when the item was added, {@code false} when an item with that name already exists
     * @throws IllegalArgumentException when a value of {@code item} or {@code texts} is not valid (same rules as
     *                                  {@link ShopEditor#saveItem})
     * @throws IllegalStateException    when not called on the server thread
     * @since 2.7-beta
     */
    boolean registerShopItem(Plugin owner, ShopItem item, Map<String, ItemTexts> texts);

    /** {@link #registerShopItem(Plugin, ShopItem, Map)} without texts. */
    default boolean registerShopItem(Plugin owner, ShopItem item) {
        return registerShopItem(owner, item, null);
    }

    /** Removes everything {@code owner} registered. Happens automatically when the plugin is disabled. */
    void unregisterAll(Plugin owner);
}
