package com.l299l.newbedwars.arena;

import com.l299l.newbedwars.api.events.LeaveReason;
import com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.player.LobbyService;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.parties.Party;
import com.l299l.newbedwars.arena.team.TeamAssigner;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.player.PlayerStateResetter;
import com.l299l.newbedwars.arena.player.inventory.ArmorContents;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.arena.shops.TeamShop;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.IronGolemLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.SilverfishLogic;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.arena.group.MapVoteManager;
import com.l299l.newbedwars.arena.setup.DecoUtils;
import com.l299l.newbedwars.scoreboard.NScoreboard;
import com.l299l.newbedwars.scoreboard.ScoreboardManager;
import com.l299l.newbedwars.world.template.ArenaMaps;
import com.l299l.newbedwars.world.template.ArenaResetPipeline;
import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Arena implements IArena {
    /** @deprecated the same map as {@link ArenaRegistry}; use its methods */
    @Deprecated
    public static final HashMap<String, IArena> arenaByName = ArenaRegistry.BY_NAME;
    /** @deprecated the same map as {@link ArenaRegistry}; use its methods */
    @Deprecated
    public static final HashMap<World, IArena> arenaByWorld = ArenaRegistry.BY_WORLD;

    public static void updateTablist(Player player) {
        if (!Properties.TablistIsolationEnabled) return;
        if (!NewBedwars.plugin.isEnabled()) return;
        IArena playerArena = ArenaRegistry.byWorld(player.getWorld());
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            IArena otherArena = ArenaRegistry.byWorld(other.getWorld());
            if (playerArena == null && otherArena == null) {
                player.showPlayer(NewBedwars.plugin, other);
                other.showPlayer(NewBedwars.plugin, player);
            } else if (playerArena != null && playerArena == otherArena) {
                boolean playerSpectator = playerArena.getSpectators().contains(player);
                boolean otherSpectator = playerArena.getSpectators().contains(other);
                if (!otherSpectator) player.showPlayer(NewBedwars.plugin, other);
                if (!playerSpectator) other.showPlayer(NewBedwars.plugin, player);
            } else {
                player.hidePlayer(NewBedwars.plugin, other);
                other.hidePlayer(NewBedwars.plugin, player);
            }
        }
    }
    private final HashMap<String, Team> teams = new HashMap<>();
    private Gamerules gamerules;
    private boolean enabled = false;
    private Setup setup = Setup.NO_SETUP;
    private GameStatus gameStatus;
    private final String arenaName;
    private final ArrayList<UUID> onSetupList = new ArrayList<>();
    private final ArrayList<Generator> generators = new ArrayList<>();
    private GeneratorLeveling generatorsLeveling;
    private int minPlayers;
    private int maxInTeam;
    private final RejoinManager rejoin = new RejoinManager();
    private int waitingTime;
    private GamePhases gamePhases;
    private int nextPhaseTime = 0;
    private int respawnTime;
    private World world;
    private final ArrayList<Player> spectatorsList = new ArrayList<>();
    private final HashMap<UUID, GamePlayer> players = new HashMap<>();
    private final HashMap<UUID, NScoreboard> scoreboards = new HashMap<>();
    private final String worldName;
    private Location waitingPos1;
    private Location waitingPos2;
    private Location waitingSpawn;
    private BossBar arenaBossBar;
    private CountdownTimer countdownTimer;
    private BukkitTask endingTimer;
    private final ScoreboardManager scoreboardManager;
    private final DragonController dragons = new DragonController(this);
    private final ArenaResetCoordinator reset = new ArenaResetCoordinator(this);
    private final List<String> loadProblems = new ArrayList<>();
    private JsonObject extra = new JsonObject();
    private JsonObject unknownFields = new JsonObject();
    private String unresolvedGamePhasesId;
    private String unresolvedGeneratorsLevelingId;
    private final BlockTracker blocks = new BlockTracker();
    private final HashSet<UUID> playersWithResourcePack = new HashSet<>();
    private String resourcePackUrl;
    private String resourcePackHash;
    private BukkitTask tntParticleTask;
    private Integer quickVoidY;
    private long gameStartMillis = 0;
    private String shopGuiId;
    private String upgradeGuiId;

    public Arena(String arenaName, World world) {
        this.arenaName = arenaName;
        this.world = world;
        this.gameStatus = GameStatus.restarting;
        worldName = world.getName();
        minPlayers = Properties.DefaultMinPlayers;
        maxInTeam = Properties.DefaultMaxInTeam;
        waitingTime = Properties.DefaultWaitingTime;
        generatorsLeveling = NewBedwars.plugin.getGeneratorLeveling().get(Properties.DefaultGeneratorConfig);
        scoreboardManager = NewBedwars.plugin.getScoreboardManager();
        GamePhases defaultPhases = Properties.GamePhases.get(Properties.DefaultGamePhase);
        gamePhases = defaultPhases != null ? new GamePhases(defaultPhases) : null;
        respawnTime = Properties.DefaultRespawnTime;
        gamerules = new Gamerules();
        world.setAutoSave(false);
        shopGuiId = Properties.DefaultTeamShopGui;
        upgradeGuiId = Properties.DefaultUpgradeShopGui;
        ArenaRegistry.register(arenaName, this);
        ArenaRegistry.bindWorld(world, this);
    }

    @Override
    public void setArenaStarting() {
        starting();
    }

    @Override
    public void start() {
        if (countdownTimer != null && gameStatus == GameStatus.starting) {
            countdownTimer.cancel();
        }
        DecoUtils.removeAllArmorStands(this);
        gameStatus = GameStatus.playing;
        gameStartMillis = System.currentTimeMillis();
        if (ArenaMaps.isEnabled() && ArenaMaps.hasTemplate(arenaName)) {
            ArenaMaps.markDirty(arenaName);
        }
        broadcast("GameStarted");
        clearWaitingArea();
        assignTeams();
        for (Team team : teams.values()) {
            team.start();
        }
        for (Generator generator : generators) {
            generator.start();
        }
        updateBossBars();
        updateScoreboards();
        if (gamePhases != null) gamePhases.start(this);
        startTntParticleTask();
        for (Team team : new ArrayList<>(teams.values())) {
            if (team.getPlayers().isEmpty()) {
                team.stop();
            }
        }
        ApiEvents.gameStart(this);
        checkWin();
    }

    private void clearWaitingArea() {
        if (waitingPos1 == null || waitingPos2 == null) return;
        int minX = Math.min(waitingPos1.getBlockX(), waitingPos2.getBlockX());
        int minY = Math.min(waitingPos1.getBlockY(), waitingPos2.getBlockY());
        int minZ = Math.min(waitingPos1.getBlockZ(), waitingPos2.getBlockZ());
        int maxX = Math.max(waitingPos1.getBlockX(), waitingPos2.getBlockX());
        int maxY = Math.max(waitingPos1.getBlockY(), waitingPos2.getBlockY());
        int maxZ = Math.max(waitingPos1.getBlockZ(), waitingPos2.getBlockZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() != Material.AIR) {
                        b.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
        shutdown(false);
    }

    @Override
    public void forceShutdown() {
        shutdown(true);
    }

    /**
     * Ends the game and empties the arena. A normal stop resets the map; {@code force} (plugin disable or
     * reload) kicks everyone and leaves the map to the dirty marker.
     */
    private void shutdown(boolean force) {
        gameStatus = GameStatus.restarting;
        if (!force) MapVoteManager.resetArena(this);

        if (tntParticleTask != null) {
            tntParticleTask.cancel();
            tntParticleTask = null;
        }
        if (endingTimer != null) {
            endingTimer.cancel();
            endingTimer = null;
        }
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
        if (gamePhases != null) {
            gamePhases.stop();
        }
        rejoin.cancelAll();

        LeaveReason reason = force ? LeaveReason.KICKED : LeaveReason.OTHER;
        for (UUID playerId : new ArrayList<>(players.keySet())) {
            GamePlayer gp = players.get(playerId);
            if (gp != null) gp.cancelRespawnTask();
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                leave(player, reason);
            }
        }
        players.clear();
        for (Player spectator : new ArrayList<>(spectatorsList)) {
            leave(spectator, reason);
        }
        spectatorsList.clear();
        nextPhaseTime = 0;

        for (Generator generator : generators) {
            generator.stop();
        }
        for (Team team : teams.values()) {
            team.stop();
        }
        discardBossBar();
        for (NScoreboard scoreboard : scoreboards.values()) {
            scoreboard.kill();
        }
        scoreboards.clear();
        playersWithResourcePack.clear();

        IronGolemLogic.cleanupAllForArena(arenaName);
        SilverfishLogic.cleanupAllForArena(this);

        if (force) {
            dragons.removeAll();
            return;
        }
        boolean correctRollback = rollback();
        if (!correctRollback) {
            Bukkit.getServer().getLogger().severe("Could not rollback arena " + arenaName);
        }
        if (!ArenaMaps.isEnabled()) {
            gameStatus = GameStatus.waiting;
        }
    }

    /**
     * Resets the arena world. With the default (template) system this only starts an asynchronous
     * pipeline and returns {@code true} when it started; the arena stays {@code restarting} until the
     * pipeline calls {@link #finishReset}. With {@code Rollback.UseLegacySystem} the world is reloaded
     * synchronously from the server folder.
     */
    @Override
    public boolean rollback() {
        return reset.rollback();
    }

    /** Called by {@link ArenaResetPipeline} when the fresh world is loaded. */
    public void finishReset(World newWorld) {
        reset.finishReset(newWorld);
    }

    /**
     * Admin command: puts the map back to its saved state right now. Only possible while nobody is in
     * the arena. Returns false when the arena is busy.
     */
    public boolean resetMap() {
        return reset.resetMap();
    }

    /**
     * Called by {@link ArenaResetPipeline} when the arena can not be reset. The arena stays
     * {@code restarting} (nobody can join) and the reset is retried automatically a few times; only when
     * that keeps failing, or there is no template to restore, the arena is disabled.
     */
    public void failReset(String reason) {
        reset.failReset(reason);
    }

    void setStatus(GameStatus status) {
        gameStatus = status;
    }

    /** A reset that keeps failing takes the arena out of use until an admin looks at it. */
    void disableBroken(String reason) {
        enabled = false;
        setup = Setup.NO_SETUP;
        loadProblems.add(reason);
    }

    public void unbindWorld() {
        ArenaRegistry.unbindWorld(world);
    }

    public void rebindWorld(World newWorld) {
        world = newWorld;
        ArenaRegistry.bindWorld(newWorld, this);
        if (waitingSpawn != null) waitingSpawn.setWorld(newWorld);
        if (waitingPos1 != null) waitingPos1.setWorld(newWorld);
        if (waitingPos2 != null) waitingPos2.setWorld(newWorld);
        for (Team team : teams.values()) {
            team.updateWorldReference(newWorld);
        }
        for (Generator generator : generators) {
            generator.getLocation().setWorld(newWorld);
        }
    }

    @Override
    public List<String> getLoadProblems() {
        return loadProblems;
    }

    @Override
    public JsonObject getExtra() { return extra; }

    @Override
    public void setExtra(JsonObject extra) { this.extra = extra != null ? extra : new JsonObject(); }

    @Override
    public void setUnresolvedIds(String gamePhasesId, String generatorsLevelingId) {
        this.unresolvedGamePhasesId = gamePhasesId;
        this.unresolvedGeneratorsLevelingId = generatorsLevelingId;
    }

    @Override
    public JsonObject getUnknownFields() { return unknownFields; }

    @Override
    public void setUnknownFields(JsonObject unknownFields) { this.unknownFields = unknownFields != null ? unknownFields : new JsonObject(); }

    @Override
    public int getGameTime() {
        if (gameStatus != GameStatus.playing || gameStartMillis == 0) return 0;
        return (int) ((System.currentTimeMillis() - gameStartMillis) / 1000);
    }

    @Override
    public void upgradeGenerators(GeneratorType type, Integer level) {
        for (Generator generator : generators) {
            if (generator.getType() == type) {
                generator.upgrade(level);
            }
        }
    }

    private void applyJoinSetup(Player player) {
        player.teleport(waitingSpawn);
        PlayerStateResetter.reset(player, GameMode.ADVENTURE);
        player.setInvulnerable(true);
        player.getInventory().setItem(8, NewBedwars.plugin.getGuiManager().getLeaveItem(player));
        addToBossBar(player);
        NScoreboard scoreboard = scoreboardManager.createPlayerScoreboard("WaitingScoreboard", this, player);
        if (scoreboard != null) {
            scoreboards.put(player.getUniqueId(), scoreboard);
        }
        players.put(player.getUniqueId(), new GamePlayer(player, null));
        updateTablist(player);
        sendResourcePack(player);
    }

    @Override
    public boolean join(Player player) {
        if (players.containsKey(player.getUniqueId())) return false;
        if (playsInOtherArena(player)) return false;
        if (players.size() >= getMaxPlayers()) {
            return false;
        }
        if (!ApiEvents.playerJoin(this, player, false)) return false;
        forfeitOtherRejoins(player);
        applyJoinSetup(player);
        broadcast("PlayerJoinedArena", Map.of(
                "/player/", coloredName(player),
                "/players/", Integer.toString(players.size()),
                "/maxplayers/", Integer.toString(getMaxPlayers())));
        checkIfCanStart();
        return true;
    }

    /** True while the player is actually in another arena's world as one of its players. */
    private boolean playsInOtherArena(Player player) {
        for (IArena other : ArenaRegistry.all()) {
            if (other != this && other.isPlayerInArena(player)
                    && player.getWorld() == other.getArenaWorld()) return true;
        }
        return false;
    }

    /** Joining this arena gives up the player's place in any game they could still rejoin. */
    private void forfeitOtherRejoins(Player player) {
        for (IArena other : new ArrayList<>(ArenaRegistry.all())) {
            if (other != this) other.forfeitRejoin(player);
        }
    }

    @Override
    public boolean forfeitRejoin(Player player) {
        if (!players.containsKey(player.getUniqueId()) || player.getWorld() == world) return false;
        rejoin.cancel(player.getUniqueId());
        GamePlayer gp = players.remove(player.getUniqueId());
        if (gp != null) gp.cancelRespawnTask();
        NewBedwars.plugin.getMessages().send(player, "RejoinForfeited", Map.of("/arena/", arenaName));
        return true;
    }

    @Override
    public boolean joinParty(List<Player> partyMembers) {
        if (partyMembers.isEmpty()) return false;
        int newTotal = players.size() + partyMembers.size();
        if (newTotal > getMaxPlayers()) return false;

        List<Player> joined = new ArrayList<>();
        for (Player member : partyMembers) {
            if (players.containsKey(member.getUniqueId())) continue;
            if (playsInOtherArena(member)) continue;
            if (!ApiEvents.playerJoin(this, member, false)) continue;
            forfeitOtherRejoins(member);
            applyJoinSetup(member);
            joined.add(member);
        }

        int currentSize = players.size();
        int maxSize = getMaxPlayers();
        for (Player member : joined) {
            final String name = member.getName();
            broadcast("PlayerJoinedArena", Map.of(
                    "/player/", name,
                    "/players/", Integer.toString(currentSize),
                    "/maxplayers/", Integer.toString(maxSize)));
        }
        checkIfCanStart();
        return true;
    }

    @Override
    public boolean rejoin(Player player) {
        if (gameStatus != GameStatus.playing) {
            return false;
        }
        if (!players.containsKey(player.getUniqueId())) {
            return false;
        }
        GamePlayer gamePlayer = players.get(player.getUniqueId());
        Team team = gamePlayer.getTeam();
        if (!team.isAlive()) {
            rejoin.cancel(player.getUniqueId());
            players.remove(player.getUniqueId());
            return false;
        }
        if (!ApiEvents.playerJoin(this, player, true)) return false;
        rejoin.cancel(player.getUniqueId());
        team.addPlayer(player);
        addToBossBar(player);
        NScoreboard scoreboard = scoreboardManager.createPlayerScoreboard("InGameScoreboard", this, player);
        if (scoreboards != null && scoreboard != null) {
            scoreboards.put(player.getUniqueId(), scoreboard);
        }
        killPlayer(player);
        updateTablist(player);
        return true;
    }

    @Override
    public void leave(Player player, LeaveReason reason) {
        MapVoteManager.playerLeft(this, player);
        if (players.containsKey(player.getUniqueId()) || spectatorsList.contains(player)) {
            ApiEvents.playerLeave(this, player, reason);
        }
        Location lobby = NewBedwars.plugin.getLobbyLocation();
        LobbyService.sendToLobby(player, PlayerSendToLobbyEvent.Reason.LEFT_ARENA);
        player.setInvulnerable(false);
        PlayerStateResetter.reset(player, GameMode.SURVIVAL);
        PlayerStateResetter.clearEffects(player);
        player.setBedSpawnLocation(lobby, true);
        playersWithResourcePack.remove(player.getUniqueId());
        if (spectatorsList.contains(player)) {
            spectatorsList.remove(player);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showPlayer(NewBedwars.plugin, player);
            }
        }
        updateTablist(player);
        if (arenaBossBar != null) {
            arenaBossBar.removePlayer(player);
        }
        NScoreboard playerScoreboard = scoreboards.remove(player.getUniqueId());
        if (playerScoreboard != null) {
            playerScoreboard.kill();
        }
        GamePlayer leavingPlayer = players.get(player.getUniqueId());
        if (leavingPlayer == null) return;
        leavingPlayer.cancelRespawnTask();
        final String playerName = coloredName(player);
        if (gameStatus == GameStatus.playing) {
            leavingPlayer.getTeam().removePlayer(player);
            if (!checkWin()) {
                if (Properties.RejoinTimeEnabled && leavingPlayer.getTeam().isAlive()) {
                    int displayCount = players.size() - 1;
                    broadcast("PlayerLeftArenaGame", Map.of(
                            "/player/", playerName,
                            "/players/", Integer.toString(displayCount),
                            "/maxplayers/", Integer.toString(getMaxPlayers())));
                    rejoin.schedule(player.getUniqueId(), 20L * 60 * Properties.RejoinTime, () -> {
                        players.remove(player.getUniqueId());
                        NewBedwars.plugin.getMessages().send(player, "RejoinTimeOver");
                    });
                } else {
                    players.remove(player.getUniqueId());
                    broadcast("PlayerLeftArenaGame", Map.of(
                            "/player/", playerName,
                            "/players/", Integer.toString(players.size()),
                            "/maxplayers/", Integer.toString(getMaxPlayers())));
                }
            } else {
                // the game just ended because of this departure
                finishLeave(player, leavingPlayer);
            }
        } else {
            finishLeave(player, leavingPlayer);
        }
    }

    private void finishLeave(Player player, GamePlayer leavingPlayer) {
        final String playerName = leavingPlayer.getTeam() != null
                ? leavingPlayer.getTeam().colored(player.getName()) : player.getName();
        if (leavingPlayer.getTeam() != null) leavingPlayer.getTeam().removePlayer(player);
        players.remove(player.getUniqueId());
        broadcast("PlayerLeftArena", Map.of(
                "/player/", playerName,
                "/players/", Integer.toString(players.size()),
                "/maxplayers/", Integer.toString(getMaxPlayers())));
        if (gameStatus == GameStatus.starting && (players.size() < minPlayers || !hasEnoughTeams())) {
            cancelStart();
        }
    }

    @Override
    public void killPlayer(Player player) {
        if (!isPlayerInArena(player)) {
            throw new IllegalArgumentException("Player is not in arena");
        }
        Team team = getTeam(player);
        if (team == null) {
            throw new IllegalArgumentException("Player is not in team");
        }
        if (team.isBedDestroyed()) {
            addSpectator(player, true);
            team.removePlayer(player);
            players.remove(player.getUniqueId());
            checkWin();
        } else {
            addSpectator(player, false);
            GamePlayer gp = players.get(player.getUniqueId());
            BukkitTask task = new BukkitRunnable() {
                int time = Math.max(1, respawnTime);
                @Override
                public void run() {
                    // Stop when the game ended or the player left (lobby, disconnect) while waiting to respawn.
                    if (gameStatus != GameStatus.playing || !isPlayerInArena(player)
                            || !player.isOnline() || !spectatorsList.contains(player)) {
                        cancel();
                        return;
                    }
                    player.sendTitle(ChatColor.RED + "" + time, ChatColor.YELLOW + "to respawn", 0, 20, 0);
                    time--;
                    if (time == 0) {
                        player.sendTitle(ChatColor.GREEN + "Respawned", "", 0, 20, 0);
                        respawnPlayer(player);
                        cancel();
                    }
                }
            }.runTaskTimer(NewBedwars.plugin, 0, 20);
            if (gp != null) gp.setRespawnTask(task);
        }


    }

    @Override
    public boolean isPlayerInArena(Player player) {
        return players.containsKey(player.getUniqueId());
    }

    @Override
    public Set<UUID> getPlayers() {
        return players.keySet();
    }

    @Override
    public int getActivePlayerCount() {
        // teams are only assigned when the game starts, so the lobby counts everyone who joined
        if (gameStatus == GameStatus.waiting || gameStatus == GameStatus.starting) return players.size();
        int count = 0;
        for (Team team : teams.values()) {
            count += team.getPlayers().size();
        }
        return count;
    }

    @Override
    public GamePlayer getPlayer(UUID player) {
        return players.get(player);
    }

    @Override
    public void cancelStart() {
        if (countdownTimer != null) countdownTimer.cancel();
        gameStatus = GameStatus.waiting;
        broadcast("StartCanceled");
        broadcast("NotEnoughPlayers", Map.of(
                "/arenaname/", arenaName,
                "/minplayers/", Integer.toString(minPlayers),
                "/players/", Integer.toString(players.size())));
    }

    /** Everyone who should see arena messages: the players and the spectators who are not in the player list. */
    private List<Player> audience() {
        Set<Player> audience = new LinkedHashSet<>();
        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getServer().getPlayer(uuid);
            if (player != null) audience.add(player);
        }
        for (Player spectator : spectatorsList) {
            if (spectator != null && spectator.isOnline()) audience.add(spectator);
        }
        return new ArrayList<>(audience);
    }

    @Override
    public void broadcast(String message) {
        for (Player player : audience()) {
            NewBedwars.plugin.getMessages().send(player, message);
        }
    }

    @Override
    public void broadcast(String message, Map<String, String> replace) {
        for (Player player : audience()) {
            String msg = NewBedwars.plugin.getMessages().getMsg(player, message);
            for (String key : replace.keySet()) {
                msg = msg.replace(key, replace.get(key));
            }
            player.sendMessage(msg);
        }
    }

    @Override
    public void broadcastTitle(String title, String subtitle, Integer fadeIn, Integer stay, Integer fadeOut) {
        for (Player player : audience()) {
            player.sendTitle(
                    ChatColor.translateAlternateColorCodes('&', title),
                    ChatColor.translateAlternateColorCodes('&', subtitle),
                    fadeIn, stay, fadeOut
            );
        }
    }

    public World getArenaWorld() {
        return world;
    }

    public void enable() {
        DecoUtils.removeAllArmorStands(this);
        enabled = true;
        setup = Setup.READY;
        gameStatus = GameStatus.waiting;
        discardBossBar();
        arenaBossBar = NewBedwars.plugin.getBossBarManager().createBossBar("WaitingBeforeStart", this);
    }

    public void disable() {
        if (gameStatus != GameStatus.waiting) return;
        List<UUID> playersList = new ArrayList<>(players.keySet());
        for (UUID p : playersList) {
            Player player = Bukkit.getServer().getPlayer(p);
            if (player == null) {
                continue;
            }
            leave(player, LeaveReason.KICKED);
            NewBedwars.plugin.getMessages().send(player, "KickedFromArena");
        }
        discardBossBar();
        enabled = false;
        setup = Setup.NO_SETUP;
        gameStatus = GameStatus.restarting;
        DecoUtils.summonAllArmorStands(this);
    }

    @Override
    public void setGamePhases(GamePhases gamePhases) {
        this.gamePhases = gamePhases != null ? new GamePhases(gamePhases) : null;
    }

    @Override
    public String getNextGamePhase() {
        if (gamePhases == null || gamePhases.getNextPhase() == null) return "";
        return gamePhases.getNextPhase().getName();
    }

    @Override
    public String getCurrentGamePhase() {
        if (gamePhases == null) return "";
        com.l299l.newbedwars.arena.phases.Phase p = gamePhases.getCurrentPhase();
        return p != null ? p.getName() : "";
    }

    @Override
    public void nextPhase() {
        if (gamePhases != null) gamePhases.nextPhase(this);
    }

    @Override
    public void advancePhase() {
        if (gameStatus != GameStatus.playing || gamePhases == null || gamePhases.isLastPhase()) return;
        gamePhases.getCurrentPhase().cancel();
        gamePhases.nextPhase(this);
    }

    @Override
    public int getPhaseTime() {
        return nextPhaseTime;
    }

    @Override
    public void setPhaseTime(int time) {
        nextPhaseTime = time;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean canBeEnabled() {
        boolean canBeEnabled = (waitingPos1 != null && waitingPos2 != null && waitingSpawn != null &&
                waitingPos1.getWorld().equals(world) && waitingPos2.getWorld().equals(world) &&
                waitingSpawn.getWorld().equals(world) && teams.size() > 1 && !generators.isEmpty()
                && gamerules != null && quickVoidY != null);
        if (canBeEnabled) {
            for (Team team : teams.values()) {
                if (team.getTeamSpawn() == null || !team.getTeamSpawn().getWorld().equals(world)) {
                    canBeEnabled = false;
                    break;
                }
                if (team.getTeamBed() == null || !team.getTeamBed().getLocation().getWorld().equals(world)) {
                    canBeEnabled = false;
                    break;
                }
                if (team.getTeamShop() == null || !team.getTeamShop().getLocation().getWorld().equals(world)) {
                    canBeEnabled = false;
                    break;
                }
                if (team.getTeamUpgrades() == null || !team.getTeamUpgrades().getLocation().getWorld().equals(world)) {
                    canBeEnabled = false;
                    break;
                }
                if (team.getGenerator() == null || !team.getGenerator().getLocation().getWorld().equals(world)) {
                    canBeEnabled = false;
                    break;
                }
                if (!team.isBuildProtAreaPosSet()) {
                    canBeEnabled = false;
                    break;
                }
                if (!team.isBasePosSet()) {
                    canBeEnabled = false;
                    break;
                }
            }
        }
        return canBeEnabled;
    }

    @Override
    public GameStatus status() {
        return gameStatus;
    }

    @Override
    public int getMinPlayers() {
        return minPlayers;
    }

    @Override
    public void setMinPlayers(int min) {
        minPlayers = min;
    }

    @Override
    public int getMaxInTeam() {
        return maxInTeam;
    }

    @Override
    public void setMaxInTeam(int max) {
        maxInTeam = max;
    }

    @Override
    public Gamerules getGamerules() {
        return gamerules;
    }

    @Override
    public void setGamerules(Gamerules gamerules) {
        this.gamerules = gamerules;
    }

    public java.util.Map<String, Team> getTeams() {
        return java.util.Collections.unmodifiableMap(teams);
    }

    @Override
    public boolean removeTeam(String name) {
        return teams.remove(name) != null;
    }

    @Override
    public List<TeamShop> getTeamShops() {
        ArrayList<TeamShop> teamShops = new ArrayList<>();
        for (Team team : teams.values()) {
            if (team.getTeamShop() != null) {
                teamShops.add(team.getTeamShop());
            }
        }
        return teamShops;
    }

    @Override
    public List<TeamUpgrades> getTeamUpgrades() {
        ArrayList<TeamUpgrades> teamUpgrades = new ArrayList<>();
        for (Team team : teams.values()) {
            if (team.getTeamUpgrades() != null) {
                teamUpgrades.add(team.getTeamUpgrades());
            }
        }
        return teamUpgrades;
    }

    public java.util.List<Generator> getGenerators() {
        return java.util.Collections.unmodifiableList(generators);
    }

    @Override
    public Generator removeGenerator(int index) {
        if (index < 0 || index >= generators.size()) return null;
        return generators.remove(index);
    }

    public GeneratorLeveling getGeneratorsLeveling() {
        return generatorsLeveling;
    }

    @Override
    public void setGeneratorsLeveling(GeneratorLeveling generatorLeveling) {
        generatorsLeveling = generatorLeveling;
    }

    @Override
    public void addGenerator(Generator generator) {
        generators.add(generator);
    }

    @Override
    public ArrayList<Player> getSpectators() {
        return spectatorsList;
    }

    @Override
    public void addSpectator(Player player, boolean canExit) {
        addSpectatorInternal(player, canExit);
    }

    @Override
    public boolean joinAsSpectator(Player player) {
        if (gameStatus != GameStatus.playing && gameStatus != GameStatus.ending) return false;
        if (gamerules == null || !gamerules.AllowSpectators) return false;
        if (spectatorsList.contains(player)) return false;
        if (players.containsKey(player.getUniqueId())) return false;
        addSpectatorInternal(player, true);
        updateTablist(player);
        addToBossBar(player);
        NScoreboard scoreboard = scoreboardManager.createPlayerScoreboard("InGameScoreboard", this, player);
        if (scoreboard != null) {
            scoreboards.put(player.getUniqueId(), scoreboard);
        }
        MapVoteManager.spectatorJoined(this, player);
        return true;
    }

    public void createTeam(String name, ChatColor color) {
        Team team = new Team(color, name, this);
        teams.put(name, team);
    }

    public Team getTeam(String name) {
        return teams.get(name);
    }

    public Team getTeam(Player player) {
        for (Team team : teams.values()) {
            if (team.getPlayers().contains(player)) {
                return team;
            }
        }
        return null;
    }

    public Team getTeamByBed(Location bed) {
        for (Team team : teams.values()) {
            if (team.getTeamBed() == null) continue;
            Location bedLocation = team.getTeamBed().getLocation();
            if (bedLocation.getBlockX() - bed.getBlockX() <= 2 && bedLocation.getBlockX() - bed.getBlockX() >= -2 &&
                    bedLocation.getBlockY() - bed.getBlockY() <= 2 && bedLocation.getBlockY() - bed.getBlockY() >= -2 &&
                    bedLocation.getBlockZ() - bed.getBlockZ() <= 2 && bedLocation.getBlockZ() - bed.getBlockZ() >= -2) {
                return team;
            }
        }
        return null;
    }

    public void changeSetup(Setup setup) {
        this.setup = setup;
    }


    public Setup getSetupMode() {
        return setup;
    }


    public ArrayList<UUID> getPlayersOnSetup() {
        return onSetupList;
    }


    public String getArenaName() {
        return arenaName;
    }

    @Override
    public String getResourcePackUrl() { return resourcePackUrl; }

    @Override
    public void setResourcePackUrl(String url) {
        if (!java.util.Objects.equals(this.resourcePackUrl, url)) {
            this.resourcePackUrl = url;
            playersWithResourcePack.clear();
            resendResourcePackToAll();
        } else {
            this.resourcePackUrl = url;
        }
    }

    @Override
    public String getResourcePackHash() { return resourcePackHash; }

    @Override
    public void setResourcePackHash(String hash) {
        if (!java.util.Objects.equals(this.resourcePackHash, hash)) {
            this.resourcePackHash = hash;
            playersWithResourcePack.clear();
            resendResourcePackToAll();
        } else {
            this.resourcePackHash = hash;
        }
    }

    @Override
    public String getShopGuiId() { return shopGuiId; }
    @Override
    public void setShopGuiId(String id) { this.shopGuiId = id; }
    @Override
    public String getUpgradeGuiId() { return upgradeGuiId; }
    @Override
    public void setUpgradeGuiId(String id) { this.upgradeGuiId = id; }

    public void setWaitingPos1(Location waitingPos1) {
        this.waitingPos1 = waitingPos1;
    }

    public void setWaitingPos2(Location waitingPos2) {
        this.waitingPos2 = waitingPos2;
    }

    public void setWaitingSpawn(Location waitingSpawn) {
        this.waitingSpawn = waitingSpawn;
        world.setSpawnLocation(waitingSpawn);
    }


    public void setWaitingTime(int time) {
        waitingTime = time;
    }

    @Override
    public void setQuickVoidY(int y) {
        this.quickVoidY = y;
    }

    @Override
    public Integer getQuickVoidY() {
        return quickVoidY;
    }

    public boolean isInTeamBase(Location loc) {
        for (Team team : teams.values()) {
            if (team.isInBuildProtArea(loc)) return true;
        }
        return false;
    }

    public boolean isNearDiamondOrEmeraldGenerator(Location loc) {
        for (Generator generator : generators) {
            if (generator.getType() != GeneratorType.DIAMOND
                    && generator.getType() != GeneratorType.EMERALD) continue;
            Location gLoc = generator.getLocation();
            if (gLoc.getWorld() != loc.getWorld()) continue;
            if (Math.abs(gLoc.getBlockX() - loc.getBlockX()) <= 2
                    && Math.abs(gLoc.getBlockY() - loc.getBlockY()) <= 2
                    && Math.abs(gLoc.getBlockZ() - loc.getBlockZ()) <= 2) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setRespawnTime(int time) {
        respawnTime = time;
    }

    @Override
    public Location getWaitingSpawn() {
        return waitingSpawn;
    }

    @Override
    public int getWaitingTime() {
        return waitingTime;
    }

    @Override
    public Location getWaitingPos1() {
        return waitingPos1;
    }

    @Override
    public Location getWaitingPos2() {
        return waitingPos2;
    }

    @Override
    public void bedDestruction() {
        for (Team team : teams.values()) {
            if (team.getTeamBed() != null && team.getTeamBed().isAlive()) {
                team.getTeamBed().destroy();
                Location loc = team.getTeamBed().getLocation();
                loc.getBlock().setType(Material.AIR, false);
                for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                    Block relative = loc.getBlock().getRelative(face);
                    if (relative.getType().name().contains("BED")) {
                        relative.setType(Material.AIR, false);
                    }
                }
            }
        }
        broadcast("BedsDestroyed");
        broadcastTitle("&c&lBeds Destroyed!", "&fAll beds are now destroyed!", 10, 60, 20);
    }

    @Override
    public void endGame(String criteria) {
        if ("DRAW".equalsIgnoreCase(criteria)) {
            broadcast("GameDraw");
            broadcastTitle("&c&lDraw!", "&fNo team wins!", 10, 60, 20);
            startEnding(null);
        } else if ("MOST_KILLS".equalsIgnoreCase(criteria)) {
            Team winner = null;
            int mostKills = -1;
            for (Team team : teams.values()) {
                int teamKills = 0;
                for (Player player : team.getPlayers()) {
                    GamePlayer gp = getPlayer(player.getUniqueId());
                    if (gp != null) teamKills += gp.getKills();
                }
                if (teamKills > mostKills) {
                    mostKills = teamKills;
                    winner = team;
                }
            }
            startEnding(winner);
        } else {
            stop();
        }
    }

    @Override
    public void enderDragonEnd() {
        dragons.removeAll();
    }

    @Override
    public void enderDragonStart() {
        if (dragons.spawn()) {
            broadcast("EnderDragonSpawned");
        }
    }

    @Override
    public void disableRegeneration() {
        world.setGameRule(GameRule.NATURAL_REGENERATION, false);
        broadcast("RegenerationDisabled");
    }

    @Override
    public void disableTeamsForge() {
        for (Team team : teams.values()) {
            Generator gen = team.getGenerator();
            if (gen != null) {
                gen.stop();
            }
        }
        broadcast("ForgeDisabled");
    }

    @Override
    public void addPlacedBlock(Location loc) {
        blocks.addPlaced(loc);
    }

    @Override
    public boolean isPlacedBlock(Location loc) {
        return blocks.isPlaced(loc);
    }

    @Override
    public void clearPlacedBlocks() {
        blocks.clear();
    }

    @Override
    public void addBlastProtBlock(Location loc) {
        blocks.addBlastProtected(loc);
    }

    @Override
    public boolean isBlastProtBlock(Location loc) {
        return blocks.isBlastProtected(loc);
    }

    @Override
    public void removeBlastProtBlock(Location loc) {
        blocks.removeBlastProtected(loc);
    }

    private void sendResourcePack(Player player) {
        if (resourcePackUrl == null || resourcePackUrl.isEmpty()) return;

        if (playersWithResourcePack.contains(player.getUniqueId())) {
            return;
        }
        
        if (resourcePackHash != null && !resourcePackHash.isEmpty()) {
            player.setResourcePack(resourcePackUrl, resourcePackHash);
        } else {
            player.setResourcePack(resourcePackUrl);
        }
        
        playersWithResourcePack.add(player.getUniqueId());
    }

    private void resendResourcePackToAll() {
        if (resourcePackUrl == null || resourcePackUrl.isEmpty()) return;
        
        for (UUID uuid : players.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                sendResourcePack(p);
            }
        }
    }

    private void startTntParticleTask() {
        if (!Properties.TntParticlesEnabled) return;
        tntParticleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : players.keySet()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null || spectatorsList.contains(p)) continue;
                    if (p.getInventory().contains(Material.TNT)) {
                        world.spawnParticle(Particle.REDSTONE, p.getLocation().add(0, 2.2, 0),
                                6, 0.3, 0.2, 0.3, 0,
                                new Particle.DustOptions(org.bukkit.Color.RED, 1.2f));
                    }
                }
            }
        }.runTaskTimer(NewBedwars.plugin, 0L, 5L);
    }

    @Override
    public String toJson() {
        return ArenaSerializer.toJson(this);
    }

    String getWorldName() {
        return worldName;
    }

    int getRespawnTime() {
        return respawnTime;
    }

    GamePhases getGamePhases() {
        return gamePhases;
    }

    String getUnresolvedGamePhasesId() {
        return unresolvedGamePhasesId;
    }

    String getUnresolvedGeneratorsLevelingId() {
        return unresolvedGeneratorsLevelingId;
    }

    private void checkIfCanStart() {
        if (gameStatus == GameStatus.waiting && players.size() >= minPlayers && !hasEnoughTeams()) {
            broadcast("WaitingForOtherTeams");
            return;
        }
        if (players.size() < minPlayers || gameStatus != GameStatus.waiting) {
            return;
        }
        starting();
    }

    private void starting() {
        gameStatus = GameStatus.starting;
        nextPhaseTime = waitingTime;
        countdownTimer = new CountdownTimer(players.keySet(), waitingTime, this);
        countdownTimer.start();
    }

    private boolean splitPartiesToStart() {
        return gamerules.SplitPartiesToStart == null || gamerules.SplitPartiesToStart;
    }

    private List<List<Player>> buildGroups(boolean keepParties) {
        List<List<Player>> groups = new ArrayList<>();
        Set<UUID> grouped = new HashSet<>();
        for (UUID id : players.keySet()) {
            if (grouped.contains(id)) continue;
            Player player = Bukkit.getPlayer(id);
            if (player == null) continue;
            List<Player> group = new ArrayList<>();
            group.add(player);
            grouped.add(id);
            if (keepParties && NewBedwars.plugin.getPartyManager().isPlayerInParty(player)) {
                Party party = NewBedwars.plugin.getPartyManager().getParty(player);
                if (party != null) {
                    for (UUID memberId : party.getMembers()) {
                        Player member = Bukkit.getPlayer(memberId);
                        if (member != null && players.containsKey(memberId) && grouped.add(memberId)) {
                            group.add(member);
                        }
                    }
                }
            }
            groups.add(group);
        }
        return groups;
    }

    private List<List<Player>> computeAssignment(boolean keepParties) {
        return TeamAssigner.assign(buildGroups(keepParties), teams.size(), maxInTeam,
                gamerules.RandomTeams, ThreadLocalRandom.current());
    }

    private static int nonEmptyTeams(List<List<Player>> assignment) {
        int count = 0;
        for (List<Player> team : assignment) if (!team.isEmpty()) count++;
        return count;
    }

    /** A game needs players in at least two teams; one big party may otherwise be alone. */
    private boolean hasEnoughTeams() {
        boolean keepParties = gamerules.AllowParties;
        if (nonEmptyTeams(computeAssignment(keepParties)) >= 2) return true;
        return keepParties && splitPartiesToStart() && nonEmptyTeams(computeAssignment(false)) >= 2;
    }

    /**
     * Distributes everyone in the arena over the teams. Party members stay together when
     * possible; teams are filled evenly (ties are random when {@code RandomTeams} is on).
     * If that would leave a single team, parties are split (gamerule {@code SplitPartiesToStart}).
     */
    private void assignTeams() {
        boolean keepParties = gamerules.AllowParties;
        List<List<Player>> assignment = computeAssignment(keepParties);
        if (keepParties && nonEmptyTeams(assignment) < 2 && splitPartiesToStart()) {
            assignment = computeAssignment(false);
        }
        List<Team> teamList = new ArrayList<>(teams.values());
        for (int i = 0; i < teamList.size(); i++) {
            for (Player player : assignment.get(i)) {
                teamList.get(i).addPlayer(player);
                players.get(player.getUniqueId()).setTeam(teamList.get(i));
            }
        }
    }

    private void updateScoreboards() {
        for (NScoreboard scoreboard : scoreboards.values()) {
            scoreboard.kill();
        }
        for (UUID player : players.keySet()) {
            Player p = Bukkit.getPlayer(player);
            if (p == null) {
                continue;
            }
            NScoreboard scoreboard = scoreboardManager.createPlayerScoreboard("InGameScoreboard", this, p);
            if (scoreboard != null) {
                scoreboards.put(p.getUniqueId(), scoreboard);
            }
        }
    }

    private void discardBossBar() {
        if (arenaBossBar != null) {
            NewBedwars.plugin.getBossBarManager().removeBossBar(arenaBossBar);
            arenaBossBar = null;
        }
    }

    /** A finished game removes the bar, so the next waiting lobby gets a fresh one on first use. */
    private void addToBossBar(Player player) {
        if (arenaBossBar == null && (gameStatus == GameStatus.waiting || gameStatus == GameStatus.starting)) {
            arenaBossBar = NewBedwars.plugin.getBossBarManager().createBossBar("WaitingBeforeStart", this);
        }
        if (arenaBossBar != null) {
            arenaBossBar.addPlayer(player);
        }
    }

    private void updateBossBars() {
        discardBossBar();
        arenaBossBar = NewBedwars.plugin.getBossBarManager().createBossBar("GameInProgress", this);
        if (arenaBossBar != null) {
            arenaBossBar.setVisible(true);
            for (UUID player : players.keySet()) {
                Player p = Bukkit.getPlayer(player);
                if (p == null) {
                    continue;
                }
                arenaBossBar.addPlayer(p);
            }
        }
    }

    private void addSpectatorInternal(Player player, boolean canExit) {
        if (!spectatorsList.contains(player)) spectatorsList.add(player);
        if (player.isDead()) {
            player.spigot().respawn();
        }
        PlayerStateResetter.reset(player, GameMode.ADVENTURE);
        player.setFallDistance(0f);
        player.setVelocity(new Vector(0, 0, 0));
        player.setInvulnerable(true);
        player.teleport(waitingSpawn);
        player.setAllowFlight(true);
        player.setFlying(true);
        Bukkit.getScheduler().runTaskLater(NewBedwars.plugin, () -> {
            if (spectatorsList.contains(player) && player.isOnline()) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }
        }, 1L);
        if (canExit) {
            player.getInventory().setItem(8, NewBedwars.plugin.getGuiManager().getLeaveItem(player));
        }
        player.getInventory().setItem(0, NewBedwars.plugin.getGuiManager().getSpectatorItem(player));
        player.getInventory().setItem(4, NewBedwars.plugin.getGuiManager().getSpectatorEffectsItem(player));
        sendResourcePack(player);
        for (UUID p : players.keySet()) {
            Player pl = Bukkit.getServer().getPlayer(p);
            if (pl == null) {
                continue;
            }
            if (spectatorsList.contains(pl)) {
                continue;
            }
            pl.hidePlayer(NewBedwars.plugin, player);
        }
    }

    private void respawnPlayer(Player player) {
        if (!spectatorsList.contains(player)) {
            throw new IllegalArgumentException("Player is not spectator");
        }
        Team team = getTeam(player);
        if (team == null) {
            throw new IllegalArgumentException("Player is not in team");
        }
        spectatorsList.remove(player);
        player.setFallDistance(0f);
        player.teleport(team.getTeamSpawn());
        PlayerStateResetter.reset(player, GameMode.SURVIVAL);
        ArmorContents armorContents = team.getArmorContents(player);
        if (armorContents != null) {
            if (!gamerules.MakeSwordsPermanent) {
                armorContents.resetSword();
            }
            armorContents.loadPlayerArmorContents(player);
        }
        player.setInvulnerable(false);
        PlayerStateResetter.clearEffects(player);
        if (team.getTeamUpgrades() != null) team.getTeamUpgrades().applyPlayerUpgrades(player);
        for (UUID p : players.keySet()) {
            Player pl = Bukkit.getServer().getPlayer(p);
            if (pl == null) {
                continue;
            }
            pl.showPlayer(NewBedwars.plugin, player);
        }
    }

    /** Offline players whose team has been eliminated can no longer rejoin. */
    private void dropRejoinOfDeadTeams() {
        for (UUID id : rejoin.waiting()) {
            GamePlayer gp = players.get(id);
            if (gp == null || (gp.getTeam() != null && !gp.getTeam().isAlive())) {
                rejoin.cancel(id);
                players.remove(id);
            }
        }
    }

    private boolean checkWin() {
        if (gameStatus != GameStatus.playing) return false;
        dropRejoinOfDeadTeams();
        int alive = teamsAlive();
        if (alive == 1) {
            for (Team team : teams.values()) {
                if (team.isAlive()) {
                    startEnding(team);
                    return true;
                }
            }
        } else if (alive == 0) {
            // everyone left or was eliminated at once: nobody wins
            startEnding(null);
            return true;
        }
        return false;
    }

    private int teamsAlive() {
        int alive = 0;
        for (Team team : teams.values()) {
            if (team.isAlive()) {
                alive++;
            }
        }
        return alive;
    }

    private void sendWinMessage(Team team) {
        broadcast("GameEnded", Map.of("/team/", team.colored(team.getName())));
        broadcastTitle("&6&lGame Ended!", "&e" + team.getColor() + team.getName() + " &fwins!", 10, 40, 10);
        Color teamColor = Color.WHITE;
        for (Player player : team.getPlayers()) {
            Location loc = player.getLocation();
            FireworkEffect effect = FireworkEffect.builder()
                    .withColor(teamColor)
                    .withFade(Color.YELLOW)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .trail(true)
                    .flicker(true)
                    .build();
            Firework fw = (Firework) world.spawnEntity(loc, EntityType.FIREWORK);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(effect);
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        }
    }

    private void persistStats(Team winner) {
        com.l299l.newbedwars.player.PlayerManager pm = NewBedwars.plugin.getPlayerManager();
        for (Map.Entry<UUID, GamePlayer> entry : players.entrySet()) {
            GamePlayer gp = entry.getValue();
            boolean won = winner != null && winner.getPlayers().contains(gp.getPlayer());
            pm.addGameResult(gp.getPlayer().getName(), gp, won);
        }
        NewBedwars.plugin.getDataManager().savePlayerStats();
    }

    private void startEnding(Team winner) {
        persistStats(winner);
        gameStatus = GameStatus.ending;
        ApiEvents.gameEnd(this, winner);
        if (gamePhases != null && gamePhases.getCurrentPhase() != null) {
            gamePhases.getCurrentPhase().cancel();
        }
        
        for (GamePlayer gp : players.values()) {
            if (gp != null) gp.cancelRespawnTask();
        }
        
        IronGolemLogic.cleanupAllForArena(arenaName);
        SilverfishLogic.cleanupAllForArena(this);
        
        for (Generator generator : generators) generator.stop();
        for (Team team : teams.values()) {
            if (team.getGenerator() != null) team.getGenerator().stop();
        }
        world.getEntitiesByClass(Item.class).forEach(item -> item.remove());
        if (winner != null) {
            sendWinMessage(winner);
        } else {
            broadcast("GameEndedDraw");
        }
        for (UUID uuid : new ArrayList<>(players.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) {
                players.remove(uuid);
            } else if (!spectatorsList.contains(p) && p.getWorld() == world) {
                // Players who already left (lobby, waiting for a rejoin) are not pulled back to watch the end.
                addSpectatorInternal(p, true);
            }
        }
        ArrayList<Player> allSpectators = new ArrayList<>(spectatorsList);
        for (Player p1 : allSpectators) {
            for (Player p2 : allSpectators) {
                if (p1 != p2 && p1.isOnline() && p2.isOnline()) {
                    p1.showPlayer(NewBedwars.plugin, p2);
                }
            }
        }
        for (NScoreboard sb : scoreboards.values()) sb.kill();
        scoreboards.clear();
        for (Player p : allSpectators) {
            if (p == null || !p.isOnline()) continue;
            NScoreboard sb = scoreboardManager.createPlayerScoreboard("EndingScoreboard", this, p);
            if (sb != null) scoreboards.put(p.getUniqueId(), sb);
        }

        discardBossBar();
        arenaBossBar = NewBedwars.plugin.getBossBarManager().createBossBar("GameEnding", this);
        if (arenaBossBar != null) {
            arenaBossBar.setVisible(true);
            for (UUID uuid : players.keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) arenaBossBar.addPlayer(p);
            }
            for (Player p : new ArrayList<>(spectatorsList)) {
                if (p != null && p.isOnline()) arenaBossBar.addPlayer(p);
            }
        }
        MapVoteManager.gameEnded(this);
        endingTimer = new BukkitRunnable() {
            int timeLeft = Properties.AfterGameWatchTime;
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    cancel();
                    Runnable moveToVotedArena = MapVoteManager.closeEndVote(Arena.this);
                    stop();
                    if (moveToVotedArena != null) moveToVotedArena.run();
                    return;
                }
                if (timeLeft <= 5) {
                    broadcastTitle("&c&lArena resetting in " + timeLeft, "", 0, 25, 0);
                }
                timeLeft--;
            }
        }.runTaskTimer(NewBedwars.plugin, 0, 20);
    }

}
