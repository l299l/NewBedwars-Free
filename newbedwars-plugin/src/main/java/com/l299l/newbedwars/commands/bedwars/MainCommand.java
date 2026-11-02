package com.l299l.newbedwars.commands.bedwars;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.commands.bedwars.admin.ArenaCommand;
import com.l299l.newbedwars.commands.bedwars.admin.ManageGameCommand;
import com.l299l.newbedwars.commands.bedwars.admin.NpcCommand;
import com.l299l.newbedwars.commands.bedwars.admin.GroupCommand;
import com.l299l.newbedwars.commands.bedwars.admin.ReloadCommand;
import com.l299l.newbedwars.commands.bedwars.admin.SetLobbyCommand;
import com.l299l.newbedwars.commands.bedwars.admin.setup.advanced.*;
import com.l299l.newbedwars.commands.bedwars.admin.setup.automatic.StartAutomaticSetupCommand;
import com.l299l.newbedwars.commands.bedwars.admin.setup.basic.*;
import com.l299l.newbedwars.commands.bedwars.admin.setup.normal.*;
import com.l299l.newbedwars.commands.bedwars.main.HelpCommand;
import com.l299l.newbedwars.commands.bedwars.main.JoinCommand;
import com.l299l.newbedwars.commands.bedwars.main.LangCommand;
import com.l299l.newbedwars.commands.bedwars.main.LeaderboardCommand;
import com.l299l.newbedwars.commands.bedwars.main.LobbyCommand;
import com.l299l.newbedwars.commands.bedwars.main.ProfileCommand;
import com.l299l.newbedwars.commands.bedwars.main.RejoinCommand;
import com.l299l.newbedwars.commands.bedwars.main.SpectateCommand;
import com.l299l.newbedwars.commands.bedwars.main.StatsCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainCommand implements TabExecutor {
    private final NewBedwars plugin;
    private final Messages msg;
    private final ArrayList<SubCommand> subCommandsLobbyPublic = new ArrayList<>();
    private final ArrayList<SubCommand> subCommandsLobbyAdmin = new ArrayList<>();
    private final ArrayList<SubCommand> subCommandsGameAdmin = new ArrayList<>();
    private final ArrayList<SubCommand> subCommandsGamePublic = new ArrayList<>();
    private final ArrayList<SubCommand> setupSubBasicCommands = new ArrayList<>();
    private final ArrayList<SubCommand> setupSubNormalCommands = new ArrayList<>();
    private final ArrayList<SubCommand> setupSubAdvancedCommands = new ArrayList<>();
    private final ArrayList<SubCommand> setupSubAutomaticCommands = new ArrayList<>();
    private HelpCommand helpCommand;

    private record Route(List<SubCommand> publicCmds, List<SubCommand> adminCmds, boolean adminNeedsArena) {
        List<SubCommand> all() {
            List<SubCommand> all = new ArrayList<>(publicCmds);
            if (adminCmds != null) all.addAll(adminCmds);
            return all;
        }
    }

    /** Where a registered command is available, mirroring the command buckets. */
    public enum Context {
        LOBBY, LOBBY_ADMIN, GAME, GAME_ADMIN, SETUP_BASIC, SETUP_NORMAL, SETUP_ADVANCED, SETUP_AUTOMATIC
    }

    private ArrayList<SubCommand> bucket(Context context) {
        return switch (context) {
            case LOBBY -> subCommandsLobbyPublic;
            case LOBBY_ADMIN -> subCommandsLobbyAdmin;
            case GAME -> subCommandsGamePublic;
            case GAME_ADMIN -> subCommandsGameAdmin;
            case SETUP_BASIC -> setupSubBasicCommands;
            case SETUP_NORMAL -> setupSubNormalCommands;
            case SETUP_ADVANCED -> setupSubAdvancedCommands;
            case SETUP_AUTOMATIC -> setupSubAutomaticCommands;
        };
    }

    /**
     * Adds a subcommand to a context.
     * @throws IllegalArgumentException when a command with that name already exists in any context
     */
    public void register(SubCommand command, Context context) {
        for (Context c : Context.values()) {
            for (SubCommand existing : bucket(c)) {
                if (existing.getName().equalsIgnoreCase(command.getName())) {
                    throw new IllegalArgumentException("command '" + command.getName() + "' already exists (" + c + ")");
                }
            }
        }
        bucket(context).add(command);
    }

    public boolean unregister(SubCommand command) {
        boolean removed = false;
        for (Context c : Context.values()) removed |= bucket(c).remove(command);
        return removed;
    }

    public MainCommand() {
        plugin = NewBedwars.plugin;
        msg = plugin.getMessages();
        setSubCommands();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(msg.getMsgToConsole("OnlyPlayer"));
            return true;
        }

        if (args.length > 0) {
            Setup setupMode = getArenaSetupMode(p);
            if (setupMode == null || setupMode == Setup.NO_SETUP || setupMode == Setup.READY) {
                Route route = routeFor(p);
                boolean handled = route.adminCmds() != null && subCmd(route.adminCmds(), args, p, route.adminNeedsArena());
                if (!handled) {
                    subCmd(route.publicCmds(), args, p, false);
                }
            } else {
                if (!isAdmin(p) && !p.hasPermission("newbedwars.bw.arena")) {
                    msg.send(p, "NoPermissions");
                    return true;
                }
                subCmd(setupCommandsFor(setupMode), args, p, true);
            }
        } else {
            helpCommand.perform(p, args, null);
        }
        return true;
    }

    private boolean isAdmin(Player p) {
        return p.hasPermission("newbedwars.bw.admin") || p.isOp();
    }

    private boolean isSpectatorInArena(Player p) {
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        return arena != null && arena.getSpectators().contains(p);
    }

    private boolean hasAnyLobbyAdminPermission(Player p) {
        return isAdmin(p)
                || p.hasPermission("newbedwars.bw.arena")
                || p.hasPermission("newbedwars.bw.setlobby")
                || p.hasPermission("newbedwars.bw.npc")
                || p.hasPermission("newbedwars.bw.group");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player p)) return null;
        Setup setupMode = getArenaSetupMode(p);
        String typed = args.length > 0 ? args[0].toLowerCase() : "";

        if (args.length == 1) {
            if (setupMode == null || setupMode == Setup.NO_SETUP || setupMode == Setup.READY) {
                return filterAndSort(subArgs(routeFor(p).all()), typed);
            } else if (isAdmin(p) || p.hasPermission("newbedwars.bw.arena")) {
                return filterAndSort(subArgs(setupCommandsFor(setupMode)), typed);
            }
        } else if (args.length >= 2) {
            String sub = args[args.length - 1].toLowerCase();
            List<String> raw = null;
            if (setupMode == null || setupMode == Setup.NO_SETUP) {
                raw = subSubArgs(lobbyRoute(p).all(), args, p);
            } else if (setupMode == Setup.READY) {
                raw = subSubArgs(routeFor(p).all(), args, p);
            } else if (isAdmin(p) || p.hasPermission("newbedwars.bw.arena")) {
                raw = joinedSubSubArgs(args, p);
            }
            if (raw != null) return filterAndSort(raw, sub);
        }
        return null;
    }

    private List<String> filterAndSort(List<String> options, String prefix) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .distinct()
                .collect(Collectors.toList());
    }

    private void setSubCommands(){
        LobbyCommand lobbyCmd = new LobbyCommand();
        LangCommand langCmd = new LangCommand();
        SpectateCommand spectateCmd = new SpectateCommand();
        subCommandsLobbyPublic.add(new JoinCommand());
        subCommandsLobbyPublic.add(new RejoinCommand());
        subCommandsLobbyPublic.add(lobbyCmd);
        subCommandsLobbyPublic.add(langCmd);
        subCommandsLobbyPublic.add(spectateCmd);
        subCommandsLobbyPublic.add(new StatsCommand());
        subCommandsLobbyPublic.add(new ProfileCommand());
        subCommandsLobbyPublic.add(new LeaderboardCommand());

        subCommandsGamePublic.add(lobbyCmd);
        subCommandsGamePublic.add(langCmd);

        subCommandsLobbyAdmin.add(new SetLobbyCommand());
        subCommandsLobbyAdmin.add(new ArenaCommand());
        subCommandsLobbyAdmin.add(new ReloadCommand());
        subCommandsLobbyAdmin.add(new NpcCommand());
        subCommandsLobbyAdmin.add(new GroupCommand());

        subCommandsGameAdmin.add(new ManageGameCommand());

        helpCommand = new HelpCommand(this::helpCommandsFor);
        subCommandsLobbyPublic.add(helpCommand);
        subCommandsGamePublic.add(helpCommand);

        setupSubBasicCommands.add(new ArenaInfoCommand());
        setupSubBasicCommands.add(new LeaveSetupModeCommand());
        setupSubBasicCommands.add(new SaveSettingsCommand());
        setupSubBasicCommands.add(new RemoveCommand());
        setupSubBasicCommands.add(new SetupModeCommand());

        setupSubNormalCommands.add(new CreateTeamCommand());
        setupSubNormalCommands.add(new SetGeneratorCommand());
        setupSubNormalCommands.add(new SetMaxInTeamCommand());
        setupSubNormalCommands.add(new SetMinPlayersCommand());
        setupSubNormalCommands.add(new SetTeamBedCommand());
        setupSubNormalCommands.add(new SetTeamsShopCommand());
        setupSubNormalCommands.add(new SetPositionCommand(SetPositionCommand.Point.TEAM_SPAWN));
        setupSubNormalCommands.add(new SetTeamsUpgradesCommand());
        setupSubNormalCommands.add(new SetPositionCommand(SetPositionCommand.Point.TEAM_BUILD_PROT_POS1));
        setupSubNormalCommands.add(new SetPositionCommand(SetPositionCommand.Point.TEAM_BUILD_PROT_POS2));
        setupSubNormalCommands.add(new SetPositionCommand(SetPositionCommand.Point.TEAM_BASE_POS1));
        setupSubNormalCommands.add(new SetPositionCommand(SetPositionCommand.Point.TEAM_BASE_POS2));
        setupSubNormalCommands.add(new SetQuickVoidYCommand());
        setupSubNormalCommands.add(new SetPositionCommand(SetPositionCommand.Point.WAITING_POS1));
        setupSubNormalCommands.add(new SetPositionCommand(SetPositionCommand.Point.WAITING_POS2));
        setupSubNormalCommands.add(new SetPositionCommand(SetPositionCommand.Point.WAITING_SPAWN));
        setupSubNormalCommands.add(new SetWaitingTimeCommand());
        setupSubAdvancedCommands.add(new GameruleCommand());
        setupSubAdvancedCommands.add(new SetResourcePackCommand());
        setupSubAdvancedCommands.add(new SetArenaGUICommand());
        setupSubAdvancedCommands.add(new SetGeneratorLevelingCommand());
        setupSubAdvancedCommands.add(new SetTeamsAccessoriesCommand());

        setupSubAutomaticCommands.add(new StartAutomaticSetupCommand());
        setupSubBasicCommands.add(new SetupHelpCommand(this::setupCommandsFor, this::getArena));
    }

    private Route lobbyRoute(Player p) {
        return new Route(subCommandsLobbyPublic, hasAnyLobbyAdminPermission(p) ? subCommandsLobbyAdmin : null, false);
    }

    private Route routeFor(Player p) {
        if (isPlayerInGame(p, false)) {
            return new Route(subCommandsGamePublic, isPlayerInGame(p, true) ? subCommandsGameAdmin : null, true);
        }
        if (isSpectatorInArena(p)) {
            return new Route(subCommandsGamePublic, isAdmin(p) || p.hasPermission("newbedwars.bw.managegame") ? subCommandsGameAdmin : null, true);
        }
        return lobbyRoute(p);
    }

    /** Commands the player may use right now (value = admin-only), mirroring the routing in onCommand. */
    private Map<SubCommand, Boolean> helpCommandsFor(Player p) {
        Route route = routeFor(p);
        Map<SubCommand, Boolean> result = new LinkedHashMap<>();
        for (SubCommand cmd : route.publicCmds()) result.put(cmd, false);
        if (route.adminCmds() != null) {
            for (SubCommand cmd : route.adminCmds()) result.put(cmd, true);
        }
        return result;
    }

    private boolean subCmd(List<SubCommand> sub, String[] args, Player p, boolean set) {
        for (SubCommand subCommand : sub) {
            if (args[0].equalsIgnoreCase(subCommand.getName())) {
                if (set) {
                    IArena arena = getArena(p);
                    if (arena != null) {
                        subCommand.perform(p, args, arena);
                    }
                } else {
                    subCommand.perform(p, args, null);
                }
                return true;
            }
        }
        return false;
    }

    private ArrayList<String> subArgs(List<SubCommand> sub) {
        ArrayList<String> names = new ArrayList<>();
        for (SubCommand subCommand : sub) {
            names.add(subCommand.getName());
        }
        return names;
    }

    private List<String> joinedSubSubArgs(String[] args, Player p) {
        ArrayList<SubCommand> allSubCommands = joinedSubSubCommands();
        return subSubArgs(allSubCommands, args, p);
    }

    private List<SubCommand> setupCommandsFor(IArena arena) {
        return setupCommandsFor(arena.getSetupMode());
    }

    /** Setup commands usable in {@code mode}, mirroring the routing in onCommand. */
    private List<SubCommand> setupCommandsFor(Setup mode) {
        List<SubCommand> result = new ArrayList<>(setupSubBasicCommands);
        switch (mode) {
            case NORMAL_SETUP -> result.addAll(setupSubNormalCommands);
            case ADVANCED_SETUP -> { result.addAll(setupSubNormalCommands); result.addAll(setupSubAdvancedCommands); }
            case AUTOMATIC_SETUP -> result.addAll(setupSubAutomaticCommands);
            default -> { }
        }
        return result;
    }

    private ArrayList<SubCommand> joinedSubSubCommands() {
        ArrayList<SubCommand> allSubCommands = new ArrayList<>();
        allSubCommands.addAll(setupSubNormalCommands);
        allSubCommands.addAll(setupSubAdvancedCommands);
        allSubCommands.addAll(setupSubAutomaticCommands);
        allSubCommands.addAll(setupSubBasicCommands);
        return allSubCommands;
    }

    private List<String> subSubArgs(List<SubCommand> sub, String[] args, Player p) {
        for (SubCommand subCommand : sub) {
            if (args[0].equalsIgnoreCase(subCommand.getName())) {
                List<String> result = subCommand.getSubcommandArguments(p, args);
                return result != null ? result : new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    private Setup getArenaSetupMode(Player p) {
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null) return null;
        Setup setupMode = arena.getSetupMode();
        if (setupMode == Setup.READY) return setupMode;
        return arena.getPlayersOnSetup().contains(p.getUniqueId()) ? setupMode : null;
    }

    private boolean isPlayerInGame(Player p, boolean admin) {
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null) return false;
        boolean is = arena.getPlayers().contains(p.getUniqueId());
        GameStatus status = arena.status();
        if (admin) {
            return (p.hasPermission("newbedwars.bw.admin") || p.hasPermission("newbedwars.bw.managegame") || p.isOp())
                    && status != GameStatus.restarting && is;
        }
        return is && status != GameStatus.restarting;
    }

    private IArena getArena(Player player) {
        World world = player.getWorld();
        IArena arena = ArenaRegistry.byWorld(world);
        if (arena == null) {
            msg.send(player, "NotInArena");
        }
        return arena;
    }

}
