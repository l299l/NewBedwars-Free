package com.l299l.newbedwars.npc;

import org.bukkit.command.CommandSender;

import java.util.List;

/** What NewBedwars needs from Citizens; implemented in {@code npc.citizens}, so no Citizens class is loaded without it. */
public interface CitizensBridge {
    /** All Citizens NPCs attached to NewBedwars. */
    List<JoinNpc> npcs();

    /** Name of the NPC selected by {@code sender} with {@code /npc select}, or null. */
    String selectedName(CommandSender sender);

    /** The selected NPC when it is attached, or null. */
    JoinNpc selected(CommandSender sender);

    /** Attaches {@code action} to the selected NPC; false when none is selected. */
    boolean attach(CommandSender sender, JoinNpcAction action);

    /** Removes NewBedwars from the selected NPC; false when none is selected or it was not attached. */
    boolean detach(CommandSender sender);

    void registerListeners();

    void shutdown();
}
