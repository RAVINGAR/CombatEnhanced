package com.ravingarinc.eldenrhym.compatibility;

import com.ravingarinc.eldenrhym.EldenRhym;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface RPGHandler {

    @NotNull
    static RPGHandler getHandler(final EldenRhym plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Heroes") != null) {
            return new KalentireHandler();
        } else if (plugin.getServer().getPluginManager().getPlugin("MMOItems") != null) {
            return new MMOHandler();
        }
        return new DefaultHandler();
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
}
