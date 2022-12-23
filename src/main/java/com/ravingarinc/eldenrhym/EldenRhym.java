package com.ravingarinc.eldenrhym;

import com.ravingarinc.eldenrhym.api.ManagerLoadException;
import com.ravingarinc.eldenrhym.api.Module;
import com.ravingarinc.eldenrhym.character.CharacterManager;
import com.ravingarinc.eldenrhym.combat.CombatListener;
import com.ravingarinc.eldenrhym.combat.CombatManager;
import com.ravingarinc.eldenrhym.file.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class EldenRhym extends JavaPlugin {
    public static boolean debug;
    private static Logger logger;

    private Map<Class<? extends Module>, Module> modules;

    /**
     * Expects a message where %s will be replaced by the provided terms
     *
     * @param level        The log level
     * @param message      The message
     * @param replacements The replacements
     */
    public static void log(final Level level, final String message, final Object... replacements) {
        log(level, message, null, replacements);
    }

    public static void log(final Level level, final String message, @Nullable final Throwable throwable, final Object... replacements) {
        String format = message;
        for (final Object replacement : replacements) {
            format = format.replace("%s", replacement.toString());
        }
        if (throwable == null) {
            logger.log(level, format);
        } else {
            logger.log(level, format, throwable);
        }
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
        loadModules();
        validateLoad();
    }

    private void loadModules() {
        modules = new HashMap<>(); // this must be initialised even if it isn't filled, so that getManager() works

        // add managers
        addModule(ConfigManager.class);
        addModule(CombatManager.class);
        addModule(CharacterManager.class);

        // add listeners
        addModule(CombatListener.class);

        // sort managers
        modules = modules.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, HashMap::new));

        // load managers
        modules.values().forEach(manager -> {
            try {
                manager.initLoad();
            } catch (final ManagerLoadException e) {
                EldenRhym.log(Level.SEVERE, e.getMessage());
            }
        });
    }

    private void validateLoad() {
        int loaded = 0;
        for (final Module module : modules.values()) {
            if (module.isLoaded()) {
                loaded++;
            }
        }
        final boolean success = loaded == modules.size();
        EldenRhym.log(success ? Level.INFO : Level.WARNING,
                loaded + " / " + modules.size() + " Modules have been loaded! " + getName() +
                        (success
                                ? " has been loaded successfully with no issues!"
                                : " has failed to load correctly as some modules could not be loaded! Please check your logs!"));

    }

    public void reload() {
        modules.values().forEach(manager -> {
            try {
                manager.initReload();
            } catch (final ManagerLoadException e) {
                EldenRhym.log(Level.SEVERE, e.getMessage());
            }
        });
        validateLoad();
    }

    private <T extends Module> void addModule(final Class<T> module) {
        final Optional<? extends Module> opt = Module.initialise(this, module);
        opt.ifPresent(t -> modules.put(module, t));
    }

    /**
     * Get the manager of the specified type otherwise an IllegalArgumentException is thrown.
     *
     * @param type The manager type
     * @param <T>  The type
     * @return The manager
     */
    @SuppressWarnings("unchecked")
    public <T extends Module> T getManager(final Class<T> type) {
        final Module module = modules.get(type);
        if (module == null) {
            throw new IllegalArgumentException("Could not find module of type " + type.getName() + ". Contact developer! Most likely EldenRhym.getManager() has been called from a Module's constructor!");
        }
        return (T) module;
    }

    @Override
    public void onDisable() {
        modules.values().forEach(Module::shutdown);
        this.getServer().getScheduler().cancelTasks(this);

        log(Level.INFO, getName() + " is disabled.");
    }
}
