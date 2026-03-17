package models.breeds;

import models.characters.Character;

public class Gnome extends Character {
    public Gnome(String name, int age, int[] stats) {
        super(name, age, stats, Breed.GNOME);
    }

    private static final double MANA_BONUS = 1.15;
    
    @Override
    public void regen() {
        stats.reg(1, MANA_BONUS);
    }
}
