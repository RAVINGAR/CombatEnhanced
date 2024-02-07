package com.ravingarinc.combat.command;

import com.ravingarinc.combat.CombatEnhanced;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        CombatEnhanced.getInstance().reload();
        return true;
    }
}
