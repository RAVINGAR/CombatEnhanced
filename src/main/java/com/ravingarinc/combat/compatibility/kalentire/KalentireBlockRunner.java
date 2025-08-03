package com.ravingarinc.combat.compatibility.kalentire;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.ravingarinc.combat.combat.event.PlayerBlockEvent;
import com.ravingarinc.combat.combat.runner.BlockRunner;
import com.ravingarinc.combat.compatibility.RPGWrapper;
import com.ravingarinc.combat.compatibility.kalentire.effect.PoiseStunEffect;
import com.ravingarinc.kalentirerpg.damage.type.DamageType;
import com.ravingarinc.kalentirerpg.item.stats.Stat;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.damage.DamagePacket;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;


public class KalentireBlockRunner extends BlockRunner {
    private CharacterManager manager = Heroes.getInstance().getCharacterManager();
    public KalentireBlockRunner(RPGWrapper handler) {
        super(handler);
    }

    @Override
    public boolean handleWithEvent(PlayerBlockEvent blockEvent, EntityDamageByEntityEvent event) {
        final Player defender = (Player) event.getEntity();
        if (defender.isBlocking() && properties.blockDamageCauses.contains(event.getCause())) {
            final int requiredStamina = getRequiredStamina(defender, event) / 2;
            if (handler.tryRemoveStamina(defender, requiredStamina)) {
                defender.sendMessage(ChatColor.RED + "< You perfectly blocked the attack! >");
                defender.playSound(defender, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F, 1.0F);
                defender.getWorld().playSound(defender, Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);

                if (event.getDamager() instanceof LivingEntity livingAttacker) {
                    throwEntity(defender, livingAttacker, properties.blockThrowStrength);
                    ((KalentireWrapper)handler).addPoiseDamage(livingAttacker,
                            10.0F * KalentireWrapper.getPerfectBlockBonus(defender));
                    livingAttacker.playHurtAnimation(0F);
                } else if (event.getDamager() instanceof AbstractArrow arrow) {
                    defender.launchProjectile(arrow.getClass(), arrow.getVelocity().multiply(-0.5)).setDamage(arrow.getDamage() * 0.5);
                    arrow.remove();
                }
                handlePostEvent(event, defender, KalentireWrapper.getPerfectBlockBonus(defender));
            } else {
                throwEntity(event.getDamager(), defender, properties.blockThrowStrength / 2.0F);
                defender.playHurtAnimation(0);
                handlePostEvent(event, defender, 1.0);
                final var hero = manager.getHero(defender);
                final var ticks = requiredStamina - hero.getStamina();
                hero.addEffect(new PoiseStunEffect(event.getDamager(), ticks * 50L, properties.stunCooldown, 0.0));
            }
            return true;
        }
        return false;
    }

    private int getRequiredStamina(Player defender, EntityDamageByEntityEvent event) {
        final var data = MMOPlayerData.get(defender.getUniqueId()).getStatMap();
        final var damager = event.getDamager();
        final var impact = damager instanceof LivingEntity livingEntity ? KalentireWrapper.getImpact(livingEntity) : 0.0;
        return (int) Math.floor(event.getDamage() + impact / data.getStat("STAMINA_POISE_DAMAGE"));
    }

    @Override
    public boolean handleWithoutEvent(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player defender) {
            if (defender.isBlocking() && properties.blockDamageCauses.contains(event.getCause())) {
                final int requiredStamina = getRequiredStamina(defender, event);
                if (handler.tryRemoveStamina(defender, requiredStamina)) {
                    defender.getWorld().playSound(defender, Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
                    handlePostEvent(event, defender, 1.0);
                } else {
                    defender.playSound(defender, Sound.ITEM_SHIELD_BREAK, 1.0F, 1.0F);
                    defender.setCooldown(Material.SHIELD, (int) handler.getShieldCooldown(defender));
                }
                throwEntity(event.getDamager(), defender, properties.blockThrowStrength / 2.0F);
                return true;
            }
        }
        return false;
    }

    private void handlePostEvent(final EntityDamageByEntityEvent event, final Player defender, final double bonusFactor) {
        handler.onDamageEvent(event);
        final var meta = MythicLib.plugin.getDamage().findAttack(event);
        final var damageMeta = meta.getDamage();
        final var statMap = MMOPlayerData.get(defender).getStatMap();

        // Todo maybe move this to Kalentire Plugin in some way shape or form idk.

        final var attacker = meta.getAttacker();
        final var penetration = attacker == null ? 0.0 : attacker.getStat(Stat.PENETRATION.mmoKey());
        final var percentilePen = attacker == null ? 0.0 : attacker.getStat(Stat.PERCENTILE_PENETRATION.mmoKey());

        for(DamagePacket packet : damageMeta.getPackets()) {
            final var element = packet.getElement();
            if(element == null) continue;
            final var type = DamageType.fromMythicElement(element);
            final var parent = type.getParent();
            var bonusNegation = 0.0;
            var bonusResistance = 0.0;
            if(!type.equals(parent)) {
                bonusNegation = statMap.getStat(parent.shieldNegation().mmoKey());
                bonusResistance = statMap.getStat(parent.shieldResistance().mmoKey());
            }
            final var negation = (statMap.getStat(type.shieldNegation().mmoKey()) + bonusNegation) * bonusFactor;
            final var resistance = (statMap.getStat(type.shieldResistance().mmoKey()) + bonusResistance) * bonusFactor;
            if(negation != 0) {
                final var fNegation = negation > 0.0 && penetration != 0.0
                        ? -1 * Math.max(0.0, negation - penetration)
                        : -1 * negation;
                packet.setValue(Math.max(0.0, packet.getValue() + fNegation));
            }
            if (resistance != 0.0) {
                packet.multiplicativeModifier(1.0 - (resistance - percentilePen));
            }
            if(packet.hasType(io.lumine.mythic.lib.damage.DamageType.PROJECTILE)) {
                packet.multiplicativeModifier((statMap.getStat(Stat.SHIELD_BONUS.mmoKey())));
            }
        }
        event.setDamage(damageMeta.getDamage());
    }
}
