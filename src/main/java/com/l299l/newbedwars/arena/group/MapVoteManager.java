package com.l299l.newbedwars.arena.group;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.parties.Party;
import com.l299l.newbedwars.parties.PartyManager;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Next-map voting at the end of a game in a grouped arena: during the after-game watch time everyone still in the
 * arena gets a vote item; when the time is up the arena stops as usual and everyone is put into the waiting lobby
 * of the arena that won.
 */
public final class MapVoteManager implements Listener {
    private static MapVoteManager instance;
    private final Map<String, MapVote> votes = new HashMap<>();
    private final Random random = new Random();

    public MapVoteManager() {
        instance = this;
    }

    public static MapVoteManager get() {
        return instance;
    }

    // ---- Static hooks called by Arena (no-ops without a manager, e.g. in unit tests) ---------------------

    /** The game ended (start of the watch time): open the vote and hand out the vote items. */
    public static void gameEnded(IArena arena) {
        if (instance != null) instance.onGameEnded(arena);
    }

    /** Someone started spectating during the watch time: they can vote (and will move) too. */
    public static void spectatorJoined(IArena arena, Player player) {
        if (instance != null && instance.isOpen(arena)) giveItem(player);
    }

    public static void playerLeft(IArena arena, Player player) {
        if (instance != null) instance.onLeave(arena, player);
    }

    /**
     * The watch time is over: picks the winner. Call before {@code stop()} and run the returned task after it,
     * which puts everyone into the winner's waiting lobby. Null when there is nothing to do.
     */
    public static Runnable closeEndVote(IArena arena) {
        return instance == null ? null : instance.onClose(arena);
    }

    public static void resetArena(IArena arena) {
        if (instance != null) instance.votes.remove(key(arena));
    }

    /** Forgets every vote (plugin reload). */
    public void clear() {
        votes.clear();
    }

    // ---- State --------------------------------------------------------------------------------------------

    private static String key(IArena arena) {
        return arena.getArenaName().toLowerCase(Locale.ROOT);
    }

    /** The group of {@code arena} when it votes (in a group of 2+ arenas and gamerule {@code MapVote} on), else null. */
    public ArenaGroup votingGroup(IArena arena) {
        ArenaGroupManager groups = ArenaGroupManager.get();
        if (groups == null) return null;
        ArenaGroup group = groups.groupOf(arena);
        if (group == null || group.getArenas().size() < 2) return null;
        if (arena.getGamerules() != null && Boolean.FALSE.equals(arena.getGamerules().MapVote)) return null;
        return group;
    }

    public MapVote vote(IArena ended) {
        return votes.computeIfAbsent(key(ended), k -> new MapVote());
    }

    /** Whether {@code ended} is in its watch time with an open vote. */
    public boolean isOpen(IArena ended) {
        MapVote v = votes.get(key(ended));
        return v != null && !v.isClosed() && ended.status() == GameStatus.ending;
    }

    /**
     * Arenas the players of {@code ended} can vote for: the other enabled, waiting arenas of the group with room
     * for everyone who would move. The ended arena itself is not offered (its map is reset).
     */
    public List<IArena> options(IArena ended) {
        ArenaGroup group = votingGroup(ended);
        List<IArena> options = new ArrayList<>();
        if (group == null) return options;
        int moving = voters(ended).size();
        for (IArena a : ArenaGroupManager.arenas(group)) {
            if (a == ended || !Boolean.TRUE.equals(a.isEnabled())) continue;
            if (a.status() != GameStatus.waiting && a.status() != GameStatus.starting) continue;
            if (ArenaJoinService.freeSlots(a) < moving) continue;
            options.add(a);
        }
        return options;
    }

    /** Everyone still in the ended arena (all players are spectators during the watch time), each once. */
    private static List<Player> voters(IArena arena) {
        List<Player> result = new ArrayList<>();
        for (Player p : arena.getSpectators()) {
            if (p == null || !p.isOnline() || result.contains(p)) continue;
            if (arena.getArenaWorld() != null && p.getWorld() != arena.getArenaWorld()) continue;
            result.add(p);
        }
        return result;
    }

    private static List<String> names(List<IArena> arenas) {
        List<String> names = new ArrayList<>();
        for (IArena a : arenas) names.add(a.getArenaName());
        return names;
    }

    // ---- Lifecycle ----------------------------------------------------------------------------------------

    private void onGameEnded(IArena arena) {
        votes.remove(key(arena));
        if (votingGroup(arena) == null) return;
        vote(arena);
        for (Player p : voters(arena)) giveItem(p);
        arena.broadcast("MapVoteOpen");
    }

    private void onLeave(IArena arena, Player player) {
        MapVote v = votes.get(key(arena));
        if (v == null) return;
        v.removeVoter(player.getUniqueId());
        refreshMenus(arena);
    }

    private Runnable onClose(IArena ended) {
        MapVote v = votes.remove(key(ended));
        ArenaGroup group = votingGroup(ended);
        if (v == null || v.isClosed() || group == null) return null;
        v.close();
        List<Player> movers = voters(ended);
        for (Player p : movers) {
            removeItem(p);
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof MapVoteGUI) p.closeInventory();
        }
        List<IArena> options = options(ended);
        String winnerName = v.winner(names(options), random);
        IArena winner = winnerName == null ? null : ArenaJoinService.arenaByName(winnerName);
        if (winner == null || movers.isEmpty()) {
            ended.broadcast("MapVoteNoArena");
            return null;
        }
        ApiEvents.mapChosen(ended, group, winner);
        final String chosen = winner.getArenaName();
        ended.broadcast("MapVoteChosen", new HashMap<>() {{ put("/arena/", chosen); }});
        return () -> moveAll(movers, winner);
    }

    /** Puts {@code movers} (now in the lobby) into {@code target}, keeping parties together. */
    private static void moveAll(List<Player> movers, IArena target) {
        Messages msg = NewBedwars.plugin.getMessages();
        PartyManager pm = NewBedwars.plugin.getPartyManager();
        Set<UUID> handled = new HashSet<>();
        for (Player p : movers) {
            if (!p.isOnline() || !handled.add(p.getUniqueId())) continue;
            Party party = pm == null ? null : pm.getParty(p);
            if (party != null && Boolean.TRUE.equals(target.getGamerules().AllowParties)) {
                List<Player> together = new ArrayList<>();
                for (Player other : movers) {
                    if (other.isOnline() && party.equals(pm.getParty(other))) {
                        together.add(other);
                        handled.add(other.getUniqueId());
                    }
                }
                if (!target.joinParty(together)) together.forEach(o -> msg.send(o, "MapVoteNoRoom"));
                continue;
            }
            if (!target.join(p)) msg.send(p, "MapVoteNoRoom");
        }
    }

    // ---- Voting -------------------------------------------------------------------------------------------

    /** {@code player} (in the ended arena {@code ended}) votes for {@code choice}. */
    public void castVote(Player player, IArena ended, IArena choice) {
        Messages msg = NewBedwars.plugin.getMessages();
        MapVote v = votes.get(key(ended));
        if (v == null || !isOpen(ended)) {
            msg.send(player, "MapVoteClosed");
            return;
        }
        if (!options(ended).contains(choice)) {
            msg.send(player, "MapVoteUnavailable");
            return;
        }
        if (choice.getArenaName().equalsIgnoreCase(v.voteOf(player.getUniqueId()))) return;
        if (!ApiEvents.mapVote(ended, player, choice)) return;
        v.vote(player.getUniqueId(), choice.getArenaName());
        int count = v.votes(choice.getArenaName());
        ended.broadcast("MapVoted", new HashMap<>() {{
            put("/player/", player.getName());
            put("/arena/", choice.getArenaName());
            put("/votes/", String.valueOf(count));
        }});
        refreshMenus(ended);
    }

    private void refreshMenus(IArena ended) {
        for (Player p : voters(ended)) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof MapVoteGUI gui) gui.render();
        }
    }

    // ---- Vote item ----------------------------------------------------------------------------------------

    private static Material itemMaterial() {
        Material m = Material.matchMaterial(Properties.MapVoteItemMaterial);
        return m == null || m.isAir() ? Material.PAPER : m;
    }

    private static void giveItem(Player p) {
        Messages messages = NewBedwars.plugin.getMessages();
        ItemStack item = new ItemStack(itemMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(messages.getMsg(p, "MapVoteItemName"));
        meta.setLore(Arrays.asList(messages.getMsg(p, "MapVoteItemLore").split("\n")));
        item.setItemMeta(meta);
        p.getInventory().setItem(itemSlot(), item);
    }

    /** The configured slot, or 2 when it is invalid or taken by a spectator item (compass 0, effects 4, leave 8). */
    static int itemSlot() {
        int slot = Properties.MapVoteItemSlot == null ? 2 : Properties.MapVoteItemSlot;
        return slot < 0 || slot > 8 || slot == 0 || slot == 4 || slot == 8 ? 2 : slot;
    }

    private static void removeItem(Player p) {
        Material m = itemMaterial();
        for (int i = 0; i < 9; i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && item.getType() == m) p.getInventory().setItem(i, null);
        }
    }

    @EventHandler
    public void onUseItem(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        IArena arena = Arena.arenaByWorld.get(p.getWorld());
        if (arena == null || !arena.getSpectators().contains(p) || !isOpen(arena)) return;
        if (p.getInventory().getItemInMainHand().getType() != itemMaterial()) return;
        e.setCancelled(true);
        p.openInventory(new MapVoteGUI(this, p, arena).getInventory());
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof MapVoteGUI gui)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getView().getTopInventory()) return;
        IArena choice = gui.arenaAt(e.getRawSlot());
        if (choice != null) castVote(p, gui.getHost(), choice);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof MapVoteGUI) e.setCancelled(true);
    }
}
