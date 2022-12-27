package com.ravingarinc.eldenrhym.character;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.AsynchronousException;
import com.ravingarinc.eldenrhym.api.BukkitApi;
import com.ravingarinc.eldenrhym.api.Pair;
import com.ravingarinc.eldenrhym.api.TaskCallback;
import com.ravingarinc.eldenrhym.api.Vector3;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Represents a thread safe entity that wraps bukkit api such that it can be called in async computations.
 * Most methods are intended to be called from asynchronous threads.
 */
@ThreadSafe
public abstract class CharacterEntity<T extends LivingEntity> {
    protected final T entity;
    protected final EldenRhym plugin;
    protected final CharacterManager characterManager;
    protected final BukkitScheduler scheduler;

    protected CharacterEntity(final EldenRhym plugin, final T entity) {
        this.entity = entity;
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
        this.characterManager = plugin.getModule(CharacterManager.class);

    }

    public T getEntity() {
        return entity;
    }

    /**
     * Gets the target from a raycast based on the direction of the entity.
     *
     * @param maxDistance the max distance to look
     * @return A possible target
     */
    @Async.Execute
    @Blocking
    public abstract Optional<CharacterEntity<?>> getTarget(double maxDistance) throws AsynchronousException;

    @Blocking
    @Async.Execute
    public Vector3 getLocation() throws AsynchronousException {
        return executeBlockingSyncComputation(() -> {
            final Location loc = entity.getLocation();
            return new Vector3(loc.getX(), loc.getY(), loc.getZ());
        });
    }

    @Blocking
    @Async.Execute
    public Vector3 getVelocity() throws AsynchronousException {
        return executeBlockingSyncComputation(this::syncGetVelocity);
    }

    @BukkitApi
    public Vector3 syncGetVelocity() {
        final Vector velocity = entity.getVelocity();
        return new Vector3(velocity.getX(), velocity.getY(), velocity.getZ());
    }

    @Blocking
    @Async.Execute
    public Vector3 getDirection() throws AsynchronousException {
        final Pair<Float, Float> pair = executeBlockingSyncComputation(() -> {
            final Location location = entity.getLocation();
            return new Pair<>(location.getYaw(), location.getPitch());
        });

        final Vector3 vector = new Vector3();

        final double rotX = pair.getLeft();
        final double rotY = pair.getRight();

        vector.setY(-Math.sin(Math.toRadians(rotY)));

        final double xz = Math.cos(Math.toRadians(rotY));

        vector.setX(-xz * Math.sin(Math.toRadians(rotX)));
        vector.setZ(xz * Math.cos(Math.toRadians(rotX)));

        return vector;
    }

    @Async.Execute
    @Blocking
    public boolean isValid() throws AsynchronousException {
        return executeBlockingSyncComputation(entity::isValid);
    }

    /**
     * Executes a computation on the main thread, then waits for the result to be returned. Can be used in an
     * asynchronous environment.
     */
    @Async.Execute
    @Blocking
    public final <V> V executeBlockingSyncComputation(final Callable<V> callable) throws AsynchronousException {
        final TaskCallback<V> callback = new TaskCallback<>(callable);
        scheduler.scheduleSyncDelayedTask(plugin, callback);
        return callback.get();
    }


    /**
     * Executes a runnable as on a new thread
     *
     * @param runnable The runnable
     */
    @Async.Execute
    @NonBlocking
    public final void executeAsyncComputation(final Runnable runnable) {
        scheduler.runTaskAsynchronously(plugin, runnable);
    }

    /**
     * Apply the given consumer to this CharacterEntity object synchronously.
     *
     * @param consumer The consumer
     */
    public final void applySynchronously(final Consumer<CharacterEntity<T>> consumer) {
        scheduler.scheduleSyncDelayedTask(plugin, () -> consumer.accept(this));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CharacterEntity<?> that = (CharacterEntity<?>) o;
        return entity.getUniqueId().equals(that.entity.getUniqueId());
    }

    @Override
    public int hashCode() {
        return entity.getUniqueId().hashCode();
    }
}
