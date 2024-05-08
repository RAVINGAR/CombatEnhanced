package com.ravingarinc.combat.compatibility.kalentire.effect;

import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;

public class PoiseImmunityEffect extends ExpirableEffect {
    public static final String EFFECT_NAME = "PoiseImmunityEffect";
    public PoiseImmunityEffect(long duration) {
        super(null, EFFECT_NAME, null, duration, null, null);
        this.types.add(EffectType.BENEFICIAL);
    }
}
