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
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatListener extends ModuleListener {
    private CombatManager manager;
    private CharacterManager characters;
    private RPGHandler handler;

    private Map<UUID, Long> lastSneaking = new ConcurrentHashMap<>();

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
        input.update(x, z);
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
    public void onPlayerSneakEvent(final PlayerToggleSneakEvent event) {
        final Player player = event.getPlayer();
        if(player.getGameMode() == GameMode.CREATIVE) return;
        final UUID uuid = player.getUniqueId();
        final Long lastTime = lastSneaking.remove(uuid);
        final long currentTime = System.currentTimeMillis();
        if(event.isSneaking()) {
            if(lastTime == null || lastTime + 2000 < currentTime) {
                lastSneaking.put(uuid, currentTime);
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    final var input = characters.getPlayer(player).getInput();
                    if(input.canDodge()) {
                        input.removeDodge();
                        tryDodge(player);
                    }
                });
            }
        } else {
            if(lastTime == null) {
                return;
            }
            if(currentTime - lastTime > 1000) {
                // if been sneaking for more than 1 second apply a timer to prevent any sneaks from occurring.
                lastSneaking.put(uuid, currentTime - 1000);
            }
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
