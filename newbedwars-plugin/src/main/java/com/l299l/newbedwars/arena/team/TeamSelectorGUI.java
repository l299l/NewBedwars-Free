package com.l299l.newbedwars.arena.team;

import com.l299l.newbedwars.placeholder.MessageContext;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.utils.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TeamSelectorGUI implements InventoryHolder {
    private final TeamSelectorManager manager;
    private final Player viewer;
    private final IArena arena;
    private final Inventory inv;
    private final Map<Integer, String> slots = new HashMap<>();

    public TeamSelectorGUI(TeamSelectorManager manager, Player viewer, IArena arena) {
        this.manager = manager;
        this.viewer = viewer;
        this.arena = arena;
        int count = arena.getTeams().size() + 1;
        int size = Math.min(54, Math.max(9, (int) Math.ceil(count / 9.0) * 9));
        inv = Bukkit.createInventory(this, size, NewBedwars.plugin.getMessages().getMsg(viewer, "TeamSelectorGuiTitle"));
        render();
    }

    public void render() {
        inv.clear();
        slots.clear();
        Messages msg = NewBedwars.plugin.getMessages();
        String mine = manager.pickedTeam(viewer, arena);
        Map<String, Integer> counts = manager.pickedCounts(arena);
        List<Team> teams = new ArrayList<>(arena.getTeams().values());
        teams.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        int slot = 0;
        for (Team team : teams) {
            if (slot >= inv.getSize() - 1) break;
            String name = team.getName();
            int count = counts.getOrDefault(name, 0);
            ItemStack item = new ItemStack(TeamColor.wool(team.getColor()), Math.min(64, Math.max(1, count)));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(msg.getMsg(viewer, "TeamSelectorEntryName").replace("/team/", team.colored(team.getDisplayName())));
            List<String> lore = new ArrayList<>(List.of(msg.getMsg(viewer, "TeamSelectorEntryLore")
                    .replace("/players/", String.valueOf(count))
                    .replace("/max/", String.valueOf(arena.getMaxInTeam())).split("\n")));
            for (Player member : manager.pickedPlayers(arena, name)) {
                lore.add(msg.getMsg(viewer, "TeamSelectorMemberLine", MessageContext.about(member))
                        .replace("/player/", member.getName()));
            }
            lore.add("");
            String status;
            if (name.equals(mine)) {
                status = "TeamSelectorYourTeam";
            } else {
                TeamPicks.Result result = manager.check(viewer, arena, name);
                status = switch (result) {
                    case OK -> "TeamSelectorClickToJoin";
                    case FULL -> "TeamSelectorFull";
                    case UNBALANCED -> "TeamSelectorUnbalanced";
                    default -> "TeamSelectorUnknownTeam";
                };
            }
            if (status.equals("TeamSelectorUnknownTeam")) {
                lore.add(msg.getMsg(viewer, status).replace("/team/", team.getDisplayName()));
            } else {
                lore.add(msg.getMsg(viewer, status));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slots.put(slot, name);
            slot++;
        }
        ItemStack random = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = random.getItemMeta();
        meta.setDisplayName(msg.getMsg(viewer, "TeamSelectorRandomName"));
        meta.setLore(new ArrayList<>(List.of(msg.getMsg(viewer, "TeamSelectorRandomLore").split("\n"))));
        random.setItemMeta(meta);
        inv.setItem(inv.getSize() - 1, random);
    }

    public IArena getArena() {
        return arena;
    }

    public String teamAt(int rawSlot) {
        return slots.get(rawSlot);
    }

    public boolean isRandomSlot(int rawSlot) {
        return rawSlot == inv.getSize() - 1;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
