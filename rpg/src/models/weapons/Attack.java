package models.weapons;

import java.util.Random;

import models.characters.Statistics;

@FunctionalInterface
public interface Attack {
    AttackResult execute(Weapon weapon, Statistics stats, Random rng);
}
