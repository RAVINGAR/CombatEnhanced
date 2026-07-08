package com.ravingarinc.combat.compatibility.kalentire.bows;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.ravingarinc.api.I;
import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.compatibility.RPGHandler;
import com.ravingarinc.combat.compatibility.kalentire.KalentireWrapper;
import com.ravingarinc.kalentirerpg.item.stats.Stat;
import com.ravingarinc.kalentirerpg.item.stats.type.Arrow;
import com.ravingarinc.kalentirerpg.progression.event.EventBus;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.player.PlayerMetadata;
import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
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
                            rawDamage * (1.0 + ((Math.sqrt(distanceSq) - shot.fallOffRange()) * KalentireWrapper.FALL_OFF_REDUCTION)));
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
        // todo add crit damage somewhere here.
        event.setDamage(rawDamage);
        wrapper.getPoiseRunner().handle(event, rawDamage, (Entity)((AbstractArrow)event.getDamager()).getShooter(),
                shot.impact());
    }


    public void add(UUID uuid, Location origin,  PlayerMetadata statMap, NBTItem consumable, double multiplier) {
        // We override this such that the event is added before it is computed.
        final var future = new CompletableFuture<ShootEvent>();
        cache.put(uuid, future);
        final Runnable runnable = () -> future.complete(compute(origin, statMap, consumable, multiplier));
        try {
            queue.put(runnable);
        } catch(InterruptedException e) {
            I.log(Level.WARNING, "Queue in bow runner was interrupted!");
        }
    }

    private static ShootEvent compute(Location origin, PlayerMetadata statMap,
                                 NBTItem consumable, double multiplier) {
        final var damage = statMap.getStat(Stat.RANGED_DAMAGE.mmoKey());

        final var type = Arrow.Type.valueOf(consumable.getString("MMOITEMS_ARROW_TYPE"));
        final var damageType = type.get(Stat.DAMAGE_TYPE);
        final var arrowDamage = damageType == null ? 0.0 :
                consumable.getDouble("MMOITEMS_" + damageType.damage().mmoKey());

        // use type.getEffect() to add effects and such
        // TODO Consider imbuements effects

        final var impact = getStat(statMap, consumable, Stat.IMPACT.mmoKey()) * multiplier;
        final var penetration = getStat(statMap, consumable, (Stat.PENETRATION.mmoKey()))  * multiplier;
        final var knockback = getStat(statMap, consumable, (Stat.KNOCKBACK.mmoKey())) * multiplier;
        final var fallOffRange = getStat(statMap, consumable, (Stat.FALL_OFF_RANGE.mmoKey()));

        return new ShootEvent(origin, damageType, (damage + arrowDamage) * multiplier, impact * multiplier,
                penetration * multiplier,
                knockback,
                fallOffRange, multiplier, new HashSet<>());
    }

    private static double getStat(PlayerMetadata stats, NBTItem consumable, String stat) {
        return stats.getStat(stat) + consumable.getStat(stat);
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

    private static final Random RANDOM = new Random(System.currentTimeMillis());
    static {
        EventBus.subscribe(EntityDamageByEntityEvent.class, (event) -> {
            getBowRunner().handle(event);
        });

        EventBus.subscribe(EntityShootBowEvent.class, (event) -> {
            final var consumed = event.getConsumable();
            if(!(event.getEntity() instanceof Player player && consumed != null)) return;

            final var category = ShootEvent.Category.matchCategory(event.getBow());
            if(category == null) return;

            final var bow = NBTItem.get(event.getBow());
            if(!bow.hasType()) return;

            final var consumable = NBTItem.get(consumed);
            if(!consumable.hasType()) return;

            final var statMap = MMOPlayerData.get(player).getStatMap().cache(EquipmentSlot.MAIN_HAND);
            final var bowType = bow.getType();
            final var hero =  Heroes.getInstance().getCharacterManager().getHero(player);
            final var effect = (PerfectAimEffect) hero.getEffect(PerfectAimEffect.NAME);

            double multiplier = event.getForce();
            double sway = getStat(statMap, consumable, Stat.AIM_SWAY.mmoKey());
            if(effect != null) {
                multiplier += getStat(statMap, consumable, Stat.AIM_BONUS.mmoKey());
                if (bowType.equalsIgnoreCase("GREATBOW") || bowType.equalsIgnoreCase("SIEGE_CROSSBOW")) {
                    sway = 0.0;
                } else {
                    sway /= 2.0;
                }
            }
            hero.tryRemoveEffect(AimingEffect.NAME);
            hero.tryRemoveEffect(PerfectAimEffect.NAME);

            getBowRunner().add(player.getUniqueId(), player.getLocation(), statMap,
                    consumable, multiplier);

            final var proj = event.getProjectile();
            final var velocity = getStat(statMap, consumable, Stat.VELOCITY.mmoKey()) * multiplier;

            proj.setVelocity(randomiseVelocity(proj.getVelocity(), sway, velocity));
        });

    }

    private static BowRunner getBowRunner() {
        return ((KalentireWrapper)CombatEnhanced.getInstance().getModule(RPGHandler.class).getWrapper()).getBowRunner();
    }

    /**
     * Randomizes a velocity vector to create a "spread" effect.
     *
     * @param initialVelocity The starting velocity vector of the projectile.
     * @param spreadFactor A value controlling the spread cone. 0 is no spread.
     * Good values are typically between 0.05 and 0.5.
     * @return A new velocity vector with a randomized direction.
     */
    private static Vector randomiseVelocity(Vector initialVelocity, double spreadFactor, double speed) {
        // 1. Get the original speed and direction
        Vector direction = initialVelocity.normalize();

        // 2. & 3. Generate a random offset vector scaled by the spread factor
        // Generates random values between -1 and 1
        double offsetX = (RANDOM.nextDouble() * 2 - 1) * spreadFactor;
        double offsetY = (RANDOM.nextDouble() * 2 - 1) * spreadFactor;
        double offsetZ = (RANDOM.nextDouble() * 2 - 1) * spreadFactor;
        Vector randomOffset = new Vector(offsetX, offsetY, offsetZ);

        // 4. Combine the original direction with the random offset and re-normalize
        Vector newDirection = direction.add(randomOffset).normalize();

        // 5. Apply the original speed to the new direction
        return newDirection.multiply(speed);
    }
}
