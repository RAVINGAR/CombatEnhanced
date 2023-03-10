package com.ravingarinc.combat.compatibility;

import com.google.common.util.concurrent.AtomicDouble;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.equipment.EquipmentChangedEvent;
import com.herocraftonline.heroes.characters.equipment.EquipmentType;
import com.ravingarinc.combat.character.CharacterPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class KalentireHandler implements RPGHandler {
    private final Heroes heroes;
    private final CharacterManager manager;

    private final Map<CharacterPlayer, EquipmentStats> stats;

    public KalentireHandler() {
        stats = new HashMap<>();
        heroes = Heroes.getInstance();
        manager = heroes.getCharacterManager();
    }

    @Override
    public void reload() {
        stats.clear();
    }

    @Override
    public boolean tryRemoveStamina(final Player player, final int amount) {
        final Hero hero = manager.getHero(player);
        final int stamina = hero.getStamina();
        if (stamina >= amount) {
            hero.setStamina(stamina - amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean tryRemoveMana(final Player player, final int amount) {
        final Hero hero = manager.getHero(player);
        final int mana = hero.getMana();
        if (mana >= amount) {
            hero.setMana(mana - amount);
            return true;
        }
        return false;
    }

    @Override
    public float getDodgeStrength(final Player player) {
        // todo
        return 1.0F;
    }

    private static class EquipmentStats {
        private final Map<EquipmentType, AtomicDouble> dodgeFactors;

        public EquipmentStats(final Player player) {
            dodgeFactors = new HashMap<>();
            dodgeFactors.put(EquipmentType.HELMET, new AtomicDouble(1.0F));
            dodgeFactors.put(EquipmentType.CHESTPLATE, new AtomicDouble(1.0F));
            dodgeFactors.put(EquipmentType.LEGGINGS, new AtomicDouble(1.0F));
            dodgeFactors.put(EquipmentType.BOOTS, new AtomicDouble(1.0F));
        }

        public void setDodgeFactor(final EquipmentType type, final double factor) {
            dodgeFactors.get(type).set(factor);
        }

        public double getDodge() {
            double dodge = 0.0;
            for (final AtomicDouble factor : dodgeFactors.values()) {
                dodge += factor.get();
            }
            dodge /= dodgeFactors.size();
            return dodge;
        }
    }

    private class KalentireListener implements Listener {
        private final Map<EquipmentType, Consumer<EquipmentChangedEvent>> handlers;

        public KalentireListener() {
            handlers = new HashMap<>();

            final Consumer<EquipmentChangedEvent> armourConsumer = (event) -> {

            };

            handlers.put(EquipmentType.HELMET, armourConsumer);
            handlers.put(EquipmentType.CHESTPLATE, armourConsumer);
            handlers.put(EquipmentType.LEGGINGS, armourConsumer);
            handlers.put(EquipmentType.BOOTS, armourConsumer);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEquipmentChange(final EquipmentChangedEvent event) {
            Optional.ofNullable(handlers.get(event.getType())).ifPresent(handler -> handler.accept(event));
        }


    }

}
