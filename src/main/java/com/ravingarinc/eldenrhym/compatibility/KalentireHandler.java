package com.ravingarinc.eldenrhym.compatibility;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.Hero;
import org.bukkit.entity.Player;

public class KalentireHandler implements RPGHandler {
    private final Heroes heroes;
    private final CharacterManager manager;

    public KalentireHandler() {
        heroes = Heroes.getInstance();
        manager = heroes.getCharacterManager();
    }

    @Override
    public boolean tryRemoveStamina(final Player player, final int amount) {
        final Hero hero = manager.getHero(player);
        final int stamina = hero.getStamina();
        if (stamina >= amount) {
            hero.setStamina(stamina - amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean tryRemoveMana(final Player player, final int amount) {
        final Hero hero = manager.getHero(player);
        final int mana = hero.getMana();
        if (mana >= amount) {
            hero.setMana(mana - amount);
            return true;
        }
        return false;
    }

    @Override
    public float getDodgeStrength(final Player player) {
        // todo
        return 1.0F;
    }
}
