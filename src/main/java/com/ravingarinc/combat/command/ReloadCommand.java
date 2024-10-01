package com.ravingarinc.combat.command;

import com.ravingarinc.api.command.BaseCommand;
import com.ravingarinc.api.module.RavinPlugin;
import com.ravingarinc.combat.CombatEnhanced;

public class ReloadCommand extends BaseCommand {
    public ReloadCommand(RavinPlugin plugin) {
        super(plugin, "combatreload", "combat.enhanced");

        setFunction((sender, args) -> {
            CombatEnhanced.getInstance().reload();
            sender.sendMessage("Plugin has been reloaded!");
            return true;
        });
    }
}
