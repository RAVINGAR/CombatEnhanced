package com.ravingarinc.combat.compatibility.kalentire.bows;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.ravingarinc.combat.compatibility.kalentire.KalentireWrapper;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Assumed a player is aiming with their main hand item.
 */
public class AimingEffect extends PeriodicExpirableEffect {

    public static final String NAME = "KalentireAimingEffect";
    private final int drawStamina;
    private final int aimStamina;

    public AimingEffect(Player player, NBTItem mainHand) {
        super(null, "KalentireAimingEffect", player, 200,
                1000L + (long) mainHand.getStat(KalentireWrapper.AIM_TIME) * 50L);
        drawStamina = (int) mainHand.getStat(KalentireWrapper.DRAW_STAMINA) / 5;
        aimStamina = (int) mainHand.getStat(KalentireWrapper.AIM_STAMINA) / 5;

        addEffectTypes(EffectType.BENEFICIAL);
        addEffectTypes(EffectType.INTERNAL);

        // TODO if this effect is interrupted it should change the apply time which essentially resets the focus.
    }

    @Override
    public void tickMonster(Monster monster) {

    }

    @Override
    public void tickHero(Hero hero) {
        final int stamina = hero.getStamina();
        if (stamina >= drawStamina) {
            hero.setStamina(stamina - drawStamina);
            return;
        }
        hero.setStamina(0);
        hero.getPlayer().swingMainHand();
        hero.getPlayer().playSound(hero.getPlayer(), Sound.ENTITY_PLAYER_BREATH, 0.5F, 1.0F);
        hero.removeEffect(this);
    }

    @Override
    public void removeFromHero(Hero hero) {
        super.removeFromHero(hero);
        if(isExpired()) {
            hero.addEffect(new PerfectAimEffect(hero.getPlayer(), aimStamina));
        }
    }
}
