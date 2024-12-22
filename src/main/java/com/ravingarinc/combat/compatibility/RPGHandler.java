package com.ravingarinc.combat.compatibility;

import com.ravingarinc.api.module.Module;
import com.ravingarinc.api.module.RavinPlugin;
import com.ravingarinc.combat.compatibility.kalentire.KalentireWrapper;

public class RPGHandler extends Module {
    private RPGWrapper wrapper;

    public RPGHandler(RavinPlugin plugin) {
        super(RPGHandler.class, plugin);
    }

    @Override
    public void load() {
        if (plugin.getServer().getPluginManager().getPlugin("KalentireRPG") != null) {
            wrapper = new KalentireWrapper(plugin);
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

    public RPGWrapper getWrapper() {
        return wrapper;
    }
}
