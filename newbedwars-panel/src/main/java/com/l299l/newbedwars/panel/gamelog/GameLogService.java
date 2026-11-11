package com.l299l.newbedwars.panel.gamelog;

import com.l299l.newbedwars.api.events.ArenaChatEvent;
import com.l299l.newbedwars.api.events.ArenaResetEvent;
import com.l299l.newbedwars.api.events.BedBreakEvent;
import com.l299l.newbedwars.api.events.BedsDestroyedEvent;
import com.l299l.newbedwars.api.events.GameEndEvent;
import com.l299l.newbedwars.api.events.GamePhaseChangeEvent;
import com.l299l.newbedwars.api.events.GameStartEvent;
import com.l299l.newbedwars.api.events.GeneratorUpgradeEvent;
import com.l299l.newbedwars.api.events.PlayerJoinArenaEvent;
import com.l299l.newbedwars.api.events.PlayerKillEvent;
import com.l299l.newbedwars.api.events.PlayerLeaveArenaEvent;
import com.l299l.newbedwars.api.events.TeamEliminatedEvent;
import com.l299l.newbedwars.api.events.TeamUpgradeEvent;
import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.GamePlayerView;
import com.l299l.newbedwars.api.view.TeamView;
import com.l299l.newbedwars.panel.dashboard.DashboardService;
import com.l299l.newbedwars.panel.gamelog.GameLog.Type;
import com.l299l.newbedwars.api.NewBedwarsAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class GameLogService implements Listener {
    private final GameLog log;
    private final CommandFilter commands;
    private final Function<World, Optional<ArenaView>> arenaOf;

    private final BiPredicate<Player, String> allowed;

    public GameLogService(GameLog log) {
        this(log, new CommandFilter(CommandFilter.DEFAULT_IGNORED), world -> NewBedwarsAPI.get().getArena(world),
                GameLogService::mayRun);
    }

    public GameLogService(GameLog log, CommandFilter commands, Function<World, Optional<ArenaView>> arenaOf,
                          BiPredicate<Player, String> allowed) {
        this.log = log;
        this.commands = commands;
        this.arenaOf = arenaOf;
        this.allowed = allowed;
    }

    public static boolean mayRun(Player player, String label) {
        Command command = Bukkit.getServer().getCommandMap().getCommand(label);
        return command == null || command.testPermissionSilent(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        Optional<ArenaView> arena = arenaOf.apply(player.getWorld());
        if (arena.isEmpty()) return;
        String line = commands.loggable(e.getMessage());
        if (line == null) return;
        String label = line.substring(1).split(" ", 2)[0];
        String note = !allowed.test(player, label) ? " (no permission)" : e.isCancelled() ? " (cancelled)" : "";
        add(arena.get(), Type.COMMAND, player, teamOf(arena.get(), player), null, null, line + note);
    }

    public GameLog log() {
        return log;
    }

    private static TeamView teamOf(ArenaView arena, Player player) {
        if (player == null) return null;
        return arena.getGamePlayer(player.getUniqueId()).flatMap(GamePlayerView::getTeam).orElse(null);
    }

    private static String name(TeamView team) {
        return team == null ? null : team.getName();
    }

    private static String color(TeamView team) {
        return team == null ? null : DashboardService.hex(team.getColor());
    }

    private void add(ArenaView arena, Type type, Player player, TeamView team, String target, TeamView targetTeam, String detail) {
        log.add(arena.getName(), type, player == null ? null : player.getName(), name(team), color(team),
                target, color(targetTeam), detail);
    }

    public void admin(String arena, String actor, String action, String detail) {
        log.add(arena, Type.ADMIN, actor, null, null, action, null, detail);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinArenaEvent e) {
        add(e.getArena(), e.isRejoin() ? Type.REJOIN : Type.JOIN, e.getPlayer(), teamOf(e.getArena(), e.getPlayer()), null, null, null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(PlayerLeaveArenaEvent e) {
        add(e.getArena(), Type.LEAVE, e.getPlayer(), teamOf(e.getArena(), e.getPlayer()), null, null, e.getReason().name());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(ArenaChatEvent e) {
        add(e.getArena(), e.isTeamChat() ? Type.TEAM_CHAT : Type.CHAT, e.getPlayer(), e.getTeam(), null, null, e.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(PlayerKillEvent e) {
        ArenaView arena = e.getArena();
        TeamView victimTeam = teamOf(arena, e.getVictim());
        String cause = e.getCause() == null ? null : e.getCause().name();
        if (e.getKiller() == null) {
            add(arena, Type.DEATH, e.getVictim(), victimTeam, null, null, (e.isFinalKill() ? "FINAL " : "") + cause);
        } else {
            add(arena, e.isFinalKill() ? Type.FINAL_KILL : Type.KILL, e.getKiller(), teamOf(arena, e.getKiller()),
                    e.getVictim().getName(), victimTeam, cause);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBed(BedBreakEvent e) {
        add(e.getArena(), Type.BED, e.getBreaker(), teamOf(e.getArena(), e.getBreaker()), name(e.getTeam()), e.getTeam(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBeds(BedsDestroyedEvent e) {
        add(e.getArena(), Type.BEDS_DESTROYED, null, null, null, null,
                e.getTeams().stream().map(TeamView::getName).collect(Collectors.joining(", ")));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPhase(GamePhaseChangeEvent e) {
        add(e.getArena(), Type.PHASE, null, null, e.getNext(), null, e.isForced() ? "forced" : null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGenerator(GeneratorUpgradeEvent e) {
        add(e.getArena(), Type.GENERATOR, null, null, e.getKind().name(), null, String.valueOf(e.getLevel()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUpgrade(TeamUpgradeEvent e) {
        add(e.getArena(), Type.UPGRADE, e.getBuyer(), e.getTeam(), e.getUpgrade(), null, String.valueOf(e.getNewLevel()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeamOut(TeamEliminatedEvent e) {
        add(e.getArena(), Type.TEAM_OUT, null, null, name(e.getTeam()), e.getTeam(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStart(GameStartEvent e) {
        log.newGame(e.getArena().getName());
        add(e.getArena(), Type.START, null, null, null, null, String.valueOf(e.getArena().getPlayers().size()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnd(GameEndEvent e) {
        add(e.getArena(), Type.END, null, null, name(e.getWinner()), e.getWinner(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReset(ArenaResetEvent e) {
        if (e.getStage() == ArenaResetEvent.Stage.POST) log.markReset(e.getArena().getName());
    }
}
