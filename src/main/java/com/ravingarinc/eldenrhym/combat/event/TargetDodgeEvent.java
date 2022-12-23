package com.ravingarinc.eldenrhym.combat.event;

import com.ravingarinc.eldenrhym.api.AsynchronousException;
import com.ravingarinc.eldenrhym.api.Vector3;
import com.ravingarinc.eldenrhym.character.CharacterEntity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

/**
 * Dodge event where entity dodges away from a target
 */
public class TargetDodgeEvent extends DodgeEvent {
    protected final CharacterEntity<? extends LivingEntity> target;

    public TargetDodgeEvent(@NotNull final CharacterEntity<? extends LivingEntity> entity,
                            @NotNull final CharacterEntity<? extends LivingEntity> target,
                            final long warmup,
                            final long duration,
                            final float strength) {
        super(entity, warmup, duration, strength);
        this.target = target;
    }

    @Override
    @Async.Execute
    @Blocking
    public Vector3 getDodge() throws AsynchronousException {
        final Vector3 entityPos = entity.getLocation();
        final Vector3 targetPos = target.getLocation();
        double d0 = targetPos.getX() - entityPos.getX();
        double d1;

        for (d1 = targetPos.getZ()
                - entityPos.getZ(); ((d0 * d0) + (d1 * d1)) < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
            d0 = (Math.random() - Math.random()) * 0.01D;
        }

        Vector3 postVec = new Vector3(d0, 0.0D, d1);
        if (postVec.length() < 1.0E-4D) {
            postVec = new Vector3();
        } else {
            postVec.normalize();
        }
        postVec.scale(-strength);

        return new Vector3(
                initialVelocity.getX() / 2.0D - postVec.getX(),
                initialVelocity.getY(),
                initialVelocity.getZ() / 2.0D - postVec.getZ()
        );
    }
}
