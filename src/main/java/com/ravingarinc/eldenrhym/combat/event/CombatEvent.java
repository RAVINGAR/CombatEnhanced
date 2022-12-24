package com.ravingarinc.eldenrhym.combat.event;

import com.ravingarinc.eldenrhym.api.AsynchronousException;
import com.ravingarinc.eldenrhym.character.CharacterEntity;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * An event relating to a given entity and results in a Future task. The result of the Future
 * if true means that this event is interrupted and should be removed from the runner.
 */
public abstract class CombatEvent<T extends CharacterEntity<?>> implements Callable<Boolean> {
    protected final T entity;
    private final long expireTime;
    private final Object lock = new Object();
    private boolean interrupted;

    /**
     * @param duration Duration in milliseconds
     */
    public CombatEvent(final T entity, final long startTime, final long duration) {
        this.entity = entity;
        this.expireTime = startTime + duration;
        this.interrupted = false;
    }

    /**
     * Interrupts this event
     */
    @Blocking
    public void interrupt() {
        synchronized (lock) {
            this.interrupted = true;
        }
    }

    /**
     * Gets whether this event is interrupted or not
     *
     * @return true if interrupted
     */
    @Blocking
    public boolean isInterrupted() {
        synchronized (lock) {
            return interrupted;
        }
    }

    /**
     * Tick this combat event and run executions.
     *
     * @throws AsynchronousException if an internal exception is thrown
     */
    @Async.Execute
    @Blocking
    protected abstract void tick() throws AsynchronousException;

    @Override
    @Async.Execute
    @Blocking
    public Boolean call() {
        try {
            tick();
        } catch (final AsynchronousException exception) {
            exception.report();
            interrupt();
        }
        return isInterrupted();
    }

    public T getCharacter() {
        return entity;
    }

    public long getExpireTime() {
        return expireTime;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CombatEvent<?> that = (CombatEvent<?>) o;
        return entity.equals(that.entity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity);
    }
}
