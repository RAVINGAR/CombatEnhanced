package com.ravingarinc.combat.combat.runner;

import com.ravingarinc.combat.api.BukkitApi;
import com.ravingarinc.combat.combat.event.CombatEvent;
import com.ravingarinc.combat.file.Properties;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class IdentifierRunner<T extends CombatEvent<?>, E extends EntityDamageEvent> extends EventRunner<T, E> {
    protected final Properties properties;
    protected final ConcurrentHashMap<UUID, T> events;

    public IdentifierRunner(final Properties properties) {
        this(new ConcurrentHashMap<>(512), properties);
    }

    private IdentifierRunner(final ConcurrentHashMap<UUID, T> events, final Properties properties) {
        super(events.values());
        this.events = events;
        this.properties = properties;
    }

    /**
     * Handles an event, returns true if the event was handled and should not be handled by any other runners
     *
     * @param bukkitEvent The event
     * @return true if handled by this runner.
     */
    @BukkitApi
    public boolean handle(@NotNull final E bukkitEvent) {
        final T event = events.get(bukkitEvent.getEntity().getUniqueId());
        return event == null ? handleWithoutEvent(bukkitEvent) : handleWithEvent(event, bukkitEvent);
    }

    /**
     * Handles an event if it exists. It is expected that if this returns true, then no other runners should be called
     * to handle the bukkitEvent
     *
     * @param event       The event
     * @param bukkitEvent The bukkit event
     * @return false if bukkit event is not applicable
     */
    public abstract boolean handleWithEvent(T event, E bukkitEvent);

    /**
     * Handles an event if it does not exist. It is expected that if this returns true, then no other runners should be called
     * to handle the bukkitEvent. Keep in mind that the entity provided by the event may not be of the appropriate type
     *
     * @param bukkitEvent The bukkit event
     * @return false the bukkit event is not applicable
     */
    public abstract boolean handleWithoutEvent(E bukkitEvent);

    @Override
    public void add(@NotNull final T event) {
        if (!event.call()) {
            events.put(event.getCharacter().getEntity().getUniqueId(), event);
        }
    }


    public void remove(@NotNull final UUID uuid) {
        final T event = events.get(uuid);
        if (event != null) {
            event.interrupt();
        }
    }


    public Optional<T> get(final UUID uuid) {
        return Optional.ofNullable(events.get(uuid));
    }

    public Optional<T> getAndRemove(final UUID uuid) {
        return Optional.ofNullable(events.remove(uuid));
    }
}
