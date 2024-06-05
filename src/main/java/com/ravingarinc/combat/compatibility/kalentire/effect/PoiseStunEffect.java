package com.ravingarinc.combat.compatibility.kalentire.effect;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.google.common.util.concurrent.AtomicDouble;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroesDamageEvent;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.file.Settings;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PoiseStunEffect extends PeriodicExpirableEffect {
    public static final String EFFECT_NAME = "PoiseStunEffect";

    public static Listener LISTENER = null;

    public static void register(CombatEnhanced plugin) {
        LISTENER = new Listener(plugin.getRPGHandler().getSettings());
        plugin.getServer().getPluginManager().registerEvents(LISTENER, plugin);
    }

    public static void unregister() {
        HandlerList.unregisterAll(LISTENER);
        LISTENER = null;
    }
    private final double vulnerability;
    private final long cooldown;
    private final Entity applierEntity;
    private final AtomicDouble bonusVulnerability = new AtomicDouble(0);
    public PoiseStunEffect(Entity applierEntity, long duration, long cooldown, double vulnerability) {
        super(null, EFFECT_NAME, null, 500L, duration, null, null);
        this.vulnerability = vulnerability;
        this.cooldown = cooldown;
        this.applierEntity = applierEntity;
        this.types.add(EffectType.STUN);
        this.types.add(EffectType.HARMFUL);
        this.types.add(EffectType.PHYSICAL);
        this.types.add(EffectType.DISABLE);
        this.types.add(EffectType.STAMINA_REGEN_FREEZING);
        this.types.add(EffectType.MANA_REGEN_FREEZING);
        this.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int)(20L * duration / 1000L), 127));
        //this.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, (int)(20L * duration / 1000L), 127));
    }

    @Override
    public void apply(CharacterTemplate character) {
        super.apply(character);
        LivingEntity entity = character.getEntity();
        World world = entity.getWorld();
        Location location = entity.getEyeLocation();
        world.playSound(entity, Sound.ITEM_SHIELD_BREAK, SoundCategory.HOSTILE, 1.0F, 1.0F);
        world.spawnParticle(Particle.CRIT, location, 20, 1, 1, 1);
        world.spawnParticle(Particle.VILLAGER_ANGRY, location.add(0.0, 0.2, 0.0), 1);
        if(applierEntity instanceof Player player) {
            player.playSound(entity, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F, 1.0F);
        }
    }

    @Override
    public void applyToHero(Hero hero) {
        super.applyToHero(hero);
        hero.getPlayer().sendMessage(ChatColor.RED + "< You have suffered a poise break! >");
        hero.getPlayer().setCooldown(Material.SHIELD, (int) (getDuration() / 50L));
    }

    @Override
    public void tick(CharacterTemplate character) {
        super.tick(character);
        LivingEntity entity = character.getEntity();
        entity.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, entity.getEyeLocation().add(0, 0.2, 0), 1);
        if(entity.isDead() || entity.getHealth() <= 0.0) {
            character.removeEffect(this);
        }
    }

    @Override
    public void tickHero(Hero hero) {

    }

    @Override
    public void tickMonster(Monster monster) {

    }

    @Override
    public void remove(CharacterTemplate character) {
        super.remove(character);
        character.addEffect(new PoiseImmunityEffect(cooldown));
    }

    public static class Listener implements org.bukkit.event.Listener {
        private final CharacterManager characterManager = Heroes.getInstance().getCharacterManager();
        private final Settings settings;
        private Listener(Settings settings) {
            this.settings = settings;
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onDamageEvent(HeroesDamageEvent event) {
            final Effect effect = event.getDefender().getEffect(EFFECT_NAME);
            if(effect == null) return;
            if(effect instanceof PoiseStunEffect poiseEffect) {
                final double rawDamage = event.getDamage();
                event.setDamage(rawDamage * (1.0 + Math.min(1.0, poiseEffect.vulnerability + (poiseEffect.bonusVulnerability.addAndGet(event.getDamage() * settings.bonusVulnerability)))));
            }
        }

        @EventHandler
        public void onJumpEvent(PlayerJumpEvent event) {
            //CombatEnhanced.log(Level.WARNING, "Debug -> Player Velocity " + event.getPlayer().getVelocity().toString());
            if(characterManager.getCharacter(event.getPlayer()).hasEffect(EFFECT_NAME)) {
                event.setCancelled(true);
            }
        }
    }
}
