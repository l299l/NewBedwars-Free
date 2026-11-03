package com.l299l.newbedwars.version;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Field;

public final class VersionCompat {

    public static final boolean IS_1_20_5_PLUS;

    public static final Enchantment PROTECTION;
    public static final Enchantment SHARPNESS;
    public static final Enchantment POWER;
    public static final Enchantment INFINITY;
    public static final Enchantment GLOW_ENCHANT;

    public static final PotionEffectType SLOWNESS;
    public static final PotionEffectType HASTE;
    public static final PotionEffectType MINING_FATIGUE;
    public static final PotionEffectType JUMP_BOOST;
    public static final PotionEffectType SPEED;
    public static final PotionEffectType INVISIBILITY;

    public static final PotionType POTION_SPEED;
    public static final PotionType POTION_JUMP;
    public static final PotionType POTION_INVISIBILITY;

    public static final Particle DUST;

    static {
        IS_1_20_5_PLUS = isAtLeast(1, 20, 5);

        PROTECTION   = enchantment("protection");
        SHARPNESS    = enchantment("sharpness");
        POWER        = enchantment("power");
        INFINITY     = enchantment("infinity");
        GLOW_ENCHANT = enchantment("unbreaking");

        SLOWNESS       = potionEffect("SLOWNESS",       "SLOW");
        HASTE          = potionEffect("HASTE",          "FAST_DIGGING");
        MINING_FATIGUE = potionEffect("MINING_FATIGUE", "SLOW_DIGGING");
        JUMP_BOOST     = potionEffect("JUMP_BOOST",     "JUMP");
        SPEED          = potionEffect("SWIFTNESS",      "SPEED");
        INVISIBILITY   = potionEffect("INVISIBILITY",   "INVISIBILITY");

        POTION_SPEED       = potionType("SWIFTNESS",   "SPEED");
        POTION_JUMP        = potionType("LEAPING",     "JUMP");
        POTION_INVISIBILITY= potionType("INVISIBILITY","INVISIBILITY");

        DUST = particle("DUST", "REDSTONE");
    }

    private VersionCompat() {}

    public static boolean isAtLeast(int major, int minor, int patch) {
        String ver = Bukkit.getBukkitVersion().split("-")[0];
        String[] p = ver.split("\\.");
        try {
            int maj = Integer.parseInt(p[0]);
            int min = p.length > 1 ? Integer.parseInt(p[1]) : 0;
            int pat = p.length > 2 ? Integer.parseInt(p[2]) : 0;
            if (maj != major) return maj > major;
            if (min != minor) return min > minor;
            return pat >= patch;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static Enchantment enchantment(String minecraftKey) {
        return Enchantment.getByKey(NamespacedKey.minecraft(minecraftKey));
    }

    private static PotionEffectType potionEffect(String modern, String legacy) {
        PotionEffectType t = PotionEffectType.getByName(modern);
        return t != null ? t : PotionEffectType.getByName(legacy);
    }

    private static PotionType potionType(String modern, String legacy) {
        try {
            return PotionType.valueOf(modern);
        } catch (IllegalArgumentException e) {
            try {
                return PotionType.valueOf(legacy);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    private static Particle particle(String modern, String legacy) {
        try {
            return Particle.valueOf(modern);
        } catch (IllegalArgumentException e) {
            try {
                return Particle.valueOf(legacy);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    public static void spawnDust(World world, Location at, int count, double spreadX, double spreadY, double spreadZ,
                                 Color color, float size) {
        if (DUST == null) return;
        world.spawnParticle(DUST, at, count, spreadX, spreadY, spreadZ, 0, new Particle.DustOptions(color, size));
    }

    @SuppressWarnings("deprecation")
    public static void setSkullOwner(SkullMeta meta, String playerName) {
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
    }

    public static void addGlowEffect(ItemMeta meta) {
        if (GLOW_ENCHANT != null) {
            meta.addEnchant(GLOW_ENCHANT, 1, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    @SuppressWarnings("deprecation")
    public static void applyPotionType(PotionMeta meta, PotionType type) {
        if (type == null) return;
        if (IS_1_20_5_PLUS) {
            try {
                meta.getClass()
                        .getMethod("setBasePotionType", PotionType.class)
                        .invoke(meta, type);
            } catch (ReflectiveOperationException ignored) {}
        } else {
            try {
                Class<?> pdClass = Class.forName("org.bukkit.potion.PotionData");
                Object pd = pdClass.getConstructor(PotionType.class, boolean.class, boolean.class)
                        .newInstance(type, false, false);
                meta.getClass()
                        .getMethod("setBasePotionData", pdClass)
                        .invoke(meta, pd);
            } catch (ReflectiveOperationException ignored) {}
        }
    }

    public static void addProtection(ItemMeta meta, int level) {
        if (level > 0 && PROTECTION != null) {
            meta.addEnchant(PROTECTION, level, true);
        }
    }

    public static org.bukkit.inventory.ItemStack createPlayerHead(String ownerName) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            setSkullOwner(meta, ownerName);
            item.setItemMeta(meta);
        }
        return item;
    }

    @SuppressWarnings("unchecked")
    public static void disableLocatorBar(World world) {
        try {
            Field field = GameRule.class.getField("LOCATOR_BAR");
            GameRule<Boolean> rule = (GameRule<Boolean>) field.get(null);
            if (rule != null) {
                world.setGameRule(rule, false);
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
        }
    }
}
