package com.ravingarinc.eldenrhym.file;

import com.ravingarinc.eldenrhym.EldenRhym;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigFile {
    private final JavaPlugin plugin;
    private final String name;
    private final File file;
    private final FileConfiguration config;

    public ConfigFile(final JavaPlugin plugin, final String name) {
        this.plugin = plugin;
        this.name = name;
        this.file = new File(plugin.getDataFolder(), name);
        this.config = YamlConfiguration.loadConfiguration(file);
        saveDefaultConfig();
    }

    private void saveDefaultConfig() {
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
    }

    public void reloadConfig() {
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            EldenRhym.log(Level.SEVERE, "Encountered error reloading config; " + name + "\n" + e.getMessage());
        }
    }

    public void saveConfig() {
        try {
            config.save(file);
        } catch (final IOException e) {
            EldenRhym.log(Level.SEVERE, "Encountered error saving config; " + name + "\n" + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

}
