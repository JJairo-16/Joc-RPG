package models.weapons;

import java.util.Random;

import models.characters.Statistics;

public class Weapon {
    private final String name;

    private final int damage;
    private final double criticalProb;
    private final double criticalDamage;

    private final WeaponType type;
    private final Attack attack;

    private boolean lastWasCrit = false;

    public Weapon(String name, int damage, double criticalProb, double criticalDamage, WeaponType type, Attack attack) {
        this.name = name;
        this.damage = damage;
        this.criticalProb = criticalProb;
        this.criticalDamage = criticalDamage;
        this.type = type;
        this.attack = attack;
    }

    public String getName() {
        return name;
    }

    public AttackResult attack(Statistics stats, Random rng) {
        return attack.execute(this, stats, rng);
    }

    public double basicAttack(Statistics stats, Random rng) {
        double baseDamage = type.getBasicDamage(damage, stats);
        lastWasCrit = throwCriticism(stats, rng);

        if (!lastWasCrit) return baseDamage;

        double multiplier = criticalDamage + stats.getLuck() * 0.01;

        return round2(baseDamage * multiplier);
    }

    public AttackResult basicAttackWithMessage(Statistics stats, Random rng) {
        double dmg = basicAttack(stats, rng);
        if (lastWasCrit) {
            return new AttackResult(dmg, "Llença un cop crític.");
        }

        return new AttackResult(dmg, "Llença un atac.");
    }

    private boolean throwCriticism(Statistics stats, Random rng) {
        double probTotal = criticalProb + (stats.getLuck() * 0.002);
        probTotal = Math.clamp(probTotal, 0.0, 0.95);
        return rng.nextDouble() < probTotal;
    }

    private double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    public boolean lastWasCritic() {
        return lastWasCrit;
    }
}
