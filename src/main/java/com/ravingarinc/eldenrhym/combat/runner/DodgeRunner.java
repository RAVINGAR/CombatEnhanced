package com.ravingarinc.eldenrhym.combat.runner;

import com.ravingarinc.eldenrhym.combat.event.DodgeEvent;
import com.ravingarinc.eldenrhym.file.Settings;
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
            entity.sendMessage(ChatColor.RED + "You dodged the attack!");
            if (entity instanceof Player player) {
                player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F, 1.0F);
            }
            event.setDamage(event.getDamage() * (1.0 - settings.dodgeMitigation));
            return true;
        }
        return false;
    }

    @Override
    public boolean handleWithoutEvent(final EntityDamageByEntityEvent bukkitEvent) {
        return false;
    }
}
