package com.l299l.newbedwars.gui.configuration.setup.guis;

import com.l299l.newbedwars.utils.TeamColor;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
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
    private final Map<Integer, Team> slotToTeam = new HashMap<>();
    private final int page;
    static final int TEAMS_PER_PAGE = 8;

    public ArenaInfoGUI(IArena arena) {
        this(arena, 0);
    }

    public ArenaInfoGUI(IArena arena, int page) {
        this.arena = arena;
        this.page = page;
        boolean isAdvanced = arena.getSetupMode() == Setup.ADVANCED_SETUP;
        int totalPages = (int) Math.ceil(arena.getTeams().size() / (double) TEAMS_PER_PAGE);
        String title = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Arena Setup Info";
        if (totalPages > 1) {
            title += " (Page " + (page + 1) + "/" + totalPages + ")";
        }
        inv = Bukkit.createInventory(this, 54, title);
        init(isAdvanced);
    }

    private void init(boolean isAdvanced) {
        // Border fill
        fillBorder();

        // Global info items
        int slot = 9;

        // Name
        slot = addItem(slot, Material.NAME_TAG, ChatColor.AQUA + "" + ChatColor.BOLD + "Name",
                ChatColor.GRAY + "Value:" + ChatColor.WHITE + " " + arena.getArenaName());

        // Setup mode
        slot = addItem(slot, Material.COMPASS, ChatColor.AQUA + "" + ChatColor.BOLD + "Setup Mode",
                ChatColor.GRAY + "Value:" + ChatColor.WHITE + " " + arena.getSetupMode().name());

        // Waiting time
        slot = addItem(slot, Material.CLOCK, ChatColor.AQUA + "" + ChatColor.BOLD + "Waiting Time",
                ChatColor.GRAY + "Value:" + ChatColor.WHITE + " " + arena.getWaitingTime() + "s");

        // Min players
        slot = addItem(slot, Material.PLAYER_HEAD, ChatColor.AQUA + "" + ChatColor.BOLD + "Min Players",
                ChatColor.GRAY + "Value:" + ChatColor.WHITE + " " + arena.getMinPlayers());

        // Max per team
        slot = addItem(slot, Material.GOLD_INGOT, ChatColor.AQUA + "" + ChatColor.BOLD + "Max Per Team",
                ChatColor.GRAY + "Value:" + ChatColor.WHITE + " " + arena.getMaxInTeam());

        // Quick void Y
        slot = addItem(slot, Material.IRON_BARS, ChatColor.AQUA + "" + ChatColor.BOLD + "Quick Void Y",
                ChatColor.GRAY + "Value:" + (arena.getQuickVoidY() != null
                        ? ChatColor.WHITE + String.valueOf(arena.getQuickVoidY())
                        : ChatColor.RED + "not set"));

        // Waiting spawn
        slot = addItem(slot, Material.BEDROCK, ChatColor.AQUA + "" + ChatColor.BOLD + "Waiting Spawn",
                ChatColor.GRAY + "Value:" + locStr(arena.getWaitingSpawn()));

        // Waiting pos1
        slot = addItem(slot, Material.RED_CONCRETE, ChatColor.AQUA + "" + ChatColor.BOLD + "Waiting Pos1",
                ChatColor.GRAY + "Value:" + locStr(arena.getWaitingPos1()));

        // Waiting pos2
        slot = addItem(slot, Material.GREEN_CONCRETE, ChatColor.AQUA + "" + ChatColor.BOLD + "Waiting Pos2",
                ChatColor.GRAY + "Value:" + locStr(arena.getWaitingPos2()));

        // Pagination: calculate teams for this page
        java.util.List<Team> allTeams = new java.util.ArrayList<>(arena.getTeams().values());
        int totalPages = (int) Math.ceil(allTeams.size() / (double) TEAMS_PER_PAGE);
        int start = page * TEAMS_PER_PAGE;
        int end = Math.min(start + TEAMS_PER_PAGE, allTeams.size());
        java.util.List<Team> pageTeams = allTeams.subList(start, end);

        // Team items in rows 3-4 (slots 18-33, 8 teams per page)
        int teamSlot = 18;
        for (int i = 0; i < pageTeams.size(); i++) {
            Team team = pageTeams.get(i);
            ItemStack teamItem = new ItemStack(TeamColor.wool(team.getColor()));
            ItemMeta teamMeta = teamItem.getItemMeta();
            teamMeta.setDisplayName(team.getColor() + "" + ChatColor.BOLD + team.getName());
            List<String> teamLore = new ArrayList<>();
            teamLore.add(ChatColor.GRAY + "Click to view team details");
            teamLore.add("");
            teamLore.add(ChatColor.GRAY + "Bed: " + tick(team.isBedSet()));
            teamLore.add(ChatColor.GRAY + "Spawn: " + tick(team.isSpawnSet()));
            teamLore.add(ChatColor.GRAY + "Shop: " + tick(team.isShopSet()));
            teamLore.add(ChatColor.GRAY + "Upgrades: " + tick(team.isUpgradesSet()));
            teamLore.add(ChatColor.GRAY + "Generator: " + tick(team.isGeneratorSet()));
            teamLore.add(ChatColor.GRAY + "BuildProt: " + tick(team.isBuildProtAreaPosSet()));
            teamLore.add(ChatColor.GRAY + "BaseArea: " + tick(team.isBasePosSet()));
            if (isAdvanced) {
                teamLore.add(ChatColor.GRAY + "Accessories: " + tick(team.isAccessoriesSet()));
            }
            teamMeta.setLore(teamLore);
            teamItem.setItemMeta(teamMeta);
            inv.setItem(teamSlot + i * 2, teamItem);
            slotToTeam.put(teamSlot + i * 2, team);
        }

        // Navigation arrow (slot 35) - shared for previous/next
        if (totalPages > 1) {
            if (page > 0) {
                ItemStack prevItem = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevItem.getItemMeta();
                prevMeta.setDisplayName(ChatColor.GRAY + "← Previous Page");
                prevItem.setItemMeta(prevMeta);
                inv.setItem(35, prevItem);
            } else if (page < totalPages - 1) {
                ItemStack nextItem = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextItem.getItemMeta();
                nextMeta.setDisplayName(ChatColor.GRAY + "Next Page →");
                nextItem.setItemMeta(nextMeta);
                inv.setItem(35, nextItem);
            }
        }

        // Generator info in row 5 (slots 36-44)
        int diamond = 0, emerald = 0;
        for (Generator g : arena.getGenerators()) {
            if (g.getType() == GeneratorType.DIAMOND) diamond++;
            else if (g.getType() == GeneratorType.EMERALD) emerald++;
        }

        inv.setItem(39, createItem(Material.DIAMOND, ChatColor.AQUA + "" + ChatColor.BOLD + "Diamond Generators",
                ChatColor.GRAY + "Count:" + ChatColor.WHITE + " " + diamond));
        inv.setItem(41, createItem(Material.EMERALD, ChatColor.AQUA + "" + ChatColor.BOLD + "Emerald Generators",
                ChatColor.GRAY + "Count:" + ChatColor.WHITE + " " + emerald));

        // Advanced settings in row 6 (slots 45-53, but border is at 45 and 53)
        // We use slots 46-52 for advanced settings (only shown in ADVANCED_SETUP mode)
        if (isAdvanced) {
            int advSlot = 46;
            
            // Gamerules summary
            Gamerules gamerules = arena.getGamerules();
            List<String> gamerulesLore = new ArrayList<>();
            gamerulesLore.add(ChatColor.GRAY + "RandomTeams: " + gameruleColor(gamerules.RandomTeams));
            gamerulesLore.add(ChatColor.GRAY + "AllowParties: " + gameruleColor(gamerules.AllowParties));
            gamerulesLore.add(ChatColor.GRAY + "AllowSpectators: " + gameruleColor(gamerules.AllowSpectators));
            gamerulesLore.add(ChatColor.GRAY + "AllowTeamChat: " + gameruleColor(gamerules.AllowTeamChat));
            gamerulesLore.add(ChatColor.GRAY + "AllowGlobalChat: " + gameruleColor(gamerules.AllowGlobalChat));
            gamerulesLore.add(ChatColor.GRAY + "AllowPrivateChat: " + gameruleColor(gamerules.AllowPrivateChat));
            gamerulesLore.add(ChatColor.GRAY + "AllowTeamDamage: " + gameruleColor(gamerules.AllowTeamDamage));
            gamerulesLore.add(ChatColor.GRAY + "MakeSwordsPermanent: " + gameruleColor(gamerules.MakeSwordsPermanent));
            gamerulesLore.add(ChatColor.GRAY + "AllowMultipleGolems: " + gameruleColor(gamerules.AllowMultipleGolems));
            gamerulesLore.add(ChatColor.GRAY + "SplitPartiesToStart: " + gameruleColor(gamerules.SplitPartiesToStart));
            inv.setItem(advSlot++, createItem(Material.JIGSAW, ChatColor.AQUA + "" + ChatColor.BOLD + "Gamerules", gamerulesLore));
            
            // Resource pack
            List<String> rpLore = new ArrayList<>();
            if (arena.getResourcePackUrl() != null) {
                rpLore.add(ChatColor.GRAY + "URL: " + ChatColor.WHITE + arena.getResourcePackUrl());
                if (arena.getResourcePackHash() != null) {
                    rpLore.add(ChatColor.GRAY + "Hash: " + ChatColor.WHITE + arena.getResourcePackHash());
                }
            } else {
                rpLore.add(ChatColor.RED + "Not set");
            }
            inv.setItem(advSlot++, createItem(Material.MUSIC_DISC_13, ChatColor.AQUA + "" + ChatColor.BOLD + "Resource Pack", rpLore));
            
            // Shop GUI
            List<String> shopGuiLore = new ArrayList<>();
            shopGuiLore.add(ChatColor.GRAY + "Shop: " + (arena.getShopGuiId() != null ? ChatColor.WHITE + arena.getShopGuiId() : ChatColor.RED + "not set"));
            shopGuiLore.add(ChatColor.GRAY + "Upgrades: " + (arena.getUpgradeGuiId() != null ? ChatColor.WHITE + arena.getUpgradeGuiId() : ChatColor.RED + "not set"));
            inv.setItem(advSlot++, createItem(Material.CHEST, ChatColor.AQUA + "" + ChatColor.BOLD + "GUIs", shopGuiLore));
            
            // Generator leveling
            List<String> genLore = new ArrayList<>();
            GeneratorLeveling leveling = arena.getGeneratorsLeveling();
            genLore.add(ChatColor.GRAY + "Config: " + (leveling != null ? ChatColor.WHITE + leveling.getId() : ChatColor.RED + "not set"));
            inv.setItem(advSlot++, createItem(Material.HOPPER, ChatColor.AQUA + "" + ChatColor.BOLD + "Generator Leveling", genLore));
        }
        
        // Enable status in bottom center
        boolean canEnable = arena.canBeEnabled();
        inv.setItem(49, createItem(
                canEnable ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                canEnable ? ChatColor.GREEN + "" + ChatColor.BOLD + "✔ Ready to enable"
                        : ChatColor.RED + "" + ChatColor.BOLD + "✘ Not ready",
                canEnable ? ChatColor.GREEN + "Use /bw enable " + arena.getArenaName()
                        : ChatColor.RED + "Complete all missing fields above"));
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
        // Top row
        for (int i = 0; i < 9; i++) inv.setItem(i, new ItemStack(border));
        // Left and right columns
        for (int row = 1; row < 6; row++) {
            inv.setItem(row * 9, new ItemStack(border));
            inv.setItem(row * 9 + 8, new ItemStack(border));
        }
        // Bottom row corners only (leave center open for advanced settings)
        inv.setItem(45, new ItemStack(border));
        inv.setItem(53, new ItemStack(border));
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (loreLines.length > 0) {
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(line);
            }
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String tick(boolean set) {
        return set ? ChatColor.GREEN + "✔" : ChatColor.RED + "✘";
    }

    private String gameruleColor(Boolean value) {
        if (value == null) return ChatColor.RED + "not set";
        return value ? ChatColor.GREEN + "true" : ChatColor.RED + "false";
    }

    private String locStr(Location loc) {
        if (loc == null) return ChatColor.RED + "not set";
        return ChatColor.WHITE + String.valueOf(loc.getBlockX()) + " " + loc.getBlockY() + " " + loc.getBlockZ();
    }

    public Team getTeam(int slot) {
        return slotToTeam.get(slot);
    }

    public IArena getArena() {
        return arena;
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
