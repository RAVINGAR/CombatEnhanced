package com.ravingarinc.eldenrhym;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class EldenRhym extends JavaPlugin {
    private static Logger logger;

    private static EldenRhym plugin;

    public static void log(final Level level, final String message) {
        logger.log(level, message);
    }

    /**
     * Expects a message where $1, $2... will be replaced by the provided terms
     *
     * @param level        The log level
     * @param message      The message
     * @param replacements The replacements
     */
    public static void log(final Level level, final String message, final Object... replacements) {
        String format = message;
        for (int i = 0; i < replacements.length; i++) {
            format = format.replaceAll("\\$" + (i + 1), replacements[i].toString());
        }
        log(level, format);
    }

    @Override
    public void onLoad() {
        logger = this.getLogger();
        //setup mmoitems here
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
