package com.ravingarinc.eldenrhym;

import com.ravingarinc.eldenrhym.api.Manager;
import com.ravingarinc.eldenrhym.api.ManagerLoadException;
import com.ravingarinc.eldenrhym.character.CharacterManager;
import com.ravingarinc.eldenrhym.combat.CombatListener;
import com.ravingarinc.eldenrhym.combat.CombatManager;
import com.ravingarinc.eldenrhym.file.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EldenRhym extends JavaPlugin {
    public static boolean debug;
    private static Logger logger;

    private List<Manager> managers;

    /**
     * Expects a message where %s will be replaced by the provided terms
     *
     * @param level        The log level
     * @param message      The message
     * @param replacements The replacements
     */
    public static void log(final Level level, final String message, final Object... replacements) {
        String format = message;
        for (int i = 0; i < replacements.length; i++) {
            format = format.replaceAll("%s" + (i + 1), replacements[i].toString());
        }
        logger.log(level, message);
    }

    public static void log(final Level level, final String message, final Throwable throwable) {
        logger.log(level, message, throwable);
    }

    public static void logIfDebug(final Supplier<String> message, final Object... replacements) {
        if (debug) {
            log(Level.WARNING, message.get(), replacements);
        }
    }

    public static void runIfDebug(final Runnable runnable) {
        if (debug) {
            runnable.run();
        }
    }

    @Override
    public void onLoad() {
        logger = this.getLogger();
        //setup mmoitems here
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        loadManagers();
        validateLoad();
    }

    private void loadManagers() {
        managers = new ArrayList<>();

        // load managers
        addManager(ConfigManager.class);
        addManager(CombatManager.class);
        addManager(CharacterManager.class);

        // load listeners
        addManager(CombatListener.class);

        Collections.sort(managers);

        managers.forEach(manager -> {
            try {
                manager.initLoad();
            } catch (final ManagerLoadException e) {
                EldenRhym.log(Level.SEVERE, e.getMessage());
            }
        });
    }

    private void validateLoad() {
        int loaded = 0;
        for (final Manager manager : managers) {
            if (manager.isLoaded()) {
                loaded++;
            }
        }
        final boolean success = loaded == managers.size();
        EldenRhym.log(success ? Level.INFO : Level.WARNING,
                loaded + "/" + managers.size() + " managers/listeners have been loaded! " +
                        (success
                                ? "EldenRhym has been loaded successfully with no issues!"
                                : managers.size() - loaded + " of these could not be loaded, this may cause problems. Check your logs!"));

    }

    public void reload() {
        managers.forEach(manager -> {
            try {
                manager.initReload();
            } catch (final ManagerLoadException e) {
                EldenRhym.log(Level.SEVERE, e.getMessage());
            }
        });
        validateLoad();
    }

    private <T extends Manager> void addManager(final Class<T> manager) {
        final Optional<T> opt = Manager.initialise(this, manager);
        opt.ifPresent(t -> managers.add(t));
    }

    /**
     * Get the manager of the specified type otherwise an IllegalArgumentException is thrown.
     *
     * @param type The manager type
     * @param <T>  The type
     * @return The manager
     */
    @SuppressWarnings("unchecked")
    public <T extends Manager> T getManager(final Class<T> type) {
        for (final Manager manager : managers) {
            if (manager.getClazz() == type) {
                return (T) manager;
            }
        }
        throw new IllegalArgumentException("Could not find manager of type " + type.getName());
    }

    @Override
    public void onDisable() {
        managers.forEach(Manager::shutdown);
        this.getServer().getScheduler().cancelTasks(this);

        log(Level.INFO, "EldenRhym is disabled.");
    }
}
