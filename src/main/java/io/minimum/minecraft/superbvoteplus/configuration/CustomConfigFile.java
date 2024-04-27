package io.minimum.minecraft.superbvoteplus.configuration;

import io.minimum.minecraft.superbvoteplus.SuperbVotePlus;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

public abstract class CustomConfigFile {

    public CustomConfigFile(String fileName){
        File customConfigFile;
        customConfigFile = new File(SuperbVotePlus.getPlugin().getDataFolder(), fileName);
        if (!customConfigFile.exists()) {
            InputStream inputStream = SuperbVotePlus.getPlugin().getResource(fileName);
            if(inputStream == null) {
                throw new IllegalArgumentException("Resource doesn't exist!");
            }
            try{
                customConfigFile.getParentFile().mkdirs();
                OutputStream outStream = new FileOutputStream(customConfigFile);
                outStream.write(inputStream.readAllBytes());
                outStream.close();
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        config = new YamlConfiguration();
        try {
            config.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    protected FileConfiguration config;
}
