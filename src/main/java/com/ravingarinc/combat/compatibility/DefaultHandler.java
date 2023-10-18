package com.ravingarinc.combat.compatibility;

import org.bukkit.entity.Player;

public class DefaultHandler implements RPGHandler {

    @Override
    public boolean tryRemoveStamina(final Player player, final int amount) {
        return true;
    }

    @Override
    public boolean tryRemoveMana(final Player player, final int amount) {
        return true;
    }

    @Override
    public int getDodgeCost(Player player) {
        return 0;
    }
}
