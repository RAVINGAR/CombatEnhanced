package com.ravingarinc.eldenrhym.character;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.AsyncHandler;
import com.ravingarinc.eldenrhym.api.AsynchronousException;
import com.ravingarinc.eldenrhym.compatibility.RPGHandler;
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
    private final RPGHandler handler;

    protected CharacterPlayer(final EldenRhym plugin, final Player entity) {
        super(plugin, entity);
        this.handler = plugin.getRPGHandler();
    }

    @Override
    @Async.Execute
    @Blocking
    public Optional<CharacterEntity<?>> getTarget(final double maxDistance) throws AsynchronousException {
        final Optional<Entity> optTarget = AsyncHandler.executeBlockingSyncComputation(() -> {
            final Location location = entity.getEyeLocation();
            final Predicate<Entity> predicate = entity -> {
                if (entity instanceof LivingEntity le) {
                    if (le.equals(this.entity)) {
                        return false;
                    }
                    return !le.isDead() && le.getHealth() > 0;
                }
                return false;
            };
            final RayTraceResult result = location.getWorld().rayTrace(location, location.getDirection(), maxDistance, FluidCollisionMode.ALWAYS, true, 2, predicate);
            return result == null ? Optional.empty() : Optional.ofNullable(result.getHitEntity());
        });
        if (optTarget.isPresent() && optTarget.get() instanceof LivingEntity entity) {
            return characterManager.getCharacter(entity);
        }
        return Optional.empty();
    }

    @Blocking
    @Async.Execute
    public boolean isBlocking() throws AsynchronousException {
        return AsyncHandler.executeBlockingSyncComputation(entity::isBlocking);
    }

    public boolean tryRemoveStamina(final int amount) {
        return handler.tryRemoveStamina(entity, amount);
    }

    public boolean tryRemoveMana(final int amount) {
        return handler.tryRemoveMana(entity, amount);
    }
}
