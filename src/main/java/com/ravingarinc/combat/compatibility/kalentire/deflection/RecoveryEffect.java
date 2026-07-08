package com.ravingarinc.combat.compatibility.kalentire.deflection;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.compatibility.RPGHandler;
import com.ravingarinc.combat.compatibility.kalentire.KalentireWrapper;
import com.ravingarinc.kalentirerpg.item.stats.Stat;
import com.ravingarinc.kalentirerpg.progression.event.EventBus;
import io.lumine.mythic.lib.api.event.AttackEvent;
import io.lumine.mythic.lib.damage.DamageType;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;

public class RecoveryEffect extends PeriodicExpirableEffect {
    public static final String NAME = "KalentireRecoveryEffect";

    private final double recoveryDamageMultiplier;
    private final double recoveryImpactMultiplier;

    public RecoveryEffect(double recoveryDamageMultiplier, double recoveryImpactMultiplier, long recoveryDuration) {
        super(null, NAME, null, 250L, recoveryDuration);
        this.recoveryDamageMultiplier = recoveryDamageMultiplier;
        this.recoveryImpactMultiplier = recoveryImpactMultiplier;
    }

    @Override
    public void tickMonster(Monster monster) {

    }

    @Override
    public void tickHero(Hero hero) {

    }

    @Override
    public void tick(CharacterTemplate character) {
        super.tick(character);
        LivingEntity entity = character.getEntity();
        entity.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, entity.getEyeLocation(), 2, 0.5, 0.5, 0.5);
        if(entity.isDead() || entity.getHealth() <= 0.0) {
            character.removeEffect(this);
        }
    }

    static {
        /* Handler for receiving damage whilst entity has the recovery effect */
        EventBus.subscribe(AttackEvent.class, EventPriority.HIGHEST, (event) -> {
            if(event.isCancelled()) return;
            final var metadata = event.getAttack();
            final var attackerStats = metadata.getAttacker();
            if(attackerStats == null) return;

            final var character = Heroes.getInstance().getCharacterManager().getCharacter(event.getEntity());
            final var effect = character.getEffect(NAME);
            if(effect == null) return;
            if(!(effect instanceof RecoveryEffect rEffect)) return;

            final var packet = metadata.getDamage().getInitialPacket();
            if(packet == null) return;
            if(!packet.hasType(DamageType.WEAPON)) return;
            packet.multiplicativeModifier(rEffect.recoveryDamageMultiplier);

            final var bonusImpact = attackerStats.getStat(Stat.IMPACT.mmoKey()) * rEffect.recoveryImpactMultiplier;
            if(bonusImpact > 0) {
                final var rpg = ((KalentireWrapper) CombatEnhanced.getInstance().getModule(RPGHandler.class).getWrapper());
                rpg.addPoiseDamage(attackerStats.getEntity(), bonusImpact);
            }
        });
    }
}
