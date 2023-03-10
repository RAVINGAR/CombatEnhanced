package com.ravingarinc.combat.character;

import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.api.AsyncHandler;
import com.ravingarinc.combat.api.AsynchronousException;
import org.bukkit.entity.Monster;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;

import java.util.Optional;

public class CharacterMonster extends CharacterEntity<Monster> {

    protected CharacterMonster(final CombatEnhanced plugin, final Monster entity) {
        super(plugin, entity);
    }

    @Override
    @Async.Execute
    @Blocking
    public Optional<CharacterEntity<?>> getTarget(final double maxDistance) throws AsynchronousException {
        final Optional<CharacterEntity<?>> optTarget = characterManager.getCharacter(AsyncHandler.executeBlockingSyncComputation(entity::getTarget));
        if (optTarget.isPresent()) {
            final CharacterEntity<?> target = optTarget.get();
            if (target.getLocation().distance(this.getLocation()) < maxDistance) {
                return Optional.of(target);
            }
        }
        return Optional.empty();
    }
}
