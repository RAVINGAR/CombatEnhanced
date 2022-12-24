package com.ravingarinc.eldenrhym.character;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.AsynchronousException;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.function.Predicate;

@ThreadSafe
public class CharacterPlayer extends CharacterEntity<Player> {
    protected CharacterPlayer(final EldenRhym plugin, final Player entity) {
        super(plugin, entity);
    }

    @Override
    @Async.Execute
    @Blocking
    public Optional<CharacterEntity<?>> getTarget(final double maxDistance) throws AsynchronousException {
        final Entity target = executeBlockingSyncComputation(() -> {
            final Location location = entity.getEyeLocation();
            final Predicate<Entity> predicate = entity -> {
                if (entity instanceof LivingEntity le) {
                    return !le.isDead() && le.getHealth() != 0;
                }
                return false;
            };
            final RayTraceResult result = location.getWorld().rayTrace(location, location.getDirection(), maxDistance, FluidCollisionMode.ALWAYS, true, 2, predicate);
            return result.getHitEntity();
        });
        if (target instanceof LivingEntity entity) {
            return characterManager.getCharacter(entity);
        }
        return Optional.empty();
    }

    @Blocking
    @Async.Execute
    public boolean isBlocking() throws AsynchronousException {
        return executeBlockingSyncComputation(entity::isBlocking);
    }
}
