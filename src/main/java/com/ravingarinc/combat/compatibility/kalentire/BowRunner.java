package com.ravingarinc.combat.compatibility.kalentire;

import com.ravingarinc.combat.combat.runner.IdentifierRunner;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BowRunner extends IdentifierRunner<ShootEvent, EntityDamageByEntityEvent> {
    public BowRunner(KalentireWrapper wrapper) {
        super(wrapper.getProperties());
    }

    @Override
    public boolean handle(@NotNull UUID uuid, @NotNull EntityDamageByEntityEvent bukkitEvent) {
        if(!(bukkitEvent.getDamager() instanceof AbstractArrow arrow)) return false;
        final var ownerUUID = arrow.getOwnerUniqueId();
        if(ownerUUID == null) return false;

        return get(ownerUUID).map(shootEvent -> handleWithEvent(shootEvent, bukkitEvent))
                .orElseGet(() -> handleWithoutEvent(bukkitEvent));
    }

    @Override
    public boolean handleWithEvent(ShootEvent event, EntityDamageByEntityEvent bukkitEvent) {

    }

    @Override
    public boolean handleWithoutEvent(EntityDamageByEntityEvent bukkitEvent) {
        return false;
    }
}
