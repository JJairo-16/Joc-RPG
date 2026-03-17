package models.breeds;

import models.characters.Character;

public class Dwarf extends Character {
    public Dwarf(String name, int age, int[] stats) {
        super(name, age, stats, Breed.DWARF);
    }

    private static final double HP_BONUS = 1.15;

    @Override
    public void regen() {
        stats.reg(HP_BONUS, 1);
    }
}
