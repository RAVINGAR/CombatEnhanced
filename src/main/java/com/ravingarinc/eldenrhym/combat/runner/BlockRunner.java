package com.ravingarinc.eldenrhym.combat.runner;

import com.ravingarinc.eldenrhym.api.AsyncHandler;
import com.ravingarinc.eldenrhym.api.Vector3;
import com.ravingarinc.eldenrhym.combat.event.PlayerBlockEvent;
import com.ravingarinc.eldenrhym.file.Settings;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class BlockRunner extends IdentifierRunner<PlayerBlockEvent, EntityDamageByEntityEvent> {

    public BlockRunner(final Settings settings) {
        super(settings);
    }

    @Override
    public boolean handleWithEvent(final PlayerBlockEvent blockEvent, final EntityDamageByEntityEvent event) {
        final Player defender = (Player) event.getEntity();
        if (defender.isBlocking() && settings.blockDamageCauses.contains(event.getCause())) {
            defender.sendMessage(ChatColor.RED + "You blocked the attack!");
            //attacker.sendMessage(ChatColor.RED + defender.getName() + " blocked the attack!");
            defender.playSound(defender, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F, 1.0F);
            defender.getLocation().getWorld().playSound(defender, Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
            if (event.getDamager() instanceof LivingEntity livingAttacker) {
                throwEntity(defender, livingAttacker, settings.blockThrowStrength);
                livingAttacker.playEffect(EntityEffect.HURT);
            } else if (event.getDamager() instanceof AbstractArrow arrow) {
                defender.launchProjectile(arrow.getClass(), arrow.getVelocity().multiply(-0.5)).setDamage(arrow.getDamage() * 0.5);
                arrow.remove();
            }
            handlePostEvent(event, defender, settings.blockSuccessMitigation);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleWithoutEvent(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player defender) {
            if (defender.isBlocking() && settings.blockDamageCauses.contains(event.getCause())) {
                defender.playSound(defender, Sound.ITEM_SHIELD_BREAK, 1.0F, 1.0F);
                defender.setCooldown(Material.SHIELD, (int) (settings.blockCooldown / 1000 * 20));
                throwEntity(event.getDamager(), defender, settings.blockThrowStrength);
                handlePostEvent(event, defender, settings.blockFailMitigation);
                return true;
            }
        }
        return false;
    }

    private void handlePostEvent(final EntityDamageByEntityEvent event, final LivingEntity defender, final double mitigation) {
        final double damage = event.getDamage() * (1.0 - mitigation);
        if (damage > 0) {
            event.setDamage(damage);
            damageEntity(defender, damage);
        } else {
            event.setCancelled(true);
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

    public void throwEntity(final Entity source, final LivingEntity target, final float strength) {
        final Vector3 sourceLoc = new Vector3(source.getLocation());
        final Vector3 targetLoc = new Vector3(target.getLocation());
        final Vector3 initialVec = new Vector3(target.getVelocity());
        final double resistance = target.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getValue();
        final boolean isOnGround = target.isOnGround();

        AsyncHandler.executeAsyncComputation(() -> {
            double d0 = sourceLoc.getX() - targetLoc.getX();
            double d1;

            for (d1 = sourceLoc.getZ()
                    - targetLoc.getZ(); ((d0 * d0) + (d1 * d1)) < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
                d0 = (Math.random() - Math.random()) * 0.01D;
            }
            final float knockbackStrength = (float) ((double) strength * (1.0D - resistance));
            if (knockbackStrength > 0.0F) {
                Vector3 postVec = new Vector3(d0, 0.0D, d1);
                if (postVec.length() < 1.0E-4D) {
                    postVec = new Vector3();
                } else {
                    postVec.normalize();
                }
                postVec.scale(knockbackStrength);

                return new Vector3(
                        initialVec.getX() / 2.0D - postVec.getX(),
                        isOnGround ? Math.min(0.4D, initialVec.getY() / 2.0D + (double) knockbackStrength) : initialVec.getY(),
                        initialVec.getZ() / 2.0D - postVec.getZ());
            }
            return null;
        }, (vec) -> {
            if (vec != null) {
                target.setVelocity(vec.toBukkitVector());
            }
        });
    }
}
