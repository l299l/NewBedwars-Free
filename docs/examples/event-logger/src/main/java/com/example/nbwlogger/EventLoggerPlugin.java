package com.example.nbwlogger;

import com.l299l.newbedwars.api.view.*;
import com.l299l.newbedwars.api.edit.*;
import com.l299l.newbedwars.api.command.*;
import com.l299l.newbedwars.api.registry.*;
import com.l299l.newbedwars.api.*;
import com.l299l.newbedwars.api.events.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Logs every NewBedwars API event to the console (and optionally to chat of ops) so each event can be
 * checked in a real game. Use {@code /nbwlog chat} to toggle chat output and {@code /nbwlog dump} to print
 * what the read-only API sees right now.
 * <p>
 * It also exercises the registration API (phase action, shop logic, armor tier, upgrade, trap, subcommand),
 * the extra data and the arena editor; see README.md for how to check each.
 * <p>
 * Set {@link #CANCEL_TEST} to true to also check cancelling: it cancels shop purchases of "Wool" and bed
 * breaks of the team named "Red", and redirects nothing.
 */
public class EventLoggerPlugin extends JavaPlugin implements Listener {
    private static final boolean CANCEL_TEST = false;
    private boolean chat = true;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        NewBedwarsAPI api = NewBedwarsAPI.get();
        getLogger().info("API version of NewBedwars: " + api.getVersion());
        registerAll(api);
    }

    /** Every registration is removed again by NewBedwars when this plugin is disabled. */
    private void registerAll(NewBedwarsAPI api) {
        // Phase action: put "NBWLOG:hello" in a phase's actions in config.yml.
        api.registerPhaseAction(this, "NBWLOG", (arena, arg) -> {
            log("PhaseAction NBWLOG arena=" + arena.getName() + " phase=" + arena.getCurrentPhase() + " arg='" + arg + "'");
            arena.getPlayers().forEach(p -> p.sendMessage(ChatColor.AQUA + "[NBWLOG] " + arg));
        });

        // Shop logic: set "itemLogic: NBWLIGHTNING" on a shop item in items.yml.
        api.registerShopLogic(this, "NBWLIGHTNING", (player, arena) -> {
            player.getWorld().strikeLightningEffect(player.getLocation());
            log("ShopLogic NBWLIGHTNING used by " + player.getName() + " in " + arena.getName());
            return true;
        });

        // Armor tier: a shop item with onBuyEvent GIVE_ARMOR and e.g. TURTLE_HELMET gives the whole tier.
        api.registerArmorTier(this, new ArmorTierDefinition("NBWTEST", 2, Material.DIAMOND_BOOTS,
                Material.IRON_LEGGINGS, Material.GOLDEN_CHESTPLATE, Material.TURTLE_HELMET));

        // Upgrade (use "upg:NBWSPEED" in the upgrade GUI file) and trap ("upg:NBWBLIND").
        api.registerUpgrade(this, UpgradeDefinition.upgrade("NBWSPEED", 2, Material.SUGAR, List.of(2, 4),
                (player, team, level) -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, level - 1));
                    log("Upgrade effect NBWSPEED level " + level + " on " + player.getName());
                }).reapplyOnRespawn());
        api.registerUpgrade(this, UpgradeDefinition.trap("NBWBLIND", 1, Material.INK_SAC, List.of(1),
                (enemy, team, arena) -> {
                    enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                    log("Trap NBWBLIND triggered on " + enemy.getName() + " at team " + team.getName());
                }));

        // Subcommands: /bw nbwinfo (in a game) and /bw nbwlobby (lobby admins).
        api.registerCommand(this, new InfoCommand("nbwinfo"), CommandContext.GAME);
        api.registerCommand(this, new InfoCommand("nbwlobby"), CommandContext.LOBBY_ADMIN);
        log("Registered phase action, shop logic, armor tier, upgrade, trap and 2 commands");
    }

    /** Prints the arena's extra data and the team upgrade levels. */
    private final class InfoCommand implements AddonCommand {
        private final String name;

        InfoCommand(String name) {
            this.name = name;
        }

        @Override public String getName() { return name; }
        @Override public String getDescription() { return "Logger addon: shows extra data and upgrade levels"; }
        @Override public String getSyntax() { return "/bw " + name; }
        @Override public String getExample() { return "/bw " + name; }

        @Override
        public void perform(Player player, String[] args, ArenaView arena) {
            player.sendMessage("Command " + name + " works. Arena: " + (arena == null ? "none" : arena.getName()));
            if (arena != null) describe(player, arena);
        }
    }

    private void describe(CommandSender to, ArenaView arena) {
        ExtraData extra = arena.getExtra(this);
        to.sendMessage(" arena extra: games=" + extra.getInt("games", 0) + " keys=" + extra.keys());
        to.sendMessage(" generators: " + arena.getGenerators().stream()
                .map(g -> g.getKind() + "@" + fmt(g.getLocation())).collect(Collectors.joining(", ")));
        for (TeamView team : arena.getTeams()) {
            to.sendMessage("  " + team.getName() + " wins=" + team.getExtra(this).getInt("wins", 0)
                    + " NBWSPEED=" + team.getUpgradeLevel("NBWSPEED") + " NBWBLIND=" + team.getUpgradeLevel("NBWBLIND")
                    + " SHARPNESS=" + team.getUpgradeLevel("SHARPNESS"));
        }
    }

    private static String fmt(Location l) {
        return l == null ? "null" : l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    /**
     * /nbwlog edit: runs every editor method on the arena you stand in while it is WAITING, then undoes it.
     * It uses your location for all positions, so the arena data ends up as it was before.
     */
    private void runEditorTest(Player player) {
        NewBedwarsAPI api = NewBedwarsAPI.get();
        ArenaView arena = api.getArena(player.getWorld()).orElse(null);
        if (arena == null) {
            player.sendMessage("Stand in the arena world (setup or waiting) first.");
            return;
        }
        try {
            ArenaEditor editor = api.edit(arena);
            int generatorsBefore = arena.getGenerators().size();
            TeamEditor team = editor.createTeam("NbwTest", ChatColor.LIGHT_PURPLE);
            team.setSpawn(player.getLocation());
            team.setColor(ChatColor.AQUA);
            player.sendMessage("Created team NbwTest, teams now: " + arena.getTeams().stream().map(TeamView::getName).toList());
            editor.addGenerator(GeneratorKind.DIAMOND, player.getLocation());
            player.sendMessage("Generators " + generatorsBefore + " -> " + arena.getGenerators().size());
            player.sendMessage("removeGenerator: " + editor.removeGenerator(arena.getGenerators().size() - 1));
            player.sendMessage("removeTeam: " + editor.removeTeam("NbwTest"));
            editor.save();
            player.sendMessage("Editor OK. Teams: " + arena.getTeams().size() + ", generators: " + arena.getGenerators().size());
        } catch (RuntimeException ex) {
            player.sendMessage(ChatColor.RED + "Editor refused: " + ex);
        }
    }

    private void log(String text) {
        getLogger().info(text);
        if (!chat) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("nbwlogger.use")) p.sendMessage(ChatColor.DARK_GRAY + "[nbw] " + ChatColor.GRAY + text);
        }
    }

    private static String a(ArenaEvent e) {
        ArenaView arena = e.getArena();
        return arena.getName() + "(" + arena.getState() + ")";
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStart(GameStartEvent e) {
        String teams = e.getArena().getTeams().stream()
                .map(t -> t.getName() + "=" + t.getPlayers().size()).collect(Collectors.joining(", "));
        ExtraData extra = e.getArena().getExtra(this);
        extra.set("games", extra.getInt("games", 0) + 1);
        log("GameStart " + a(e) + " teams: " + teams + " (games played with this addon: " + extra.getInt("games", 0) + ")");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnd(GameEndEvent e) {
        TeamView w = e.getWinner();
        if (w != null) {
            ExtraData extra = w.getExtra(this);
            extra.set("wins", extra.getInt("wins", 0) + 1);
        }
        log("GameEnd " + a(e) + " winner=" + (w == null ? "DRAW" : w.getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReset(ArenaResetEvent e) {
        log("ArenaReset " + e.getStage() + " " + a(e));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinArenaEvent e) {
        log("Join " + e.getPlayer().getName() + " -> " + a(e) + " rejoin=" + e.isRejoin() + " cancelled=" + e.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(PlayerLeaveArenaEvent e) {
        log("Leave " + e.getPlayer().getName() + " <- " + a(e) + " reason=" + e.getReason());
    }

    @EventHandler
    public void onBed(BedBreakEvent e) {
        if (CANCEL_TEST && e.getTeam().getName().equalsIgnoreCase("Red")) e.setCancelled(true);
        log("BedBreak team=" + e.getTeam().getName() + " by " + e.getBreaker().getName() + " cancelled=" + e.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(PlayerKillEvent e) {
        String killer = e.getKiller() == null ? "none" : e.getKiller().getName();
        log("Kill victim=" + e.getVictim().getName() + " killer=" + killer + " final=" + e.isFinalKill() + " cause=" + e.getCause());
    }

    @EventHandler
    public void onShop(ShopPurchaseEvent e) {
        if (CANCEL_TEST && e.getItemName().equalsIgnoreCase("Wool")) e.setCancelled(true);
        log("Shop " + e.getPlayer().getName() + " buys " + e.getItemName() + " (" + e.getKind() + ") for "
                + e.getPrice() + " " + e.getPriceType() + " cancelled=" + e.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUpgrade(TeamUpgradeEvent e) {
        log("Upgrade team=" + e.getTeam().getName() + " " + e.getUpgrade() + " -> level " + e.getNewLevel()
                + " by " + e.getBuyer().getName() + " cancelled=" + e.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmor(ArmorEquipEvent e) {
        StringBuilder sb = new StringBuilder();
        for (var item : e.getArmor()) sb.append(item == null ? "-" : item.getType().name()).append(' ');
        log("ArmorEquip " + e.getPlayer().getName() + " [" + sb.toString().trim() + "]");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLobby(PlayerSendToLobbyEvent e) {
        log("SendToLobby " + e.getPlayer().getName() + " reason=" + e.getReason() + " cancelled=" + e.isCancelled());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("chat")) {
            chat = !chat;
            sender.sendMessage("Chat output: " + chat);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("edit")) {
            if (sender instanceof Player p) runEditorTest(p);
            else sender.sendMessage("Players only.");
            return true;
        }
        NewBedwarsAPI api = NewBedwarsAPI.get();
        sender.sendMessage("NewBedwars " + api.getVersion() + ", arenas: " + api.getArenas().size());
        for (ArenaView arena : api.getArenas()) {
            sender.sendMessage(" " + arena.getName() + " state=" + arena.getState() + " enabled=" + arena.isEnabled()
                    + " players=" + arena.getPlayers().size() + " spectators=" + arena.getSpectators().size()
                    + " phase=" + arena.getCurrentPhase() + " time=" + arena.getGameTime());
            for (TeamView team : arena.getTeams()) {
                sender.sendMessage("   " + team.getColor() + team.getName() + ChatColor.RESET + " alive=" + team.isAlive()
                        + " bedDestroyed=" + team.isBedDestroyed() + " players=" + team.getPlayers().size());
            }
            describe(sender, arena);
        }
        if (sender instanceof Player p) {
            api.getArena(p).ifPresentOrElse(
                    arena -> arena.getGamePlayer(p.getUniqueId()).ifPresent(gp -> sender.sendMessage("You: kills=" + gp.getKills()
                            + " finals=" + gp.getFinalKills() + " deaths=" + gp.getDeaths() + " beds=" + gp.getBedsBroken()
                            + " team=" + gp.getTeam().map(TeamView::getName).orElse("none"))),
                    () -> sender.sendMessage("You are not in an arena."));
        }
        return true;
    }
}
