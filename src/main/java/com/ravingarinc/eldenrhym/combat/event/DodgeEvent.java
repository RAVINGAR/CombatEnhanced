package com.ravingarinc.eldenrhym.combat.event;

import com.ravingarinc.eldenrhym.api.AsyncHandler;
import com.ravingarinc.eldenrhym.api.AsynchronousException;
import com.ravingarinc.eldenrhym.api.Vector3;
import com.ravingarinc.eldenrhym.character.CharacterEntity;
import com.ravingarinc.eldenrhym.file.Settings;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a dodge event independent of a target entity
 */
public class DodgeEvent extends CombatEvent<CharacterEntity<?>, EntityDamageByEntityEvent> {

    protected final Settings settings;

    protected final long warmup;

    protected final AtomicBoolean dodging;

    protected final int particleCount;
    protected final BlockData defaultData;
    protected Vector3 initialVelocity;
    protected Vector3 location;

    public DodgeEvent(@NotNull final CharacterEntity<?> entity,
                      final Vector3 location,
                      final long start,
                      final Settings settings, final BlockData data) {
        super(entity, start, settings.dodgeWarmup + settings.dodgeDuration);
        this.initialVelocity = new Vector3();
        this.settings = settings;
        this.warmup = start + settings.dodgeWarmup;
        this.location = location;
        this.particleCount = settings.dodgeParticleCount;
        this.dodging = new AtomicBoolean(false);
        this.defaultData = data;
    }

    @Override
    @Async.Execute
    @Blocking
    protected void tick() throws AsynchronousException {
        final long current = System.currentTimeMillis();
        if (dodging.get()) {
            AsyncHandler.applySynchronously(entity, (entity) -> spawnParticle(entity.getEntity().getLocation()));
        } else if (current > warmup) {
            initialVelocity = entity.getVelocity();
            if (initialVelocity.getY() > 0) {
                interrupt();
                return;
            }
            //Throw entity
            final Vector3 velocity = getDodge();
            if (velocity == null) {
                interrupt();
                return;
            }
            dodging.getAndSet(true);
            AsyncHandler.applySynchronously(entity, (entity) -> {
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
        location.getWorld().spawnParticle(Particle.FALLING_DUST, location.add(0, 0.3, 0), particleCount, 0.15, 0.1, 0.15, 0.05, defaultData);
    }

    @Blocking
    @Async.Execute
    @Nullable
    public Vector3 getDodge() throws AsynchronousException {
        Vector3 direction = location.getDirection();
        final Vector3 postLoc = entity.getLocation();
        final double d0 = postLoc.getX() - location.getX();
        final double d1 = postLoc.getZ() - location.getZ();
        final float strength = settings.dodgeStrength;

        if (d0 == 0 && d1 == 0) {
            // case -> Player is not moving. Make them backpedal
            if (direction.length() < 1.0E-4D) {
                direction = new Vector3();
            }
            direction.scale(strength);
            return new Vector3(
                    initialVelocity.getX() / 2.0D - direction.getX(),
                    initialVelocity.getY(),
                    initialVelocity.getZ() / 2.0D - direction.getZ()
            );
        } else {
            // case -> Player is currently moving
            Vector3 movement = new Vector3(d0, 0.0, d1);
            final double length = movement.length();
            movement.normalize();
            final double e0 = movement.getX() - direction.getX();
            final double e1 = movement.getZ() - direction.getZ();
            if (e0 > -0.05 && e0 < 0.05 && e1 > -0.05 && e1 < 0.05) {
                // case -> Player is moving in the direction they are facing
                //         we do not want a dodge
                return null;
            }
            /*
            for (; ((d0 * d0) + (d1 * d1)) < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
                d0 = (Math.random() - Math.random()) * 0.01D;
            }
            */
            if (length < 1.0E-4D) {
                movement = new Vector3();
            }
            movement.scale(-strength);

            return new Vector3(
                    initialVelocity.getX() / 2.0D - movement.getX(),
                    initialVelocity.getY(),
                    initialVelocity.getZ() / 2.0D - movement.getZ()
            );
        }
    }

    public boolean isDodging() {
        return dodging.get();
    }
}
