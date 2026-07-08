package com.ravingarinc.combat.compatibility.kalentire.deflection;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.combat.CombatManager;
import com.ravingarinc.combat.compatibility.RPGHandler;
import com.ravingarinc.combat.compatibility.kalentire.KalentireWrapper;
import com.ravingarinc.combat.file.Properties;
import com.ravingarinc.kalentirerpg.item.stats.Stat;
import com.ravingarinc.kalentirerpg.progression.event.EventBus;
import io.lumine.mythic.lib.api.event.AttackEvent;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.damage.DamageType;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;

public class DeflectionEffect extends PeriodicExpirableEffect {
    public static final String NAME = "KalentireDeflectionEffect";

    private final double deflectionRatio;
    private final double recoveryDamageMultiplier;
    private final double recoveryImpactMultiplier;
    private final long recoveryDuration;

    public DeflectionEffect(Player player, long deflectDuration, long recoveryDuration) {
        super(null, NAME, player, 250L, deflectDuration, "(Debug) Deflection has been applied", "(Debug) Deflection has ended.");
        final var properties = CombatEnhanced.getInstance().getModule(Properties.class);
        deflectionRatio = properties.damageDeflectionRatio;
        recoveryDamageMultiplier = properties.recoveryDamageMultiplier;
        recoveryImpactMultiplier = properties.recoveryImpactMultiplier;
        this.recoveryDuration = recoveryDuration;
        this.types.add(EffectType.BENEFICIAL);
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
        entity.getWorld().spawnParticle(Particle.CRIT_MAGIC, entity.getEyeLocation(), 2, 0.5, 0.5, 0.5);
        if(entity.isDead() || entity.getHealth() <= 0.0) {
            character.removeEffect(this);
        }
    }

    @Override
    public void remove(CharacterTemplate character) {
        super.remove(character);
        character.addEffect(new RecoveryEffect(recoveryDamageMultiplier, recoveryImpactMultiplier, recoveryDuration));
    }

    static {
        EventBus.subscribe(PlayerArmSwingEvent.class, EventPriority.NORMAL, (event) -> {
            final var combatManager = CombatEnhanced.getInstance().getModule(CombatManager.class);
            final var uuid = event.getPlayer().getUniqueId();
            if(combatManager.justDodged(uuid) || combatManager.justBlocked(uuid)) {
                return;
            }
            if(combatManager.isBlocking(uuid) || combatManager.isDodging(uuid)) {
                return;
            }
            final var hero = Heroes.getInstance().getCharacterManager().getHero(event.getPlayer());
            if(hero.hasEffect(DeflectionEffect.NAME) || hero.hasEffect(RecoveryEffect.NAME)) {
                return;
            }
            final var deflectTicks = (long) MMOPlayerData.get(uuid).getStatMap().getStat(Stat.DEFLECT_TICKS.mmoKey());
            final var recoveryTicks = (long) MMOPlayerData.get(uuid).getStatMap().getStat(Stat.RECOVERY_TICKS.mmoKey());
            if(deflectTicks > 0.0) {
                hero.addEffect(new DeflectionEffect(hero.getPlayer(), deflectTicks * 50L, recoveryTicks * 50L));
            }
        });

        /* Handler for receiving damage whilst entity has the deflection effect */
        EventBus.subscribe(AttackEvent.class, EventPriority.HIGHEST, (event) -> {
            final var metadata = event.getAttack();
            if(metadata.getDamage().isWeaponCriticalStrike()) return; // Critical hits cannot be deflected
            final var attackerStats = metadata.getAttacker();
            if(attackerStats == null) return;

            final var character = Heroes.getInstance().getCharacterManager().getCharacter(event.getEntity());
            final var effect = character.getEffect(NAME);
            if(effect == null) return;
            if(!(effect instanceof DeflectionEffect dEffect)) return;

            final var packet = metadata.getDamage().getInitialPacket();
            if(packet == null) return;
            if(!packet.hasType(DamageType.WEAPON)) return;
            final var damage = packet.getFinalValue();

            packet.multiplicativeModifier(1.0 - dEffect.deflectionRatio);
            final var deflectedDamage = damage * dEffect.deflectionRatio;
            if(deflectedDamage > 0.0) {
                final var rpg = ((KalentireWrapper)CombatEnhanced.getInstance().getModule(RPGHandler.class).getWrapper());
                rpg.addPoiseDamage(attackerStats.getEntity(), deflectedDamage);
            }
        });
    }
}
