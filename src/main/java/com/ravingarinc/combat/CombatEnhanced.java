package com.ravingarinc.combat;

import com.ravingarinc.api.I;
import com.ravingarinc.api.module.RavinPluginJava;
import com.ravingarinc.combat.api.AsyncHandler;
import com.ravingarinc.combat.character.CharacterListener;
import com.ravingarinc.combat.character.CharacterManager;
import com.ravingarinc.combat.combat.CombatListener;
import com.ravingarinc.combat.combat.CombatManager;
import com.ravingarinc.combat.command.ReloadCommand;
import com.ravingarinc.combat.compatibility.RPGHandler;
import com.ravingarinc.combat.compatibility.RPGWrapper;
import com.ravingarinc.combat.file.Properties;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public final class CombatEnhanced extends RavinPluginJava {

    private static CombatEnhanced instance;

    private RPGWrapper handler;

    /**
     * Expects a message where %s will be replaced by the provided terms
     *
     * @param level        The log level
     * @param message      The message
     * @param replacements The replacements
     */
    public static void log(final Level level, final String message, final Object... replacements) {
        I.log(level, message, replacements);
    }

    public static void log(final Level level, final String message, @Nullable final Throwable throwable, final Object... replacements) {
        I.log(level, message, throwable, replacements);
    }

    public static void log(final Level level, final String message, final Throwable throwable) {
        I.log(level, message, throwable);
    }

    public static CombatEnhanced getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        //setup mmoitems here
        CombatEnhanced.instance = this;
        super.onLoad();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        AsyncHandler.load(this);
        super.onEnable();
    }

    @Override
    public void loadCommands() {
        new ReloadCommand(this).register();
    }


    @Override
    public void loadModules() {

        addModule(Properties.class);
        addModule(CharacterManager.class);
        addModule(CombatManager.class);

        // add listeners
        addModule(CombatListener.class);
        addModule(CharacterListener.class);

        addModule(RPGHandler.class);
    }

    public RPGWrapper getRPGHandler() {
        return handler;
    }
}
