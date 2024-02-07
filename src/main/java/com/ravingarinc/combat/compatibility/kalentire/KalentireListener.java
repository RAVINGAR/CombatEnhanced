package com.ravingarinc.combat.compatibility.kalentire;

import com.herocraftonline.heroes.characters.equipment.EquipmentChangedEvent;
import com.herocraftonline.heroes.characters.equipment.EquipmentType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class KalentireListener implements Listener {
    private final KalentireHandler kalentireHandler;
    private final Map<EquipmentType, Consumer<EquipmentChangedEvent>> handlers;

    public KalentireListener(KalentireHandler kalentireHandler) {
        this.kalentireHandler = kalentireHandler;
        handlers = new HashMap<>();

        final Consumer<EquipmentChangedEvent> armourConsumer = (event) -> {
            KalentireHandler.EquipmentStats stats = kalentireHandler.getStats(event.getPlayer());
            stats.setDodgeFactor(event.getType(), kalentireHandler.parseDodgeStrength(event.getNewArmorPiece()));
            stats.setDodgeCost(event.getType(), kalentireHandler.parseDodgeCost(event.getNewArmorPiece()));
        };

        handlers.put(EquipmentType.HELMET, armourConsumer);
        handlers.put(EquipmentType.CHESTPLATE, armourConsumer);
        handlers.put(EquipmentType.LEGGINGS, armourConsumer);
        handlers.put(EquipmentType.BOOTS, armourConsumer);

        // Todo shields for block stats!
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEquipmentChange(final EquipmentChangedEvent event) {
        Optional.ofNullable(handlers.get(event.getType())).ifPresent(handler -> handler.accept(event));
    }


    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        kalentireHandler.loadStats(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        kalentireHandler.removePlayer(event.getPlayer());
    }
}
