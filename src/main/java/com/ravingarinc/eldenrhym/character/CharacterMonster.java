package com.ravingarinc.eldenrhym.character;

import com.ravingarinc.eldenrhym.EldenRhym;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;

import java.util.Optional;

public class CharacterMonster extends CharacterEntity<Monster> {

    protected CharacterMonster(final EldenRhym plugin, final Monster entity) {
        super(plugin, entity);
    }

    @Override
    public Optional<LivingEntity> getTarget(final double maxDistance) {
        final LivingEntity target = entity.getTarget();
        if (target != null && target.getLocation().distance(entity.getLocation()) < maxDistance) {
            return Optional.of(target);
        }
        return Optional.empty();
    }
}
