package com.ravingarinc.eldenrhym.api;

import com.ravingarinc.eldenrhym.EldenRhym;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

public abstract class Module implements Comparable<Module> {
    protected final EldenRhym plugin;
    protected final Class<? extends Module> clazz;

    protected final List<Class<? extends Module>> dependsOn;
    private boolean isLoaded;

    /**
     * The constructor for a Module, should only ever be called by {@link Module#initialise(EldenRhym, Class)}.
     * Implementations of Managers should have one public constructor with a EldenRhym object parameter.
     * The implementing constructor CANNOT call {@link EldenRhym#getManager(Class)} otherwise potential issues
     * may occur. This must be done in {@link this#load()}.
     *
     * @param identifier The class of this manager
     * @param plugin     The owning plugin
     * @param dependsOn  Other managers which are loaded before this manager
     */
    @SafeVarargs
    protected Module(final Class<? extends Module> identifier, final EldenRhym plugin, final Class<? extends Module>... dependsOn) {
        this.plugin = plugin;
        this.clazz = identifier;
        this.dependsOn = new ArrayList<>();
        for (final Class<? extends Module> manager : dependsOn) {
            this.dependsOn.add(manager);
        }
        this.isLoaded = false;
    }

    public static <T> Optional<T> initialise(final EldenRhym plugin, final Class<T> identifier) {
        try {
            final Constructor<T> constructor = identifier.getConstructor(EldenRhym.class);
            return Optional.of(constructor.newInstance(plugin));
        } catch (final NoSuchMethodException e) {
            EldenRhym.log(Level.SEVERE, "Could not find valid constructor for " + identifier.getName());
        } catch (final InvocationTargetException e) {
            EldenRhym.log(Level.SEVERE, "Failed to initialise manager '%s'!", e, identifier.getName());
        } catch (InstantiationException | IllegalAccessException e) {
            EldenRhym.log(Level.SEVERE, "Something went severely wrong creating new instance of manager '%s'!", e, identifier.getName());
        }
        return Optional.empty();
    }

    public Class<? extends Module> getClazz() {
        return clazz;
    }

    /**
     * Specific Module implementation of reload. This is called by {@link Module#initReload()} after isLoaded
     * is set to false. Where {@link Module#load()} is called after this method
     */
    protected abstract void reload();

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void initReload() throws ManagerLoadException {
        isLoaded = false;
        try {
            reload();
        } catch (final Exception e) {
            throw new ManagerLoadException(this, e);
        }
        initLoad();
    }

    /**
     * Load this manager. Will only be called if this manager's dependents are loaded.
     */
    protected abstract void load();

    /**
     * Called after initialisaton of all managers but before any managers of which this manager depends on.
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void initLoad() throws ManagerLoadException {
        boolean canLoad = true;
        for (final Class<? extends Module> clazz : dependsOn) {
            if (!plugin.getManager(clazz).isLoaded()) {
                canLoad = false;
                break;
            }
        }
        if (!canLoad) {
            throw new ManagerLoadException(this, ManagerLoadException.Reason.DEPENDENCY);
        }
        try {
            load();
        } catch (final Exception e) {
            throw new ManagerLoadException(this, e);
        }
        EldenRhym.log(Level.INFO, getName() + " has been loaded successfully!");
        isLoaded = true;
    }

    public List<Class<? extends Module>> getDependsOn() {
        return Collections.unmodifiableList(dependsOn);
    }

    public String getName() {
        final String[] split = clazz.getName().split("\\.");
        return split[split.length - 1];
    }

    public abstract void shutdown();

    @Override
    public int compareTo(@NotNull final Module module) {
        final Class<? extends Module> clazz = module.getClazz();
        if (clazz.equals(this.getClazz())) {
            return 0;
        }
        if (dependsOn.contains(clazz)) {
            return -1;
        } else {
            return 1;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Module module = (Module) o;
        return clazz.equals(module.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz);
    }

    public boolean isLoaded() {
        return isLoaded;
    }
}