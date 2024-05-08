package com.ravingarinc.combat.combat.runner;

import com.ravingarinc.combat.combat.event.CombatEvent;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Run asynchronously
 */
public abstract class EventRunner<T extends CombatEvent<?>, E extends Event> extends BukkitRunnable {
    protected final Collection<T> events;
    protected final List<T> toRemove;

    public EventRunner(final Collection<T> events) {
        this.events = events;
        this.toRemove = new LinkedList<>();
    }

    public void add(@NotNull final T event) {
        if (!event.call()) {
            events.add(event);
        }
    }


    public void remove(@NotNull final T event) {
        events.remove(event);
    }

    @Override
    @Blocking
    @Async.Execute
    public void run() {
        events.parallelStream().forEach(event -> {
            if (event.call()) {
                toRemove.add(event);
            }
        });
        if (!toRemove.isEmpty()) {
            toRemove.forEach(this::remove);
            toRemove.clear();
        }
    }
}