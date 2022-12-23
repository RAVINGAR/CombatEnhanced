package com.ravingarinc.eldenrhym.character;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.AsynchronousException;
import com.ravingarinc.eldenrhym.api.Pair;
import com.ravingarinc.eldenrhym.api.Vector3;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Represents a thread safe entity that wraps bukkit api such that it can be called in async computations.
 * Most methods are intended to be called from asynchronous threads.
 */
@ThreadSafe
public abstract class CharacterEntity<T extends LivingEntity> {
    protected final T entity;
    private final EldenRhym plugin;
    private final BukkitScheduler scheduler;

    protected CharacterEntity(final EldenRhym plugin, final T entity) {
        this.entity = entity;
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
    }

    public T getEntity() {
        return entity;
    }

    //TODO Rewrite this maybe to use non bukkit api and run async
    public abstract Optional<LivingEntity> getTarget(double maxDistance);

    @Blocking
    @Async.Execute
    public Vector3 getLocation() throws AsynchronousException {
        return executeSyncComputation(() -> {
            final Location loc = entity.getLocation();
            return new Vector3(loc.getX(), loc.getY(), loc.getZ());
        });
    }

    @Blocking
    @Async.Execute
    public Vector3 getVelocity() throws AsynchronousException {
        return executeSyncComputation(() -> {
            final Vector velocity = entity.getVelocity();
            return new Vector3(velocity.getX(), velocity.getY(), velocity.getZ());
        });
    }

    @Blocking
    @Async.Execute
    public Vector3 getDirection() throws AsynchronousException {
        final Pair<Float, Float> pair = executeSyncComputation(() -> {
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

    /**
     * Executes a computation on the main thread, then waits for the result to be returned. Can be used in an
     * asynchronous environment.
     */
    @Blocking
    @Async.Execute
    protected <V> V executeSyncComputation(final Callable<V> callable) throws AsynchronousException {
        final Future<V> future = scheduler.callSyncMethod(plugin, callable);
        try {
            return future.get(1000, TimeUnit.MILLISECONDS);
        } catch (final ExecutionException e) {
            throw new AsynchronousException("Encountered issue waiting for computation for CharacterEntity.", e);
        } catch (final InterruptedException e) {
            throw new AsynchronousException("Thread was interrupted while waiting for computation for CharacterEntity.", e);
        } catch (final TimeoutException e) {
            throw new AsynchronousException("Computation for CharacterEntity timed out! (Took longer than 1 second!)", e);
        }
    }

    /**
     * Apply the given consumer to this CharacterEntity object synchronously.
     *
     * @param consumer The consumer
     */
    public void applySynchronously(final Consumer<CharacterEntity<T>> consumer) {
        scheduler.runTask(plugin, () -> consumer.accept(this));
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
