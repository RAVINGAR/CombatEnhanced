package com.ravingarinc.eldenrhym.combat;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.DependentListener;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class CombatListener extends DependentListener {
    private final CombatManager manager;

    public CombatListener(final EldenRhym plugin) {
        super(CombatListener.class, plugin, CombatManager.class);
        this.manager = plugin.getManager(CombatManager.class);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSwapHands(final PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
        final Player player = event.getPlayer();
        if (player.isBlocking() || player.isInsideVehicle() || cannotDodge(player)) {
            return;
        }
        final UUID uuid = player.getUniqueId();
        if (manager.isBlocking(uuid) || manager.isDodging(uuid)) {
            return;
        }
        manager.queueDodgeEvent(player);
    }


    private boolean cannotDodge(final Player player) {
        if (player.getVelocity().getY() > 0) {
            return true;
        }

        final Location location = player.getLocation();
        return location.getWorld().getBlockAt(location.subtract(0, -1, 0)).getType().isAir();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockEvent(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.OFF_HAND) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Player player = event.getPlayer();
            final ItemStack stack = event.getItem();
            //todo check if u need to check for a cooldown here
            if (player.isInsideVehicle() || !player.isBlocking()) {
                return;
            }
            if (stack == null || stack.getType() != Material.SHIELD) {
                return;
            }

            final UUID uuid = player.getUniqueId();
            final PlayerData data = PlayerData.get(uuid);
            if (data.getRPG().getStamina() > 0 && !manager.isDodging(uuid)) {
                manager.queueBlockEvent(player);
            }
        }
    }
}
