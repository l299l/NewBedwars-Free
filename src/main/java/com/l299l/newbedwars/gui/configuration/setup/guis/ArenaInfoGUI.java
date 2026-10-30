package com.l299l.newbedwars.gui.configuration.setup.guis;

import com.l299l.newbedwars.gui.ItemBuilder;

import com.l299l.newbedwars.utils.TeamColor;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.setup.ArenaInfo;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

public class ArenaInfoGUI implements InventoryHolder {
    private final Inventory inv;
    private final IArena arena;
    private final Player viewer;
    private final Map<Integer, Team> slotToTeam = new HashMap<>();
    private final int page;
    static final int TEAMS_PER_PAGE = 8;

    public ArenaInfoGUI(Player viewer, IArena arena) {
        this(viewer, arena, 0);
    }

    public ArenaInfoGUI(Player viewer, IArena arena, int page) {
        this.viewer = viewer;
        this.arena = arena;
        this.page = page;
        boolean isAdvanced = arena.getSetupMode() == Setup.ADVANCED_SETUP;
        int totalPages = (int) Math.ceil(arena.getTeams().size() / (double) TEAMS_PER_PAGE);
        String title = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + text("Info-GuiTitle");
        if (totalPages > 1) {
            title += " " + text("Info-GuiPage").replace("/page/", String.valueOf(page + 1))
                    .replace("/pages/", String.valueOf(totalPages));
        }
        inv = Bukkit.createInventory(this, 54, title);
        init(isAdvanced);
    }

    private void init(boolean isAdvanced) {
        fillBorder();

        int slot = 9;

        slot = addItem(slot, Material.NAME_TAG, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-Name"),
                value() + ChatColor.WHITE + arena.getArenaName());

        slot = addItem(slot, Material.COMPASS, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-SetupMode"),
                value() + ChatColor.WHITE + arena.getSetupMode().name());

        slot = addItem(slot, Material.CLOCK, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-WaitingTime"),
                value() + ChatColor.WHITE + arena.getWaitingTime() + "s");

        slot = addItem(slot, Material.PLAYER_HEAD, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-MinPlayers"),
                value() + ChatColor.WHITE + arena.getMinPlayers());

        slot = addItem(slot, Material.GOLD_INGOT, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-MaxPerTeam"),
                value() + ChatColor.WHITE + arena.getMaxInTeam());

        slot = addItem(slot, Material.IRON_BARS, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-QuickVoidY"),
                value() + ArenaInfo.value(viewer, arena.getQuickVoidY()));

        slot = addItem(slot, Material.BEDROCK, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-WaitingSpawn"),
                value() + locStr(arena.getWaitingSpawn()));

        slot = addItem(slot, Material.RED_CONCRETE, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-WaitingPos1"),
                value() + locStr(arena.getWaitingPos1()));

        slot = addItem(slot, Material.GREEN_CONCRETE, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-WaitingPos2"),
                value() + locStr(arena.getWaitingPos2()));

        java.util.List<Team> allTeams = new java.util.ArrayList<>(arena.getTeams().values());
        int totalPages = (int) Math.ceil(allTeams.size() / (double) TEAMS_PER_PAGE);
        int start = page * TEAMS_PER_PAGE;
        int end = Math.min(start + TEAMS_PER_PAGE, allTeams.size());
        java.util.List<Team> pageTeams = allTeams.subList(start, end);

        int teamSlot = 18;
        for (int i = 0; i < pageTeams.size(); i++) {
            Team team = pageTeams.get(i);
            ItemStack teamItem = new ItemStack(TeamColor.wool(team.getColor()));
            ItemMeta teamMeta = teamItem.getItemMeta();
            teamMeta.setDisplayName(team.getColor() + "" + ChatColor.BOLD + team.getName());
            List<String> teamLore = new ArrayList<>();
            teamLore.add(ChatColor.GRAY + text("Info-TeamClick"));
            teamLore.add("");
            for (ArenaInfo.TeamSetting setting : ArenaInfo.teamSettings(team, isAdvanced)) {
                teamLore.add(ChatColor.GRAY + setting.label(viewer) + ": " + tick(setting.set()));
            }
            teamMeta.setLore(teamLore);
            teamItem.setItemMeta(teamMeta);
            inv.setItem(teamSlot + i * 2, teamItem);
            slotToTeam.put(teamSlot + i * 2, team);
        }

        if (totalPages > 1) {
            if (page > 0) {
                ItemStack prevItem = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevItem.getItemMeta();
                prevMeta.setDisplayName(ChatColor.GRAY + text("Info-PrevPage"));
                prevItem.setItemMeta(prevMeta);
                inv.setItem(34, prevItem);
            }
            if (page < totalPages - 1) {
                ItemStack nextItem = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextItem.getItemMeta();
                nextMeta.setDisplayName(ChatColor.GRAY + text("Info-NextPage"));
                nextItem.setItemMeta(nextMeta);
                inv.setItem(35, nextItem);
            }
        }

        int diamond = ArenaInfo.generatorCount(arena, GeneratorType.DIAMOND);
        int emerald = ArenaInfo.generatorCount(arena, GeneratorType.EMERALD);

        inv.setItem(39, createItem(Material.DIAMOND, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-DiamondGenerators"),
                ChatColor.GRAY + text("Info-Count") + ": " + ChatColor.WHITE + diamond));
        inv.setItem(41, createItem(Material.EMERALD, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-EmeraldGenerators"),
                ChatColor.GRAY + text("Info-Count") + ": " + ChatColor.WHITE + emerald));

        // We use slots 46-52 for advanced settings (only shown in ADVANCED_SETUP mode)
        if (isAdvanced) {
            int advSlot = 46;
            
            List<String> gamerulesLore = new ArrayList<>();
            ArenaInfo.gamerules(arena.getGamerules()).forEach((name, value) ->
                    gamerulesLore.add(ChatColor.GRAY + name + ": " + ArenaInfo.bool(viewer, value)));
            inv.setItem(advSlot++, createItem(Material.JIGSAW, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-Gamerules"), gamerulesLore));
            
            List<String> rpLore = new ArrayList<>();
            if (arena.getResourcePackUrl() != null) {
                rpLore.add(ChatColor.GRAY + "URL: " + ChatColor.WHITE + arena.getResourcePackUrl());
                if (arena.getResourcePackHash() != null) {
                    rpLore.add(ChatColor.GRAY + "Hash: " + ChatColor.WHITE + arena.getResourcePackHash());
                }
            } else {
                rpLore.add(ChatColor.RED + text("Info-NotSet"));
            }
            inv.setItem(advSlot++, createItem(Material.MUSIC_DISC_13, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-ResourcePack"), rpLore));
            
            List<String> shopGuiLore = new ArrayList<>();
            shopGuiLore.add(ChatColor.GRAY + text("Info-Shop") + ": " + ArenaInfo.value(viewer, arena.getShopGuiId()));
            shopGuiLore.add(ChatColor.GRAY + text("Info-Upgrades") + ": " + ArenaInfo.value(viewer, arena.getUpgradeGuiId()));
            inv.setItem(advSlot++, createItem(Material.CHEST, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-GUIs"), shopGuiLore));
            
            List<String> genLore = new ArrayList<>();
            GeneratorLeveling leveling = arena.getGeneratorsLeveling();
            genLore.add(ChatColor.GRAY + text("Info-Config") + ": " + ArenaInfo.value(viewer, leveling != null ? leveling.getId() : null));
            inv.setItem(advSlot++, createItem(Material.HOPPER, ChatColor.AQUA + "" + ChatColor.BOLD + text("Info-GeneratorLeveling"), genLore));
        }
        
        boolean canEnable = arena.canBeEnabled();
        inv.setItem(49, createItem(
                canEnable ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                canEnable ? ChatColor.GREEN + "" + ChatColor.BOLD + "✔ " + text("Info-ReadyShort")
                        : ChatColor.RED + "" + ChatColor.BOLD + "✘ " + text("Info-NotReadyShort"),
                canEnable ? ChatColor.GREEN + text("Info-ReadyHint").replace("/arena/", arena.getArenaName())
                        : ChatColor.RED + text("Info-NotReadyHint")));
    }

    private int addItem(int slot, Material material, String name, String... loreLines) {
        if (slot >= 54) return slot;
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(line);
        }
        inv.setItem(slot, createItem(material, name, lore));
        return slot + 1;
    }

    private void fillBorder() {
        Material border = Material.BLACK_STAINED_GLASS_PANE;
        for (int i = 0; i < 9; i++) inv.setItem(i, new ItemStack(border));
        for (int row = 1; row < 6; row++) {
            inv.setItem(row * 9, new ItemStack(border));
            inv.setItem(row * 9 + 8, new ItemStack(border));
        }
        inv.setItem(45, new ItemStack(border));
        inv.setItem(53, new ItemStack(border));
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        return ItemBuilder.of(material).name(name).lore(loreLines).build();
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        return ItemBuilder.of(material).name(name).lore(lore).build();
    }

    private String tick(boolean set) {
        return ArenaInfo.tick(set);
    }


    private String locStr(Location loc) {
        return ArenaInfo.loc(viewer, loc);
    }

    private String text(String key) {
        return ArenaInfo.text(viewer, key);
    }

    /** "Value: " in gray. */
    private String value() {
        return ChatColor.GRAY + text("Info-Value") + ": ";
    }

    public Team getTeam(int slot) {
        return slotToTeam.get(slot);
    }

    public IArena getArena() {
        return arena;
    }

    public Player getViewer() {
        return viewer;
    }

    public int getPage() {
        return page;
    }

    public int getTotalPages() {
        return (int) Math.ceil(arena.getTeams().size() / (double) TEAMS_PER_PAGE);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
