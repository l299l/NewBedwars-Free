package com.l299l.newbedwars.commands.bedwars;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.commands.bedwars.admin.ArenaCommand;
import com.l299l.newbedwars.commands.bedwars.admin.ManageGameCommand;
import com.l299l.newbedwars.commands.bedwars.admin.SetLobbyCommand;
import com.l299l.newbedwars.commands.bedwars.admin.SetupGuisCommand;
import com.l299l.newbedwars.commands.bedwars.admin.setup.advenced.AddSpecialGamerulesCommand;
import com.l299l.newbedwars.commands.bedwars.admin.setup.advenced.SetTeamsAccessoriesCommand;
import com.l299l.newbedwars.commands.bedwars.admin.setup.automatic.StartAutomaticSetupCommand;
import com.l299l.newbedwars.commands.bedwars.admin.setup.basic.*;
import com.l299l.newbedwars.commands.bedwars.admin.setup.normal.*;
import com.l299l.newbedwars.commands.bedwars.main.HelpCommand;
import com.l299l.newbedwars.commands.bedwars.main.JoinCommand;
import com.l299l.newbedwars.commands.bedwars.main.LangCommand;
import com.l299l.newbedwars.commands.bedwars.main.LobbyCommand;
import com.l299l.newbedwars.commands.bedwars.main.RejoinCommand;
import com.l299l.newbedwars.commands.bedwars.main.SpectateCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainCommand implements TabExecutor {
    private final NewBedwars plugin;
    private final Messages msg;
    private final ArrayList<SubCommand> subCommandsLobbyPublic;
    private final ArrayList<SubCommand> subCommandsLobbyAdmin;
    private final ArrayList<SubCommand> subCommandsGameAdmin;
    private final ArrayList<SubCommand> subCommandsGamePublic;
    private HelpCommand helpCommand;
    private final ArrayList<SubCommand> setupSubBasicCommands;
    private final ArrayList<SubCommand> setupSubNormalCommands;
    private final ArrayList<SubCommand> setupSubAdvencedCommands;
    private final ArrayList<SubCommand> setupSubAutomaticCommands;

    public MainCommand() {
        plugin = NewBedwars.plugin;
        msg = plugin.getMessages();
        subCommandsLobbyPublic = new ArrayList<>();
        subCommandsLobbyAdmin = new ArrayList<>();
        subCommandsGameAdmin = new ArrayList<>();
        subCommandsGamePublic = new ArrayList<>();
        setupSubBasicCommands = new ArrayList<>();
        setupSubNormalCommands = new ArrayList<>();
        setupSubAdvencedCommands = new ArrayList<>();
        setupSubAutomaticCommands = new ArrayList<>();
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
                boolean handled = false;
                if (isPlayerInGame(p, false)) {
                    if (isPlayerInGame(p, true)) {
                        handled = subCmd(subCommandsGameAdmin, args, p, true);
                    }
                    if (!handled) {
                        subCmd(subCommandsGamePublic, args, p, false);
                    }
                } else if (isSpectatorInArena(p)) {
                    if (isAdmin(p) || p.hasPermission("newbedwars.bw.managegame")) {
                        handled = subCmd(subCommandsGameAdmin, args, p, true);
                    }
                    if (!handled) {
                        subCmd(subCommandsGamePublic, args, p, false);
                    }
                } else {
                    if (hasAnyLobbyAdminPermission(p)) {
                        handled = subCmd(subCommandsLobbyAdmin, args, p, false);
                    }
                    if (!handled) {
                        subCmd(subCommandsLobbyPublic, args, p, false);
                    }
                }
            } else {
                if (!isAdmin(p) && !p.hasPermission("newbedwars.bw.arena")) {
                    msg.send(p, "NoPermissions");
                    return true;
                }
                subCmd(setupSubBasicCommands, args, p, true);
                switch (setupMode) {
                    case NORMAL_SETUP -> { subCmd(setupSubNormalCommands, args, p, true); return true; }
                    case ADVANCED_SETUP -> { subCmd(setupSubNormalCommands, args, p, true); subCmd(setupSubAdvencedCommands, args, p, true); return true; }
                    case BUILDING_MODE -> { subCmd(setupSubAutomaticCommands, args, p, true); return true; }
                }
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
        try {
            IArena arena = Arena.arenaByWorld.get(p.getWorld());
            return arena != null && arena.getSpectators().contains(p);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasAnyLobbyAdminPermission(Player p) {
        return isAdmin(p)
                || p.hasPermission("newbedwars.bw.arena")
                || p.hasPermission("newbedwars.bw.setlobby")
                || p.hasPermission("newbedwars.bw.setupguis");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player p)) return null;
        Setup setupMode = getArenaSetupMode(p);
        String typed = args.length > 0 ? args[0].toLowerCase() : "";

        if (args.length == 1) {
            if (setupMode == null || setupMode == Setup.NO_SETUP || setupMode == Setup.READY) {
                ArrayList<String> names;
                if (isPlayerInGame(p, false)) {
                    names = new ArrayList<>(subArgs(subCommandsGamePublic));
                    if (isPlayerInGame(p, true)) names.addAll(subArgs(subCommandsGameAdmin));
                } else if (isSpectatorInArena(p)) {
                    names = new ArrayList<>(subArgs(subCommandsGamePublic));
                    if (isAdmin(p) || p.hasPermission("newbedwars.bw.managegame")) names.addAll(subArgs(subCommandsGameAdmin));
                } else {
                    names = new ArrayList<>(subArgs(subCommandsLobbyPublic));
                    if (hasAnyLobbyAdminPermission(p)) names.addAll(subArgs(subCommandsLobbyAdmin));
                }
                return filterAndSort(names, typed);
            } else if (isAdmin(p) || p.hasPermission("newbedwars.bw.arena")) {
                ArrayList<String> basic = subArgs(setupSubBasicCommands);
                switch (setupMode) {
                    case NORMAL_SETUP -> basic.addAll(subArgs(setupSubNormalCommands));
                    case ADVANCED_SETUP -> { basic.addAll(subArgs(setupSubNormalCommands)); basic.addAll(subArgs(setupSubAdvencedCommands)); }
                    case BUILDING_MODE -> basic.addAll(subArgs(setupSubAutomaticCommands));
                }
                return filterAndSort(basic, typed);
            }
        } else if (args.length >= 2) {
            String sub = args[args.length - 1].toLowerCase();
            List<String> raw = null;
            if (setupMode == null || setupMode == Setup.NO_SETUP) {
                ArrayList<SubCommand> all = new ArrayList<>(subCommandsLobbyPublic);
                if (hasAnyLobbyAdminPermission(p)) all.addAll(subCommandsLobbyAdmin);
                raw = subSubArgs(all, args, p);
            } else if (setupMode == Setup.READY) {
                if (isPlayerInGame(p, false)) {
                    ArrayList<SubCommand> all = new ArrayList<>(subCommandsGamePublic);
                    if (isPlayerInGame(p, true)) all.addAll(subCommandsGameAdmin);
                    raw = subSubArgs(all, args, p);
                } else if (isSpectatorInArena(p)) {
                    ArrayList<SubCommand> all = new ArrayList<>(subCommandsGamePublic);
                    if (isAdmin(p) || p.hasPermission("newbedwars.bw.managegame")) all.addAll(subCommandsGameAdmin);
                    raw = subSubArgs(all, args, p);
                } else {
                    ArrayList<SubCommand> all = new ArrayList<>(subCommandsLobbyPublic);
                    if (hasAnyLobbyAdminPermission(p)) all.addAll(subCommandsLobbyAdmin);
                    raw = subSubArgs(all, args, p);
                }
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

        subCommandsGamePublic.add(lobbyCmd);
        subCommandsGamePublic.add(langCmd);

        subCommandsLobbyAdmin.add(new SetupGuisCommand());
        subCommandsLobbyAdmin.add(new SetLobbyCommand());
        subCommandsLobbyAdmin.add(new ArenaCommand());

        subCommandsGameAdmin.add(new ManageGameCommand());

        ArrayList<SubCommand> allLobby = new ArrayList<>(subCommandsLobbyPublic);
        allLobby.addAll(subCommandsLobbyAdmin);
        helpCommand = new HelpCommand(allLobby, subCommandsGameAdmin);
        subCommandsLobbyPublic.add(helpCommand);
        subCommandsGamePublic.add(helpCommand);

        setupSubBasicCommands.add(new ArenaInfoCommand());
        setupSubBasicCommands.add(new LeaveSetupModeCommand());
        setupSubBasicCommands.add(new SaveSettingsCommand());
        setupSubBasicCommands.add(new RemoveCommand());

        setupSubNormalCommands.add(new CreateTeamCommand());
        setupSubNormalCommands.add(new SetGeneratorCommand());
        setupSubNormalCommands.add(new SetMaxInTeamCommand());
        setupSubNormalCommands.add(new SetMinPlayersCommand());
        setupSubNormalCommands.add(new SetTeamBedCommand());
        setupSubNormalCommands.add(new SetTeamsShopCommand());
        setupSubNormalCommands.add(new SetTeamsSpawnCommand());
        setupSubNormalCommands.add(new SetTeamsUpgradesCommand());
        setupSubNormalCommands.add(new SetTeamBuildProtAreaPos1Command());
        setupSubNormalCommands.add(new SetTeamBuildProtAreaPos2Command());
        setupSubNormalCommands.add(new SetTeamBasePos1Command());
        setupSubNormalCommands.add(new SetTeamBasePos2Command());
        setupSubNormalCommands.add(new SetQuickVoidYCommand());
        setupSubNormalCommands.add(new SetWaitingPos1Command());
        setupSubNormalCommands.add(new SetWaitingPos2Command());
        setupSubNormalCommands.add(new SetWaitingSpawnCommand());
        setupSubNormalCommands.add(new SetWaitingTimeCommand());

        setupSubAdvencedCommands.add(new AddSpecialGamerulesCommand());
        setupSubAdvencedCommands.add(new SetTeamsAccessoriesCommand());

        setupSubAutomaticCommands.add(new StartAutomaticSetupCommand());
        setupSubBasicCommands.add(new SetupHelpCommand(joinedSubSubCommands()));


    }

    private boolean subCmd(ArrayList<SubCommand> sub, String[] args, Player p, Boolean set) {
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

    private ArrayList<String> subArgs(ArrayList<SubCommand> sub) {
        ArrayList<String> subcommandsArguments = new ArrayList<>();
        for (SubCommand subCommand : sub) {
            subcommandsArguments.add(subCommand.getName());
        }

        return subcommandsArguments;
    }

    private List<String> joinedSubSubArgs(String[] args, Player p) {
        ArrayList<SubCommand> allSubCommands = joinedSubSubCommands();
        return subSubArgs(allSubCommands, args, p);
    }

    private ArrayList<SubCommand> joinedSubSubCommands() {
        ArrayList<SubCommand> allSubCommands = new ArrayList<>();
        allSubCommands.addAll(setupSubNormalCommands);
        allSubCommands.addAll(setupSubAdvencedCommands);
        allSubCommands.addAll(setupSubAutomaticCommands);
        allSubCommands.addAll(setupSubBasicCommands);
        return allSubCommands;
    }

    private List<String> subSubArgs(ArrayList<SubCommand> sub, String[] args, Player p) {
        for (SubCommand subCommand : sub) {
            if (args[0].equalsIgnoreCase(subCommand.getName())) {
                List<String> result = subCommand.getSubcommandArguments(p, args);
                return result != null ? result : new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    private Setup getArenaSetupMode(Player p) {
        Setup setupMode = null;
        try {
            IArena arena = Arena.arenaByWorld.get(p.getWorld());
            setupMode = arena.getSetupMode();
            if (setupMode == Setup.READY) {
                return setupMode;
            }
            if (!arena.getPlayersOnSetup().contains(p.getUniqueId())) {
                return null;
            }
        }catch (Exception e) {
            return null;
        }
        return setupMode;
    }

    private boolean isPlayerInGame(Player p, boolean admin) {
        try {
            IArena arena = Arena.arenaByWorld.get(p.getWorld());
            boolean is = arena.getPlayers().contains(p.getUniqueId());
            GameStatus status = arena.status();
            if (admin && (p.hasPermission("newbedwars.bw.admin") || p.hasPermission("newbedwars.bw.managegame") || p.isOp()) && status != GameStatus.restarting) {
                return is;
            } else if (admin) {
                return false;
            } else {
                return is && status != GameStatus.restarting;
            }
        }catch (Exception e) {
            return false;
        }
    }

    private IArena getArena(Player player) {
        World world = player.getWorld();
        IArena arena;
        try {
            arena = Arena.arenaByWorld.get(world);
            return arena;
        }catch (Exception e) {
            msg.send(player, "NotInArena");
            return null;
        }
    }

}
