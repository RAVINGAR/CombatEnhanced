package com.ravingarinc.combat.compatibility.kalentire.bows;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.ravingarinc.kalentirerpg.item.stats.Stat;
import com.ravingarinc.kalentirerpg.progression.event.EventBus;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.meta.CrossbowMeta;

/**
 * Assumed a player is aiming with their main hand item.
 */
public class AimingEffect extends PeriodicExpirableEffect {

    public static final String NAME = "KalentireAimingEffect";
    private final int drawStamina;
    private final int aimStamina;

    public AimingEffect(Player player, NBTItem mainHand) {
        super(null, NAME, player, 1000,
                1000L + (long) mainHand.getStat(Stat.AIM_TIME.mmoKey()) * 50L);
        drawStamina = (int) mainHand.getStat(Stat.STAMINA_COST.mmoKey()) / 5;
        aimStamina = (int) mainHand.getStat(Stat.AIM_COST.mmoKey()) / 5;

        addEffectTypes(EffectType.BENEFICIAL);
        addEffectTypes(EffectType.INTERNAL);

        // TODO if this effect is interrupted it should change the apply time which essentially resets the focus.
    }

    @Override
    public void tickMonster(Monster monster) {

    }

    @Override
    public void tickHero(Hero hero) {
        final int stamina = hero.getStamina();
        if (stamina >= drawStamina) {
            hero.setStamina(stamina - drawStamina);
            return;
        }
        hero.setStamina(0);
        hero.getPlayer().swingMainHand();
        hero.getPlayer().playSound(hero.getPlayer(), Sound.ENTITY_PLAYER_BREATH, 0.5F, 1.0F);
        hero.removeEffect(this);
    }

    @Override
    public void removeFromHero(Hero hero) {
        super.removeFromHero(hero);
        if(isExpired()) {
            hero.addEffect(new PerfectAimEffect(hero.getPlayer(), aimStamina));
        }
    }

    static {
        EventBus.subscribe(PlayerDropItemEvent.class,
                event -> onPlayerStopAiming(event.getPlayer()));
        EventBus.subscribe(PlayerSwapHandItemsEvent.class,
                event -> onPlayerStopAiming(event.getPlayer()));
        EventBus.subscribe(PlayerQuitEvent.class,
                event -> onPlayerStopAiming(event.getPlayer()));
        EventBus.subscribe(EntityDeathEvent.class,
                event -> { if(event.getEntity() instanceof Player player) onPlayerStopAiming(player); });

        EventBus.subscribe(PlayerInteractEvent.class, EventPriority.HIGHEST, event -> {
            if(event.useItemInHand() == Event.Result.DENY) {
                return;
            }
            if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                onRightClickEvent(event);
            } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                onLeftClickEvent(event);
            }
        });
    }

    /**
     * Called for right clicking and holding a bow.
     * @param event
     */
    private static void onRightClickEvent(final PlayerInteractEvent event) {
        final var player = event.getPlayer();
        final var mainHand = player.getInventory().getItemInMainHand();
        final var offhand = player.getInventory().getItemInOffHand();
        if(mainHand.getType() == Material.BOW || mainHand.getType() == Material.CROSSBOW) {
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
    private static void onLeftClickEvent(final PlayerInteractEvent event) {
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

    private static void onPlayerStopAiming(Player player) {
        final var hero = Heroes.getInstance().getCharacterManager().getHero(player);
        hero.tryRemoveEffect(AimingEffect.NAME);
        hero.tryRemoveEffect(PerfectAimEffect.NAME);
    }
}
