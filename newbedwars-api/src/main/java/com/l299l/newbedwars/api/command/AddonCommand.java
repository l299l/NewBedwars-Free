package com.l299l.newbedwars.api.command;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/** A {@code /bw <name>} subcommand added by an addon. It is listed in {@code /bw help} like the built-in ones. */
public interface AddonCommand {
    /** Name after {@code /bw}; must not clash with an existing subcommand. */
    String getName();

    String getDescription();

    String getSyntax();

    String getExample();

    /**
     * @param args the whole argument list; {@code args[0]} is the command name
     * @param arena the arena the player is in or sets up, or {@code null} outside arenas
     */
    void perform(Player player, String[] args, ArenaView arena);

    default List<String> tabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }
}
