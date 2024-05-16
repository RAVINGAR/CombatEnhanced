package com.ravingarinc.combat.combat.event;

import com.ravingarinc.combat.api.AsynchronousException;
import com.ravingarinc.combat.character.CharacterPlayer;
import com.ravingarinc.combat.file.Settings;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import org.bukkit.Material;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;

/*
We can make this an abstract class later if we want to allow for monsters to block also
 */
public class PlayerBlockEvent extends CombatEvent<CharacterPlayer> {
    protected Settings settings;

    public PlayerBlockEvent(final CharacterPlayer entity, final long start, final Settings settings) {
        super(entity, start, settings.blockDuration);
        this.settings = settings;
    }

    @Override
    @Async.Execute
    @Blocking
    public void tick() throws AsynchronousException {
        if (System.currentTimeMillis() > getExpireTime()) {
            interrupt();
        }
        if(!entity.isBlocking()) {
            final var recovery = (int) MMOPlayerData.get(entity.getUniqueId()).getStatMap().getStat("BLOCK_RECOVERY");
            entity.getEntity().setCooldown(Material.SHIELD, recovery);
            interrupt();
        }
    }
}
