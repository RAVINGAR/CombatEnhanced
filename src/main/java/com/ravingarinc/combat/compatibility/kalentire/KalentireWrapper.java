package com.ravingarinc.combat.compatibility.kalentire;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.ravingarinc.api.module.RavinPlugin;
import com.ravingarinc.combat.character.CharacterManager;
import com.ravingarinc.combat.combat.runner.BlockRunner;
import com.ravingarinc.combat.combat.runner.PoiseRunner;
import com.ravingarinc.combat.compatibility.RPGWrapper;
import com.ravingarinc.combat.compatibility.kalentire.bows.AimingEffect;
import com.ravingarinc.combat.compatibility.kalentire.bows.BowRunner;
import com.ravingarinc.combat.compatibility.kalentire.bows.PerfectAimEffect;
import com.ravingarinc.combat.compatibility.kalentire.bows.ShootEvent;
import com.ravingarinc.combat.compatibility.kalentire.effect.PoiseStunEffect;
import com.ravingarinc.combat.file.Properties;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.player.PlayerMetadata;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.util.Vector;

import java.util.Random;

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

    public static final String PENETRATION = "KALENTIRE_PENETRATION";

    public static final String PERCENT_PENETRATION = "KALENTIRE_PERCENTILE_PENETRATION";

    public static final String STAMINA_DRAIN = "HEROES_STAMINA_DRAIN";

    public static final String BLOCK_RECOVERY = "BLOCK_RECOVERY";

    public static final String PERFECT_BLOCK_BONUS = "PERFECT_BLOCK_BONUS";

    public static final String BLOCK_BUFFER = "BLOCK_BUFFER";

    public static final String RANGED_DAMAGE = "RANGED_DAMAGE";

    public static final String FALL_OFF_RANGE = "FALL_OFF_RANGE";

    public static final double FALL_OFF_REDUCTION = 0.05; // % damage reduction per block out of range.

    public static final String DRAW_STAMINA = "DRAW_STAMINA"; // per second, exeute divided by 5

    public static final String AIM_STAMINA = "AIM_STAMINA";

    public static final String AIM_BONUS = "AIM_BONUS";

    public static final String AIM_TIME = "AIM_TIME";

    public static final String VELOCITY = "VELOCITY";

    public static final String SWAY = "SWAY";

    public static final String KNOCKBACK = "HEROES_KNOCKBACK";

    private final Random random = new Random(System.currentTimeMillis());

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
        if(player.getAttackCooldown() != 1.0) {
            return 0.0; // todo test if this is right
        }
        return MMOPlayerData.get(player.getUniqueId()).getStatMap().getStat(KalentireWrapper.IMPACT);
    }

    private static double getImpactForMonster(Monster monster) {
        // todo deez nuts

        return 0.0;
    }


    private static double getStat(PlayerMetadata stats, NBTItem consumable, String stat) {
        return stats.getStat(stat) + consumable.getStat(stat);
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
        final var uuid = event.getEntity().getUniqueId();
        poiseRunner.removeAll(uuid);
        if(event.getEntity() instanceof Player player) {
            onPlayerStopAiming(player);
        }

    }

    @EventHandler
    public void onQuitEvent(final PlayerQuitEvent event) {
        final var uuid = event.getPlayer().getUniqueId();
        poiseRunner.removeAll(uuid);
        onPlayerStopAiming(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEvent(final PlayerInteractEvent event) {
        if(event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            onRightClickEvent(event);
        } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            onLeftClickEvent(event);
        }
    }

    /**
     * Called for right clicking and holding a bow.
     * @param event
     */
    private void onRightClickEvent(final PlayerInteractEvent event) {
        final var player = event.getPlayer();
        final var mainHand = player.getInventory().getItemInMainHand();
        final var offhand = player.getInventory().getItemInOffHand();
        if(mainHand.getType() == Material.BOW) {
            if(!offhand.getType().isEmpty()) {
                player.sendMessage(Component.text("You are unable to use this item with only one-hand!", NamedTextColor.RED));
                event.setUseItemInHand(Event.Result.DENY);
                return;
            }
            final var effect = new AimingEffect(player, NBTItem.get(mainHand));
            Heroes.getInstance().getCharacterManager().getHero(player).addEffect(effect);
        }

        if(offhand.getType() == Material.BOW || offhand.getType() == Material.CROSSBOW) {
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    /**
     * Called for left clicking a crossbow to toggle the aiming effect.
     * @param event
     */
    private void onLeftClickEvent(final PlayerInteractEvent event) {
        final var player = event.getPlayer();
        final var mainHand = player.getInventory().getItemInMainHand();
        final var offhand = player.getInventory().getItemInOffHand();

        if(mainHand.getItemMeta() instanceof CrossbowMeta meta && meta.hasChargedProjectiles()) {
            if(!offhand.getType().isEmpty()) {
                player.sendMessage(Component.text("You are unable to use this item with only one-hand!", NamedTextColor.RED));
                event.setUseItemInHand(Event.Result.DENY);
                return;
            }
            final var hero = Heroes.getInstance().getCharacterManager().getHero(player);
            if(hero.hasEffect(AimingEffect.NAME)) {
                hero.tryRemoveEffect(AimingEffect.NAME);
                player.sendMessage(Component.text("< You are no longer readying your crossbow! >", NamedTextColor.RED));
            } else if(hero.hasEffect(PerfectAimEffect.NAME)) {
                hero.tryRemoveEffect(PerfectAimEffect.NAME);
                player.sendMessage(Component.text("< You are no longer readying your crossbow! >", NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("< You are readying your crossbow! >", NamedTextColor.RED));
                player.playSound(player, Sound.ITEM_CROSSBOW_LOADING_START, SoundCategory.PLAYERS, 0.5F, 1.0F);
                final var effect = new AimingEffect(player, NBTItem.get(mainHand));
                hero.addEffect(effect);
            }
        }
    }

    public void onPlayerStopAiming(Player player) {
        final var hero = Heroes.getInstance().getCharacterManager().getHero(player);
        hero.tryRemoveEffect("KalentireAimingEffect");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItemEvent(final PlayerDropItemEvent event) {
        onPlayerStopAiming(event.getPlayer());
    }

    @EventHandler
    public void onPlayerSwapHandsEvent(final PlayerSwapHandItemsEvent event) {
        onPlayerStopAiming(event.getPlayer());
    }

    @EventHandler
    public void onArrowShoot(final EntityShootBowEvent event) {
        final var consumed = event.getConsumable();
        if(event.getEntity() instanceof Player player && consumed != null) {
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
            double sway = getStat(statMap, consumable, KalentireWrapper.SWAY);
            if(effect != null) {
                multiplier += getStat(statMap, consumable, KalentireWrapper.AIM_BONUS);
                if (bowType.equalsIgnoreCase("GREATBOW") || bowType.equalsIgnoreCase("SIEGE_CROSSBOW")) {
                    sway = 0.0;
                } else {
                    sway /= 2.0;
                }
            }
            hero.tryRemoveEffect(AimingEffect.NAME);
            hero.tryRemoveEffect(PerfectAimEffect.NAME);

            bowRunner.add(player.getUniqueId(), player.getLocation(), statMap,
                    consumable, multiplier);

            final var proj = event.getProjectile();
            final var velocity = getStat(statMap, consumable, KalentireWrapper.VELOCITY) * multiplier;

            proj.setVelocity(randomiseVelocity(proj.getVelocity(), sway, velocity));
        }
    }

        /**
         * Randomizes a velocity vector to create a "spread" effect.
         *
         * @param initialVelocity The starting velocity vector of the projectile.
         * @param spreadFactor A value controlling the spread cone. 0 is no spread.
         * Good values are typically between 0.05 and 0.5.
         * @return A new velocity vector with a randomized direction.
         */
    public Vector randomiseVelocity(Vector initialVelocity, double spreadFactor, double speed) {
        // 1. Get the original speed and direction
        Vector direction = initialVelocity.normalize();

        // 2. & 3. Generate a random offset vector scaled by the spread factor
        // Generates random values between -1 and 1
        double offsetX = (random.nextDouble() * 2 - 1) * spreadFactor;
        double offsetY = (random.nextDouble() * 2 - 1) * spreadFactor;
        double offsetZ = (random.nextDouble() * 2 - 1) * spreadFactor;
        Vector randomOffset = new Vector(offsetX, offsetY, offsetZ);

        // 4. Combine the original direction with the random offset and re-normalize
        Vector newDirection = direction.add(randomOffset).normalize();

        // 5. Apply the original speed to the new direction
        return newDirection.multiply(speed);
    }

    @Override
    public BlockRunner getBlockRunner() {
        return new KalentireBlockRunner(this);
    }
}
