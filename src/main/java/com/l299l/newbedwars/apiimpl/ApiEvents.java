package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.api.ArenaView;
import com.l299l.newbedwars.api.TeamView;
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

/** The only place that builds and fires API events, so the rest of the plugin stays free of event details. */
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

    public static void gameStart(IArena arena) {
        call(new GameStartEvent(view(arena)));
    }

    public static void gameEnd(IArena arena, Team winner) {
        call(new GameEndEvent(view(arena), view(winner)));
    }

    public static void arenaReset(IArena arena, ArenaResetEvent.Stage stage) {
        call(new ArenaResetEvent(view(arena), stage));
    }

    /** @return false when a listener cancelled the join */
    public static boolean playerJoin(IArena arena, Player player, boolean rejoin) {
        return !call(new PlayerJoinArenaEvent(view(arena), player, rejoin)).isCancelled();
    }

    public static void playerLeave(IArena arena, Player player, LeaveReason reason) {
        call(new PlayerLeaveArenaEvent(view(arena), player, reason));
    }

    /** @return false when a listener cancelled the bed break */
    public static boolean bedBreak(IArena arena, Team team, Player breaker) {
        return !call(new BedBreakEvent(view(arena), view(team), breaker)).isCancelled();
    }

    public static void playerKill(IArena arena, Player victim, Player killer, boolean finalKill, DamageCause cause) {
        call(new PlayerKillEvent(view(arena), victim, killer, finalKill, cause));
    }

    /** @return false when a listener cancelled the purchase */
    public static boolean shopPurchase(IArena arena, Player player, String item, ShopPurchaseEvent.Kind kind, String priceType, int price) {
        return !call(new ShopPurchaseEvent(view(arena), player, item, kind, priceType, price)).isCancelled();
    }

    /** @return false when a listener cancelled the upgrade */
    public static boolean teamUpgrade(IArena arena, Team team, String upgrade, int newLevel, Player buyer) {
        return !call(new TeamUpgradeEvent(view(arena), view(team), upgrade, newLevel, buyer)).isCancelled();
    }

    /** @return the armor to apply, possibly replaced by a listener */
    public static ItemStack[] armorEquip(IArena arena, Player player, ItemStack[] armor) {
        return call(new ArmorEquipEvent(view(arena), player, armor)).getArmor();
    }
}
