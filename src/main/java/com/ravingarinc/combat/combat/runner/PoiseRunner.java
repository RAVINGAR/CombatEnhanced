package com.ravingarinc.combat.combat.runner;

import com.herocraftonline.heroes.api.events.HeroesDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.api.BukkitApi;
import com.ravingarinc.combat.character.CharacterManager;
import com.ravingarinc.combat.combat.event.DamageEvent;
import com.ravingarinc.combat.compatibility.kalentire.KalentireHandler;
import com.ravingarinc.combat.compatibility.kalentire.effect.PoiseStunEffect;
import com.ravingarinc.combat.compatibility.kalentire.effect.PoiseImmunityEffect;
import com.ravingarinc.combat.file.Settings;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PoiseRunner extends BukkitRunnable {
    private final CombatEnhanced plugin;

    private final KalentireHandler handler;
    private final CharacterManager characterManager;
    private final Settings settings;

    private final List<DamageEvent> toRemove = new LinkedList<>();

    private final ConcurrentHashMap<UUID, Collection<DamageEvent>> mappedEvents;
    public PoiseRunner(CombatEnhanced plugin, KalentireHandler handler) {
        this.plugin = plugin;
        this.handler = handler;
        this.characterManager = plugin.getModule(CharacterManager.class);
        this.settings = plugin.getRPGHandler().getSettings();
        this.mappedEvents = new ConcurrentHashMap<>(512);
    }

    @Override
    public void run() {
        mappedEvents.entrySet().parallelStream().forEach((entry) -> {
            entry.getValue().parallelStream().forEach(event -> {
                if (event.call()) {
                    toRemove.add(event);
                }
            });

        });
        if (!toRemove.isEmpty()) {
            toRemove.forEach(this::remove);
            toRemove.clear();
        }
    }

    @BukkitApi
    public void handle(final HeroesDamageEvent event) {
        final CharacterTemplate character = event.getDefender();
        final var opt = characterManager.getCharacter(character.getEntity());
        if(opt.isEmpty()) {
            return;
        }
        if(character.hasEffect(PoiseStunEffect.EFFECT_NAME)) {
            return;
        }
        final var entity = opt.get();
        final var set = mappedEvents.computeIfAbsent(entity.getEntity().getUniqueId(), u -> ConcurrentHashMap.newKeySet());
        final var impact = KalentireHandler.getImpact(entity.getEntity());
        set.add(new DamageEvent(entity, event.getDamage() + impact, System.currentTimeMillis(), settings));
        if(character.hasEffect(PoiseImmunityEffect.EFFECT_NAME)) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final var poiseDamage = set.stream().mapToDouble(DamageEvent::getDamage).sum();
            final var damageOverPoise = poiseDamage - (handler.getPoise(entity.getEntity()));
            if(damageOverPoise <= 0) {
                return; // Still below poise
            }
            character.addEffect(new PoiseStunEffect(event.getAttacker().getEntity(), (int)(damageOverPoise / settings.stunThreshold) * settings.stunDurationPerThreshold, settings.stunCooldown, damageOverPoise * settings.vulnerabilityPerPoise));
            // Now we do the fun stuff -> Applying a stun event
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
