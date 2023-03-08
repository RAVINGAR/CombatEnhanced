package com.ravingarinc.eldenrhym.compatibility;

import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.player.RPGPlayer;
import org.bukkit.entity.Player;

public class MMOHandler implements RPGHandler {

    @Override
    public boolean tryRemoveStamina(final Player player, final int amount) {
        final RPGPlayer rpg = PlayerData.get(player.getUniqueId()).getRPG();
        final double stamina = rpg.getStamina();
        if (stamina >= amount) {
            rpg.setStamina(stamina - amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean tryRemoveMana(final Player player, final int amount) {
        final RPGPlayer rpg = PlayerData.get(player.getUniqueId()).getRPG();
        final double mana = rpg.getMana();
        if (mana >= amount) {
            rpg.setMana(mana - amount);
            return true;
        }
        return false;
    }
}
