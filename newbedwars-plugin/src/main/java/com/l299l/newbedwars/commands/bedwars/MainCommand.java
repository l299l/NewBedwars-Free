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
import com.l299l.newbedwars.commands.bedwars.admin.UpdateCommand;
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
import java.util.Arrays;
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
    private final ArrayList<SubCommand> consoleCommands = new ArrayList<>();
    private HelpCommand helpCommand;

    private record Route(List<SubCommand> publicCmds, List<SubCommand> adminCmds, boolean adminNeedsArena) {
        List<SubCommand> all() {
            List<SubCommand> all = new ArrayList<>(publicCmds);
            if (adminCmds != null) all.addAll(adminCmds);
            return all;
        }
    }

    public enum Context {
        LOBBY, LOBBY_ADMIN, GAME, GAME_ADMIN, SETUP_BASIC, SETUP_NORMAL, SETUP_ADVANCED, SETUP_AUTOMATIC, CONSOLE
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
            case CONSOLE -> consoleCommands;
        };
    }

    public void register(SubCommand command, Context... contexts) {
        if (contexts == null || contexts.length == 0) throw new IllegalArgumentException("no context for command '" + command.getName() + "'");
        for (Context context : contexts) {
            if (context == null) throw new IllegalArgumentException("null context for command '" + command.getName() + "'");
        }
        for (Context c : Context.values()) {
            if (Arrays.stream(contexts).noneMatch(wanted -> group(wanted) == group(c))) continue;
            for (SubCommand existing : bucket(c)) {
                if (existing != command && existing.getName().equalsIgnoreCase(command.getName())) {
                    throw new IllegalArgumentException("command '" + command.getName() + "' already exists (" + c + ")");
                }
            }
        }
        for (Context context : contexts) {
            List<SubCommand> bucket = bucket(context);
            if (!bucket.contains(command)) bucket.add(command);
        }
    }

    static int group(Context context) {
        return switch (context) {
            case LOBBY, LOBBY_ADMIN -> 0;
            case GAME, GAME_ADMIN -> 1;
            case SETUP_BASIC, SETUP_NORMAL, SETUP_ADVANCED, SETUP_AUTOMATIC -> 2;
            case CONSOLE -> 3;
        };
    }

    public List<Context> contextsOf(SubCommand command) {
        List<Context> result = new ArrayList<>();
        for (Context c : Context.values()) {
            if (bucket(c).contains(command)) result.add(c);
        }
        return result;
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
            onConsoleCommand(sender, args);
            return true;
        }

        if (args.length > 0) {
            Setup setupMode = getArenaSetupMode(p);
            if (setupMode == null || setupMode == Setup.NO_SETUP || setupMode == Setup.READY) {
                Route route = routeFor(p);
                boolean handled = route.adminCmds() != null && subCmd(route.adminCmds(), args, p, route.adminNeedsArena());
                if (!handled) {
                    subCmd(route.publicCmds(), args, p, false, route.adminNeedsArena());
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

    private void onConsoleCommand(CommandSender sender, String[] args) {
        if (args.length > 0) {
            for (SubCommand command : consoleCommands) {
                if (command.getName().equalsIgnoreCase(args[0])) {
                    command.performConsole(sender, args);
                    return;
                }
            }
        }
        if (consoleCommands.isEmpty()) {
            sender.sendMessage(msg.getMsgToConsole("OnlyPlayer"));
            return;
        }
        String names = consoleCommands.stream().map(SubCommand::getName).sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
        sender.sendMessage(msg.getMsgToConsole("ConsoleCommands").replace("/commands/", names));
    }

    private List<String> consoleTabComplete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return filterAndSort(subArgs(consoleCommands), args.length == 0 ? "" : args[0].toLowerCase());
        }
        for (SubCommand command : consoleCommands) {
            if (command.getName().equalsIgnoreCase(args[0])) {
                List<String> raw = command.getConsoleArguments(sender, args);
                return filterAndSort(raw == null ? List.of() : raw, args[args.length - 1].toLowerCase());
            }
        }
        return List.of();
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
                || p.hasPermission("newbedwars.bw.group")
                || p.hasPermission("newbedwars.bw.update");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player p)) return consoleTabComplete(sender, args);
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
        register(new JoinCommand(), Context.LOBBY);
        register(new RejoinCommand(), Context.LOBBY);
        register(lobbyCmd, Context.LOBBY, Context.GAME);
        register(langCmd, Context.LOBBY, Context.GAME);
        register(spectateCmd, Context.LOBBY);
        register(new StatsCommand(), Context.LOBBY);
        register(new ProfileCommand(), Context.LOBBY);
        register(new LeaderboardCommand(), Context.LOBBY);

        register(new SetLobbyCommand(), Context.LOBBY_ADMIN);
        register(new ArenaCommand(), Context.LOBBY_ADMIN);
        register(new ReloadCommand(), Context.LOBBY_ADMIN);
        register(new NpcCommand(), Context.LOBBY_ADMIN);
        register(new GroupCommand(), Context.LOBBY_ADMIN);
        register(new UpdateCommand(), Context.LOBBY_ADMIN);

        register(new ManageGameCommand(), Context.GAME_ADMIN);

        helpCommand = new HelpCommand(this::helpCommandsFor);
        register(helpCommand, Context.LOBBY, Context.GAME);

        register(new ArenaInfoCommand(), Context.SETUP_BASIC);
        register(new LeaveSetupModeCommand(), Context.SETUP_BASIC);
        register(new SaveSettingsCommand(), Context.SETUP_BASIC);
        register(new RemoveCommand(), Context.SETUP_BASIC);
        register(new SetupModeCommand(), Context.SETUP_BASIC);

        register(new CreateTeamCommand(), Context.SETUP_NORMAL);
        register(new SetGeneratorCommand(), Context.SETUP_NORMAL);
        register(new SetMaxInTeamCommand(), Context.SETUP_NORMAL);
        register(new SetMinPlayersCommand(), Context.SETUP_NORMAL);
        register(new SetTeamBedCommand(), Context.SETUP_NORMAL);
        register(new SetTeamsShopCommand(), Context.SETUP_NORMAL);
        register(new SetPositionCommand(SetPositionCommand.Point.TEAM_SPAWN), Context.SETUP_NORMAL);
        register(new SetTeamsUpgradesCommand(), Context.SETUP_NORMAL);
        register(new SetPositionCommand(SetPositionCommand.Point.TEAM_BUILD_PROT_POS1), Context.SETUP_NORMAL);
        register(new SetPositionCommand(SetPositionCommand.Point.TEAM_BUILD_PROT_POS2), Context.SETUP_NORMAL);
        register(new SetPositionCommand(SetPositionCommand.Point.TEAM_BASE_POS1), Context.SETUP_NORMAL);
        register(new SetPositionCommand(SetPositionCommand.Point.TEAM_BASE_POS2), Context.SETUP_NORMAL);
        register(new SetQuickVoidYCommand(), Context.SETUP_NORMAL);
        register(new SetPositionCommand(SetPositionCommand.Point.WAITING_POS1), Context.SETUP_NORMAL);
        register(new SetPositionCommand(SetPositionCommand.Point.WAITING_POS2), Context.SETUP_NORMAL);
        register(new SetPositionCommand(SetPositionCommand.Point.WAITING_SPAWN), Context.SETUP_NORMAL);
        register(new SetWaitingTimeCommand(), Context.SETUP_NORMAL);
        register(new GameruleCommand(), Context.SETUP_ADVANCED);
        register(new SetResourcePackCommand(), Context.SETUP_ADVANCED);
        register(new SetArenaGUICommand(), Context.SETUP_ADVANCED);
        register(new SetGeneratorLevelingCommand(), Context.SETUP_ADVANCED);
        register(new SetTeamsAccessoriesCommand(), Context.SETUP_ADVANCED);

        register(new StartAutomaticSetupCommand(), Context.SETUP_AUTOMATIC);
        register(new SetupHelpCommand(this::setupCommandsFor, this::getArena), Context.SETUP_BASIC);
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
        return subCmd(sub, args, p, set, false);
    }

    private boolean subCmd(List<SubCommand> sub, String[] args, Player p, boolean set, boolean inArena) {
        for (SubCommand subCommand : sub) {
            if (args[0].equalsIgnoreCase(subCommand.getName())) {
                if (set) {
                    IArena arena = getArena(p);
                    if (arena != null) {
                        subCommand.perform(p, args, arena);
                    }
                } else {
                    subCommand.perform(p, args, inArena ? ArenaRegistry.byWorld(p.getWorld()) : null);
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
