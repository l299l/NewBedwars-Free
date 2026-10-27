package com.l299l.newbedwars.commands.bedwars.admin.setup.basic;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Paginated, clickable setup command list, limited to the commands of the arena's current setup mode.
 * {@code /bw setupHelp <command>} shows the details of one command.
 */
public class SetupHelpCommand extends SubCommand {
    private static final int PER_PAGE = 8;
    private final Function<IArena, List<SubCommand>> available;
    private final Function<Player, IArena> arenaOf;

    public SetupHelpCommand(Function<IArena, List<SubCommand>> available, Function<Player, IArena> arenaOf) {
        this.available = available;
        this.arenaOf = arenaOf;
    }

    @Override
    public String getName() {
        return "setupHelp";
    }

    @Override
    public String getDescription() {
        return "Shows the setup commands available in the current setup mode.";
    }

    @Override
    public String getSyntax() {
        return "/bw setupHelp (<page>) (<command>)";
    }

    @Override
    public String getExample() {
        return "/bw setupHelp gamerule";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        Messages msg = NewBedwars.plugin.getMessages();
        List<SubCommand> commands = available.apply(arena);
        if (args.length == 1) {
            printHelp(player, msg, commands, 1);
        } else if (args.length == 2) {
            try {
                printHelp(player, msg, commands, Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                printCommandHelp(player, msg, commands, args[1]);
            }
        } else {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length != 2) return null;
        List<String> ans = new ArrayList<>();
        IArena arena = arenaOf.apply(player);
        if (arena == null) return ans;
        List<SubCommand> commands = available.apply(arena);
        int pages = Math.max(1, (commands.size() + PER_PAGE - 1) / PER_PAGE);
        for (int i = 1; i <= pages; i++) ans.add(String.valueOf(i));
        for (SubCommand command : commands) ans.add(command.getName());
        return ans;
    }

    private void printHelp(Player player, Messages msg, List<SubCommand> commands, int page) {
        int pages = Math.max(1, (commands.size() + PER_PAGE - 1) / PER_PAGE);
        if (page < 1 || page > pages) {
            msg.send(player, "HelpInvalidPage");
            return;
        }
        player.sendMessage(msg.getMsg(player, "SetupHelpHeader")
                .replace("/page/", String.valueOf(page)).replace("/pages/", String.valueOf(pages)));
        int from = (page - 1) * PER_PAGE;
        for (int i = from; i < Math.min(commands.size(), from + PER_PAGE); i++) {
            SubCommand cmd = commands.get(i);
            TextComponent line = new TextComponent(TextComponent.fromLegacyText(
                    ChatColor.YELLOW + cmd.getSyntax() + ChatColor.GRAY + " - " + cmd.getDescription()));
            String hover = ChatColor.GOLD + cmd.getDescription()
                    + "\n" + ChatColor.GRAY + msg.getMsg(player, "HelpExample") + " " + ChatColor.WHITE + cmd.getExample()
                    + "\n" + ChatColor.DARK_GRAY + msg.getMsg(player, "HelpClickToUse");
            line.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw " + cmd.getName() + " "));
            line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));
            player.spigot().sendMessage(line);
        }
        sendNavigation(player, msg, page, pages);
    }

    private void printCommandHelp(Player player, Messages msg, List<SubCommand> commands, String name) {
        for (SubCommand command : commands) {
            if (!command.getName().equalsIgnoreCase(name)) continue;
            player.sendMessage(msg.getMsg(player, "SetupHelpCommandHeader").replace("/command/", command.getName()));
            player.sendMessage(ChatColor.GRAY + command.getDescription());
            player.sendMessage(ChatColor.GREEN + msg.getMsg(player, "SetupHelpSyntax") + " " + ChatColor.YELLOW + command.getSyntax());
            player.sendMessage(ChatColor.GREEN + msg.getMsg(player, "HelpExample") + " " + ChatColor.YELLOW + command.getExample());
            return;
        }
        msg.send(player, "CommandDoesntExists");
    }

    private void sendNavigation(Player player, Messages msg, int page, int pages) {
        if (pages == 1) return;
        ComponentBuilder nav = new ComponentBuilder("");
        if (page > 1) {
            TextComponent prev = new TextComponent(TextComponent.fromLegacyText(msg.getMsg(player, "HelpPrevPage")));
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw setupHelp " + (page - 1)));
            nav.append(prev);
        }
        if (page > 1 && page < pages) nav.append(TextComponent.fromLegacyText(ChatColor.DARK_GRAY + "  |  "));
        if (page < pages) {
            TextComponent next = new TextComponent(TextComponent.fromLegacyText(msg.getMsg(player, "HelpNextPage")));
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw setupHelp " + (page + 1)));
            nav.append(next);
        }
        player.spigot().sendMessage(nav.create());
    }
}
