package com.l299l.newbedwars.config.data.yaml.guis;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.gui.GuiSave;
import com.l299l.newbedwars.gui.configuration.game.guis.other.GuiCategory;
import com.l299l.newbedwars.gui.configuration.game.guis.other.GuiUpgrade;
import com.tchristofferson.configupdater.ConfigUpdater;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class GuiDataManager {
    List<GuiSave> guiSaves;

    public GuiDataManager() {
        guiSaves = new ArrayList<>();
    }
    public void load() {
        Path guiDir = new File(NewBedwars.plugin.getDataFolder(), "data/guis").toPath();
        if (guiDir.toFile().exists()) {
            File[] guis = guiDir.toFile().listFiles();
            if (guis != null) {
                for (File gui: guis) {
                    guiSaves.add(new GuiDataYaml(gui).getGuiSave());
                }
            }
        }else {
            guiDir.toFile().mkdirs();
            List<File> exampleGuis = createExampleGuis();
            if (exampleGuis != null) {
                for (File exampleGui: exampleGuis) {
                    guiSaves.add(new GuiDataYaml(exampleGui).getGuiSave());
                }
            }
        }
    }

    public void save() {
        for (GuiSave guiSave: guiSaves) {
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
                fileWriter.write("HeaderSize: " + guiSave.getGuiData("HeaderSize") + "\n");
                fileWriter.write("FooterSize: " + guiSave.getGuiData("FooterSize") + "\n");
                fileWriter.write("Slots:\n");
                for (int i = 0; i < guiSave.getSize(); i++) {
                    Object item = guiSave.getItem(i);
                    if (item == null) {
                        fileWriter.write("  " + (i+1) + ": empty\n");
                    }else if (item instanceof String) {
                        fileWriter.write("  " + (i+1) + ": " + item + "\n");
                    }else if (item instanceof CustomItem customItem){
                        fileWriter.write("  " + (i+1) + ": ci:" + customItem.getName() + "\n");
                    }else if (item instanceof GuiCategory guiCategory) {
                        fileWriter.write("  " + (i+1) + ": cat:" + guiCategory.id() + "\n");
                    }else if (item instanceof GuiUpgrade guiUpgrade) {
                        fileWriter.write("  " + (i+1) + ": upg:" + guiUpgrade.id() + "\n");
                    }
                }
                fileWriter.write("Categories:\n");
                HashMap<String, GuiCategory> guiCategories = (HashMap<String, GuiCategory>) guiSave.getGuiData("Categories");
                for (GuiCategory guiCategory: guiCategories.values()) {
                    fileWriter.write("  - ID: " + guiCategory.id() + "\n");
                    fileWriter.write("    Name: " + guiCategory.name() + "\n");
                    fileWriter.write("    Icon: " + guiCategory.icon().getName() + "\n");
                    fileWriter.write("    Items:\n");
                    for (Object catObj: guiCategory.items()) {
                        if (catObj instanceof CustomItem customItem) {
                            fileWriter.write("      - ci:" + customItem.getName() + "\n");
                        }else if (catObj instanceof GuiUpgrade guiUpgrade) {
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

    public List<GuiSave> getGuiSaves() {
        return guiSaves;
    }

    public void addGuiSave(GuiSave guiSave) {
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
