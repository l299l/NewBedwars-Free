package com.l299l.newbedwars.config.data.yaml.guis;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.gui.GUISave;
import com.l299l.newbedwars.gui.model.GUICategory;
import com.l299l.newbedwars.gui.model.GUIEntry;
import com.l299l.newbedwars.gui.model.GUIUpgrade;
import com.tchristofferson.configupdater.ConfigUpdater;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class GUIDataManager {
    List<GUISave> guiSaves;

    public GUIDataManager() {
        guiSaves = new ArrayList<>();
    }
    public void load() {
        Path guiDir = new File(NewBedwars.plugin.getDataFolder(), "data/guis").toPath();
        if (guiDir.toFile().exists()) {
            File[] guis = guiDir.toFile().listFiles();
            if (guis != null) {
                for (File gui: guis) {
                    guiSaves.add(new GUIDataYaml(gui).getGuiSave());
                }
            }
        }else {
            guiDir.toFile().mkdirs();
            List<File> exampleGuis = createExampleGuis();
            if (exampleGuis != null) {
                for (File exampleGui: exampleGuis) {
                    guiSaves.add(new GUIDataYaml(exampleGui).getGuiSave());
                }
            }
        }
    }

    public void save() {
        for (GUISave guiSave: guiSaves) {
            String[] nameS = guiSave.getName().split(" ");
            StringBuilder name = new StringBuilder();
            boolean first = true;
            for (String s: nameS) {
                if (first) {
                    name.append(s.toLowerCase());
                    first = false;
                    continue;
                }
                String firstLetter = s.substring(0, 1).toUpperCase();
                String rest = s.substring(1).toLowerCase();
                name.append(firstLetter).append(rest);
            }
            File guiFile = new File(NewBedwars.plugin.getDataFolder(), "data/guis/" + name + ".yml");
            try {
                if (!guiFile.exists()) {
                    guiFile.createNewFile();
                }
                FileWriter fileWriter = new FileWriter(guiFile);
                fileWriter.write("GuiClass: " + guiSave.getGuiClass().getSimpleName() + "\n");
                fileWriter.write("GuiId: " + guiSave.getGuiId() + "\n");
                fileWriter.write("GuiName: " + guiSave.getName() + "\n");
                fileWriter.write("Size: " + guiSave.getSize() + "\n");
                fileWriter.write("CloseOnTransaction: " + guiSave.getCloseOnTransaction() + "\n");
                fileWriter.write("HeaderSize: " + guiSave.getHeaderSize() + "\n");
                fileWriter.write("FooterSize: " + guiSave.getFooterSize() + "\n");
                fileWriter.write("Slots:\n");
                for (int i = 0; i < guiSave.getSize(); i++) {
                    GUIEntry item = guiSave.getItem(i);
                    if (item == null) {
                        fileWriter.write("  " + (i+1) + ": empty\n");
                    }else if (item instanceof CustomItem customItem){
                        fileWriter.write("  " + (i+1) + ": ci:" + customItem.getName() + "\n");
                    }else if (item instanceof GUICategory guiCategory) {
                        fileWriter.write("  " + (i+1) + ": cat:" + guiCategory.id() + "\n");
                    }else if (item instanceof GUIUpgrade guiUpgrade) {
                        fileWriter.write("  " + (i+1) + ": upg:" + guiUpgrade.id() + "\n");
                    }
                }
                fileWriter.write("Categories:\n");
                for (GUICategory guiCategory: guiSave.getCategories().values()) {
                    fileWriter.write("  - ID: " + guiCategory.id() + "\n");
                    fileWriter.write("    Name: " + guiCategory.name() + "\n");
                    fileWriter.write("    Icon: " + guiCategory.icon().getName() + "\n");
                    fileWriter.write("    Items:\n");
                    for (GUIEntry catObj: guiCategory.items()) {
                        if (catObj instanceof CustomItem customItem) {
                            fileWriter.write("      - ci:" + customItem.getName() + "\n");
                        }else if (catObj instanceof GUIUpgrade guiUpgrade) {
                            fileWriter.write("      - upg:" + guiUpgrade.id() + "\n");
                        }else {
                            fileWriter.write("      - wool\n");
                        }
                    }
                }
                fileWriter.close();
                ConfigUpdater.update(NewBedwars.plugin, "exampleShop.yml", guiFile, Collections.singletonList("Slots"));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public List<GUISave> getGuiSaves() {
        return guiSaves;
    }

    public void addGuiSave(GUISave guiSave) {
        guiSaves.add(guiSave);
    }

    private List<File> createExampleGuis() {
        List<File> files = new ArrayList<>();
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("exampleShop.yml");
            InputStream inputStream2 = getClass().getClassLoader().getResourceAsStream("exampleUpgrade.yml");
            File file = new File(NewBedwars.plugin.getDataFolder(), "data/guis/exampleShop.yml");
            File file2 = new File(NewBedwars.plugin.getDataFolder(), "data/guis/exampleUpgrade.yml");
            assert inputStream != null;
            assert inputStream2 != null;
            FileUtils.copyInputStreamToFile(inputStream, file);
            FileUtils.copyInputStreamToFile(inputStream2, file2);
            files.add(file);
            files.add(file2);
            return files;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
