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
public abstract class EventRunner<T extends CombatEvent<?, E>, E extends Event> extends BukkitRunnable {
    private final Collection<T> events;
    private final List<T> toRemove;

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

    /*
    TODO
      If this doesnt work I have a solution,
      basically a Synchronous Runnable on a timer can receive intended requests. These requests are executed async
      These requests could be add/remove and the run(). When performing get(), must make sure that add()
      and remove() aren't being executed. The solution is almost to create a last map sort of idea
      (is this not what concurrent hashmap is?). So if add() and remove() are being using, then get() from thr last copy.

     */
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