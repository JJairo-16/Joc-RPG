package models.breeds;

import models.characters.Character;
import models.weapons.AttackResult;

public class Tiefling extends Character {
    public Tiefling(String name, int age, int[] stats) {
        super(name, age, stats, Breed.TIEFLING);
    }

    private static final double DOUBLE_ATTACK_PROB = 0.05;

    @Override
    public AttackResult attack() {
        AttackResult attack = super.attack();
        if (rng.nextDouble() < DOUBLE_ATTACK_PROB)
            return new AttackResult(attack.damage() * 2.0, attack.message(), attack.target());
        
        return attack;
    }
}
