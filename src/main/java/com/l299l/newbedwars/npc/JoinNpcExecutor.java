package com.l299l.newbedwars.npc;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import com.l299l.newbedwars.gui.configuration.game.guis.ArenaSelectGUI;
import org.bukkit.entity.Player;

import java.util.Map;

/** Runs a join NPC's action for the player who clicked it. */
public final class JoinNpcExecutor {
    private JoinNpcExecutor() {}

    public static void perform(Player p, JoinNpcAction action) {
        switch (action.getType()) {
            case GROUP -> {
                if (!ArenaJoinService.mayJoin(p)) return;
                IArena best = ArenaJoinService.findBest(ArenaRegistry.all(), action::matches, ArenaJoinService.joinSize(p));
                if (best == null) {
                    NewBedwars.plugin.getMessages().send(p, "NpcNoFreeArena", Map.of("/group/", action.getTarget()));
                    return;
                }
                ArenaJoinService.joinArena(p, best);
            }
            case ARENA -> {
                IArena arena = ArenaRegistry.find(action.getTarget());
                if (arena == null) {
                    NewBedwars.plugin.getMessages().send(p, "ArenaNotExists");
                    return;
                }
                ArenaJoinService.join(p, arena);
            }
            case SELECTOR -> {
                if (!ArenaJoinService.mayJoin(p)) return;
                p.openInventory(new ArenaSelectGUI(p, action::matches).getInventory());
            }
            case NETWORK -> NewBedwars.plugin.getMessages().send(p, "NpcNetworkUnavailable");
        }
    }
}
