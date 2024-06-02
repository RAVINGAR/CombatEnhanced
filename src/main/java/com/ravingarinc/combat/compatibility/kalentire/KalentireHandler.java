package com.ravingarinc.combat.compatibility.kalentire;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroesDamageEvent;
import com.herocraftonline.heroes.api.events.ProjectileDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.Hero;
import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.combat.CombatManager;
import com.ravingarinc.combat.combat.runner.PoiseRunner;
import com.ravingarinc.combat.compatibility.RPGHandler;
import com.ravingarinc.combat.compatibility.kalentire.effect.PoiseStunEffect;
import com.ravingarinc.combat.file.Settings;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class KalentireHandler implements RPGHandler, Listener {
    private final CombatEnhanced plugin;
    private CharacterManager manager = null;

    private final Settings settings;

    private PoiseRunner poiseRunner;

    // This should mirror Kalentire API
    public static final String DODGE_TICKS = "DODGE_TICKS";

    public static final String POISE = "POISE";

    public static final String IMPACT = "IMPACT";

    public static final String STAMINA_DRAIN = "HEROES_STAMINA_DRAIN";

    public static final String BLOCK_RECOVERY = "BLOCK_RECOVERY";

    public static final String PERFECT_BLOCK_BONUS = "PERFECT_BLOCK_BONUS";




    public KalentireHandler(final CombatEnhanced plugin) {
        this.plugin = plugin;
        this.settings = plugin.getModule(CombatManager.class).getSettings();
        // TODO Dodge Values should be based on player movement
    }

    @Override
    public void load() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        PoiseStunEffect.register(plugin);

        poiseRunner = new PoiseRunner(plugin, this);
        poiseRunner.runTaskTimerAsynchronously(plugin, 0L, 5L);
    }

    @Override
    public void cancel() {
        HandlerList.unregisterAll(this);
        PoiseStunEffect.unregister();

        poiseRunner.cancel();
    }

    public CharacterManager getManager() {
        if(manager == null) {
            manager = Heroes.getInstance().getCharacterManager();
        }
        return manager;
    }

    @Override
    public boolean tryRemoveStamina(final Player player, final int amount) {
        final Hero hero = getManager().getHero(player);
        final int stamina = hero.getStamina();
        if (stamina >= amount) {
            hero.setStamina(stamina - amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean tryRemoveMana(final Player player, final int amount) {
        final Hero hero = getManager().getHero(player);
        final int mana = hero.getMana();
        if (mana >= amount) {
            hero.setMana(mana - amount);
            return true;
        }
        return false;
    }

    @Override
    public float getDodgeStrength(final Player player) {
        final var speed = (float)player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
        //CombatEnhanced.log(Level.WARNING, "Debug -> Player's Speed is " + speed);
        return Math.min(1.0F, (0.9F - (0.13F - speed))); // TODO Figure out this formula
    }

    @Override
    public int getDodgeCost(Player player) {
        final var data = MMOPlayerData.get(player);
        return (int) Math.floor(getSettings().dodgeStaminaCost * (1.0 + data.getStatMap().getStat(STAMINA_DRAIN)));
    }

    @Override
    public long getShieldCooldown(final Player player) {
        final var data = MMOPlayerData.get(player);
        return (int) data.getStatMap().getStat(BLOCK_RECOVERY) / 50L;
    }

    public static double getPerfectBlockBonus(final Player player) {
        return MMOPlayerData.get(player).getStatMap().getStat(PERFECT_BLOCK_BONUS);
    }

    @Override
    public Settings getSettings() {
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
        return data.getStatMap().getStat(POISE);
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
        return MMOPlayerData.get(player.getUniqueId()).getStatMap().getStat(KalentireHandler.IMPACT);
    }

    private static double getImpactForMonster(Monster monster) {
        // todo deez nuts

        return 0.0;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onHeroesDamageEvent(final HeroesDamageEvent event) {
        double damage = event.getDamage();
        if(event instanceof WeaponDamageEvent || event instanceof ProjectileDamageEvent) {
            final var metadata = MythicLib.plugin.getDamage().findAttack(event.getOriginalEvent());
            damage = metadata.getDamage().getDamage();
        }
        poiseRunner.handle(event, damage);
    }

    @EventHandler
    public void onDeathEvent(final EntityDeathEvent event) {
        poiseRunner.removeAll(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuitEvent(final PlayerQuitEvent event) {
        poiseRunner.removeAll(event.getPlayer().getUniqueId());
    }

}
