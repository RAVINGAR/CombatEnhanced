package com.ravingarinc.combat.combat;

import com.ravingarinc.api.module.Module;
import com.ravingarinc.api.module.RavinPlugin;
import com.ravingarinc.combat.api.BukkitApi;
import com.ravingarinc.combat.api.Vector3;
import com.ravingarinc.combat.character.CharacterManager;
import com.ravingarinc.combat.combat.event.DodgeEvent;
import com.ravingarinc.combat.combat.event.PlayerBlockEvent;
import com.ravingarinc.combat.combat.runner.DodgeRunner;
import com.ravingarinc.combat.combat.runner.IdentifierRunner;
import com.ravingarinc.combat.compatibility.RPGHandler;
import com.ravingarinc.combat.compatibility.RPGWrapper;
import com.ravingarinc.combat.file.Properties;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;

/**
 * Manages combat interactions and computations. Any methods marked with AsyncHandler.Execute means that the method is
 * called from an asynchronous thread.
 */
@ThreadSafe
public class CombatManager extends Module {
    private static final long PERIOD = 2L;
    private final BlockData defaultData;
    private final BukkitScheduler scheduler;
    private final Map<UUID, Long> lastBlocks;
    private final Map<UUID, Long> lastDodges;
    private CharacterManager characterManager;
    private IdentifierRunner<PlayerBlockEvent, EntityDamageByEntityEvent> blockRunner;
    private IdentifierRunner<DodgeEvent, EntityDamageByEntityEvent> dodgeRunner;

    private final List<IdentifierRunner<?, EntityDamageByEntityEvent>> damageEventRunners = new ArrayList<>();

    private RPGWrapper handler;
    private Properties properties;

    public CombatManager(final RavinPlugin plugin) {
        super(CombatManager.class, plugin, Properties.class, CharacterManager.class);
        this.scheduler = plugin.getServer().getScheduler();

        this.lastDodges = new HashMap<>();
        this.lastBlocks = new HashMap<>();
        this.defaultData = plugin.getServer().createBlockData(Material.COBWEB);
    }

    public boolean justBlocked(final UUID uuid) {
        final Long last = lastBlocks.get(uuid);
        if (last != null) {
            return System.currentTimeMillis() < last + properties.globalCooldown;
        }
        return false;
    }

    public boolean justDodged(final UUID uuid) {
        final Long last = lastDodges.get(uuid);
        if (last != null) {
            return System.currentTimeMillis() < last + properties.globalCooldown;
        }
        return false;
    }

    public void clearEntity(final UUID uuid) {
        this.lastBlocks.remove(uuid);
        this.lastDodges.remove(uuid);
        damageEventRunners.forEach(runner -> runner.remove(uuid));
    }

    public boolean isBlocking(final UUID uuid) {
        return blockRunner.get(uuid).isPresent();
    }

    public boolean isDodging(final UUID uuid) {
        return dodgeRunner.get(uuid).map(DodgeEvent::isDodging).orElse(false);
    }

    public void queueBlockEvent(@NotNull final Player entity) {
        final long time = System.currentTimeMillis();
        lastBlocks.put(entity.getUniqueId(), time);
        scheduler.runTaskAsynchronously(plugin, () -> blockRunner.add(new PlayerBlockEvent(characterManager.getPlayer(entity), time, properties)));
    }

    public void queueDodgeEvent(@NotNull final LivingEntity entity) {
        final long start = System.currentTimeMillis();
        lastDodges.put(entity.getUniqueId(), start);
        final Vector3 location = new Vector3(entity.getLocation());
        characterManager.getCharacter(entity).ifPresent(character -> {
            scheduler.runTaskAsynchronously(plugin, () -> {
                dodgeRunner.add(new DodgeEvent(character, location, start, properties, handler, defaultData));
            });
        });
    }

    @BukkitApi
    public void handle(final EntityDamageByEntityEvent event) {
        if(!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        for(IdentifierRunner<?, EntityDamageByEntityEvent> runner : damageEventRunners) {
            if(runner.handle(event)) {
                return;
            }
        }
        // Handle only if runner is not handled.
        handler.onDamageEvent(event);
    }

    public void handleBlockInteraction(final Player player) {
        scheduler.scheduleSyncDelayedTask(plugin, () -> {
            if (player.isBlocking()) {
                final UUID uuid = player.getUniqueId();
                if (justBlocked(uuid) || isDodging(uuid)) {
                    return;
                }
                queueBlockEvent(player);
            }
        }, 6L);
    }

    @Override
    public void load() {
        properties = plugin.getModule(Properties.class);
        handler = plugin.getModule(RPGHandler.class).getWrapper();
        characterManager = plugin.getModule(CharacterManager.class);
        dodgeRunner = new DodgeRunner(handler);
        blockRunner = handler.getBlockRunner();

        registerRunner(dodgeRunner);
        registerRunner(blockRunner);

        handler.injectRunners(this);

        damageEventRunners.forEach(runner -> runner.runTaskTimerAsynchronously(plugin, 5, PERIOD));
    }

    public void registerRunner(final IdentifierRunner<?, EntityDamageByEntityEvent> runner) {
        damageEventRunners.add(runner);
    }

    @Override
    public void cancel() {
        damageEventRunners.forEach(BukkitRunnable::cancel);
        damageEventRunners.clear();
    }

}
