package com.ravingarinc.combat.compatibility.kalentire.bows;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.nms.api.FriendlyPotionType;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Assumed a player is aiming with their main hand item.
 */
public class PerfectAimEffect extends PeriodicEffect {

    public static final String NAME = "KalentirePerfectAimEffect";

    private final int aimStamina;

    public PerfectAimEffect(Player player, int aimStamina) {
        super(null, NAME, player, 200);
        this.aimStamina = aimStamina;

        addEffectTypes(EffectType.BENEFICIAL);
        addPotionType(FriendlyPotionType.SLOWNESS, 3);
        // TODO if this effect is interrupted it should change the apply time which essentially resets the focus.
    }

    @Override
    public void applyToHero(Hero hero) {
        hero.getPlayer().playSound(hero.getPlayer(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5F, 0.1F);
    }

    @Override
    public void tickHero(Hero hero) {
        final int stamina = hero.getStamina();
        if (stamina >= aimStamina) {
            hero.setStamina(stamina - aimStamina);
            return;
        }
        hero.setStamina(0);
        hero.getPlayer().swingMainHand();
        hero.getPlayer().playSound(hero.getPlayer(), Sound.ENTITY_PLAYER_BREATH, 0.5F, 1.0F);
        hero.removeEffect(this);
    }
}
