package com.l299l.newbedwars.world.schematic;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.l299l.newbedwars.NewBedwars;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SchematicManager {

    public static boolean isWorldEditPresent() {
        return Bukkit.getPluginManager().getPlugin("WorldEdit") != null
                || Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
    }

    public static File getSchematicsDir() {
        File dir = new File(NewBedwars.plugin.getDataFolder(), "schematics");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** Returns the WorldEdit/FAWE schematics folder, or null if neither is installed. */
    private static File getWorldEditSchematicsDir() {
        org.bukkit.plugin.Plugin we = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        if (we == null) we = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (we == null) return null;
        return new File(we.getDataFolder(), "schematics");
    }

    public static List<String> listSchematics() {
        Set<String> names = new LinkedHashSet<>();
        addSchematicsFrom(getSchematicsDir(), names);
        File weDir = getWorldEditSchematicsDir();
        if (weDir != null) addSchematicsFrom(weDir, names);
        return new ArrayList<>(names);
    }

    private static void addSchematicsFrom(File dir, Set<String> names) {
        File[] files = dir == null ? null : dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String n = f.getName();
            if (n.endsWith(".schem")) names.add(n.substring(0, n.length() - 6));
            else if (n.endsWith(".schematic")) names.add(n.substring(0, n.length() - 10));
        }
    }

    /** Finds a schematic file by name, checking the plugin folder then the WorldEdit folder. */
    private static File findSchematicFile(String name) {
        for (File dir : new File[]{getSchematicsDir(), getWorldEditSchematicsDir()}) {
            if (dir == null) continue;
            File f = new File(dir, name + ".schem");
            if (f.exists()) return f;
            f = new File(dir, name + ".schematic");
            if (f.exists()) return f;
        }
        return null;
    }

    /**
     * Pastes a saved schematic into the given Bukkit world at the world's spawn point (0, 64, 0).
     */
    public static void pasteIntoWorld(World world, String name) throws IOException, WorldEditException {
        File file = findSchematicFile(name);
        if (file == null) throw new SchematicNotFoundException(name);

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;

        Clipboard clipboard;
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            clipboard = reader.read();
        }

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        BlockVector3 pasteAt = BlockVector3.at(0, 64, 0);
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(pasteAt)
                    .ignoreAirBlocks(false)
                    .build();
            Operations.complete(operation);
        }
    }

    public static class SchematicNotFoundException extends IOException {
        public SchematicNotFoundException(String name) {
            super("Schematic not found: " + name);
        }
    }
}
