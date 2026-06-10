package com.l299l.newbedwars.utils.holograms;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Hologram {
    private final List<ArmorStand> armorStands;
    private final String text;

    public Hologram(String text, Location location, HashMap<String, String> replacements) {
        armorStands = new ArrayList<>();
        this.text = text;
        text = ChatColor.translateAlternateColorCodes('&', text);
        if (replacements != null) {
            for (String key : replacements.keySet()) {
                text = text.replaceAll(key, replacements.get(key));
            }
        }
        String[] lines = text.split(";|\n");
        Vector vector = new Vector(0, 0.5, 0);
        for (int i = lines.length - 1; i >= 0; i--) {
            ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location.add(vector), EntityType.ARMOR_STAND);
            armorStand.setCustomName(lines[i]);
            armorStand.setCustomNameVisible(true);
            armorStand.setGravity(false);
            armorStand.setAI(false);
            armorStand.setVisible(false);
            armorStand.setInvulnerable(true);
            armorStand.setCollidable(false);
            armorStand.setCanPickupItems(false);
            vector = new Vector(0, 0.3, 0);
            armorStands.add(armorStand);
        }
    }

    public void remove() {
        for (ArmorStand armorStand : armorStands) {
            armorStand.remove();
        }
    }

    public void reload() {
        String text = this.text;
        text = ChatColor.translateAlternateColorCodes('&', text);
        String[] lines = text.split(";|\n");
        int j = 0;
        for (int i = lines.length - 1; i >= 0; i--) {
            armorStands.get(j).setCustomName(lines[i]);
            j++;
        }
    }

    public void reload(HashMap<String, String> replacements) {
        String text = this.text;
        text = ChatColor.translateAlternateColorCodes('&', text);
        if (replacements != null) {
            for (String key : replacements.keySet()) {
                text = text.replaceAll(key, replacements.get(key));
            }
        }
        String[] lines = text.split(";|\n");
        int j = 0;
        for (int i = lines.length - 1; i >= 0; i--) {
            armorStands.get(j).setCustomName(lines[i]);
            j++;
        }
    }

    public ArmorStand getArmorStand(int index) {
        return armorStands.get(index);
    }

    public ArmorStand getArmorStand() {
        return armorStands.get(0);
    }

}
