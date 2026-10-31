package com.l299l.newbedwars.arena.group;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import com.l299l.newbedwars.version.VersionCompat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The vote menu: one icon per map that can be voted for, with live vote counts. */
public final class MapVoteGUI implements InventoryHolder {
    private final MapVoteManager manager;
    private final Player viewer;
    private final IArena host;
    private final Inventory inv;
    private final Map<Integer, VoteOption> slots = new HashMap<>();

    public MapVoteGUI(MapVoteManager manager, Player viewer, IArena host) {
        this.manager = manager;
        this.viewer = viewer;
        this.host = host;
        int count = Math.max(1, manager.allOptions(host).size());
        int size = Math.min(54, Math.max(9, (int) Math.ceil(count / 9.0) * 9));
        inv = Bukkit.createInventory(this, size, NewBedwars.plugin.getMessages().getMsg(viewer, "MapVoteGuiTitle"));
        render();
    }

    /** Redraws the icons (options and vote counts change while the menu is open). */
    public void render() {
        inv.clear();
        slots.clear();
        ArenaGroup group = manager.votingGroup(host);
        if (group == null) return;
        Material icon = Material.matchMaterial(group.getDisplayItem());
        if (icon == null || icon.isAir() || !icon.isItem()) icon = Material.PAPER;
        Messages msg = NewBedwars.plugin.getMessages();
        MapVote vote = manager.vote(host);
        String own = vote.voteOf(viewer.getUniqueId());
        int slot = 0;
        for (VoteOption option : manager.allOptions(host)) {
            if (slot >= inv.getSize()) break;
            boolean mine = option.matches(own);
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(replace(msg.getMsg(viewer, "MapVoteEntryName"), option, vote));
            List<String> lore = new ArrayList<>(List.of(replace(msg.getMsg(viewer, "MapVoteEntryLore"), option, vote).split("\n")));
            if (option.isRemote()) lore.add(org.bukkit.ChatColor.GRAY + msg.getMsg(viewer, "GuiServer") + ": " + org.bukkit.ChatColor.WHITE + option.server());
            lore.add(msg.getMsg(viewer, mine ? "MapVoteYourVote" : "MapVoteClickToVote"));
            meta.setLore(lore);
            if (mine) {
                meta.addEnchant(VersionCompat.GLOW_ENCHANT, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slots.put(slot, option);
            slot++;
        }
    }

    private static String replace(String text, VoteOption option, MapVote vote) {
        return text.replace("/arena/", option.arenaName())
                .replace("/votes/", String.valueOf(vote.votes(option.key())))
                .replace("/players/", String.valueOf(option.players()))
                .replace("/max/", String.valueOf(option.maxPlayers()));
    }

    public VoteOption optionAt(int slot) {
        return slots.get(slot);
    }

    /** The arena on this server at {@code slot}, or null (empty slot or an arena on another server). */
    public IArena arenaAt(int slot) {
        VoteOption o = slots.get(slot);
        return o == null ? null : o.localArena();
    }

    public IArena getHost() {
        return host;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
