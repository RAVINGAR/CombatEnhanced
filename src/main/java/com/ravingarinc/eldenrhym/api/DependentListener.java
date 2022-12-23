package com.ravingarinc.eldenrhym.api;

import com.ravingarinc.eldenrhym.EldenRhym;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class DependentListener extends Manager implements Listener {

    @SafeVarargs
    protected DependentListener(final Class<? extends Manager> identifier, final EldenRhym plugin, final Class<? extends Manager>... dependsOn) {
        super(identifier, plugin, dependsOn);
    }

    @Override
    protected void reload() {
        HandlerList.unregisterAll(this);
    }

    @Override
    protected void load() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void shutdown() {
        // there is nothing to be shut down...
    }
}
