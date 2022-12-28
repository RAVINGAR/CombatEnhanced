package com.ravingarinc.eldenrhym.character;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.ModuleListener;
import com.ravingarinc.eldenrhym.combat.CombatManager;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CharacterListener extends ModuleListener {
    private CharacterManager characterManager;
    private CombatManager combatManager;

    public CharacterListener(final EldenRhym plugin) {
        super(CharacterListener.class, plugin, CharacterManager.class, CombatManager.class);
    }

    @Override
    protected void load() {
        characterManager = plugin.getModule(CharacterManager.class);
        combatManager = plugin.getModule(CombatManager.class);
        super.load();
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        characterManager.getPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent event) {
        combatManager.clearEntity(event.getPlayer().getUniqueId());
        characterManager.removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onEntityDespawn(final EntityDeathEvent event) {
        if (event.getEntity() instanceof Monster monster) {
            combatManager.clearEntity(monster.getUniqueId());
            characterManager.queueForRemoval(monster);
        }
    }
}
