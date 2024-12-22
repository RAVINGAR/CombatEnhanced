package com.ravingarinc.combat.compatibility.kalentire;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.ravingarinc.api.I;
import com.ravingarinc.kalentirerpg.item.stats.Stat;
import com.ravingarinc.kalentirerpg.item.stats.type.Arrow;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.player.PlayerMetadata;
import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class BowRunner extends BukkitRunnable {
    private final KalentireWrapper wrapper;
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private final Map<UUID, CompletableFuture<ShootEvent>> cache = new ConcurrentHashMap<>();
    private final AtomicBoolean internalCancel = new AtomicBoolean(false);

    public BowRunner(KalentireWrapper wrapper) {
        super();
        this.wrapper = wrapper;
    }

    public boolean handle(@NotNull EntityDamageByEntityEvent bukkitEvent) {
        if(!(bukkitEvent.getDamager() instanceof AbstractArrow arrow)) return false;
        final var ownerUUID = arrow.getOwnerUniqueId();
        if(ownerUUID == null) return false;

        final var future = cache.get(ownerUUID);
        if(future == null) return false;

        try {
            final var result = future.get(100, TimeUnit.MILLISECONDS);
            handleWithEvent(result, bukkitEvent);
            return true;
        } catch(InterruptedException | ExecutionException | TimeoutException e) {
            I.log(Level.WARNING, "Encountered issue in bow runner!", e);
        }
        return false;
    }
    public void handleWithEvent(ShootEvent shot, EntityDamageByEntityEvent event) {
        final var defender = (LivingEntity)event.getEntity();
        final var origin = shot.origin();
        final var defenderLocation = defender.getLocation();
        final double distanceSq;
        if(origin.getWorld().equals(defenderLocation.getWorld())) {
            distanceSq = origin.distanceSquared(defenderLocation);
        } else {
            distanceSq = 64;
        }
        final var rangeSq = Math.pow(shot.fallOffRange(), 2);
        var rawDamage = shot.damage();
        if(distanceSq > rangeSq) {
            rawDamage =
                    Math.max(0.0,
                            rawDamage * (1.0 + ((Math.sqrt(distanceSq) - shot.fallOffRange()) * shot.fallOffReduction())));
        }
        final var metadata = MythicLib.plugin.getDamage().findAttack(event);
        //metadata.getDamage().getInitialPacket().setValue(0.0);

        if(rawDamage > 0.0) {
            final var packet = metadata.getDamage().getPackets().get(0);
            packet.setValue(rawDamage);
            packet.setElement(shot.damageType().toMythicElement());
            NMSHandler.getInterface().knockBack(defender,
                    event.getDamager().getLocation(), (float)shot.knockback());
            final var heroesCharacter = Heroes.getInstance().getCharacterManager().getCharacter(defender);
            shot.effects().forEach(heroesCharacter::addEffect);
        }
        event.setDamage(rawDamage);
        wrapper.getPoiseRunner().handle(event, rawDamage, (Entity)((AbstractArrow)event.getDamager()).getShooter(),
                shot.impact());
    }


    public void add(UUID uuid, Location origin,  PlayerMetadata statMap, NBTItem consumable,
                    float force) {
        // We override this such that the event is added before it is computed.
        final var future = new CompletableFuture<ShootEvent>();
        cache.put(uuid, future);
        final Runnable runnable = () -> future.complete(compute(origin, statMap, consumable, force));
        try {
            queue.put(runnable);
        } catch(InterruptedException e) {
            I.log(Level.WARNING, "Queue in bow runner was interrupted!");
        }
    }

    private static ShootEvent compute(Location origin, PlayerMetadata statMap,
                                 NBTItem consumable,
                                float force) {
        final var damage = statMap.getStat(KalentireWrapper.RANGED_DAMAGE);

        final var type = Arrow.Type.valueOf(consumable.getString("MMOITEMS_ARROW_TYPE"));
        final var damageType = type.get(Stat.DAMAGE_TYPE);
        final var arrowDamage = damageType == null ? 0.0 :
                consumable.getDouble("MMOITEMS_" + damageType.damage().mmoKey());

        // use type.getEffect() to add effects and such
        final var multiplier = force == 1.0F ? 1.0F + statMap.getStat(KalentireWrapper.FULL_DRAW_BONUS) : force;
        // TODO Consider imbuements effects
        final var impact =
                statMap.getStat(KalentireWrapper.IMPACT) + consumable.getDouble("MMOITEMS_" + KalentireWrapper.IMPACT);
        final var penetration = statMap.getStat("KALENTIRE_PENETRATION") + consumable.getDouble(
                "MMOITEMS_KALENTIRE_PENETRATION");
        final var knockback =
                statMap.getStat(KalentireWrapper.KNOCKBACK) + consumable.getDouble("MMOITEMS_" + KalentireWrapper.KNOCKBACK);
        final var fallOffRange =
                statMap.getStat(KalentireWrapper.FALL_OFF_RANGE) + consumable.getDouble("MMOITEMS_" + KalentireWrapper.FALL_OFF_RANGE);
        final var fallOffReduction = statMap.getStat(KalentireWrapper.FALL_OFF_REDUCTION);

        return new ShootEvent(origin, damageType, (damage + arrowDamage) * multiplier, impact * multiplier,
                penetration * multiplier,
                knockback,
                fallOffRange, fallOffReduction, multiplier, new HashSet<>());
    }

    @Override
    public void run() {
        while(!internalCancel.get()) {
            try {
                queue.take().run();
            } catch (InterruptedException e) {
                internalCancel.set(true);
            }
        }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        queue.add(() -> internalCancel.set(true));
    }
}
