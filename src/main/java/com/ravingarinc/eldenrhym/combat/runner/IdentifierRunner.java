package com.ravingarinc.eldenrhym.combat.runner;

import com.ravingarinc.eldenrhym.combat.event.CombatEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@NotThreadSafe
public class IdentifierRunner<T extends CombatEvent<?>> extends EventRunner<T> {
    private final ConcurrentHashMap<UUID, T> events;

    public IdentifierRunner() {
        this(new ConcurrentHashMap<>(512));
    }

    private IdentifierRunner(final ConcurrentHashMap<UUID, T> events) {
        super(events.values());
        this.events = events;
    }

    @Override
    public void add(@NotNull final T event) {
        if (!event.call()) {
            events.put(event.getCharacter().getEntity().getUniqueId(), event);
        }
    }

    public void remove(@NotNull final UUID uuid) {
        events.get(uuid).interrupt();
    }


    public Optional<T> get(final UUID uuid) {
        return Optional.ofNullable(events.get(uuid));
    }
}
