package com.ravingarinc.combat.compatibility.kalentire;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.ravingarinc.api.module.RavinPlugin;
import com.ravingarinc.combat.character.CharacterManager;
import com.ravingarinc.combat.combat.runner.BlockRunner;
import com.ravingarinc.combat.combat.runner.PoiseRunner;
import com.ravingarinc.combat.compatibility.RPGWrapper;
import com.ravingarinc.combat.compatibility.kalentire.effect.PoiseStunEffect;
import com.ravingarinc.combat.file.Properties;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class KalentireWrapper implements RPGWrapper, Listener {
    private final RavinPlugin plugin;

    private final Properties settings;

    private PoiseRunner poiseRunner;

    private CharacterManager characterManager;

    private BowRunner bowRunner = null;

    // This should mirror Kalentire API
    public static final String DODGE_TICKS = "DODGE_TICKS";

    public static final String POISE = "POISE";

    public static final String IMPACT = "IMPACT";

    public static final String STAMINA_DRAIN = "HEROES_STAMINA_DRAIN";

    public static final String BLOCK_RECOVERY = "BLOCK_RECOVERY";

    public static final String PERFECT_BLOCK_BONUS = "PERFECT_BLOCK_BONUS";

    public static final String BLOCK_BUFFER = "BLOCK_BUFFER";

    public static final String FALL_OFF_REDUCTION = "FALL_OFF_REDUCTION";
    public static final String FALL_OFF_RANGE = "FALL_OFF_RANGE";

    public static final String DRAW_SPEED = "DRAW_SPEED";

    public static final String FULL_DRAW_BONUS = "FULL_DRAW_BONUS";

    public static final String VELOCITY = "VELOCITY";

    public static final String RANGED_DAMAGE = "RANGED_MULTIPLIER";

    public static final String KNOCKBACK = "HEROES_KNOCKBACK";

    public KalentireWrapper(final RavinPlugin plugin) {
        this.plugin = plugin;
        this.settings = plugin.getModule(Properties.class);
        this.characterManager = plugin.getModule(com.ravingarinc.combat.character.CharacterManager.class);
        // TODO Dodge Values should be based on player movement
    }

    @Override
    public void load() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        PoiseStunEffect.register(plugin);

        bowRunner = new BowRunner(this);
        bowRunner.runTaskAsynchronously(plugin);
        poiseRunner = new PoiseRunner(plugin, this);
        poiseRunner.runTaskTimerAsynchronously(plugin, 0L, 5L);
    }

    @Override
    public void cancel() {
        HandlerList.unregisterAll(this);
        PoiseStunEffect.unregister();

        bowRunner.cancel();
        poiseRunner.cancel();
    }

    @Override
    public boolean tryRemoveStamina(final Player player, final int amount) {
        final Hero hero = Heroes.getInstance().getCharacterManager().getHero(player);
        final int stamina = hero.getStamina();
        if (stamina >= amount) {
            hero.setStamina(stamina - amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean tryRemoveMana(final Player player, final int amount) {
        final Hero hero = Heroes.getInstance().getCharacterManager().getHero(player);
        final int mana = hero.getMana();
        if (mana >= amount) {
            hero.setMana(mana - amount);
            return true;
        }
        return false;
    }

    @Override
    public float getDodgeStrength(final Player player) {
        //CombatEnhanced.log(Level.WARNING, "Debug -> Player's Speed is " + speed);
        return settings.dodgeStrength + (Heroes.getInstance().getCharacterManager().getHero(player).getAttributeValue(AttributeType.DEXTERITY) * (settings.dodgeStrength / 20F));
    }

    @Override
    public long getDodgeDuration(Player player) {
        return settings.dodgeDuration + (long) Math.floor(MMOPlayerData.get(player).getStatMap().getStat(DODGE_TICKS) * 50.0);
    }

    @Override
    public int getDodgeCost(Player player) {
        return this.getProperties().dodgeStaminaCost;
    }
    // We dont need to consider the drain here, since heroes should do it automagically!

    @Override
    public long getShieldCooldown(final Player player) {
        final var data = MMOPlayerData.get(player);
        return (int) data.getStatMap().getStat(BLOCK_RECOVERY) / 50L;
    }

    public static double getPerfectBlockBonus(final Player player) {
        return MMOPlayerData.get(player).getStatMap().getStat(PERFECT_BLOCK_BONUS);
    }

    public void addPoiseDamage(final LivingEntity target, final double poiseDamage) {
        characterManager.getCharacter(target).ifPresent(character -> {
            this.poiseRunner.addPoiseDamage(character, poiseDamage);
        });
    }

    @Override
    public Properties getProperties() {
        return settings;
    }

    public double getPoise(LivingEntity character) {
        if(character instanceof Player player) {
            return getPoiseForPlayer(player);
        } else if(character instanceof Monster monster) {
            return getPoiseForMonster(monster);
        }
        return settings.mobDefaultPoise;
    }

    private double getPoiseForPlayer(Player player) {
        final var data = MMOPlayerData.getOrNull(player.getUniqueId());
        if(data == null) return 0.0;
        final var stats = data.getStatMap();
        var basePoise = stats.getStat(POISE);
        if(player.isBlocking()) {
            basePoise += stats.getStat(BLOCK_BUFFER);
        }
        return basePoise;
    }

    private double getPoiseForMonster(Monster monster) {
        final var mob = MythicBukkit.inst().getMobManager().getActiveMob(monster.getUniqueId());
        if(mob.isEmpty()) return settings.mobDefaultPoise;
        //final var activeMob = mob.get();
        // todo figure this out
        return settings.mobDefaultPoise;
    }

    public static double getImpact(LivingEntity character) {
        if(character instanceof Player player) {
            return getImpactForPlayer(player);
        } else if(character instanceof Monster monster) {
            return getImpactForMonster(monster);
        }
        return 0.0;
    }

    private static double getImpactForPlayer(Player player) {
        return MMOPlayerData.get(player.getUniqueId()).getStatMap().getStat(KalentireWrapper.IMPACT);
    }

    private static double getImpactForMonster(Monster monster) {
        // todo deez nuts

        return 0.0;
    }


    /**
     * Handles event before armour mitigiation is considered.
     * @param event
     */
    @Override
    public void onDamageEvent(final EntityDamageEvent event) {


        double damage = event.getDamage();
        double impact = 0.0;
        Entity source = null;
        if(event instanceof EntityDamageByEntityEvent castEvent) {
            if(bowRunner.handle(castEvent)) {
                // This returns true and therefore does not consider poise calculations as the bow runner should
                // handle it itself.
                return;
            }
            if(castEvent.getDamager() instanceof LivingEntity entity) {
                impact = KalentireWrapper.getImpact(entity);
                source = entity;
            }
            damage = MythicLib.plugin.getDamage().findAttack(event).getDamage().getDamage();
        }
        if(damage > 0.0) {
            poiseRunner.handle(event, damage, source, impact);
        }
    }

    public PoiseRunner getPoiseRunner() {
        return poiseRunner;
    }

    @EventHandler
    public void onDeathEvent(final EntityDeathEvent event) {
        poiseRunner.removeAll(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuitEvent(final PlayerQuitEvent event) {
        poiseRunner.removeAll(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onArrowShoot(final EntityShootBowEvent event) {
        final var consumed = event.getConsumable();
        if(event.getEntity() instanceof Player player && consumed != null) {
            final var category = ShootEvent.Category.matchCategory(event.getBow());
            if(category == null) return;

            final var statMap = MMOPlayerData.get(player).getStatMap().cache(EquipmentSlot.MAIN_HAND); // todo do we cache
            // here?
            final var consumable = NBTItem.get(consumed);
            if(!consumable.hasType()) return;

            bowRunner.add(player.getUniqueId(), player.getLocation(), statMap,
                    consumable, event.getForce());
            final var velocity =
                    (statMap.getStat(KalentireWrapper.VELOCITY) + consumable.getDouble(KalentireWrapper.VELOCITY));
            final var proj = event.getProjectile();
            proj.setVelocity(proj.getVelocity().multiply(velocity));
        }
    }

    @Override
    public BlockRunner getBlockRunner() {
        return new KalentireBlockRunner(this);
    }
}
