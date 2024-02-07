package com.ravingarinc.combat.api;

import com.ravingarinc.combat.CombatEnhanced;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class ModuleListener extends Module implements Listener {

    @SafeVarargs
    protected ModuleListener(final Class<? extends Module> identifier, final CombatEnhanced plugin, final Class<? extends Module>... dependsOn) {
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
    protected void shutdown() {
        // there is nothing to be shut down...
    }
}
