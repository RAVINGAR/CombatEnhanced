package com.ravingarinc.combat.compatibility;

import com.ravingarinc.combat.combat.CombatManager;
import com.ravingarinc.combat.combat.runner.BlockRunner;
import com.ravingarinc.combat.file.Properties;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public interface RPGWrapper {

    default void load() {}

    default void cancel() {}
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
        return getProperties().dodgeStrength;
    }

    default int getDodgeCost(final Player player) {
        return getProperties().dodgeStaminaCost;
    }

    default long getShieldCooldown(final Player player) {
        return getProperties().blockCooldown;
    }

    /**
     * Get the duration of the dodge in milliseconds.n
     */
    default long getDodgeDuration(final Player player) { return getProperties().dodgeDuration; }

    Properties getProperties();

    default BlockRunner getBlockRunner() {
        return new BlockRunner(this);
    }
    default void injectRunners(CombatManager manager) {

    }

    default void onDamageEvent(EntityDamageEvent event) {}
}
