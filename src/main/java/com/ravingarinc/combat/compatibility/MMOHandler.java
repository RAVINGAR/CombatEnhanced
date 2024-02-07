package com.ravingarinc.combat.compatibility;

import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.combat.CombatManager;
import com.ravingarinc.combat.file.Settings;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.player.RPGPlayer;
import org.bukkit.entity.Player;

public class MMOHandler implements RPGHandler {
    private final CombatEnhanced plugin;
    private final Settings settings;

    public MMOHandler(final CombatEnhanced plugin) {
        this.plugin = plugin;
        this.settings = plugin.getModule(CombatManager.class).getSettings();
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
    public int getDodgeCost(Player player) {
        return settings.dodgeStaminaCost;
    }

    @Override
    public long getShieldCooldown(final Player player) {
        return settings.blockCooldown * 50;
    }
}
