package com.ravingarinc.combat.compatibility;

import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.combat.CombatManager;
import com.ravingarinc.combat.file.Settings;
import org.bukkit.entity.Player;

public class DefaultHandler implements RPGHandler {
    private final Settings settings;

    public DefaultHandler(CombatEnhanced plugin) {
        settings = plugin.getModule(CombatManager.class).getSettings();
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
    public Settings getSettings() {
        return settings;
    }
}
