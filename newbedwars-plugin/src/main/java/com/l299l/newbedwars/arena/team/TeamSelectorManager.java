package com.l299l.newbedwars.arena.team;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.utils.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TeamSelectorManager implements Listener {
    public static final String PERMISSION = "newbedwars.teamselect";
    private static TeamSelectorManager instance;

    public TeamSelectorManager() {
        instance = this;
    }

    public static TeamSelectorManager get() {
        return instance;
    }

    public static void joined(IArena arena, Player player) {
        if (instance == null) return;
        if (instance.enabled(arena) && allowed(player)) instance.giveItem(player, arena);
        instance.refreshMenus(arena);
    }

    public static void refresh(IArena arena) {
        if (instance != null) instance.refreshMenus(arena);
    }

    public boolean enabled(IArena arena) {
        return arena != null && arena.getGamerules() != null && Boolean.TRUE.equals(arena.getGamerules().TeamSelection);
    }

    private static boolean allowed(Player p) {
        return p.hasPermission(PERMISSION) || p.isOp();
    }

    private static boolean waiting(IArena arena) {
        return arena.status() == GameStatus.waiting || arena.status() == GameStatus.starting;
    }

    private boolean balanced(IArena arena) {
        return !Boolean.FALSE.equals(arena.getGamerules().TeamSelectionBalanced);
    }

    public boolean canPickNow(Player p, IArena arena) {
        return enabled(arena) && allowed(p) && arena.isPlayerInArena(p) && waiting(arena);
    }

    private boolean usable(Player p, IArena arena) {
        Messages msg = NewBedwars.plugin.getMessages();
        if (!enabled(arena)) {
            msg.send(p, "TeamSelectorDisabled");
            return false;
        }
        if (!allowed(p)) {
            msg.send(p, "TeamSelectorNoPermission");
            return false;
        }
        if (!arena.isPlayerInArena(p) || !waiting(arena)) {
            msg.send(p, "TeamSelectorNotNow");
            return false;
        }
        return true;
    }

    public void open(Player p, IArena arena) {
        if (!usable(p, arena)) return;
        p.openInventory(new TeamSelectorGUI(this, p, arena).getInventory());
    }

    public String pickedTeam(Player p, IArena arena) {
        if (arena == null || !arena.isPlayerInArena(p)) return null;
        TeamPicks picks = arena.getTeamPicks();
        for (Map.Entry<UUID, String> pick : picks.all().entrySet()) {
            Player picker = Bukkit.getPlayer(pick.getKey());
            if (picker == null) continue;
            for (Player member : arena.pickGroup(picker)) {
                if (member.getUniqueId().equals(p.getUniqueId())) return pick.getValue();
            }
        }
        return null;
    }

    private Map<String, Integer> counts(IArena arena, UUID excluded) {
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<UUID, String> pick : arena.getTeamPicks().all().entrySet()) {
            if (pick.getKey().equals(excluded)) continue;
            Player picker = Bukkit.getPlayer(pick.getKey());
            if (picker == null) continue;
            int size = arena.pickGroup(picker).size();
            if (size > 0) counts.merge(pick.getValue(), size, Integer::sum);
        }
        return counts;
    }

    public Map<String, Integer> pickedCounts(IArena arena) {
        return counts(arena, null);
    }

    public List<Player> pickedPlayers(IArena arena, String team) {
        List<Player> result = new ArrayList<>();
        for (Map.Entry<UUID, String> pick : arena.getTeamPicks().all().entrySet()) {
            if (!pick.getValue().equals(team)) continue;
            Player picker = Bukkit.getPlayer(pick.getKey());
            if (picker == null) continue;
            for (Player member : arena.pickGroup(picker)) {
                if (!result.contains(member)) result.add(member);
            }
        }
        return result;
    }

    public TeamPicks.Result check(Player viewer, IArena arena, String team) {
        int size = Math.max(1, arena.pickGroup(viewer).size());
        return TeamPicks.canPick(team, arena.getTeams().keySet(), size,
                counts(arena, viewer.getUniqueId()), arena.getMaxInTeam(), balanced(arena));
    }

    public boolean pick(Player p, IArena arena, String teamOrNull) {
        if (!usable(p, arena)) return false;
        Messages msg = NewBedwars.plugin.getMessages();
        List<Player> group = arena.pickGroup(p);
        if (group.isEmpty()) {
            msg.send(p, "TeamSelectorLeaderOnly");
            return false;
        }
        TeamPicks picks = arena.getTeamPicks();
        String previousName = picks.get(p.getUniqueId());
        Team previous = previousName == null ? null : arena.getTeams().get(previousName);
        if (teamOrNull == null) {
            if (previousName != null && !ApiEvents.teamPick(arena, p, null, previous, group)) return false;
            picks.clear(p.getUniqueId());
            for (Player member : group) {
                msg.send(member, "TeamSelectorCleared");
                updateItem(member, arena);
            }
            changed(arena);
            return true;
        }
        if (teamOrNull.equals(previousName)) {
            sendPicked(group, arena.getTeams().get(teamOrNull));
            return true;
        }
        TeamPicks.Result result = TeamPicks.canPick(teamOrNull, arena.getTeams().keySet(), group.size(),
                counts(arena, p.getUniqueId()), arena.getMaxInTeam(), balanced(arena));
        switch (result) {
            case UNKNOWN_TEAM -> {
                p.sendMessage(msg.getMsg(p, "TeamSelectorUnknownTeam").replace("/team/", teamOrNull));
                return false;
            }
            case FULL -> {
                msg.send(p, group.size() > 1 ? "TeamSelectorPartyTooBig" : "TeamSelectorTeamFull");
                return false;
            }
            case UNBALANCED -> {
                msg.send(p, "TeamSelectorTeamUnbalanced");
                return false;
            }
            default -> {
            }
        }
        Team team = arena.getTeams().get(teamOrNull);
        if (!ApiEvents.teamPick(arena, p, team, previous, group)) return false;
        picks.set(p.getUniqueId(), teamOrNull);
        sendPicked(group, team);
        changed(arena);
        return true;
    }

    private void sendPicked(List<Player> group, Team team) {
        for (Player member : group) {
            member.sendMessage(NewBedwars.plugin.getMessages().getMsg(member, "TeamSelectorPicked")
                    .replace("/team/", team.colored(team.getDisplayName())));
            updateItem(member, team.getArena());
        }
    }

    private void changed(IArena arena) {
        refreshMenus(arena);
        arena.onTeamPicksChanged();
    }

    public void refreshMenus(IArena arena) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof TeamSelectorGUI gui && gui.getArena() == arena) {
                gui.render();
            }
        }
    }

    static int itemSlot() {
        Integer slot = Properties.TeamSelectorItemSlot;
        return slot == null || slot < 0 || slot > 7 ? 0 : slot;
    }

    private static Material baseMaterial() {
        Material m = Material.matchMaterial(Properties.TeamSelectorItemMaterial == null ? "" : Properties.TeamSelectorItemMaterial);
        return m == null || m.isAir() ? Material.WHITE_WOOL : m;
    }

    private void giveItem(Player p, IArena arena) {
        String picked = pickedTeam(p, arena);
        Team team = picked == null ? null : arena.getTeams().get(picked);
        Messages messages = NewBedwars.plugin.getMessages();
        ItemStack item = new ItemStack(team == null ? baseMaterial() : TeamColor.wool(team.getColor()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(messages.getMsg(p, "TeamSelectorItemName"));
            meta.setLore(Arrays.asList(messages.getMsg(p, "TeamSelectorItemLore").split("\n")));
            item.setItemMeta(meta);
        }
        p.getInventory().setItem(itemSlot(), item);
    }

    private void updateItem(Player p, IArena arena) {
        if (enabled(arena) && allowed(p) && arena.isPlayerInArena(p) && waiting(arena)) giveItem(p, arena);
    }

    @EventHandler
    public void onUseItem(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null || !enabled(arena) || !arena.isPlayerInArena(p) || !waiting(arena)) return;
        if (p.getInventory().getHeldItemSlot() != itemSlot()) return;
        ItemStack held = p.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) return;
        e.setCancelled(true);
        open(p, arena);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof TeamSelectorGUI gui)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getView().getTopInventory()) return;
        if (gui.isRandomSlot(e.getRawSlot())) {
            pick(p, gui.getArena(), null);
            return;
        }
        String team = gui.teamAt(e.getRawSlot());
        if (team != null) pick(p, gui.getArena(), team);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof TeamSelectorGUI) e.setCancelled(true);
    }
}
