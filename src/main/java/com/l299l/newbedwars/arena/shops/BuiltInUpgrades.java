package com.l299l.newbedwars.arena.shops;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.version.VersionCompat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** The upgrades and traps that ship with the plugin. */
final class BuiltInUpgrades {
    private BuiltInUpgrades() {}

    static UpgradeHandler protection() {
        return new UpgradeHandler() {
            @Override
            public void apply(Player player, Team team, int level) {
                ItemStack[] armor = player.getInventory().getArmorContents();
                for (ItemStack piece : armor) {
                    if (piece != null && piece.getType() != Material.AIR) {
                        ItemMeta meta = piece.getItemMeta();
                        if (meta != null) {
                            VersionCompat.addProtection(meta, level);
                            piece.setItemMeta(meta);
                        }
                    }
                }
                player.getInventory().setArmorContents(armor);
            }
        };
    }

    static UpgradeHandler sharpness() {
        return new UpgradeHandler() {
            @Override
            public void apply(Player player, Team team, int level) {
                for (int i = 0; i < 36; i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (item != null && item.getType().name().endsWith("_SWORD")) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null && VersionCompat.SHARPNESS != null) {
                            meta.addEnchant(VersionCompat.SHARPNESS, level, true);
                            item.setItemMeta(meta);
                        }
                        player.getInventory().setItem(i, item);
                    }
                }
            }

            @Override
            public boolean reapplyOnRespawn() {
                return true;
            }
        };
    }

    static UpgradeHandler haste() {
        return new UpgradeHandler() {
            @Override
            public void apply(Player player, Team team, int level) {
                player.addPotionEffect(new PotionEffect(VersionCompat.HASTE, Integer.MAX_VALUE, level - 1, true, false));
            }

            @Override
            public boolean reapplyOnRespawn() {
                return true;
            }
        };
    }

    static UpgradeHandler forge() {
        return new UpgradeHandler() {
            @Override
            public void onPurchase(TeamUpgrades upgrades, Team team, int newLevel) {
                Generator gen = team.getGenerator();
                // the generator starts at level 1, so Forge level N means generator level N + 1 (capped at its max)
                if (gen != null) gen.upgrade(newLevel + 1);
            }
        };
    }

    static UpgradeHandler healPool() {
        return new UpgradeHandler() {
            @Override
            public void onPurchase(TeamUpgrades upgrades, Team team, int newLevel) {
                upgrades.startHealPool();
            }
        };
    }

    static UpgradeHandler alarmTrap() {
        return new UpgradeHandler() {
            @Override
            public boolean isTrap() {
                return true;
            }

            @Override
            public void trigger(Player enemy, Team team, IArena arena) {
                String enemyName = enemy.getName();
                for (Player ally : team.getPlayers()) {
                    ally.sendTitle(
                            ChatColor.RED + "" + ChatColor.BOLD + "⚠ ALARM ⚠",
                            ChatColor.YELLOW + enemyName + " is in your base!",
                            10, 60, 20
                    );
                }
                for (Player ally : team.getPlayers()) {
                    String alarmMsg = NewBedwars.plugin.getMessages().getMsg(ally, "AlarmTrapTriggered");
                    if (alarmMsg != null) ally.sendMessage(alarmMsg.replace("/team/", team.getName()));
                }
            }
        };
    }

    static UpgradeHandler blindTrap() {
        return new UpgradeHandler() {
            @Override
            public boolean isTrap() {
                return true;
            }

            @Override
            public void trigger(Player enemy, Team team, IArena arena) {
                enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 0, true, false));
                enemy.addPotionEffect(new PotionEffect(VersionCompat.SLOWNESS, 120, 1, true, false));
            }
        };
    }

    static UpgradeHandler miningFatigueTrap() {
        return new UpgradeHandler() {
            @Override
            public boolean isTrap() {
                return true;
            }

            @Override
            public void trigger(Player enemy, Team team, IArena arena) {
                enemy.addPotionEffect(new PotionEffect(VersionCompat.MINING_FATIGUE, 200, 2, true, false));
            }
        };
    }
}
