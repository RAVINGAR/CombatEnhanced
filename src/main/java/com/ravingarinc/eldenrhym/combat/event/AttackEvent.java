package com.ravingarinc.eldenrhym.combat.event;

import com.ravingarinc.eldenrhym.api.AsynchronousException;
import com.ravingarinc.eldenrhym.character.CharacterEntity;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;

public class AttackEvent extends CombatEvent<CharacterEntity<?>> {
    public AttackEvent(final long start, final long duration) {
        super(null, start, duration);

    }

    @Override
    @Async.Execute
    @Blocking
    public void tick() throws AsynchronousException {
        //check here if is blocking.
    }
}
