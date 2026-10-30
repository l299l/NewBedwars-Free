package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.apiimpl.view.ApiArena;
import com.l299l.newbedwars.apiimpl.edit.ApiEditor;
import com.l299l.newbedwars.apiimpl.adapter.ApiShopLogic;
import com.l299l.newbedwars.apiimpl.adapter.ApiUpgradeHandler;
import com.l299l.newbedwars.apiimpl.adapter.ApiSubCommand;
import com.l299l.newbedwars.apiimpl.adapter.ApiRegistrations;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.command.AddonCommand;
import com.l299l.newbedwars.api.edit.ArenaEditor;
import com.l299l.newbedwars.api.registry.ArmorTierDefinition;
import com.l299l.newbedwars.api.command.CommandContext;
import com.l299l.newbedwars.api.registry.PhaseActionHandler;
import com.l299l.newbedwars.api.registry.ShopLogic;
import com.l299l.newbedwars.api.registry.UpgradeDefinition;
import com.l299l.newbedwars.arena.phases.PhaseActionRegistry;
import com.l299l.newbedwars.arena.player.inventory.ArmorTier;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogicRegistry;
import com.l299l.newbedwars.commands.bedwars.MainCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.NewBedwarsAPI;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.l299l.newbedwars.api.view.ArenaGroupView;
import com.l299l.newbedwars.apiimpl.view.ApiArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public final class NewBedwarsApiImpl implements NewBedwarsAPI, Listener {
    private final ApiRegistrations registrations = new ApiRegistrations();

    @Override
    public String getVersion() {
        return NewBedwars.plugin.getDescription().getVersion();
    }

    @Override
    public Collection<ArenaView> getArenas() {
        Collection<ArenaView> result = new ArrayList<>();
        for (IArena arena : Arena.arenaByName.values()) result.add(new ApiArena(arena));
        return result;
    }

    @Override
    public Optional<ArenaView> getArena(String name) {
        IArena arena = Arena.arenaByName.get(name);
        return arena == null ? Optional.empty() : Optional.of(new ApiArena(arena));
    }

    @Override
    public Optional<ArenaView> getArena(World world) {
        IArena arena = Arena.arenaByWorld.get(world);
        return arena == null ? Optional.empty() : Optional.of(new ApiArena(arena));
    }

    @Override
    public Optional<ArenaView> getArena(Player player) {
        for (IArena arena : Arena.arenaByName.values()) {
            if (Boolean.TRUE.equals(arena.isPlayerInArena(player)) || arena.getSpectators().contains(player)) {
                return Optional.of(new ApiArena(arena));
            }
        }
        return Optional.empty();
    }

    @Override
    public Collection<ArenaGroupView> getGroups() {
        Collection<ArenaGroupView> result = new ArrayList<>();
        ArenaGroupManager groups = ArenaGroupManager.get();
        if (groups != null) {
            for (ArenaGroup g : groups.getGroups()) result.add(new ApiArenaGroup(g));
        }
        return result;
    }

    @Override
    public Optional<ArenaGroupView> getGroup(String name) {
        ArenaGroup g = ArenaGroupManager.lookup(name);
        return g == null ? Optional.empty() : Optional.of(new ApiArenaGroup(g));
    }

    @Override
    public ArenaEditor edit(ArenaView arena) {
        IArena internal = Arena.arenaByName.get(arena.getName());
        if (internal == null) throw new IllegalArgumentException("arena " + arena.getName() + " does not exist (any more)");
        return new ApiEditor(internal);
    }

    @Override
    public void registerPhaseAction(Plugin owner, String name, PhaseActionHandler handler) {
        PhaseActionRegistry.register(name, (arena, argument) -> handler.execute(new ApiArena(arena), argument));
        registrations.add(owner, () -> PhaseActionRegistry.unregister(name));
    }

    @Override
    public void registerCommand(Plugin owner, AddonCommand command, CommandContext context) {
        ApiSubCommand subCommand = new ApiSubCommand(command);
        MainCommand main = NewBedwars.plugin.getMainCommand();
        main.register(subCommand, MainCommand.Context.valueOf(context.name()));
        registrations.add(owner, () -> main.unregister(subCommand));
    }

    @Override
    public void registerShopLogic(Plugin owner, String key, ShopLogic logic) {
        CustomLogicRegistry.register(key, () -> new ApiShopLogic(logic));
        registrations.add(owner, () -> CustomLogicRegistry.unregister(key));
    }

    @Override
    public void registerArmorTier(Plugin owner, ArmorTierDefinition tier) {
        ArmorTier registered = ArmorTier.register(tier.name(), tier.rank(), tier.boots(), tier.leggings(),
                tier.chestplate(), tier.helmet());
        registrations.add(owner, () -> ArmorTier.unregister(registered));
    }

    @Override
    public void registerUpgrade(Plugin owner, UpgradeDefinition upgrade) {
        if (upgrade.getEffect() == null && upgrade.getTrap() == null) {
            throw new IllegalArgumentException("an upgrade needs an effect or a trap");
        }
        Upgrade registered = Upgrade.register(upgrade.getName(), upgrade.getMaxLevel(), new ApiUpgradeHandler(upgrade), upgrade.getPrices());
        registrations.add(owner, () -> Upgrade.unregister(registered));
        var items = NewBedwars.plugin.getCustomItemManager();
        if (items.getCustomItem(upgrade.getName()) == null) items.createCustomUpdateItem(upgrade.getName(), upgrade.getIcon());
    }

    @Override
    public void unregisterAll(Plugin owner) {
        registrations.releaseAll(owner);
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        registrations.releaseAll(event.getPlugin());
    }
}
