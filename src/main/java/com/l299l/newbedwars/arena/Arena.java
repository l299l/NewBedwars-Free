package com.l299l.newbedwars.arena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.parties.Party;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.player.inventory.ArmorContents;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.arena.gamerules.SpecialGamerule;
import com.l299l.newbedwars.arena.shops.TeamShop;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.utils.CountdownTimer;
import com.l299l.newbedwars.utils.DecoUtils;
import com.l299l.newbedwars.utils.JsonUtils;
import com.l299l.newbedwars.scoreboard.NScoreboard;
import com.l299l.newbedwars.scoreboard.ScoreboardManager;
import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
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

public class Arena implements IArena {
    public static HashMap<String, IArena> arenaByName = new HashMap<>();
    public static HashMap<World, IArena> arenaByWorld = new HashMap<World, IArena>();

    public static void updateTablist(Player player) {
        if (!Properties.TablistIsolationEnabled) return;
        IArena playerArena = arenaByWorld.get(player.getWorld());
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            IArena otherArena = arenaByWorld.get(other.getWorld());
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
    private final HashMap<String, Team> teams;
    private final HashMap<SpecialGamerule, Boolean> specialGamerules;
    private Gamerules gamerules;
    private Boolean enabled;
    private Setup setup;
    private GameStatus gameStatus;
    private final String arenaName;
    private final ArrayList<UUID> onSetupList;
    private final ArrayList<Generator> generators;
    private GeneratorLeveling generatorsLeveling;
    private Integer minPlayers;
    private Integer maxInTeam;
    private Integer waitingTime;
    private GamePhases gamePhases;
    private Integer nextPhaseTime;
    private Integer respawnTime;
    private World world;
    private final ArrayList<Player> spectatorsList;
    private final HashMap<UUID, GamePlayer> players;
    private final HashMap<UUID, NScoreboard> scoreboards;
    private final String worldName;
    private Location waitingPos1;
    private Location waitingPos2;
    private Location waitingSpawn;
    private BossBar arenaBossBar;
    private CountdownTimer countdownTimer;
    private BukkitTask endingTimer;
    private final ScoreboardManager scoreboardManager;
    private final HashMap<Team, EnderDragon> teamDragons = new HashMap<>();
    private final HashMap<Team, BukkitTask> dragonTasks = new HashMap<>();
    private final HashSet<String> placedBlocks;
    private final HashSet<String> blastProtBlocks;
    private BukkitTask tntParticleTask;
    private Integer quickVoidY;

    public Arena(String arenaName, World world) {
        this.arenaName = arenaName;
        this.world = world;
        this.gameStatus = GameStatus.restarting;
        players = new HashMap<>();
        worldName = world.getName();
        minPlayers = Properties.DefaultMinPlayers;
        maxInTeam = Properties.DefaultMaxInTeam;
        waitingTime = Properties.DefaultWaitingTime;
        generatorsLeveling = NewBedwars.plugin.getGeneratorLeveling().get(Properties.DefaultGeneratorConfig);
        arenaBossBar = NewBedwars.plugin.getBossBarManager().createBossBar("WaitingBeforeStart", this);
        scoreboardManager = NewBedwars.plugin.getScoreboardManager();
        GamePhases defaultPhases = Properties.GamePhases.get(Properties.DefaultGamePhase);
        gamePhases = defaultPhases != null ? new GamePhases(defaultPhases) : null;
        respawnTime = Properties.DefaultRespawnTime;
        nextPhaseTime = 0;
        gamerules = new Gamerules();
        setup = Setup.NO_SETUP;
        enabled = false;
        world.setAutoSave(false);
        onSetupList = new ArrayList<>();
        teams = new HashMap<String, Team>();
        generators = new ArrayList<>();
        spectatorsList = new ArrayList<Player>();
        specialGamerules = new HashMap<>();
        scoreboards = new HashMap<>();
        placedBlocks = new HashSet<>();
        blastProtBlocks = new HashSet<>();
        arenaByName.put(arenaName, this);
        arenaByWorld.put(world, this);
    }

    @Override
    public void start() {
        if (countdownTimer != null && gameStatus == GameStatus.starting) {
            countdownTimer.cancel();
        }
        DecoUtils.removeAllArmorStands(this);
        gameStatus = GameStatus.playing;
        broadcast("GameStarted");
        clearWaitingArea();
        for (Team team : teams.values()) {
            team.start();
        }
        for (Generator generator : generators) {
            generator.start();
        }
        updateBossBars();
        updateScoreboards();
        gamePhases.start(this);
        startTntParticleTask();
        for (Team team : new ArrayList<>(teams.values())) {
            if (team.getPlayers().isEmpty()) {
                team.stop();
            }
        }
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
        gameStatus = GameStatus.restarting;

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

        for (UUID playerId : new ArrayList<>(players.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                leave(player);
            }
        }
        players.clear();
        for (Player spectator : new ArrayList<>(spectatorsList)) {
            leave(spectator);
        }
        spectatorsList.clear();
        nextPhaseTime = 0;

        for (Generator generator : generators) {
            generator.stop();
        }

        for (Team team : teams.values()) {
            team.stop();
        }

        if (arenaBossBar != null) {
            arenaBossBar.removeAll();
            arenaBossBar.setVisible(false);
        }

        for (NScoreboard scoreboard : scoreboards.values()) {
            scoreboard.kill();
        }
        scoreboards.clear();

        boolean correctRollback = rollback();
        if (!correctRollback) {
            Bukkit.getServer().getLogger().severe("Could not rollback arena " + arenaName);
        }

        gameStatus = GameStatus.waiting;
    }

    @Override
    public boolean rollback() {
        clearPlacedBlocks();
        for (BukkitTask task : dragonTasks.values()) {
            if (!task.isCancelled()) task.cancel();
        }
        dragonTasks.clear();
        for (EnderDragon dragon : teamDragons.values()) {
            if (dragon != null && dragon.isValid()) dragon.remove();
        }
        teamDragons.clear();
        try {
            world.setKeepSpawnInMemory(false);
            world.setAutoSave(false);
            for (Chunk chunk : world.getLoadedChunks()) {
                chunk.unload(true);
            }
        } catch (Exception ignored) {}
        arenaByWorld.remove(world);
        boolean correct = Bukkit.getServer().unloadWorld(world, false);
        if (!correct) {
            Bukkit.getServer().getLogger().severe("Could not unload arena world " + worldName);
            return false;
        }
        World newWorld = Bukkit.getServer().createWorld(new WorldCreator(worldName));
        if (newWorld == null) {
            Bukkit.getServer().getLogger().severe("Could not recreate arena world " + worldName);
            return false;
        }
        newWorld.setAutoSave(false);
        newWorld.setKeepSpawnInMemory(false);
        newWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        newWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
        newWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        newWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
        world = newWorld;
        arenaByWorld.put(newWorld, this);
        DecoUtils.removeAllArmorStands(this);
        if (waitingSpawn != null) waitingSpawn.setWorld(newWorld);
        if (waitingPos1 != null) waitingPos1.setWorld(newWorld);
        if (waitingPos2 != null) waitingPos2.setWorld(newWorld);
        for (Team team : teams.values()) {
            team.updateWorldReference(newWorld);
        }
        for (Generator generator : generators) {
            generator.getLocation().setWorld(newWorld);
        }
        return true;
    }

    @Override
    public Integer getGameTime() {
        return 0;
    }

    @Override
    public void upgradeGenerators(GeneratorType type, Integer level) {
        for (Generator generator : generators) {
            if (generator.getType() == type) {
                generator.upgrade(level);
            }
        }
    }

    private void applyJoinSetup(Player player, Team team) {
        player.teleport(waitingSpawn);
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setExp(0);
        player.setLevel(0);
        player.setFireTicks(0);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setInvulnerable(true);
        player.getInventory().setItem(8, NewBedwars.plugin.getGuiManager().getLeaveItem(player));
        if (arenaBossBar != null) {
            arenaBossBar.addPlayer(player);
        }
        NScoreboard scoreboard = scoreboardManager.createPlayerScoreboard("WaitingScoreboard", this, player);
        if (scoreboard != null) {
            scoreboards.put(player.getUniqueId(), scoreboard);
        }
        players.put(player.getUniqueId(), new GamePlayer(player, team));
        updateTablist(player);
    }

    @Override
    public boolean join(Player player) {
        if (players.containsKey(player.getUniqueId())) return false;
        if (players.size() > teams.size() * maxInTeam) {
            return false;
        } else if (players.size() + 1 >= minPlayers && gameStatus == GameStatus.waiting) {
            starting();
        }
        Team team = joinPlayerToTeam(player);
        if (team == null) {
            return false;
        }
        applyJoinSetup(player, team);
        broadcast("PlayerJoinedArena", new HashMap<String, String>() {{
            put("/player/", player.getName());
            put("/players/", Integer.toString(players.size()));
            put("/maxplayers/", Integer.toString(teams.size() * maxInTeam));
        }});
        return true;
    }

    @Override
    public boolean joinParty(List<Player> partyMembers) {
        if (partyMembers.isEmpty()) return false;
        int newTotal = players.size() + partyMembers.size();
        if (newTotal > teams.size() * maxInTeam) return false;

        // Find a team with enough consecutive free slots for all party members
        Team targetTeam = null;
        for (Team team : teams.values()) {
            int freeSlots = maxInTeam - team.getPlayers().size();
            if (freeSlots >= partyMembers.size()) {
                targetTeam = team;
                break;
            }
        }
        if (targetTeam == null) return false;

        if (players.size() + partyMembers.size() >= minPlayers && gameStatus == GameStatus.waiting) {
            starting();
        }

        final Team finalTeam = targetTeam;
        List<Player> joined = new ArrayList<>();
        for (Player member : partyMembers) {
            if (players.containsKey(member.getUniqueId())) continue;
            finalTeam.addPlayer(member);
            applyJoinSetup(member, finalTeam);
            joined.add(member);
        }

        int currentSize = players.size();
        int maxSize = teams.size() * maxInTeam;
        for (Player member : joined) {
            final String name = member.getName();
            broadcast("PlayerJoinedArena", new HashMap<String, String>() {{
                put("/player/", name);
                put("/players/", Integer.toString(currentSize));
                put("/maxplayers/", Integer.toString(maxSize));
            }});
        }
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
        team.addPlayer(player);
        if (arenaBossBar != null) {
            arenaBossBar.addPlayer(player);
        }
        NScoreboard scoreboard = scoreboardManager.createPlayerScoreboard("InGameScoreboard", this, player);
        if (scoreboards != null && scoreboard != null) {
            scoreboards.put(player.getUniqueId(), scoreboard);
        }
        killPlayer(player);
        updateTablist(player);
        return true;
    }

    @Override
    public void leave(Player player) {
        Location lobby = NewBedwars.plugin.getLobbyLocation();
        player.teleport(lobby);
        player.setInvulnerable(false);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setExp(0);
        player.setLevel(0);
        player.setFireTicks(0);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.setBedSpawnLocation(lobby, true);
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
        final String playerName = player.getName();
        if (gameStatus == GameStatus.playing) {
            leavingPlayer.getTeam().removePlayer(player);
            if (!checkWin()) {
                if (Properties.RejoinTimeEnabled) {
                    int displayCount = players.size() - 1;
                    broadcast("PlayerLeftArena", new HashMap<String, String>() {{
                        put("/player/", playerName);
                        put("/players/", Integer.toString(displayCount));
                        put("/maxplayers/", Integer.toString(teams.size() * maxInTeam));
                    }});
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            players.remove(player.getUniqueId());
                            NewBedwars.plugin.getMessages().send(player, "RejoinTimeOver");
                        }
                    }.runTaskLater(NewBedwars.plugin, 20L * 60 * Properties.RejoinTime);
                } else {
                    players.remove(player.getUniqueId());
                    broadcast("PlayerLeftArena", new HashMap<String, String>() {{
                        put("/player/", playerName);
                        put("/players/", Integer.toString(players.size()));
                        put("/maxplayers/", Integer.toString(teams.size() * maxInTeam));
                    }});
                }
            }
        } else {
            leavingPlayer.getTeam().removePlayer(player);
            players.remove(player.getUniqueId());
            broadcast("PlayerLeftArena", new HashMap<String, String>() {{
                put("/player/", playerName);
                put("/players/", Integer.toString(players.size()));
                put("/maxplayers/", Integer.toString(teams.size() * maxInTeam));
            }});
            if (gameStatus == GameStatus.starting && players.size() < minPlayers) {
                cancelStart();
            }
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
            if (checkWin()) {
            }
        }else {
            addSpectator(player, false);
            new BukkitRunnable() {
                int time = respawnTime;
                @Override
                public void run() {
                    if (isPlayerInArena(player)) {
                        player.sendTitle(ChatColor.RED + "" + time, ChatColor.YELLOW + "to respawn", 0, 20, 0);
                        time--;
                        if (time == 0) {
                            player.sendTitle(ChatColor.GREEN + "Respawned", "", 0, 20, 0);
                            respawnPlayer(player);
                            cancel();
                        }
                    }else {
                        cancel();
                    }
                }
            }.runTaskTimer(NewBedwars.plugin, 0, 20);
        }


    }

    @Override
    public Boolean isPlayerInArena(Player player) {
        return players.containsKey(player.getUniqueId());
    }

    @Override
    public Set<UUID> getPlayers() {
        return players.keySet();
    }

    @Override
    public GamePlayer getPlayer(UUID player) {
        return players.get(player);
    }

    @Override
    public void cancelStart() {
        countdownTimer.cancel();
        gameStatus = GameStatus.waiting;
        broadcast("StartCanceled");
        broadcast("NotEnoughPlayers", new HashMap<String, String>() {{
            put("/arenaname/", arenaName);
            put("/minplayers/", minPlayers.toString());
            put("/players/", Integer.toString(players.size()));
        }});
    }

    @Override
    public void broadcast(String message) {
        for (UUID p : players.keySet()) {
            Player player = Bukkit.getServer().getPlayer(p);
            if (player == null) {
                continue;
            }
            NewBedwars.plugin.getMessages().send(player, message);
        }
    }

    @Override
    public void broadcast(String message, HashMap<String, String> replace) {
        for (UUID p : players.keySet()) {
            Player player = Bukkit.getServer().getPlayer(p);
            if (player == null) {
                continue;
            }
            String msg = NewBedwars.plugin.getMessages().getMsg(player, message);
            for (String key : replace.keySet()) {
                msg = msg.replaceAll(key, replace.get(key));
            }
            player.sendMessage(msg);
        }
    }

    @Override
    public void broadcastTitle(String title, String subtitle, Integer fadeIn, Integer stay, Integer fadeOut) {
        for (UUID p : players.keySet()) {
            Player player = Bukkit.getServer().getPlayer(p);
            if (player != null) {
                player.sendTitle(
                        ChatColor.translateAlternateColorCodes('&', title),
                        ChatColor.translateAlternateColorCodes('&', subtitle),
                        fadeIn, stay, fadeOut
                );
            }
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
        arenaBossBar = NewBedwars.plugin.getBossBarManager().createBossBar("WaitingBeforeStart", this);
    }

    public void disable() {
        if (gameStatus == GameStatus.waiting) {
            for (UUID p : players.keySet()) {
                Player player = Bukkit.getServer().getPlayer(p);
                if (player == null) {
                    continue;
                }
                leave(player);
                NewBedwars.plugin.getMessages().send(player, "KickedFromArena");
            }
        }
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
        gamePhases.nextPhase(this);
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

    public Boolean isEnabled() {
        return enabled;
    }

    @Override
    public Boolean canBeEnabled() {
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
    public Boolean canBeDisabled() {
        return gameStatus == GameStatus.restarting || gameStatus == GameStatus.waiting;
    }

    @Override
    public GameStatus status() {
        return gameStatus;
    }

    @Override
    public Integer getMinPlayers() {
        return minPlayers;
    }

    @Override
    public void setMinPlayers(Integer min) {
        minPlayers = min;
    }

    @Override
    public Integer getMaxInTeam() {
        return maxInTeam;
    }

    @Override
    public void setMaxInTeam(Integer max) {
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

    public HashMap<String, Team> getTeams() {
        return teams;
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

    public ArrayList<Generator> getGenerators() {
        return generators;
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
        if (arenaBossBar != null) {
            arenaBossBar.addPlayer(player);
        }
        NScoreboard scoreboard = scoreboardManager.createPlayerScoreboard("InGameScoreboard", this, player);
        if (scoreboard != null) {
            scoreboards.put(player.getUniqueId(), scoreboard);
        }
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

    @Override
    public void setSpecialGamerules(HashMap<String, Boolean> gamerules) {
        for (String gamerule : gamerules.keySet()) {
            setSpecialGamerule(gamerule, gamerules.get(gamerule));
        }
    }

    @Override
    public void setSpecialGamerule(String gamerule, Boolean value) {
        SpecialGamerule specialGamerule = SpecialGamerule.getByName(gamerule);
        if (specialGamerule != null) {
            specialGamerules.put(specialGamerule, value);
        }
    }

    public void setWaitingTime(Integer time) {
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
    public void setRespawnTime(Integer time) {
        respawnTime = time;
    }

    @Override
    public Location getWaitingSpawn() {
        return waitingSpawn;
    }

    @Override
    public Integer getWaitingTime() {
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
        for (BukkitTask task : new ArrayList<>(dragonTasks.values())) {
            if (!task.isCancelled()) task.cancel();
        }
        dragonTasks.clear();
        for (EnderDragon dragon : new ArrayList<>(teamDragons.values())) {
            if (dragon != null && dragon.isValid()) dragon.remove();
        }
        teamDragons.clear();
    }

    @Override
    public void enderDragonStart() {
        boolean anySpawned = false;
        for (Team team : teams.values()) {
            if (!team.isAlive()) continue;
            TeamUpgrades tu = team.getTeamUpgrades();
            if (tu == null) continue;
            int dragonLevel = tu.getUpgradeLevel(Upgrade.DRAGONBUFF);
            if (dragonLevel < 1) continue;

            Location spawnLoc = team.getTeamSpawn().clone().add(0, Properties.DragonSpawnHeight, 0);
            EnderDragon dragon = (EnderDragon) world.spawnEntity(spawnLoc, EntityType.ENDER_DRAGON);
            dragon.setInvulnerable(true);
            dragon.setPhase(EnderDragon.Phase.CIRCLING);
            dragon.setCustomName(team.getColor() + team.getName() + " Dragon");
            dragon.setCustomNameVisible(true);
            teamDragons.put(team, dragon);
            anySpawned = true;

            final Team owningTeam = team;
            final int level = dragonLevel;
            final int fireballInterval = (level >= 2) ? 10 : 20;
            BukkitTask task = new BukkitRunnable() {
                int fireballTick = 0;

                @Override
                public void run() {
                    EnderDragon d = teamDragons.get(owningTeam);
                    if (d == null || !d.isValid()) {
                        cancel();
                        teamDragons.remove(owningTeam);
                        dragonTasks.remove(owningTeam);
                        return;
                    }

                    // STRAFING phase causes Paper log spam — redirect to CHARGE_PLAYER.
                    if (d.getPhase() == EnderDragon.Phase.STRAFING) {
                        d.setPhase(EnderDragon.Phase.CHARGE_PLAYER);
                    }

                    List<Player> enemies = new ArrayList<>();
                    for (Team t : teams.values()) {
                        if (t == owningTeam || !t.isAlive()) continue;
                        for (Player p : t.getPlayers()) {
                            if (p.isOnline() && p.getWorld().equals(world)
                                    && (p.getGameMode() == GameMode.SURVIVAL
                                    || p.getGameMode() == GameMode.ADVENTURE)) {
                                enemies.add(p);
                            }
                        }
                    }

                    fireballTick++;
                    if (fireballTick >= fireballInterval && !enemies.isEmpty()) {
                        fireballTick = 0;
                        Player target = enemies.stream()
                                .min(java.util.Comparator.comparingDouble(Player::getHealth))
                                .orElse(null);
                        if (target != null) {
                            DragonFireball fireball = (DragonFireball) world.spawnEntity(
                                    d.getLocation().clone().add(0, -1, 0), EntityType.DRAGON_FIREBALL);
                            Vector fbDir = target.getLocation().toVector()
                                    .subtract(d.getLocation().toVector()).normalize().multiply(0.5);
                            fireball.setDirection(fbDir);
                            fireball.setShooter(d);
                        }
                    }
                }
            }.runTaskTimer(NewBedwars.plugin, 0L, 20L);

            dragonTasks.put(team, task);
        }
        if (anySpawned) {
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

    public void addPlacedBlock(Location loc) {
        placedBlocks.add(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
    }

    public boolean isPlacedBlock(Location loc) {
        return placedBlocks.contains(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
    }

    public void clearPlacedBlocks() {
        placedBlocks.clear();
        blastProtBlocks.clear();
    }

    @Override
    public void addBlastProtBlock(Location loc) {
        blastProtBlocks.add(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
    }

    @Override
    public boolean isBlastProtBlock(Location loc) {
        return blastProtBlocks.contains(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
    }

    @Override
    public void removeBlastProtBlock(Location loc) {
        blastProtBlocks.remove(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
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
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"arenaName\": \"").append(arenaName).append("\",");
        sb.append("\"worldName\": \"").append(worldName).append("\",");
        sb.append("\"minPlayers\": ").append(minPlayers).append(",");
        sb.append("\"maxInTeam\": ").append(maxInTeam).append(",");
        sb.append("\"waitingTime\": ").append(waitingTime).append(",");
        sb.append("\"respawnTime\": ").append(respawnTime).append(",");
        sb.append("\"setup\": \"").append(setup).append("\",");
        sb.append("\"enabled\": ").append(enabled).append(",");
        sb.append("\"quickVoidY\": ").append(quickVoidY == null ? "null" : quickVoidY).append(",");
        sb.append("\"waitingPos1\": ").append(waitingPos1 == null ? "null" : JsonUtils.locationToJson(waitingPos1)).append(",");
        sb.append("\"waitingPos2\": ").append(waitingPos2 == null ? "null" : JsonUtils.locationToJson(waitingPos2)).append(",");
        sb.append("\"waitingSpawn\": ").append(waitingSpawn == null ? "null" : JsonUtils.locationToJson(waitingSpawn)).append(",");
        sb.append("\"specialGamerules\": {");
        int j = 0;
        for (SpecialGamerule gamerule : specialGamerules.keySet()) {
            sb.append("\"").append(gamerule.getName()).append("\": ").append(specialGamerules.get(gamerule));
            if (j < specialGamerules.size() - 1) {
                sb.append(",");
            }
            j++;
        }
        sb.append("},");
        sb.append("\"gamePhasesId\": \"").append(gamePhases == null ? "null" : gamePhases.getID()).append("\",");
        sb.append("\"generatorsLeveling\": \"").append(generatorsLeveling == null ? "null" : generatorsLeveling.getId()).append("\",");
        sb.append("\"gamerules\": ").append(gamerules.toJson()).append(",");
        sb.append("\"teams\": [");
        int i = 0;
        for (Team team : teams.values()) {
            sb.append(team.toJson());
            if (i < teams.size() - 1) {
                sb.append(",");
            }
            i++;
        }
        sb.append("],");
        sb.append("\"generators\": [");
        i = 0;
        for (Generator generator : generators) {
            sb.append(JsonUtils.generatorToJson(generator));
            if (i < generators.size() - 1) {
                sb.append(",");
            }
            i++;
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private void starting() {
        if (players.size() < minPlayers) {
            broadcast("NotEnoughPlayers", new HashMap<String, String>() {{
                put("/arenaname/", arenaName);
                put("/minplayers/", minPlayers.toString());
                put("/players/", Integer.toString(players.size()));
            }});
        }
        gameStatus = GameStatus.starting;
        nextPhaseTime = waitingTime;
        countdownTimer = new CountdownTimer(players.keySet(), waitingTime, this);
        countdownTimer.start();
    }

    private Team joinPlayerToTeam(Player player) {
        List<String> teamNames = new ArrayList<>(teams.keySet());
        List<Team> emptyTeams = new ArrayList<>();
        for (String teamName : teamNames) {
            Team team = teams.get(teamName);
            if (team.getPlayers().isEmpty()) {
                emptyTeams.add(team);
            }
        }
        if (gamerules.RandomTeams) {
            if (!emptyTeams.isEmpty()) {
                int random = (int) (Math.random() * emptyTeams.size());
                Team team = emptyTeams.get(random);
                team.addPlayer(player);
                return team;
            }else {
                int random = (int) (Math.random() * teamNames.size());
                Team team = teams.get(teamNames.get(random));
                if (team.getPlayers().size() < maxInTeam) {
                    team.addPlayer(player);
                    return team;
                }else {
                    for (String teamName : teamNames) {
                        team = teams.get(teamName);
                        if (team.getPlayers().size() < maxInTeam) {
                            team.addPlayer(player);
                            return team;
                        }
                    }
                }
            }
        } else if (gamerules.AllowParties && NewBedwars.plugin.getPartyManager().isPlayerInParty(player)) {
            // Try to join the same team as existing party members already in the arena
            Party party = NewBedwars.plugin.getPartyManager().getParty(player);
            if (party != null) {
                for (UUID memberId : party.getMembers()) {
                    if (players.containsKey(memberId)) {
                        Team memberTeam = players.get(memberId).getTeam();
                        if (memberTeam.getPlayers().size() < maxInTeam) {
                            memberTeam.addPlayer(player);
                            return memberTeam;
                        }
                    }
                }
            }
            // No party members in arena yet — use standard assignment
            if (!emptyTeams.isEmpty()) {
                Team team = emptyTeams.get(0);
                team.addPlayer(player);
                return team;
            }
            for (String teamName : teamNames) {
                Team team = teams.get(teamName);
                if (team.getPlayers().size() < maxInTeam) {
                    team.addPlayer(player);
                    return team;
                }
            }
        } else {
            if (!emptyTeams.isEmpty()) {
                Team team = emptyTeams.get(0);
                team.addPlayer(player);
                return team;
            }else {
                for (String teamName : teamNames) {
                    Team team = teams.get(teamName);
                    if (team.getPlayers().size() < maxInTeam) {
                        team.addPlayer(player);
                        return team;
                    }
                }
            }
        }
        return null;
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

    private void updateBossBars() {
        if (arenaBossBar != null) {
            arenaBossBar.removeAll();
            arenaBossBar.setVisible(false);
        }
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
        spectatorsList.add(player);
        if (player.isDead()) {
            player.spigot().respawn();
        }
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().setArmorContents(null);
        player.setExp(0);
        player.setLevel(0);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setVelocity(new Vector(0, 0, 0));
        player.setInvulnerable(true);
        player.teleport(waitingSpawn);
        player.setAllowFlight(true);
        player.setFlying(true);
        // Teleport resets flight state (especially cross-world) — re-apply on the next tick
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
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().clear();
        ArmorContents armorContents = team.getArmorContents(player);
        if (armorContents != null) {
            if (!gamerules.MakeSwordsPermanent) {
                armorContents.resetSword();
            }
            armorContents.loadPlayerArmorContents(player);
        }
        player.setExp(0);
        player.setLevel(0);
        player.setFireTicks(0);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setInvulnerable(false);
        player.setGameMode(GameMode.SURVIVAL);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        team.getTeamUpgrades().applyPlayerUpgrades(player);
        for (UUID p : players.keySet()) {
            Player pl = Bukkit.getServer().getPlayer(p);
            if (pl == null) {
                continue;
            }
            pl.showPlayer(NewBedwars.plugin, player);
        }
    }

    private boolean checkWin() {
        if (teamsAlive() == 1) {
            for (Team team : teams.values()) {
                if (team.isAlive()) {
                    startEnding(team);
                    return true;
                }
            }
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
        broadcast("GameEnded", new HashMap<String, String>() {{
            put("/team/", team.getName());
        }});
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

    private void startEnding(Team winner) {
        gameStatus = GameStatus.ending;
        if (gamePhases != null && gamePhases.getCurrentPhase() != null) {
            gamePhases.getCurrentPhase().cancel();
        }
        for (Generator generator : generators) generator.stop();
        for (Team team : teams.values()) {
            if (team.getGenerator() != null) team.getGenerator().stop();
        }
        world.getEntitiesByClass(Item.class).forEach(item -> item.remove());
        if (winner != null) {
            sendWinMessage(winner);
        }
        for (UUID uuid : new ArrayList<>(players.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) {
                players.remove(uuid);
            } else if (!spectatorsList.contains(p)) {
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

        if (arenaBossBar != null) {
            arenaBossBar.removeAll();
            arenaBossBar.setVisible(false);
        }
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
        endingTimer = new BukkitRunnable() {
            int timeLeft = Properties.AfterGameWatchTime;
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    cancel();
                    stop();
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
