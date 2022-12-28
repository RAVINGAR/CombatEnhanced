package com.ravingarinc.eldenrhym.character;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.Module;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CharacterManager extends Module {
    private final Map<UUID, CharacterPlayer> playerMap;
    private final Map<UUID, CharacterMonster> monsterMap;
    private final BukkitScheduler scheduler;
    private EntityReaper reaper;

    public CharacterManager(final EldenRhym plugin) {
        super(CharacterManager.class, plugin);
        this.playerMap = new ConcurrentHashMap<>();
        this.monsterMap = new ConcurrentHashMap<>();
        this.scheduler = plugin.getServer().getScheduler();
    }

    /**
     * Gets the CharacterEntity associated with this entity or creates one if there is none.
     *
     * @param entity The entity
     * @return The CharacterEntity object
     */
    @Async.Execute
    @NotNull
    public CharacterPlayer getPlayer(@NotNull final Player entity) {
        final UUID uuid = entity.getUniqueId();
        CharacterPlayer player = playerMap.get(uuid);
        if (player == null) {
            player = new CharacterPlayer(plugin, entity);
            playerMap.put(uuid, player);
        }
        return player;
    }

    public void removePlayer(@NotNull final Player entity) {
        scheduler.runTaskAsynchronously(plugin, () -> playerMap.remove(entity.getUniqueId()));
    }

    public void removeMonster(@NotNull final Monster entity) {
        scheduler.runTaskAsynchronously(plugin, () -> monsterMap.remove(entity.getUniqueId()));
    }

    public void removeEntity(@NotNull final LivingEntity entity) {
        if (entity instanceof Player player) {
            removePlayer(player);
        } else if (entity instanceof Monster monster) {
            removeMonster(monster);
        }
    }

    /**
     * Gets the CharacterEntity associated with this entity or creates one if there is none.
     *
     * @param entity The entity
     * @return The CharacterEntity object
     */
    @Async.Execute
    @NotNull
    public CharacterMonster getMonster(@NotNull final Monster entity) {
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
    public Optional<CharacterEntity<?>> getCharacter(@Nullable final LivingEntity entity) {
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
        reaper.cancel();

        final List<LivingEntity> loadedEntities = new ArrayList<>();
        playerMap.forEach((uuid, character) -> loadedEntities.add(character.getEntity()));
        monsterMap.forEach((uuid, character) -> loadedEntities.add(character.getEntity()));

        playerMap.clear();
        monsterMap.clear();

        loadedEntities.forEach(entity -> {
            if (entity instanceof Player player) {
                getPlayer(player);
            } else if (entity instanceof Monster monster) {
                if (!entity.isDead() && entity.isValid()) {
                    getMonster(monster);
                }
            }
        });
    }

    @Override
    protected void load() {
        reaper = new EntityReaper();
        reaper.runTaskTimer(plugin, 20L, 100L);
    }

    @Override
    protected void shutdown() {

    }

    public void queueForRemoval(final LivingEntity entity) {
        if (reaper != null) {
            this.reaper.queueForRemoval(entity);
        }
    }

    private class EntityReaper extends BukkitRunnable {
        private final Queue<LivingEntity> toRemove;

        private EntityReaper() {
            toRemove = new ConcurrentLinkedQueue<>();
        }

        public void queueForRemoval(final LivingEntity entity) {
            toRemove.add(entity);
        }

        @Override
        public void run() {
            monsterMap.values().forEach(entity -> {
                final LivingEntity livingEntity = entity.getEntity();
                if (!livingEntity.isValid()) {
                    toRemove.add(livingEntity);
                }
            });
            new ArrayList<>(toRemove).forEach(CharacterManager.this::removeEntity);
        }
    }
}
