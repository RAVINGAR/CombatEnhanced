package com.ravingarinc.combat.combat.runner;

import com.herocraftonline.heroes.api.events.HeroesDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.ravingarinc.api.module.RavinPlugin;
import com.ravingarinc.combat.api.BukkitApi;
import com.ravingarinc.combat.character.CharacterEntity;
import com.ravingarinc.combat.character.CharacterManager;
import com.ravingarinc.combat.combat.event.DamageEvent;
import com.ravingarinc.combat.compatibility.kalentire.KalentireWrapper;
import com.ravingarinc.combat.compatibility.kalentire.effect.PoiseImmunityEffect;
import com.ravingarinc.combat.compatibility.kalentire.effect.PoiseStunEffect;
import com.ravingarinc.combat.file.Properties;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PoiseRunner extends BukkitRunnable {
    private final RavinPlugin plugin;

    private final KalentireWrapper handler;
    private final CharacterManager characterManager;
    private final Properties settings;

    private final List<DamageEvent> toRemove = new LinkedList<>();

    private final ConcurrentHashMap<UUID, Collection<DamageEvent>> mappedEvents;

    public PoiseRunner(RavinPlugin plugin, KalentireWrapper handler) {
        this.plugin = plugin;
        this.handler = handler;
        this.characterManager = plugin.getModule(CharacterManager.class);
        this.settings = plugin.getModule(Properties.class);
        this.mappedEvents = new ConcurrentHashMap<>(512);
    }

    @Override
    public void run() {
        mappedEvents.forEach((key, value) -> value.forEach(event -> {
            if (event.call()) {
                toRemove.add(event);
            }
        }));
        final var iterator = toRemove.iterator();
        while(iterator.hasNext()) {
            remove(iterator.next());
        }
        toRemove.clear();
    }

    public void addPoiseDamage(CharacterEntity<?> entity, final double poiseDamage) {
        final var set = mappedEvents.computeIfAbsent(entity.getUniqueId(), u -> ConcurrentHashMap.newKeySet());
        set.add(new DamageEvent(entity, poiseDamage, System.currentTimeMillis(), settings.poiseWindow));
    }

    @BukkitApi
    public void handle(final HeroesDamageEvent event, final double damage) {
        final CharacterTemplate character = event.getDefender();
        final var opt = characterManager.getCharacter(character.getEntity());
        if(opt.isEmpty()) {
            return;
        }
        if(character.hasEffect(PoiseStunEffect.EFFECT_NAME)) {
            return;
        }
        final var entity = opt.get();
        final var set = mappedEvents.computeIfAbsent(character.getUUID(), u -> ConcurrentHashMap.newKeySet());
        final var impact = KalentireWrapper.getImpact(event.getAttacker().getEntity());
        set.add(new DamageEvent(entity, damage + impact, System.currentTimeMillis(), settings.poiseWindow));
        if(character.hasEffect(PoiseImmunityEffect.EFFECT_NAME)) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var poiseDamage = 0.0;
            for (DamageEvent damageEvent : set) {
                poiseDamage += (damageEvent.getDamage() * damageEvent.getTimeFactor());
                // the time factor basically decreases the value of the damage based on how close it is to expiry!
            }
            final var damageOverPoise = poiseDamage - (handler.getPoise(entity.getEntity()));
            if(damageOverPoise <= 0) {
                return; // Still below poise
            }
            // todo the issue is that if a player was blocking all this damage, if they were to suddenly unblock and
            //  then hit with another attack  that might even be small in scale, this would cause it to instantly
            //  cause a stun lock since the higher damage with a shield whilst blocking temporarily increased tolerance.
            final var effect = new PoiseStunEffect(
                    event.getAttacker().getEntity(),
                    Math.min(settings.minStunDuration + (int)(damageOverPoise / settings.stunThreshold) * settings.stunDurationPerThreshold, settings.maxStunDuration),
                    settings.stunCooldown,
                    damageOverPoise * settings.vulnerabilityPerPoise);
            Bukkit.getScheduler().runTask(plugin, () -> character.addEffect(effect));
        });
    }

    public void remove(@NotNull DamageEvent event) {
        final var events = mappedEvents.get(event.getCharacter().getEntity().getUniqueId());
        if(events != null) {
            events.remove(event);
        }
    }

    public void removeAll(@NotNull final UUID uuid) {
        final var set = mappedEvents.remove(uuid);
        if(set != null) {
            set.clear();
        }
    }
}
