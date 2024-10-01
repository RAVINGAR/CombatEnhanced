package com.ravingarinc.combat.compatibility;

import com.ravingarinc.api.module.RavinPlugin;
import com.ravingarinc.combat.file.Properties;
import org.bukkit.entity.Player;

public class DefaultWrapper implements RPGWrapper {
    private final Properties settings;

    public DefaultWrapper(RavinPlugin plugin) {
        settings = plugin.getModule(Properties.class);
    }


    @Override
    public boolean tryRemoveStamina(final Player player, final int amount) {
        return true;
    }

    @Override
    public boolean tryRemoveMana(final Player player, final int amount) {
        return true;
    }


    @Override
    public Properties getProperties() {
        return settings;
    }
}
