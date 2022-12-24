package com.ravingarinc.eldenrhym.combat.event;

import com.ravingarinc.eldenrhym.api.AsynchronousException;
import com.ravingarinc.eldenrhym.character.CharacterPlayer;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;

/*
We can make this an abstract class later if we want to allow for monsters to block also
 */
public class PlayerBlockEvent extends CombatEvent<CharacterPlayer> {
    public PlayerBlockEvent(final CharacterPlayer entity, final long start, final long duration) {
        super(entity, start, duration);
    }

    @Override
    @Async.Execute
    @Blocking
    public void tick() throws AsynchronousException {
        if (System.currentTimeMillis() > getExpireTime() || !entity.isBlocking()) {
            interrupt();
        }
    }
}
