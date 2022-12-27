package com.ravingarinc.eldenrhym.command;

import com.ravingarinc.eldenrhym.EldenRhym;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        EldenRhym.getInstance().reload();
        return true;
    }
}
