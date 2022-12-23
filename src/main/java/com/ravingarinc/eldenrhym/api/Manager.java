package com.ravingarinc.eldenrhym.api;

import com.ravingarinc.eldenrhym.EldenRhym;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

public abstract class Manager implements Comparable<Manager> {
    protected final EldenRhym plugin;
    protected final Class<? extends Manager> clazz;

    protected final List<Class<? extends Manager>> dependsOn;

    @SafeVarargs
    protected Manager(final Class<? extends Manager> identifier, final EldenRhym plugin, final Class<? extends Manager>... dependsOn) {
        this.plugin = plugin;
        this.clazz = identifier;
        this.dependsOn = new ArrayList<>();
        for (final Class<? extends Manager> manager : dependsOn) {
            this.dependsOn.add(manager);
        }
    }

    public static <T> Optional<T> initialise(final EldenRhym plugin, final Class<T> identifier) {
        try {
            final Constructor<T> constructor = identifier.getConstructor(EldenRhym.class);
            return Optional.of(constructor.newInstance(plugin));
        } catch (final NoSuchMethodException e) {
            EldenRhym.log(Level.SEVERE, "Could not find valid constructor for " + identifier.getName());
        } catch (final InvocationTargetException e) {
            EldenRhym.log(Level.SEVERE, "Failed to initialise manager '%s'", identifier.getName());
        } catch (InstantiationException | IllegalAccessException e) {
            EldenRhym.log(Level.SEVERE, "Something went severely wrong creating new instance of manager '%s'!", identifier.getName());
        }
        return Optional.empty();
    }

    public Class<? extends Manager> getClazz() {
        return clazz;
    }

    public abstract void reload();

    /**
     * Called after all other managers have loaded. If overridden, should be called last in the method
     */
    public void load() {
        final String[] split = clazz.getName().split("\\.");
        EldenRhym.log(Level.INFO, split[split.length - 1] + " has been loaded successfully!");
    }

    public abstract void shutdown();

    @Override
    public int compareTo(@NotNull final Manager manager) {
        final Class<? extends Manager> clazz = manager.getClazz();
        if (clazz.equals(this.getClazz())) {
            return 0;
        }
        if (dependsOn.contains(clazz)) {
            return 1;
        } else {
            return -1;
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
        final Manager manager = (Manager) o;
        return clazz.equals(manager.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz);
    }
}