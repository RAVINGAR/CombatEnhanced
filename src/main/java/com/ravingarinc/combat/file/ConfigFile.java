package com.ravingarinc.combat.file;

import com.ravingarinc.combat.CombatEnhanced;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        try (final InputStream stream = plugin.getResource(name)) {
            if (stream != null) {
                final YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
                this.config.setDefaults(defaultConfig);
            }
        } catch (final IOException e) {
            CombatEnhanced.log(Level.SEVERE, "Encountered issue loading default configuration for file '%s'!", name, e);
        }
        config.options().copyDefaults(true);
        saveConfig();
    }

    public final void reloadConfig() {
        saveDefaultConfig();
    }

    public final void saveConfig() {
        try {
            config.save(file);
        } catch (final IOException e) {
            CombatEnhanced.log(Level.SEVERE, "Encountered error saving config; " + name + "\n" + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

}
