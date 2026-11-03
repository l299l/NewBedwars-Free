package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.apiimpl.view.ApiArena;
import com.l299l.newbedwars.apiimpl.view.ApiArenaGroup;
import com.l299l.newbedwars.api.events.MapChosenEvent;
import com.l299l.newbedwars.api.events.MapVoteEvent;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.apiimpl.view.ApiTeam;
import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import com.l299l.newbedwars.api.events.ArenaResetEvent;
import com.l299l.newbedwars.api.events.ArmorEquipEvent;
import com.l299l.newbedwars.api.events.BedBreakEvent;
import com.l299l.newbedwars.api.events.GameEndEvent;
import com.l299l.newbedwars.api.events.GameStartEvent;
import com.l299l.newbedwars.api.events.LeaveReason;
import com.l299l.newbedwars.api.events.PlayerJoinArenaEvent;
import com.l299l.newbedwars.api.events.PlayerKillEvent;
import com.l299l.newbedwars.api.events.PlayerLeaveArenaEvent;
import com.l299l.newbedwars.api.events.ShopPurchaseEvent;
import com.l299l.newbedwars.api.events.TeamUpgradeEvent;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

public final class ApiEvents {
    private ApiEvents() {}

    private static ArenaView view(IArena arena) {
        return new ApiArena(arena);
    }

    private static TeamView view(Team team) {
        return team == null ? null : new ApiTeam(team);
    }

    private static <E extends Event> E call(E event) {
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    public static void networkArenasUpdate(java.util.Collection<com.l299l.newbedwars.api.view.NetworkArenaView> arenas) {
        call(new com.l299l.newbedwars.api.events.NetworkArenasUpdateEvent(java.util.Collections.unmodifiableCollection(arenas)));
    }

    public static boolean networkJoinRequest(Player player, com.l299l.newbedwars.common.protocol.payload.TargetKind kind, String target) {
        com.l299l.newbedwars.api.events.NetworkJoinRequestEvent.Kind k = switch (kind) {
            case ANY -> com.l299l.newbedwars.api.events.NetworkJoinRequestEvent.Kind.ANY;
            case GROUP -> com.l299l.newbedwars.api.events.NetworkJoinRequestEvent.Kind.GROUP;
            case ARENA -> com.l299l.newbedwars.api.events.NetworkJoinRequestEvent.Kind.ARENA;
            case REJOIN -> com.l299l.newbedwars.api.events.NetworkJoinRequestEvent.Kind.REJOIN;
            case SPECTATE -> com.l299l.newbedwars.api.events.NetworkJoinRequestEvent.Kind.SPECTATE;
        };
        return !call(new com.l299l.newbedwars.api.events.NetworkJoinRequestEvent(player, k, target)).isCancelled();
    }

    public static void gameStart(IArena arena) {
        call(new GameStartEvent(view(arena)));
    }

    public static void gameEnd(IArena arena, Team winner) {
        call(new GameEndEvent(view(arena), view(winner)));
    }

    public static void arenaReset(IArena arena, ArenaResetEvent.Stage stage) {
        call(new ArenaResetEvent(view(arena), stage));
    }

    public static boolean playerJoin(IArena arena, Player player, boolean rejoin) {
        return !call(new PlayerJoinArenaEvent(view(arena), player, rejoin)).isCancelled();
    }

    public static boolean mapVote(IArena host, Player player, IArena voted) {
        return !call(new MapVoteEvent(view(host), player, view(voted))).isCancelled();
    }

    public static boolean mapVote(IArena host, Player player, com.l299l.newbedwars.arena.group.VoteOption voted) {
        if (!voted.isRemote()) return mapVote(host, player, voted.localArena());
        return !call(new MapVoteEvent(view(host), player, null, voted.remoteArena())).isCancelled();
    }

    public static void mapChosen(IArena host, ArenaGroup group, IArena chosen) {
        call(new MapChosenEvent(view(host), new ApiArenaGroup(group), view(chosen)));
    }

    public static void mapChosen(IArena host, ArenaGroup group, com.l299l.newbedwars.arena.group.VoteOption chosen) {
        if (!chosen.isRemote()) {
            mapChosen(host, group, chosen.localArena());
            return;
        }
        call(new MapChosenEvent(view(host), new ApiArenaGroup(group), null, chosen.remoteArena()));
    }

    public static void playerLeave(IArena arena, Player player, LeaveReason reason) {
        call(new PlayerLeaveArenaEvent(view(arena), player, reason));
    }

    public static boolean bedBreak(IArena arena, Team team, Player breaker) {
        return !call(new BedBreakEvent(view(arena), view(team), breaker)).isCancelled();
    }

    public static void playerKill(IArena arena, Player victim, Player killer, boolean finalKill, DamageCause cause) {
        call(new PlayerKillEvent(view(arena), victim, killer, finalKill, cause));
    }

    public static boolean shopPurchase(IArena arena, Player player, String item, ShopPurchaseEvent.Kind kind, String priceType, int price) {
        return !call(new ShopPurchaseEvent(view(arena), player, item, kind, priceType, price)).isCancelled();
    }

    public static boolean teamUpgrade(IArena arena, Team team, String upgrade, int newLevel, Player buyer) {
        return !call(new TeamUpgradeEvent(view(arena), view(team), upgrade, newLevel, buyer)).isCancelled();
    }

    public static ItemStack[] armorEquip(IArena arena, Player player, ItemStack[] armor) {
        return call(new ArmorEquipEvent(view(arena), player, armor)).getArmor();
    }
}
