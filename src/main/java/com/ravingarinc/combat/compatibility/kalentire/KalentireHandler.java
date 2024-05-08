package com.ravingarinc.combat.compatibility.kalentire;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroesDamageEvent;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.Hero;
import com.ravingarinc.api.I;
import com.ravingarinc.api.Sync;
import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.character.CharacterEntity;
import com.ravingarinc.combat.character.CharacterMonster;
import com.ravingarinc.combat.character.CharacterPlayer;
import com.ravingarinc.combat.combat.CombatManager;
import com.ravingarinc.combat.combat.runner.PoiseRunner;
import com.ravingarinc.combat.compatibility.RPGHandler;
import com.ravingarinc.combat.compatibility.kalentire.effect.PoiseBreakEffect;
import com.ravingarinc.combat.file.Settings;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Level;

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

    public static final String BLOCK_COOLDOWN = "BLOCK_COOLDOWN";




    public KalentireHandler(final CombatEnhanced plugin) {
        this.plugin = plugin;
        this.settings = plugin.getModule(CombatManager.class).getSettings();
        // TODO Dodge Values should be based on player movement
    }

    @Override
    public void load() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        PoiseBreakEffect.register(plugin);

        poiseRunner = new PoiseRunner(plugin, this);
        poiseRunner.runTaskTimerAsynchronously(plugin, 0L, 5L);
    }

    @Override
    public void cancel() {
        HandlerList.unregisterAll(this);
        PoiseBreakEffect.unregister();

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
        I.log(Level.WARNING, "Debug -> Player's Speed is " + speed);
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
        return (int) data.getStatMap().getStat(BLOCK_COOLDOWN) / 50L;
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    @Sync.AsyncOnly
    public double getPoise(CharacterEntity<?> character) {
        if(character instanceof CharacterPlayer player) {
            return settings.poiseBaseThreshold + getPoiseForPlayer(player);
        } else if(character instanceof CharacterMonster monster) {
            return settings.poiseBaseThreshold + getPoiseForMonster(monster);
        }
        return settings.poiseBaseThreshold;
    }

    private double getPoiseForPlayer(CharacterPlayer player) {
        final var data = MMOPlayerData.getOrNull(player.getUniqueId());
        if(data == null) return 0.0;
        return data.getStatMap().getStat(POISE);
    }

    private double getPoiseForMonster(CharacterMonster monster) {
        // TODO these nuts
        //   check if mythic mob and get poise from them
        return 0.0;
    }


    @EventHandler
    public void onHeroesDamageEvent(final HeroesDamageEvent event) {
        if(event.getDamage() == 0) {
            return;
        }
        poiseRunner.handle(event);
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
