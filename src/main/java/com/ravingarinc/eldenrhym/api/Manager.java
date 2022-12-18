package com.ravingarinc.eldenrhym.api;

import com.ravingarinc.eldenrhym.EldenRhym;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.logging.Level;

public abstract class Manager {
    protected final EldenRhym plugin;
    protected final Class<? extends Manager> clazz;

    protected Manager(final EldenRhym plugin, final Class<? extends Manager> identifier) {
        this.plugin = plugin;
        this.clazz = identifier;
    }

    public static <T> Optional<T> initialise(final EldenRhym plugin, final Class<T> identifier) {
        try {
            final Constructor<T> constructor = identifier.getConstructor(EldenRhym.class);
            return Optional.of(constructor.newInstance(plugin));
        } catch (final NoSuchMethodException e) {
            EldenRhym.log(Level.SEVERE, "Could not find valid constructor for " + identifier.getName());
        } catch (final InvocationTargetException e) {
            EldenRhym.log(Level.SEVERE, String.format("Failed to initialise manager '%s'", identifier.getName()), e.getTargetException());
        } catch (InstantiationException | IllegalAccessException e) {
            EldenRhym.log(Level.SEVERE, String.format("Something went severely wrong creating new instance of manager '%s'!", identifier.getName()), e);
        }
        return Optional.empty();
    }

    public Class<? extends Manager> getClazz() {
        return clazz;
    }

    /**
     * Called after all other managers have loaded. If overridden, should be called last in the method
     */
    public void postLoad() {
        EldenRhym.log(Level.INFO, clazz.getName() + " has been loaded successfully!");
    }

    public abstract void shutdown();
}