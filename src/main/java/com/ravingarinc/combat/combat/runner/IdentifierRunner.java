package com.ravingarinc.combat.combat.runner;

import com.ravingarinc.combat.api.BukkitApi;
import com.ravingarinc.combat.combat.event.CombatEvent;
import com.ravingarinc.combat.file.Settings;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class IdentifierRunner<T extends CombatEvent<?, E>, E extends Event> extends EventRunner<T, E> {
    protected final Settings settings;
    private final ConcurrentHashMap<UUID, T> events;

    public IdentifierRunner(final Settings settings) {
        this(new ConcurrentHashMap<>(512), settings);
    }

    private IdentifierRunner(final ConcurrentHashMap<UUID, T> events, final Settings settings) {
        super(events.values());
        this.events = events;
        this.settings = settings;
    }

    /**
     * Handles an event, returns true if the event was handled and should not be handled by any other runners
     *
     * @param uuid        The uuid of the entity
     * @param bukkitEvent The event
     * @return true if handled by this runner.
     */
    @BukkitApi
    public boolean handle(@NotNull final UUID uuid, @NotNull final E bukkitEvent) {
        final T event = events.get(uuid);
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
}
