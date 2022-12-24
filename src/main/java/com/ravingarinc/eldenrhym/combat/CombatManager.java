package com.ravingarinc.eldenrhym.combat;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.AsynchronousException;
import com.ravingarinc.eldenrhym.api.Module;
import com.ravingarinc.eldenrhym.character.CharacterEntity;
import com.ravingarinc.eldenrhym.character.CharacterManager;
import com.ravingarinc.eldenrhym.combat.event.DodgeEvent;
import com.ravingarinc.eldenrhym.combat.event.PlayerBlockEvent;
import com.ravingarinc.eldenrhym.combat.event.TargetDodgeEvent;
import com.ravingarinc.eldenrhym.combat.runner.CombatRunner;
import com.ravingarinc.eldenrhym.combat.runner.IdentifierRunner;
import com.ravingarinc.eldenrhym.file.ConfigManager;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages combat interactions and computations. Any methods marked with Async.Execute means that the method is
 * called from an asynchronous thread.
 */
@ThreadSafe
public class CombatManager extends Module {
    private static final long PERIOD = 5L;
    private final Settings settings;
    private final BukkitScheduler scheduler;
    private CharacterManager characterManager;
    private CombatRunner runner;
    private IdentifierRunner<PlayerBlockEvent> blockRunner;

    private IdentifierRunner<DodgeEvent> dodgeRunner;

    public CombatManager(final EldenRhym plugin) {
        super(CombatManager.class, plugin, ConfigManager.class, CharacterManager.class);
        this.settings = new Settings();
        this.scheduler = plugin.getServer().getScheduler();
    }

    public boolean isBlocking(final UUID uuid) {
        return blockRunner.get(uuid).isPresent();
    }

    public boolean isDodging(final UUID uuid) {
        return dodgeRunner.get(uuid).map(DodgeEvent::isDodging).orElse(false);
    }

    public void queueBlockEvent(@NotNull final Player entity) {
        final long time = System.currentTimeMillis();
        scheduler.runTaskAsynchronously(plugin, () -> blockRunner.add(new PlayerBlockEvent(characterManager.getPlayer(entity), time, settings.blockDuration)));
        EldenRhym.logIfDebug(() -> "Queued block event for " + entity.getName());
    }

    public void queueDodgeEvent(@NotNull final Player entity) {
        final long time = System.currentTimeMillis();
        scheduler.runTaskAsynchronously(plugin, () -> queueDodgeEvent(characterManager.getPlayer(entity), time));
    }

    public void queueDodgeEvent(@NotNull final Monster entity) {
        final long time = System.currentTimeMillis();
        scheduler.runTaskAsynchronously(plugin, () -> queueDodgeEvent(characterManager.getMonster(entity), time));
    }

    @Async.Execute
    private void queueDodgeEvent(final CharacterEntity<?> character, final long start) {
        try {
            character.getTarget(8)
                    .ifPresentOrElse((target) ->
                                    dodgeRunner.add(new TargetDodgeEvent(character, target, start, settings.dodgeWarmup, settings.dodgeDuration, settings.dodgeStrength)),
                            () -> dodgeRunner.add(new DodgeEvent(character, start, settings.dodgeWarmup, settings.dodgeDuration, settings.dodgeStrength)));
        } catch (final AsynchronousException e) {
            EldenRhym.log(Level.SEVERE, "Encountered issue adding dodge event!", e);
        }
    }


    @Override
    protected void reload() {
        runner.cancel();
        dodgeRunner.cancel();
        blockRunner.cancel();
    }

    @Override
    protected void load() {
        characterManager = plugin.getModule(CharacterManager.class);
        runner = new CombatRunner();
        dodgeRunner = new IdentifierRunner<>();
        blockRunner = new IdentifierRunner<>();

        runner.runTaskTimerAsynchronously(plugin, 0, PERIOD);
        dodgeRunner.runTaskTimerAsynchronously(plugin, 5, PERIOD);
        blockRunner.runTaskTimerAsynchronously(plugin, 5, PERIOD);
    }

    @Override
    protected void shutdown() {
        dodgeRunner.cancel();
        blockRunner.cancel();
        runner.cancel();
    }

    public Settings getSettings() {
        return settings;
    }

    public static class Settings {
        public boolean counterEnabled = true;
        public long counterStartTime = 100;
        public long counterEndTime = 500;

        public boolean parryEnabled = true;

        public boolean dodgeEnabled = true;
        public long dodgeWarmup = 100;
        public long dodgeDuration = 300;
        public float dodgeStrength = 0.5F;
        public double dodgeMitigation = 0.5;

        public boolean blockEnabled = true;
        public long blockDuration = 600;
        public double blockMitigation = 1.0;
    }
}
