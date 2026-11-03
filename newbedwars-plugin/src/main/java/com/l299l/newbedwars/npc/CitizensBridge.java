package com.l299l.newbedwars.npc;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface CitizensBridge {
    List<JoinNpc> npcs();

    String selectedName(CommandSender sender);

    JoinNpc selected(CommandSender sender);

    boolean attach(CommandSender sender, JoinNpcAction action);

    boolean detach(CommandSender sender);

    void registerListeners();

    void shutdown();
}
