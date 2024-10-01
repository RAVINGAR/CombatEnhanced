package com.ravingarinc.combat.compatibility;

import com.ravingarinc.api.module.RavinPlugin;
import com.ravingarinc.combat.file.Properties;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.player.RPGPlayer;
import org.bukkit.entity.Player;

public class MMOWrapper implements RPGWrapper {
    private final Properties settings;

    public MMOWrapper(final RavinPlugin plugin) {
        this.settings = plugin.getModule(Properties.class);
    }

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

    @Override
    public Properties getProperties() {
        return settings;
    }
}
