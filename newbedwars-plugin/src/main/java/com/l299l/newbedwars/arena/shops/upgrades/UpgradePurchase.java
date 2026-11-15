package com.l299l.newbedwars.arena.shops.upgrades;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.events.ShopPurchaseEvent;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.data.yaml.guis.GuiDefinition;
import com.l299l.newbedwars.gui.GUISave;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class UpgradePurchase {

    public enum Outcome { BOUGHT, MAXED, NOT_ENOUGH, CANCELLED }

    public record Result(Outcome outcome, UpgradeTier tier) {
        public boolean bought() {
            return outcome == Outcome.BOUGHT;
        }
    }

    private UpgradePurchase() {}

    public static UpgradePricing pricing() {
        return NewBedwars.plugin.getDataManager().getUpgradePricing();
    }

    public static GuiDefinition gui(IArena arena) {
        if (arena == null || NewBedwars.plugin.getGuiManager() == null) return null;
        GUISave save = NewBedwars.plugin.getGuiManager().getGui(arena.getUpgradeGuiId());
        return save == null ? null : save.getDefinition();
    }

    public static UpgradeTier nextTier(IArena arena, Upgrade upgrade, int currentLevel) {
        return pricing().next(upgrade, currentLevel, gui(arena));
    }

    public static int maxLevel(IArena arena, Upgrade upgrade) {
        return pricing().maxLevel(upgrade, gui(arena));
    }

    public static Result buy(Player player, IArena arena, Team team, Upgrade upgrade, String itemName) {
        return buy(player, arena, team, upgrade, itemName, pricing(), gui(arena));
    }

    static Result buy(Player player, IArena arena, Team team, Upgrade upgrade, String itemName,
                      UpgradePricing pricing, GuiDefinition gui) {
        TeamUpgrades tu = team.getTeamUpgrades();
        int currentLevel = tu.getUpgradeLevel(upgrade);
        UpgradeTier tier = pricing.next(upgrade, currentLevel, gui);
        if (tier == null) return new Result(Outcome.MAXED, null);
        if (!player.getInventory().contains(tier.material(), tier.price())) return new Result(Outcome.NOT_ENOUGH, tier);
        if (!ApiEvents.shopPurchase(arena, player, itemName, ShopPurchaseEvent.Kind.UPGRADE, tier.currency().name(), tier.price())
                || !ApiEvents.teamUpgrade(arena, team, upgrade.name(), currentLevel + 1, player)) {
            return new Result(Outcome.CANCELLED, tier);
        }
        if (tu.getUpgradeLevel(upgrade) != currentLevel || !player.getInventory().contains(tier.material(), tier.price())) {
            return new Result(Outcome.CANCELLED, tier);
        }
        if (tier.price() > 0) player.getInventory().removeItem(new ItemStack(tier.material(), tier.price()));
        tu.upgrade(upgrade);
        return new Result(Outcome.BOUGHT, tier);
    }
}
