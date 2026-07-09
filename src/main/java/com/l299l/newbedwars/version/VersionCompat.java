package com.l299l.newbedwars.version;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Field;

/** Cross-version compatibility layer for 1.17 – 26.1.2. Reflection handles PotionData/setBasePotionType API gap in 1.20.5. */
public final class VersionCompat {

    /** True when running on Paper 1.20.5 or newer. */
    public static final boolean IS_1_20_5_PLUS;

    // ── Enchantments (NamespacedKey lookup is stable across all versions) ─────
    public static final Enchantment PROTECTION;   // was PROTECTION_ENVIRONMENTAL
    public static final Enchantment SHARPNESS;    // was DAMAGE_ALL
    public static final Enchantment POWER;        // was ARROW_DAMAGE
    public static final Enchantment INFINITY;     // was ARROW_INFINITE
    /** Any valid enchantment used solely to make an item visually glow (always hidden). */
    public static final Enchantment GLOW_ENCHANT; // unbreaking – exists in every version

    // ── PotionEffectTypes (name changed in 1.20.5) ────────────────────────────
    public static final PotionEffectType SLOWNESS;       // was SLOW
    public static final PotionEffectType HASTE;          // was FAST_DIGGING
    public static final PotionEffectType MINING_FATIGUE; // was SLOW_DIGGING
    public static final PotionEffectType JUMP_BOOST;     // was JUMP
    public static final PotionEffectType SPEED;          // SWIFTNESS in 1.20.5+
    public static final PotionEffectType INVISIBILITY;   // unchanged across versions

    // ── PotionTypes (name changed in 1.20.5) ──────────────────────────────────
    public static final PotionType POTION_SPEED;       // was SPEED  → SWIFTNESS
    public static final PotionType POTION_JUMP;        // was JUMP   → LEAPING
    public static final PotionType POTION_INVISIBILITY;// stable in all versions

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
    }

    private VersionCompat() {}

    // ── Version detection ─────────────────────────────────────────────────────

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

    // ── Private resolvers ─────────────────────────────────────────────────────

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

    // ── Public API ────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    public static void setSkullOwner(SkullMeta meta, String playerName) {
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
    }

    /** Adds enchantment shimmer to an item using hidden Unbreaking I (valid in all versions). */
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

    /**
     * Disables the "Locator Bar" gamerule (the XP-bar player tracker added in 26.x) on servers
     * that have it. {@code GameRule.LOCATOR_BAR} does not exist in the 1.17.1 API we compile
     * against, so it can't be referenced directly — but the *running* server's own copy of
     * {@code org.bukkit.GameRule} (not our compile-time stub) does have the field on 26.x, so a
     * plain reflective field lookup finds it there. On any version without this gamerule,
     * getField() throws NoSuchFieldException and this is a silent no-op.
     */
    @SuppressWarnings("unchecked")
    public static void disableLocatorBar(World world) {
        try {
            Field field = GameRule.class.getField("LOCATOR_BAR");
            GameRule<Boolean> rule = (GameRule<Boolean>) field.get(null);
            if (rule != null) {
                world.setGameRule(rule, false);
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // No Locator Bar gamerule on this server version — nothing to disable.
        }
    }
}
