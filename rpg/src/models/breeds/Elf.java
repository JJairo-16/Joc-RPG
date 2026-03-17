package models.breeds;

import models.characters.Character;

public class Elf extends Character {
    public Elf(String name, int age, int[] stats) {
        super(name, age, stats, Breed.ELF);
    }

    private static final double DODGE_LUCK = 0.15;
    
    @Override
    protected double tryToDodge() {
        return super.tryToDodge() * (1 + DODGE_LUCK);
    }
}
