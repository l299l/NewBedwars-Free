package com.l299l.newbedwars.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import com.l299l.newbedwars.NewBedwars;
import java.util.logging.Level;

public class PluginFiles {

    public static String readFileContent(File file) {
        String fileContent = "";
        StringBuilder contentBuilder = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null)
            {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not read " + file.getPath(), e);
        }

        fileContent = contentBuilder.toString();
        return fileContent;
    }
    @Nullable
    public InputStream getFilesResource(@Nonnull String filename, ClassLoader classLoader) {
        try {
            URL url = classLoader.getResource(filename);
            if (url == null) {
                return null;
            } else {
                URLConnection connection = url.openConnection();
                connection.setUseCaches(false);
                return connection.getInputStream();
            }
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not open the bundled resource " + filename, e);
            return null;
        }
    }
}
