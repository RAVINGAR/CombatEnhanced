package com.ravingarinc.combat.compatibility.kalentire;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.combat.event.PlayerBlockEvent;
import com.ravingarinc.combat.combat.runner.BlockRunner;
import com.ravingarinc.combat.compatibility.kalentire.effect.PoiseStunEffect;
import com.ravingarinc.combat.file.Settings;
import com.ravingarinc.kalentirerpg.damage.type.DamageType;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class KalentireBlockRunner extends BlockRunner {
    private CharacterManager manager = Heroes.getInstance().getCharacterManager();
    public KalentireBlockRunner(Settings settings, CombatEnhanced plugin) {
        super(settings, plugin);
    }

    @Override
    public boolean handleWithEvent(PlayerBlockEvent blockEvent, EntityDamageByEntityEvent event) {
        final Player defender = (Player) event.getEntity();
        if (defender.isBlocking() && settings.blockDamageCauses.contains(event.getCause())) {
            final int requiredStamina = getRequiredStamina(defender, event) / 2;
            if (handler.tryRemoveStamina(defender, requiredStamina)) {
                defender.sendMessage(ChatColor.RED + "You blocked the attack!");
                defender.playSound(defender, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F, 1.0F);
                defender.getWorld().playSound(defender, Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);

                if (event.getDamager() instanceof LivingEntity livingAttacker) {
                    throwEntity(defender, livingAttacker, settings.blockThrowStrength);
                    livingAttacker.playEffect(EntityEffect.HURT);
                } else if (event.getDamager() instanceof AbstractArrow arrow) {
                    defender.launchProjectile(arrow.getClass(), arrow.getVelocity().multiply(-0.5)).setDamage(arrow.getDamage() * 0.5);
                    arrow.remove();
                }
                handlePostEvent(event, defender, KalentireHandler.getPerfectBlockBonus(defender));
            } else {
                throwEntity(event.getDamager(), defender, settings.blockThrowStrength);
                defender.playHurtAnimation(0);
                handlePostEvent(event, defender, 1.0);
                final var hero = manager.getHero(defender);
                final var ticks = requiredStamina - hero.getStamina();
                hero.addEffect(new PoiseStunEffect(ticks * 50L, settings.stunCooldown, 0.0));
            }
            return true;
        }
        return false;
    }

    private int getRequiredStamina(Player defender, EntityDamageByEntityEvent event) {
        final var data = MMOPlayerData.get(defender.getUniqueId()).getStatMap();
        final var damager = event.getDamager();
        final var impact = damager instanceof LivingEntity livingEntity ? ((KalentireHandler)handler).getImpact(livingEntity) : 0.0;
        return (int) Math.floor(event.getDamage() + impact / data.getStat("STAMINA_POISE_DAMAGE"));
    }

    @Override
    public boolean handleWithoutEvent(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player defender) {
            if (defender.isBlocking() && settings.blockDamageCauses.contains(event.getCause())) {
                final int requiredStamina = getRequiredStamina(defender, event);
                if (handler.tryRemoveStamina(defender, requiredStamina)) {
                    defender.getWorld().playSound(defender, Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
                    handlePostEvent(event, defender, 1.0);
                } else {
                    defender.playSound(defender, Sound.ITEM_SHIELD_BREAK, 1.0F, 1.0F);
                    defender.setCooldown(Material.SHIELD, (int) handler.getShieldCooldown(defender));
                }
                throwEntity(event.getDamager(), defender, settings.blockThrowStrength);
                return true;
            }
        }
        return false;
    }

    private void handlePostEvent(final EntityDamageByEntityEvent event, final Player defender, final double bonusFactor) {
        var damage = event.getDamage();
        final var meta = MythicLib.plugin.getDamage().findAttack(event);
        final var damageMeta = meta.getDamage();
        final var elements = damageMeta.collectElements();
        final var statMap = MMOPlayerData.get(defender).getStatMap();
        elements.forEach((element) -> {
            final var type = DamageType.fromMythicElement(element);
            final var parent = type.getParent();
            var bonusNegation = 0.0;
            var bonusResistance = 0.0;
            if(!type.equals(parent)) {
                bonusNegation = statMap.getStat(parent.negation() + "_SHIELD");
                bonusResistance = statMap.getStat(parent.resistance() + "_SHIELD");
            }
            final var negation = statMap.getStat(type.negation() + "_SHIELD") + bonusNegation;
            if(negation != 0) {
                damageMeta.add(-bonusFactor * negation, element);
            }
            final var resistance = statMap.getStat(type.resistance() + "_SHIELD") + bonusResistance;
            if(resistance != 0) {
                damageMeta.multiplicativeModifier(1.0 - (bonusFactor * resistance), element);
            }
        });
        damageEntity(defender, damage);
    }
}
