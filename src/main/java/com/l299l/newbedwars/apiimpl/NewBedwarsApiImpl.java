package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.AddonCommand;
import com.l299l.newbedwars.api.ArenaEditor;
import com.l299l.newbedwars.api.ArmorTierDefinition;
import com.l299l.newbedwars.api.CommandContext;
import com.l299l.newbedwars.api.PhaseActionHandler;
import com.l299l.newbedwars.api.ShopLogic;
import com.l299l.newbedwars.api.UpgradeDefinition;
import com.l299l.newbedwars.arena.phases.PhaseActionRegistry;
import com.l299l.newbedwars.arena.player.inventory.ArmorTier;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogicRegistry;
import com.l299l.newbedwars.commands.bedwars.MainCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import com.l299l.newbedwars.api.ArenaView;
import com.l299l.newbedwars.api.NewBedwarsAPI;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public final class NewBedwarsApiImpl implements NewBedwarsAPI, Listener {
    private final AddonRegistrations registrations = new AddonRegistrations();

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
        AddonSubCommand subCommand = new AddonSubCommand(command);
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
        ArmorTier registered = ArmorTier.register(tier.getName(), tier.getRank(), tier.getBoots(), tier.getLeggings(),
                tier.getChestplate(), tier.getHelmet());
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
