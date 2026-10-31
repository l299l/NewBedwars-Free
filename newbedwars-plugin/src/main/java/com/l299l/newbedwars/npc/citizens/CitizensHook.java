package com.l299l.newbedwars.npc.citizens;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.npc.CitizensBridge;
import com.l299l.newbedwars.npc.JoinNpc;
import com.l299l.newbedwars.npc.JoinNpcAction;
import com.l299l.newbedwars.npc.JoinNpcManager;
import com.l299l.newbedwars.config.properties.Properties;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/** The only class (with the trait) that touches Citizens; created only when the Citizens plugin is enabled. */
public final class CitizensHook implements CitizensBridge, Listener {
    private final JoinNpcManager manager;

    public CitizensHook(JoinNpcManager manager) {
        this.manager = manager;
        Class<?> registered = CitizensAPI.getTraitFactory().getTraitClass(NewBedwarsJoinTrait.NAME);
        if (registered != NewBedwarsJoinTrait.class) {
            if (registered != null) {
                // Left over from an earlier load of NewBedwars (plugin reloaded): replace it with this class.
                for (TraitInfo info : new ArrayList<>(CitizensAPI.getTraitFactory().getRegisteredTraits())) {
                    if (info.getTraitClass() == registered) CitizensAPI.getTraitFactory().deregisterTrait(info);
                }
            }
            CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(NewBedwarsJoinTrait.class).withName(NewBedwarsJoinTrait.NAME));
        }
    }

    @Override
    public void registerListeners() {
        NewBedwars.plugin.getServer().getPluginManager().registerEvents(this, NewBedwars.plugin);
    }

    @Override
    public void shutdown() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        // The trait stays registered: deregistering would make Citizens drop the trait data on its next save.
    }

    @Override
    public List<JoinNpc> npcs() {
        List<JoinNpc> result = new ArrayList<>();
        if (!CitizensAPI.hasImplementation()) return result;
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            JoinNpc view = view(npc);
            if (view != null) result.add(view);
        }
        return result;
    }

    @Override
    public String selectedName(CommandSender sender) {
        NPC npc = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
        return npc == null ? null : label(npc);
    }

    @Override
    public JoinNpc selected(CommandSender sender) {
        NPC npc = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
        return npc == null ? null : view(npc);
    }

    @Override
    public boolean attach(CommandSender sender, JoinNpcAction action) {
        NPC npc = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
        if (npc == null) return false;
        NewBedwarsJoinTrait trait;
        if (npc.hasTrait(NewBedwarsJoinTrait.class)) {
            trait = npc.getTraitNullable(NewBedwarsJoinTrait.class);
        } else {
            // Not getOrAddTrait: after a removal it returns the removed trait instead of adding a new one.
            trait = new NewBedwarsJoinTrait();
            npc.addTrait(trait);
        }
        trait.setAction(action);
        manager.refreshSoon();
        return true;
    }

    @Override
    public boolean detach(CommandSender sender) {
        NPC npc = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
        if (npc == null || !npc.hasTrait(NewBedwarsJoinTrait.class)) return false;
        NewBedwarsJoinTrait trait = npc.getTraitNullable(NewBedwarsJoinTrait.class);
        if (trait != null) trait.clearAction();
        npc.removeTrait(NewBedwarsJoinTrait.class);
        manager.refreshSoon();
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onRightClick(NPCRightClickEvent e) {
        JoinNpc npc = view(e.getNPC());
        if (npc != null) manager.click(e.getClicker(), npc);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeftClick(NPCLeftClickEvent e) {
        if (!Properties.JoinNpcLeftClick) return;
        JoinNpc npc = view(e.getNPC());
        if (npc != null) manager.click(e.getClicker(), npc);
    }

    private JoinNpc view(NPC npc) {
        // hasTrait first: Citizens' getTraitNullable still returns a trait after removeTrait (its lookup keeps the slot).
        if (!npc.hasTrait(NewBedwarsJoinTrait.class)) return null;
        NewBedwarsJoinTrait trait = npc.getTraitNullable(NewBedwarsJoinTrait.class);
        if (trait == null) return null;
        JoinNpcAction action = trait.getAction();
        if (action == null) return null;
        String label = label(npc);
        return new JoinNpc() {
            @Override
            public String key() {
                return "citizens:" + npc.getId();
            }

            @Override
            public String displayName() {
                return label;
            }

            @Override
            public JoinNpcAction action() {
                return action;
            }

            @Override
            public Location location() {
                if (!npc.isSpawned() || npc.getEntity() == null) return null;
                return npc.getEntity().getLocation();
            }

            @Override
            public double height() {
                Entity entity = npc.getEntity();
                return entity == null ? 1.8 : entity.getHeight();
            }
        };
    }

    private static String label(NPC npc) {
        return "Citizens #" + npc.getId() + " " + npc.getName();
    }
}
