package com.ravingarinc.eldenrhym.combat.runner;

import com.ravingarinc.eldenrhym.character.CharacterEntity;
import com.ravingarinc.eldenrhym.combat.event.CombatEvent;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.ConcurrentSkipListSet;

@NotThreadSafe
public class CombatRunner extends EventRunner<CombatEvent<CharacterEntity<?>>> {
    public CombatRunner() {
        super(new ConcurrentSkipListSet<>((o1, o2) -> (int) (o1.getExpireTime() - o2.getExpireTime())));
    }
}
