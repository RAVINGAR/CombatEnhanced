package com.ravingarinc.eldenrhym.combat;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.AsynchronousException;
import com.ravingarinc.eldenrhym.api.Module;
import com.ravingarinc.eldenrhym.character.CharacterEntity;
import com.ravingarinc.eldenrhym.character.CharacterManager;
import com.ravingarinc.eldenrhym.combat.event.DodgeEvent;
import com.ravingarinc.eldenrhym.combat.event.PlayerBlockEvent;
import com.ravingarinc.eldenrhym.combat.event.TargetDodgeEvent;
import com.ravingarinc.eldenrhym.combat.runner.CombatRunner;
import com.ravingarinc.eldenrhym.combat.runner.IdentifierRunner;
import com.ravingarinc.eldenrhym.file.ConfigManager;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages combat interactions and computations. Any methods marked with Async.Execute means that the method is
 * called from an asynchronous thread.
 */
@ThreadSafe
public class CombatManager extends Module {
    private static final long PERIOD = 2L;
    private final Settings settings;
    private final BukkitScheduler scheduler;
    private final Map<UUID, Long> lastBlocks;
    private final Map<UUID, Long> lastDodges; //todo remove UUIDs when player's log out or die, and same for mobs
    private CharacterManager characterManager;
    private CombatRunner runner;
    private IdentifierRunner<PlayerBlockEvent> blockRunner;
    private IdentifierRunner<DodgeEvent> dodgeRunner;

    public CombatManager(final EldenRhym plugin) {
        super(CombatManager.class, plugin, ConfigManager.class, CharacterManager.class);
        this.settings = new Settings();
        this.scheduler = plugin.getServer().getScheduler();

        this.lastDodges = new HashMap<>();
        this.lastBlocks = new HashMap<>();
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
        scheduler.runTaskAsynchronously(plugin, () -> blockRunner.add(new PlayerBlockEvent(characterManager.getPlayer(entity), time, settings.blockDuration)));
    }

    public void queueDodgeEvent(@NotNull final Player entity) {
        final long time = System.currentTimeMillis();
        lastDodges.put(entity.getUniqueId(), time);
        scheduler.runTaskAsynchronously(plugin, () -> queueDodgeEvent(characterManager.getPlayer(entity), time));
    }

    public void queueDodgeEvent(@NotNull final Monster entity) {
        final long time = System.currentTimeMillis();
        lastDodges.put(entity.getUniqueId(), time);
        scheduler.runTaskAsynchronously(plugin, () -> queueDodgeEvent(characterManager.getMonster(entity), time));
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

    /**
     * Handle a block event on an EntityDamageEvent only if the entity is blocking
     *
     * @param defender    The damage taker in the event
     * @param cause       The damage cause
     * @param eventDamage The original event damage
     * @return The new damage of the event. If 0, the event should be cancelled
     */
    public double handleBlockEvent(final Player defender, final DamageCause cause, final double eventDamage) {
        double postDamage = eventDamage;
        if (settings.blockDamageCauses.contains(cause)) {
            final double mitigation;
            if (isBlocking(defender.getUniqueId())) {
                // defender.sendMessage(ChatColor.GRAY + "You blocked the attack!");
                // attacker.sendMessage(ChatColor.RED + defender.getName() + " blocked the attack!");
                defender.playSound(defender, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F, 1.0F);
                mitigation = settings.blockSuccessMitigation;
            } else {
                defender.playSound(defender, Sound.ITEM_SHIELD_BREAK, 1.0F, 1.0F);
                defender.setCooldown(Material.SHIELD, (int) (settings.blockCooldown / 1000 * 20));
                mitigation = settings.blockFailMitigation;
            }
            postDamage *= (1.0 - mitigation);
            if (postDamage > 0) {
                damageEntity(defender, postDamage);
            }
        }
        return postDamage;
    }

    /**
     * Handle a dodge event on an EntityDamageEvent only if the entity is dodging
     *
     * @param defender    The damage taker in the event
     * @param cause       The damage cause
     * @param eventDamage The original event damage
     * @return The new damage of the event. If 0, the event should be cancelled
     */
    public double handleDodgeEvent(final LivingEntity defender, final DamageCause cause, final double eventDamage) {
        double postDamage = eventDamage;
        if (settings.dodgeDamageCauses.contains(cause)) {
            if (isDodging(defender.getUniqueId())) {
                // defender.sendMessage(ChatColor.RED + "You dodged the attack!");
                // attacker.sendMessage(ChatColor.RED + defender.getName() + " dodged the attack!");
                if (defender instanceof Player player) {
                    player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F, 1.0F);
                }
                postDamage *= (1.0 - settings.dodgeMitigation);
            }
        }
        return postDamage;
    }

    @Async.Execute
    private void queueDodgeEvent(final CharacterEntity<?> character, final long start) {
        try {
            character.getTarget(8)
                    .ifPresentOrElse((target) ->
                                    dodgeRunner.add(new TargetDodgeEvent(character, target, start, settings.dodgeWarmup, settings.dodgeDuration, settings.dodgeStrength)),
                            () -> dodgeRunner.add(new DodgeEvent(character, start, settings.dodgeWarmup, settings.dodgeDuration, settings.dodgeStrength)));
        } catch (final AsynchronousException e) {
            EldenRhym.log(Level.SEVERE, "Encountered issue adding dodge event!", e);
        }
    }

    public void damageEntity(final LivingEntity target, final double originalDamage) {
        if (target.isDead() || (target.getHealth() <= 0)) {
            return;
        }
        double damage = originalDamage;

        final double oldShield = target.getAbsorptionAmount();
        double newShield = oldShield - damage;
        if (newShield < 0) {
            newShield = 0;
        }
        damage -= oldShield - newShield;
        final double oldHealth = target.getHealth();
        double newHealth = oldHealth - damage;
        if (newHealth < 0) {
            newHealth = 0;
        }

        target.setHealth(newHealth);
        target.setAbsorptionAmount(newShield);
        target.playEffect(EntityEffect.HURT);
    }


    @Override
    protected void reload() {
        runner.cancel();
        dodgeRunner.cancel();
        blockRunner.cancel();
    }

    @Override
    protected void load() {
        characterManager = plugin.getModule(CharacterManager.class);
        runner = new CombatRunner();
        dodgeRunner = new IdentifierRunner<>();
        blockRunner = new IdentifierRunner<>();

        runner.runTaskTimerAsynchronously(plugin, 0, PERIOD);
        dodgeRunner.runTaskTimerAsynchronously(plugin, 5, PERIOD);
        blockRunner.runTaskTimerAsynchronously(plugin, 5, PERIOD);
    }

    @Override
    protected void shutdown() {
        dodgeRunner.cancel();
        blockRunner.cancel();
        runner.cancel();
    }

    public Settings getSettings() {
        return settings;
    }

    public static class Settings {

        public long globalCooldown = 50;
        public boolean counterEnabled = true;
        public long counterStartTime = 100;
        public long counterEndTime = 500;

        public boolean parryEnabled = true;

        public boolean dodgeEnabled = true;
        public long dodgeWarmup = 100;
        public long dodgeDuration = 300;
        public float dodgeStrength = 0.5F;
        public double dodgeMitigation = 0.5;

        public List<DamageCause> dodgeDamageCauses = new ArrayList<>();

        public boolean blockEnabled = true;
        public long blockDuration = 600;

        public double blockSuccessMitigation = 1.0;

        public double blockFailMitigation = 0.5;
        public long blockCooldown = 500;

        public List<DamageCause> blockDamageCauses = new ArrayList<>();
    }
}
