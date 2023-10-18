package com.ravingarinc.combat.compatibility;

import com.ravingarinc.combat.CombatEnhanced;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface RPGHandler {

    @NotNull
    static RPGHandler getHandler(final CombatEnhanced plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("KalentireRPG") != null) {
            return new KalentireHandler(plugin);
        } else if (plugin.getServer().getPluginManager().getPlugin("MMOItems") != null) {
            return new MMOHandler(plugin);
        }
        return new DefaultHandler();
    }

    /**
     * Reload this handler
     */
    default void reload() {
    }

    /**
     * Attempts to remove stamina, only if the given player has enough.
     *
     * @param player The player
     * @param amount The amount
     * @return true if stamina was taken, or false if not
     */
    boolean tryRemoveStamina(Player player, int amount);

    /**
     * Attempts to remove mana, only if the given player has enough.
     *
     * @param player The player
     * @param amount The amount
     * @return true if mana was taken, or false if not
     */
    boolean tryRemoveMana(Player player, int amount);

    default float getDodgeStrength(final Player player) {
        return 1.0F;
    }

    int getDodgeCost(final Player player);
    default long getShieldCooldown(final Player player) {
        return 60L;
    }
}
