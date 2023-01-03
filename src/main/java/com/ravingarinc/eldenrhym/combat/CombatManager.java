package com.ravingarinc.eldenrhym.combat;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.BukkitApi;
import com.ravingarinc.eldenrhym.api.Module;
import com.ravingarinc.eldenrhym.api.Vector3;
import com.ravingarinc.eldenrhym.character.CharacterManager;
import com.ravingarinc.eldenrhym.combat.event.DodgeEvent;
import com.ravingarinc.eldenrhym.combat.event.PlayerBlockEvent;
import com.ravingarinc.eldenrhym.combat.runner.BlockRunner;
import com.ravingarinc.eldenrhym.combat.runner.DodgeRunner;
import com.ravingarinc.eldenrhym.combat.runner.IdentifierRunner;
import com.ravingarinc.eldenrhym.file.ConfigManager;
import com.ravingarinc.eldenrhym.file.Settings;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages combat interactions and computations. Any methods marked with AsyncHandler.Execute means that the method is
 * called from an asynchronous thread.
 */
@ThreadSafe
public class CombatManager extends Module {
    private static final long PERIOD = 2L;
    private final BlockData defaultData;
    private final Settings settings;
    private final BukkitScheduler scheduler;
    private final Map<UUID, Long> lastBlocks;
    private final Map<UUID, Long> lastDodges;
    private CharacterManager characterManager;
    private IdentifierRunner<PlayerBlockEvent, EntityDamageByEntityEvent> blockRunner;
    private IdentifierRunner<DodgeEvent, EntityDamageByEntityEvent> dodgeRunner;

    public CombatManager(final EldenRhym plugin) {
        super(CombatManager.class, plugin, ConfigManager.class, CharacterManager.class);
        this.settings = new Settings();
        this.scheduler = plugin.getServer().getScheduler();

        this.lastDodges = new HashMap<>();
        this.lastBlocks = new HashMap<>();
        this.defaultData = plugin.getServer().createBlockData(Material.COBWEB);
    }

    public boolean justBlocked(final UUID uuid) {
        final Long last = lastBlocks.get(uuid);
        if (last != null) {
            return System.currentTimeMillis() < last + settings.globalCooldown;
        }
        return false;
    }

    public boolean justDodged(final UUID uuid) {
        final Long last = lastDodges.get(uuid);
        if (last != null) {
            return System.currentTimeMillis() < last + settings.globalCooldown;
        }
        return false;
    }

    public void clearEntity(final UUID uuid) {
        this.lastBlocks.remove(uuid);
        this.lastDodges.remove(uuid);
        this.dodgeRunner.remove(uuid);
        this.blockRunner.remove(uuid);
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
        scheduler.runTaskAsynchronously(plugin, () -> blockRunner.add(new PlayerBlockEvent(characterManager.getPlayer(entity), time, settings)));
    }

    public void queueDodgeEvent(@NotNull final LivingEntity entity) {
        final long start = System.currentTimeMillis();
        lastDodges.put(entity.getUniqueId(), start);
        final Vector3 location = new Vector3(entity.getLocation());
        scheduler.runTaskAsynchronously(plugin, () ->
                characterManager.getCharacter(entity).ifPresent(character ->
                        dodgeRunner.add(new DodgeEvent(character, location, start, settings, defaultData))));
    }

    @BukkitApi
    public void handle(final EntityDamageByEntityEvent event) {
        final UUID uuid = event.getEntity().getUniqueId();
        if (!blockRunner.handle(uuid, event)) {
            dodgeRunner.handle(uuid, event);
        }
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
    protected void reload() {
        dodgeRunner.cancel();
        blockRunner.cancel();
    }

    @Override
    protected void load() {
        characterManager = plugin.getModule(CharacterManager.class);
        dodgeRunner = new DodgeRunner(settings);
        blockRunner = new BlockRunner(settings);

        dodgeRunner.runTaskTimerAsynchronously(plugin, 5, PERIOD);
        blockRunner.runTaskTimerAsynchronously(plugin, 5, PERIOD);
    }

    @Override
    protected void shutdown() {
        dodgeRunner.cancel();
        blockRunner.cancel();
    }

    public Settings getSettings() {
        return settings;
    }

}
