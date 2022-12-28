package com.ravingarinc.eldenrhym.character;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.AsyncHandler;
import com.ravingarinc.eldenrhym.api.AsynchronousException;
import com.ravingarinc.eldenrhym.api.Vector3;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;

/**
 * Represents a thread safe entity that wraps bukkit api such that it can be called in async computations.
 * Most methods are intended to be called from asynchronous threads.
 */
@ThreadSafe
public abstract class CharacterEntity<T extends LivingEntity> {
    protected final T entity;
    protected final CharacterManager characterManager;

    protected CharacterEntity(final EldenRhym plugin, final T entity) {
        this.entity = entity;
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
        return AsyncHandler.executeBlockingSyncComputation(() -> new Vector3(entity.getLocation()));
    }

    @Blocking
    @Async.Execute
    public Vector3 getVelocity() throws AsynchronousException {
        return AsyncHandler.executeBlockingSyncComputation(() -> new Vector3(entity.getVelocity()));
    }

    @Blocking
    @Async.Execute
    public Vector3 getEyeLocation() throws AsynchronousException {
        return AsyncHandler.executeBlockingSyncComputation(() -> new Vector3(entity.getEyeLocation()));
    }

    @Async.Execute
    @Blocking
    public boolean isValid() throws AsynchronousException {
        return AsyncHandler.executeBlockingSyncComputation(entity::isValid);
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
