package com.ravingarinc.combat.combat.runner;

import com.ravingarinc.combat.combat.event.DodgeEvent;
import com.ravingarinc.combat.file.Settings;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.damage.DamageType;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DodgeRunner extends IdentifierRunner<DodgeEvent, EntityDamageByEntityEvent> {



    public DodgeRunner(final Settings settings) {
        super(settings);
    }

    @Override
    public boolean handleWithEvent(final DodgeEvent dodgeEvent, final EntityDamageByEntityEvent event) {
        // We can assume that event.getEntity is a LivingEntity since the UUID must belong to one
        final LivingEntity entity = (LivingEntity) event.getEntity();
        if (settings.dodgeDamageCauses.contains(event.getCause())) {
            if (entity instanceof Player player) {
                entity.sendMessage(ChatColor.RED + "< You dodged the attack! >");
                player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F, 1.0F);
            }
            final var meta = MythicLib.inst().getDamage().findAttack(event).getDamage();
            final var mitigation = 1.0 - settings.dodgeMitigation;
            meta.multiplicativeModifier(mitigation, DamageType.PHYSICAL);
            meta.multiplicativeModifier(mitigation, DamageType.MAGIC);
            meta.multiplicativeModifier(mitigation, DamageType.PROJECTILE);
            meta.multiplicativeModifier(mitigation, DamageType.SKILL);
            meta.multiplicativeModifier(mitigation, DamageType.WEAPON);
            event.setDamage(event.getDamage() * (mitigation));
            return true;
        }
        return false;
    }

    @Override
    public boolean handleWithoutEvent(final EntityDamageByEntityEvent bukkitEvent) {
        return false;
    }
}
