package com.ravingarinc.combat.compatibility.kalentire;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.ravingarinc.api.module.RavinPlugin;
import com.ravingarinc.combat.character.CharacterManager;
import com.ravingarinc.combat.combat.runner.BlockRunner;
import com.ravingarinc.combat.combat.runner.PoiseRunner;
import com.ravingarinc.combat.compatibility.RPGWrapper;
import com.ravingarinc.combat.compatibility.kalentire.bows.BowRunner;
import com.ravingarinc.combat.compatibility.kalentire.effect.PoiseStunEffect;
import com.ravingarinc.combat.file.Properties;
import com.ravingarinc.kalentirerpg.item.stats.Stat;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.player.PlayerMetadata;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Random;

public class KalentireWrapper implements RPGWrapper, Listener {
    private final RavinPlugin plugin;

    private final Properties settings;

    private PoiseRunner poiseRunner;

    private CharacterManager characterManager;

    private BowRunner bowRunner = null;


    public static final double FALL_OFF_REDUCTION = 0.05; // % damage reduction per block out of range.

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
        return settings.dodgeDuration + (long) Math.floor(MMOPlayerData.get(player).getStatMap().getStat(Stat.DODGE_TICKS.mmoKey()) * 50.0);
    }

    @Override
    public int getDodgeCost(Player player) {
        return this.getProperties().dodgeStaminaCost;
    }
    // We dont need to consider the drain here, since heroes should do it automagically!

    @Override
    public long getShieldCooldown(final Player player) {
        final var data = MMOPlayerData.get(player);
        return (int) data.getStatMap().getStat(Stat.BLOCK_RECOVERY.mmoKey()) / 50L;
    }

    public static double getPerfectBlockBonus(final Player player) {
        return MMOPlayerData.get(player).getStatMap().getStat(Stat.PERFECT_BLOCK_BONUS.mmoKey());
    }

    // Same as dealing 'impact damage'
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
        var basePoise = stats.getStat(Stat.POISE.mmoKey());
        if(player.isBlocking()) {
            basePoise += stats.getStat(Stat.POISE_BUFFER.mmoKey());
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
        return MMOPlayerData.get(player.getUniqueId()).getStatMap().getStat(Stat.IMPACT.mmoKey());
    }

    private static double getImpactForMonster(Monster monster) {
        // todo deez nuts

        return 0.0;
    }


    private static double getStat(PlayerMetadata stats, NBTItem consumable, String stat) {
        return stats.getStat(stat) + consumable.getStat(stat);
    }

    public PoiseRunner getPoiseRunner() {
        return poiseRunner;
    }

    public BowRunner getBowRunner() {
        return bowRunner;
    }

    @Override
    public BlockRunner getBlockRunner() {
        return new KalentireBlockRunner(this);
    }
}
