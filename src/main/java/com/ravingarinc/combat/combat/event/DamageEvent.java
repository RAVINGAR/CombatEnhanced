package com.ravingarinc.combat.combat.event;

import com.ravingarinc.combat.api.AsynchronousException;
import com.ravingarinc.combat.character.CharacterEntity;

import java.util.Objects;

public class DamageEvent extends CombatEvent<CharacterEntity<?>> {
    private final double damage;
    public DamageEvent(CharacterEntity<?> entity, double damage, long startTime, long poiseWindow) {
        super(entity, startTime, poiseWindow);
        this.damage = damage;
    }

    public double getDamage() {
        return damage;
    }

    @Override
    protected void tick() throws AsynchronousException {
        if(System.currentTimeMillis() > getExpireTime()) {
            interrupt();
        }
    }

    public float getTimeFactor() {
        final var difference = expireTime - System.currentTimeMillis();
        if(difference <= 0) {
            return 0F;
        }
        return 1.0F - ((float) difference / duration);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DamageEvent that = (DamageEvent) o;
        return Double.compare(damage, that.damage) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), damage);
    }
}
