package com.ravingarinc.combat.combat;

import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.api.ModuleListener;
import com.ravingarinc.combat.compatibility.RPGHandler;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.UUID;

public class CombatListener extends ModuleListener {
    private CombatManager manager;
    private RPGHandler handler;

    public CombatListener(final CombatEnhanced plugin) {
        super(CombatListener.class, plugin, CombatManager.class);
    }

    @Override
    protected void load() {
        manager = plugin.getModule(CombatManager.class);
        handler = plugin.getRPGHandler();

        super.load();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSwapHands(final PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);

        final Player player = event.getPlayer();
        final Vector velocity =  player.getVelocity();
        if (player.isBlocking() || player.isInsideVehicle() || velocity.getY() > 0) {
            return;
        }
        final UUID uuid = player.getUniqueId();
        if (manager.justDodged(uuid) || manager.isBlocking(uuid) || manager.isDodging(uuid)) {
            return;
        }
        if (handler.tryRemoveStamina(player, handler.getDodgeCost(player))) {
            manager.queueDodgeEvent(player);
        } else {
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_SNARE, 0.5F, 0.5F);
        }
    }

    @EventHandler
    public void onBlockEvent(final PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            final Player player = event.getPlayer();
            final ItemStack item = event.getItem();
            if (item == null) {
                return;
            }
            final Material type = item.getType();
            if (type == Material.SHIELD && !player.hasCooldown(Material.SHIELD)) {
                manager.handleBlockInteraction(player);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDamageEvent(final EntityDamageByEntityEvent event) {
        manager.handle(event);
    }
}
