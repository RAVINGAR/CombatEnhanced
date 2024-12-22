package com.ravingarinc.combat.compatibility.kalentire;

import com.herocraftonline.heroes.characters.effects.Effect;
import com.ravingarinc.kalentirerpg.damage.type.Damage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public record ShootEvent(Location origin, Damage damageType, double damage, double impact, double penetration,
                         double knockback, double fallOffRange, double fallOffReduction, double force,
                         Collection<Effect> effects) {
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
