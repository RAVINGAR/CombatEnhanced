package com.ravingarinc.combat.combat.event;

import com.ravingarinc.combat.api.AsynchronousException;
import com.ravingarinc.combat.character.CharacterEntity;
import com.ravingarinc.combat.file.Settings;

public class DamageEvent extends CombatEvent<CharacterEntity<?>> {
    private final double damage;
    public DamageEvent(CharacterEntity<?> entity, double damage, long startTime, Settings settings) {
        super(entity, startTime, settings.poiseWindow);
        this.damage = damage;
    }

    public double getDamage() {
        return damage;
    }

    @Override
    protected void tick() throws AsynchronousException {
        // Todo implement, basically if the character has an immunity from poise active, then prevent them from gaining
        // any more poise
    }
}
