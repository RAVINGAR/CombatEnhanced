package com.ravingarinc.combat.compatibility.kalentire;

import com.herocraftonline.heroes.characters.effects.Effect;
import com.ravingarinc.combat.api.AsynchronousException;
import com.ravingarinc.combat.character.CharacterEntity;
import com.ravingarinc.combat.character.CharacterPlayer;
import com.ravingarinc.combat.combat.event.CombatEvent;
import com.ravingarinc.kalentirerpg.damage.type.Damage;
import com.ravingarinc.kalentirerpg.item.stats.Stat;
import com.ravingarinc.kalentirerpg.item.stats.type.Arrow;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.stat.StatMap;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;

public class ShootEvent extends CombatEvent<CharacterEntity<?>> {
    public static final long TIMEOUT = 60000L;

    private Stats stats = null;

    private final StatMap statMap;
    private final Category category;
    private final NBTItem consumable;
    private final float force;

    public ShootEvent(Category category, CharacterPlayer entity, StatMap statMap, NBTItem consumable,
                      float force) {
        super(entity, System.currentTimeMillis(), TIMEOUT);
        this.category = category;
        this.statMap = statMap;
        this.force = force;
        this.consumable = consumable;
    }

    @Override
    protected synchronized void tick() throws AsynchronousException {
        if(stats == null) {
            stats = computeStats();
        }
    }

    private Stats computeStats() {
        final var damage = statMap.getStat(KalentireWrapper.RANGED_DAMAGE);

        final var type = Arrow.Type.valueOf(consumable.getType());
        final var damageType = type.get(Stat.DAMAGE_TYPE);
        final var arrowDamage = consumable.getDouble(damageType.damage().mmoKey());


        // use type.getEffect() to add effects and such
        final var multiplier = force == 1.0F ? 1.0F + statMap.getStat(KalentireWrapper.FULL_DRAW_BONUS) : force;
        // TODO Consider imbuements effects
        final var impact = statMap.getStat(KalentireWrapper.IMPACT) + consumable.getDouble(KalentireWrapper.IMPACT);
        final var penetration = statMap.getStat("KALENTIRE_PENETRATION") + consumable.getDouble(
                "KALENTIRE_PENETRATION");
        final var knockback = statMap.getStat(KalentireWrapper.KNOCKBACK) + consumable.getDouble(KalentireWrapper.KNOCKBACK);
        final var fallOffRange = statMap.getStat(KalentireWrapper.FALL_OFF_RANGE) + consumable.getDouble(KalentireWrapper.FALL_OFF_RANGE);
        final var fallOffReduction = statMap.getStat(KalentireWrapper.FALL_OFF_REDUCTION);

        return new Stats(damageType, (damage + arrowDamage) * multiplier, impact * multiplier,
                penetration * multiplier,
                knockback,
                fallOffRange, fallOffReduction, multiplier, new HashSet<>());
    }

    public synchronized Stats getStats() {
        if(stats == null) {
            stats = computeStats();
        }
        return stats;
    }

    public record Stats(Damage damageType, double damage, double impact, double penetration,
                        double knockback, double fallOffRange, double fallOffReduction, double force,
                        Collection<Effect> effects) {}

    public enum Category {
        BOW(Material.BOW), CROSSBOW(Material.CROSSBOW);

        private final Material material;
        Category(Material match) {
            this.material = match;
        }

        @Nullable
        public static Category matchCategory(ItemStack item) {
            if(item == null) return null;
            for(Category category : Category.values()) {
                if(category.material.equals(item.getType())) {
                    return category;
                }
            }
            return null;
        }
    }
}
