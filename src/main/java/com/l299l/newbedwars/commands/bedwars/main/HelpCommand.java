package com.l299l.newbedwars.commands.bedwars.main;

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
import java.util.Map;
import java.util.function.Function;

/**
 * Paginated, clickable and localized command list built from the {@link SubCommand} metadata
 * of the commands available to the player in their current context.
 */
public class HelpCommand extends SubCommand {
    private static final int PER_PAGE = 8;
    /** Commands available to the player; the value is true for admin-only commands. */
    private final Function<Player, Map<SubCommand, Boolean>> available;

    public HelpCommand(Function<Player, Map<SubCommand, Boolean>> available) {
        this.available = available;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Shows available commands.";
    }

    @Override
    public String getSyntax() {
        return "/bw help [page]";
    }

    @Override
    public String getExample() {
        return "/bw help 2";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        Messages msg = NewBedwars.plugin.getMessages();
        Map<SubCommand, Boolean> commands = available.apply(player);
        List<Map.Entry<SubCommand, Boolean>> entries = new ArrayList<>();
        for (Map.Entry<SubCommand, Boolean> entry : commands.entrySet()) {
            if (!entry.getKey().getName().equalsIgnoreCase("help")) entries.add(entry);
        }
        int pages = Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                msg.send(player, "HelpInvalidPage");
                return;
            }
            if (page < 1 || page > pages) {
                msg.send(player, "HelpInvalidPage");
                return;
            }
        }

        player.sendMessage(msg.getMsg(player, "HelpHeader")
                .replace("/page/", String.valueOf(page)).replace("/pages/", String.valueOf(pages)));
        int from = (page - 1) * PER_PAGE;
        for (int i = from; i < Math.min(entries.size(), from + PER_PAGE); i++) {
            SubCommand cmd = entries.get(i).getKey();
            boolean admin = entries.get(i).getValue();
            String description = description(msg, player, cmd);
            TextComponent line = new TextComponent(TextComponent.fromLegacyText(
                    (admin ? ChatColor.RED : ChatColor.YELLOW) + cmd.getSyntax() + ChatColor.GRAY + " - " + description));
            String hover = ChatColor.GOLD + description
                    + "\n" + ChatColor.GRAY + msg.getMsg(player, "HelpExample") + " " + ChatColor.WHITE + cmd.getExample()
                    + "\n" + ChatColor.DARK_GRAY + msg.getMsg(player, "HelpClickToUse");
            line.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw " + cmd.getName() + " "));
            line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));
            player.spigot().sendMessage(line);
        }
        player.sendMessage(msg.getMsg(player, "HelpParty"));
        sendNavigation(player, msg, page, pages);
    }

    /** Localized description (key {@code HelpDesc-<name>}), falling back to the command's own text. */
    private String description(Messages msg, Player player, SubCommand cmd) {
        String localized = msg.getMsg(player, "HelpDesc-" + cmd.getName());
        return localized.isEmpty() ? cmd.getDescription() : localized;
    }

    private void sendNavigation(Player player, Messages msg, int page, int pages) {
        if (pages == 1) return;
        ComponentBuilder nav = new ComponentBuilder("");
        if (page > 1) {
            TextComponent prev = new TextComponent(TextComponent.fromLegacyText(msg.getMsg(player, "HelpPrevPage")));
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw help " + (page - 1)));
            nav.append(prev);
        }
        if (page > 1 && page < pages) nav.append(TextComponent.fromLegacyText(ChatColor.DARK_GRAY + "  |  "));
        if (page < pages) {
            TextComponent next = new TextComponent(TextComponent.fromLegacyText(msg.getMsg(player, "HelpNextPage")));
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw help " + (page + 1)));
            nav.append(next);
        }
        player.spigot().sendMessage(nav.create());
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
