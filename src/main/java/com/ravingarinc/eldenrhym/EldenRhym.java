package com.ravingarinc.eldenrhym;

import com.ravingarinc.eldenrhym.api.AsyncHandler;
import com.ravingarinc.eldenrhym.api.Module;
import com.ravingarinc.eldenrhym.api.ModuleLoadException;
import com.ravingarinc.eldenrhym.character.CharacterListener;
import com.ravingarinc.eldenrhym.character.CharacterManager;
import com.ravingarinc.eldenrhym.combat.CombatListener;
import com.ravingarinc.eldenrhym.combat.CombatManager;
import com.ravingarinc.eldenrhym.command.ReloadCommand;
import com.ravingarinc.eldenrhym.file.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EldenRhym extends JavaPlugin {
    public static boolean debug;
    private static Logger logger;

    private static EldenRhym instance;

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

    public static EldenRhym getInstance() {
        return instance;
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
        EldenRhym.instance = this;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        AsyncHandler.load(this);
        loadModules();
        validateLoad();

        getCommand("rhymreload").setExecutor(new ReloadCommand());
    }

    private void loadModules() {
        modules = new LinkedHashMap<>();

        // add managers
        addModule(ConfigManager.class);
        addModule(CharacterManager.class);
        addModule(CombatManager.class);

        // add listeners
        addModule(CombatListener.class);
        addModule(CharacterListener.class);

        // load modules
        modules.values().forEach(manager -> {
            try {
                manager.initLoad();
            } catch (final ModuleLoadException e) {
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
        if (loaded == modules.size()) {
            EldenRhym.log(Level.INFO, "%s has been enabled successfully!", getName());
        } else {
            EldenRhym.log(Level.WARNING, "%s module/s have failed to load! Please check your logs!", (modules.size() - loaded));
        }
    }

    public void reload() {
        modules.values().forEach(manager -> {
            try {
                manager.initReload();
            } catch (final ModuleLoadException e) {
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
    public <T extends Module> T getModule(final Class<T> type) {
        final Module module = modules.get(type);
        if (module == null) {
            throw new IllegalArgumentException("Could not find module of type " + type.getName() + ". Contact developer! Most likely EldenRhym.getManager() has been called from a Module's constructor!");
        }
        return (T) module;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void onDisable() {
        modules.values().forEach(module -> {
            try {
                module.tryShutdown();
            } catch (final Exception e) {
                EldenRhym.log(Level.SEVERE, "Encountered issue shutting down module '%s'!", e, module.getName());
            }
        });
        this.getServer().getScheduler().cancelTasks(this);

        log(Level.INFO, getName() + " is disabled.");
    }
}
