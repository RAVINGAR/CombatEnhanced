package com.ravingarinc.combat.compatibility.kalentire;

import com.google.common.util.concurrent.AtomicDouble;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.equipment.EquipmentType;
import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.combat.CombatManager;
import com.ravingarinc.combat.compatibility.RPGHandler;
import com.ravingarinc.combat.file.Settings;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class KalentireHandler implements RPGHandler {
    private final CombatEnhanced plugin;
    private CharacterManager manager = null;

    private final Map<Player, EquipmentStats> stats;

    private final Map<String, Float> dodgeValues;
    private final Map<String, Integer> dodgeCosts;

    private Settings settings = null;

    // This should mirror Kalentire API
    public static final String DODGE_TICKS = "DODGE_TICKS";

    public static final String POISE = "POISE";

    public static final String IMPACT = "IMPACT";

    public static final String STAMINA_DRAIN = "HEROES_STAMINA_DRAIN";

    public KalentireHandler(final CombatEnhanced plugin) {
        stats = new HashMap<>();
        this.plugin = plugin;

        dodgeValues = new HashMap<>();
        dodgeValues.put("LIGHT_ARMOUR", 0.9F);
        dodgeValues.put("MEDIUM_ARMOUR", 0.75F);
        dodgeValues.put("HEAVY_ARMOUR", 0.6F);

        dodgeCosts = new HashMap<>();
        dodgeCosts.put("LIGHT_ARMOUR", 10);
        dodgeCosts.put("MEDIUM_ARMOUR", 15);
        dodgeCosts.put("HEAVY_ARMOUR", 20);

        plugin.getServer().getPluginManager().registerEvents(new KalentireListener(this), plugin);
    }

    public void removePlayer(Player player) {
        this.stats.remove(player);
    }

    public CharacterManager getManager() {
        if(manager == null) {
            manager = Heroes.getInstance().getCharacterManager();
        }
        return manager;
    }

    public Settings getSettings() {
        if(settings == null) {
            settings = plugin.getModule(CombatManager.class).getSettings();
        }
        return settings;
    }

    public float parseDodgeStrength(final ItemStack stack) {
        if (stack == null) {
            return 1.0F;
        }
        final NBTItem item = NBTItem.get(stack);
        final Float val = dodgeValues.get(item.getString("MMOITEMS_ITEM_ID"));
        return val == null ? 1.0F : val;
    }

    public int parseDodgeCost(final ItemStack stack) {
        if (stack == null) {
            return getSettings().dodgeStaminaCost;
        }
        final NBTItem item = NBTItem.get(stack);
        final Integer val = dodgeCosts.get(item.getString("MMOITEMS_ITEM_ID"));
        return val == null ? getSettings().dodgeStaminaCost : val;
    }

    @Override
    public void reload() {
        stats.clear();
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
        return getStats(player).getDodgeStrength();
    }

    @Override
    public int getDodgeCost(Player player) {
        return getStats(player).getDodgeCost();
    }

    @Override
    public long getShieldCooldown(final Player player) {
        // todo
        return 500 / 50;
    }

    @NotNull
    protected EquipmentStats getStats(final Player player) {
        return Objects.requireNonNull(stats.get(player), "Player stats should have been initialised already!");
    }

    protected void loadStats(final Player player) {
        if (stats.containsKey(player)) {
            return;
        }
        stats.put(player, new EquipmentStats(player));
    }

    public class EquipmentStats {
        private final Map<EquipmentType, AtomicDouble> dodgeFactors;
        private final Map<EquipmentType, AtomicInteger> dodgeCosts;

        public EquipmentStats(final Player player) {
            dodgeFactors = new HashMap<>();
            dodgeCosts = new HashMap<>();

            final PlayerInventory inventory = player.getInventory();

            dodgeFactors.put(EquipmentType.HELMET, new AtomicDouble(parseDodgeStrength(inventory.getItem(EquipmentSlot.HEAD))));
            dodgeFactors.put(EquipmentType.CHESTPLATE, new AtomicDouble(parseDodgeStrength(inventory.getItem(EquipmentSlot.CHEST))));
            dodgeFactors.put(EquipmentType.LEGGINGS, new AtomicDouble(parseDodgeStrength(inventory.getItem(EquipmentSlot.LEGS))));
            dodgeFactors.put(EquipmentType.BOOTS, new AtomicDouble(parseDodgeStrength(inventory.getItem(EquipmentSlot.FEET))));

            dodgeCosts.put(EquipmentType.HELMET, new AtomicInteger(parseDodgeCost(inventory.getItem(EquipmentSlot.HEAD))));
            dodgeCosts.put(EquipmentType.CHESTPLATE, new AtomicInteger(parseDodgeCost(inventory.getItem(EquipmentSlot.CHEST))));
            dodgeCosts.put(EquipmentType.LEGGINGS, new AtomicInteger(parseDodgeCost(inventory.getItem(EquipmentSlot.LEGS))));
            dodgeCosts.put(EquipmentType.BOOTS, new AtomicInteger(parseDodgeCost(inventory.getItem(EquipmentSlot.FEET))));
        }

        public void setDodgeFactor(final EquipmentType type, final float factor) {
            dodgeFactors.get(type).set(factor);
        }

        public void setDodgeCost(final EquipmentType type, final int cost) {
            dodgeCosts.get(type).setRelease(cost);
        }

        public float getDodgeStrength() {
            double dodge = 0.0;
            for (final AtomicDouble factor : dodgeFactors.values()) {
                dodge += factor.get();
            }
            dodge /= dodgeFactors.size();
            return (float) dodge;
        }

        public int getDodgeCost() {
            double dodge = 0.0;
            for (final AtomicInteger factor : dodgeCosts.values()) {
                dodge += factor.get();
            }
            dodge /= dodgeCosts.size();
            return (int) dodge;
        }
    }

}
