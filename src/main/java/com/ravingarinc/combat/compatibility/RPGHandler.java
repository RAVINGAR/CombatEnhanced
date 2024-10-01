package com.ravingarinc.combat.compatibility;

import com.ravingarinc.api.module.Module;
import com.ravingarinc.api.module.RavinPlugin;
import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.compatibility.kalentire.KalentireWrapper;
import com.ravingarinc.combat.file.Properties;
import org.bukkit.entity.Player;

public class RPGHandler extends Module implements RPGWrapper {
    private RPGWrapper wrapper;

    public RPGHandler(RavinPlugin plugin) {
        super(RPGHandler.class, plugin);
    }

    @Override
    public void load() {
        if (plugin.getServer().getPluginManager().getPlugin("KalentireRPG") != null) {
            wrapper = new KalentireWrapper((CombatEnhanced) plugin);
        } else if (plugin.getServer().getPluginManager().getPlugin("MMOItems") != null) {
            wrapper = new MMOWrapper(plugin);
        } else {
            wrapper = new DefaultWrapper(plugin);
        }
        wrapper.load();
    }

    @Override
    public void cancel() {
        wrapper.cancel();
    }

    @Override
    public boolean tryRemoveStamina(Player player, int amount) {
        return wrapper.tryRemoveStamina(player, amount);
    }

    @Override
    public boolean tryRemoveMana(Player player, int amount) {
        return wrapper.tryRemoveMana(player, amount);
    }

    @Override
    public float getDodgeStrength(Player player) {
        return wrapper.getDodgeStrength(player);
    }

    @Override
    public int getDodgeCost(Player player) {
        return wrapper.getDodgeCost(player);
    }

    @Override
    public long getShieldCooldown(Player player) {
        return wrapper.getShieldCooldown(player);
    }

    @Override
    public long getDodgeDuration(Player player) {
        return wrapper.getDodgeDuration(player);
    }

    @Override
    public Properties getProperties() {
        return plugin.getModule(Properties.class);
    }
}
