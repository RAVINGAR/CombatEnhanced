package com.ravingarinc.eldenrhym.character;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.Manager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CharacterManager extends Manager {
    private final Map<UUID, CharacterPlayer> playerMap;
    private final Map<UUID, CharacterMonster> monsterMap;

    public CharacterManager(final EldenRhym plugin) {
        super(CharacterManager.class, plugin);
        this.playerMap = new HashMap<>();
        this.monsterMap = new HashMap<>();
    }

    /**
     * Gets the CharacterEntity associated with this entity or creates one if there is none.
     *
     * @param entity The entity
     * @return The CharacterEntity object
     */
    @Async.Execute
    @NotNull
    public CharacterPlayer getPlayer(final Player entity) {
        final UUID uuid = entity.getUniqueId();
        CharacterPlayer player = playerMap.get(uuid);
        if (player == null) {
            player = new CharacterPlayer(plugin, entity);
            playerMap.put(uuid, player);
        }
        return player;
    }

    /**
     * Gets the CharacterEntity associated with this entity or creates one if there is none.
     *
     * @param entity The entity
     * @return The CharacterEntity object
     */
    @Async.Execute
    @NotNull
    public CharacterMonster getMonster(final Monster entity) {
        final UUID uuid = entity.getUniqueId();
        CharacterMonster monster = monsterMap.get(uuid);
        if (monster == null) {
            monster = new CharacterMonster(plugin, entity);
            monsterMap.put(uuid, monster);
        }
        return monster;
    }

    /**
     * Get the CharacterEntity associated with this LivingEntity only if it is either a Monster or a Player.
     * Otherwise an empty optional will be returned.
     *
     * @param entity The entity
     * @return An optional which may or may not contain a CharacterEntity
     */
    @Async.Execute
    public Optional<CharacterEntity<?>> getCharacter(final LivingEntity entity) {
        if (entity instanceof Player player) {
            return Optional.of(getPlayer(player));
        }
        if (entity instanceof Monster monster) {
            return Optional.of(getMonster(monster));
        }
        return Optional.empty();
    }

    @Override
    protected void reload() {

    }

    @Override
    protected void load() {

    }

    @Override
    public void shutdown() {

    }

    private class MonsterReaper implements Runnable {

        public MonsterReaper() {
        }

        @Override
        public void run() {

        }
    }
}
