package com.ravingarinc.combat.combat;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.api.ModuleListener;
import com.ravingarinc.combat.character.CharacterManager;
import com.ravingarinc.combat.compatibility.RPGHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;

public class CombatListener extends ModuleListener {
    private CombatManager manager;
    private CharacterManager characters;
    private RPGHandler handler;

    public CombatListener(final CombatEnhanced plugin) {
        super(CombatListener.class, plugin, CombatManager.class);
    }

    @Override
    protected void load() {
        manager = plugin.getModule(CombatManager.class);
        characters = plugin.getModule(CharacterManager.class);
        handler = plugin.getRPGHandler();

        var protocol = ProtocolLibrary.getProtocolManager();
        var packets = new ArrayList<PacketType>();
        packets.add(PacketType.Play.Client.POSITION);
        packets.add(PacketType.Play.Client.POSITION_LOOK);
        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR, packets, ListenerOptions.ASYNC) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if(event.isCancelled()) return;
                var packet = event.getPacket();
                onPlayerInput(event.getPlayer(),
                        packet.getDoubles().read(0),
                        packet.getDoubles().read(2));
            }
        });

        super.load();
    }

    private void onPlayerInput(Player player, double x, double z) {
        final var character = characters.getPlayer(player);
        final var input = character.getInput();
        if(input.update(x, z)) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                tryDodge(player);
            });
        }
    }

    public void tryDodge(Player player) {
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
