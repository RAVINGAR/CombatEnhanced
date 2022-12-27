package com.ravingarinc.eldenrhym.combat.event;

import com.ravingarinc.eldenrhym.api.AsynchronousException;
import com.ravingarinc.eldenrhym.api.Vector3;
import com.ravingarinc.eldenrhym.character.CharacterEntity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a dodge event independent of a target entity
 */
public class DodgeEvent extends CombatEvent<CharacterEntity<?>> {
    protected final long warmup;
    protected final float strength;

    protected final AtomicBoolean dodging;


    protected Vector3 initialVelocity = new Vector3();

    public DodgeEvent(@NotNull final CharacterEntity<?> entity,
                      final long start,
                      final long warmup,
                      final long duration,
                      final float strength) {
        super(entity, start, warmup + duration);
        this.warmup = warmup + start;
        this.strength = strength;
        this.dodging = new AtomicBoolean(false);
    }


    @Override
    @Async.Execute
    @Blocking
    protected void tick() throws AsynchronousException {
        final long current = System.currentTimeMillis();
        if (dodging.get()) {
            entity.applySynchronously((entity) -> spawnParticle(entity.getEntity().getLocation()));
        } else if (current > warmup) {
            initialVelocity = entity.getVelocity();
            if (initialVelocity.getY() > 0) {
                interrupt();
                return;
            }
            //Throw entity
            final Vector3 velocity = getDodge();
            dodging.getAndSet(true);
            entity.applySynchronously((entity) -> {
                final LivingEntity livingEntity = entity.getEntity();
                livingEntity.setVelocity(velocity.toBukkitVector());
                final Location location = livingEntity.getLocation();
                location.getWorld().playSound(location, Sound.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 0.7F, 1.1F);
                location.getWorld().playSound(location, Sound.BLOCK_BAMBOO_FALL, SoundCategory.PLAYERS, 0.3F, 0.9F);
                spawnParticle(location);
            });
        }
        if (current > getExpireTime()) {
            interrupt();
        }
    }

    private void spawnParticle(final Location location) {
        location.getWorld().spawnParticle(Particle.CLOUD, location.add(0, 0.3, 0), 5, 0.15, 0.1, 0.15, 0.01);
    }

    @Blocking
    @Async.Execute
    public Vector3 getDodge() throws AsynchronousException {
        Vector3 direction = entity.getDirection();
        direction.scale(-1);

        if (direction.length() < 1.0E-4D) {
            direction = new Vector3();
        }
        direction.scale(strength);
        return new Vector3(
                initialVelocity.getX() / 2.0D - direction.getX(),
                initialVelocity.getY(),
                initialVelocity.getZ() / 2.0D - direction.getZ()
        );
    }

    public boolean isDodging() {
        return dodging.get();
    }
}
