package com.l299l.newbedwars.arena;

import com.google.gson.JsonObject;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.arena.shops.TeamShop;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.gui.GUISave;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public interface IArena {
    void start();
    void setArenaStarting();
    void stop();
    void forceShutdown();
    boolean rollback();
    /** Problems found while loading the arena (empty when it is fine). */
    List<String> getLoadProblems();
    /** Free-form data owned by addons, saved and loaded unchanged. */
    JsonObject getExtra();
    void setExtra(JsonObject extra);
    /** Top-level fields of the arena file this version does not know; written back unchanged. */
    JsonObject getUnknownFields();
    void setUnknownFields(JsonObject unknownFields);
    /** IDs from the arena file that did not resolve; saved back unchanged so a missing config does not erase them. */
    void setUnresolvedIds(String gamePhasesId, String generatorsLevelingId);
    Integer getGameTime();
    void upgradeGenerators(GeneratorType type, Integer level);
    boolean join(Player player);
    boolean joinParty(List<Player> partyMembers);
    boolean rejoin(Player player);
    void leave(Player player, com.l299l.newbedwars.api.events.LeaveReason reason);

    default void leave(Player player) {
        leave(player, com.l299l.newbedwars.api.events.LeaveReason.OTHER);
    }
    Boolean isPlayerInArena(Player player);
    Set<UUID> getPlayers();
    int getActivePlayerCount();
    GamePlayer getPlayer(UUID player);
    void killPlayer(Player player);
    void cancelStart();

    /**
     * Gives up the player's place in this game when they are kept only for a rejoin (not in this arena's world).
     * @return true when a place was removed
     */
    boolean forfeitRejoin(Player player);
    void broadcast(String message);
    void broadcast(String message, HashMap<String, String> replace);
    void broadcastTitle(String title, String subtitle, Integer fadeIn, Integer stay, Integer fadeOut);
    World getArenaWorld();
    void enable();
    void disable();
    void setGamePhases(GamePhases gamePhases);
    String getNextGamePhase();
    String getCurrentGamePhase();
    void nextPhase();
    void advancePhase();
    int getPhaseTime();
    void setPhaseTime(int time);
    Boolean isEnabled();
    Boolean canBeEnabled();
    GameStatus status();
    Integer getMinPlayers();
    Gamerules getGamerules();
    GeneratorLeveling getGeneratorsLeveling();
    void setGeneratorsLeveling(GeneratorLeveling generatorLeveling);
    void setGamerules(Gamerules gamerules);
    /** Unmodifiable view; use {@link #createTeam} and {@link #removeTeam} to change the teams. */
    java.util.Map<String, Team> getTeams();
    Integer getMaxInTeam();
    /** Unmodifiable view; use {@link #addGenerator} and {@link #removeGenerator} to change the generators. */
    java.util.List<Generator> getGenerators();
    void addGenerator(Generator generator);
    /** @return the removed generator, or {@code null} for an invalid index */
    Generator removeGenerator(int index);
    /** Removes a team (and its setup armor stands are left to the caller). @return false when there is no such team */
    boolean removeTeam(String name);
    ArrayList<Player> getSpectators();
    void addSpectator(Player player, boolean canExit);
    boolean joinAsSpectator(Player player);
    void createTeam(String name, ChatColor color);
    Team getTeam(String name);
    Team getTeam(Player player);

    /** The player's name in their team color (plain when they have no team). */
    default String coloredName(Player player) {
        Team team = getTeam(player);
        return team == null ? player.getName() : team.colored(player.getName());
    }
    List<TeamShop> getTeamShops();
    List<TeamUpgrades> getTeamUpgrades();
    Team getTeamByBed(Location bed);
    void changeSetup(Setup setup);
    Setup getSetupMode();
    ArrayList<UUID> getPlayersOnSetup();
    String getArenaName();
    void setMaxInTeam(Integer max);
    void setMinPlayers(Integer min);
    void setWaitingPos1(Location waitingPos1);
    void setWaitingPos2(Location waitingPos2);
    void setWaitingSpawn(Location waitingSpawn);
    void setWaitingTime(Integer time);
    void setRespawnTime(Integer time);
    Location getWaitingSpawn();
    Integer getWaitingTime();
    Location getWaitingPos1();
    Location getWaitingPos2();
    void setQuickVoidY(int y);
    Integer getQuickVoidY();
    void bedDestruction();
    void endGame(String criteria);
    void enderDragonEnd();
    void enderDragonStart();
    void disableRegeneration();
    void disableTeamsForge();
    void addPlacedBlock(Location loc);
    boolean isPlacedBlock(Location loc);
    void clearPlacedBlocks();
    void addBlastProtBlock(Location loc);
    boolean isBlastProtBlock(Location loc);
    void removeBlastProtBlock(Location loc);
    String toJson();
    String getResourcePackUrl();
    void setResourcePackUrl(String url);
    String getResourcePackHash();
    void setResourcePackHash(String hash);
    String getShopGuiId();
    void setShopGuiId(String id);
    String getUpgradeGuiId();
    void setUpgradeGuiId(String id);
}
