package models.weapons;

import java.util.Random;

import models.characters.Statistics;

/**
 * Representa una arma amb dany base, probabilitat/multiplicador de crític i un atac associat.
 */
public class Weapon {

    private final String name;
    private final String description;

    private final int damage;
    private final double criticalProb;
    private final double criticalDamage;

    private final WeaponType type;
    private final Attack attack;
    private final double manaPrice;

    private boolean lastWasCrit = false;

    public Weapon(
            String name,
            String description,
            int damage,
            double criticalProb,
            double criticalDamage,
            WeaponType type,
            Attack attack,
            double price
    ) {
        this.name = name;
        this.description = description;
        this.damage = damage;
        this.criticalProb = criticalProb;
        this.criticalDamage = criticalDamage;
        this.type = type;
        this.attack = attack;
        this.manaPrice = price;
    }

    public String getName() {
        return name;
    }

    /**
     * Executa l'atac de l'arma si hi ha mana suficient; si no, fa un atac físic bàsic.
     */
    public AttackResult attack(Statistics stats, Random rng) {
        if (stats.getMana() < manaPrice) {
            return new AttackResult(
                    WeaponType.PHYSICAL.getBasicDamage(5, stats),
                    "no li quedava mana, aixi que li dona un cop."
            );
        }

        return attack.execute(this, stats, rng);
    }

    /**
     * Calcula el dany base aplicant (si toca) el crític i deixa registrat l'últim resultat.
     */
    public double basicAttack(Statistics stats, Random rng) {
        double baseDamage = type.getBasicDamage(damage, stats);
        lastWasCrit = throwCriticism(stats, rng);

        if (!lastWasCrit) return baseDamage;

        double multiplier = criticalDamage + stats.getLuck() * 0.01;
        return round2(baseDamage * multiplier);
    }

    /**
     * Com {@link #basicAttack(Statistics, Random)} però retornant també un missatge.
     */
    public AttackResult basicAttackWithMessage(Statistics stats, Random rng) {
        double dmg = basicAttack(stats, rng);
        if (lastWasCrit) {
            return new AttackResult(dmg, "Llença un cop crític.");
        }
        return new AttackResult(dmg, "Llença un atac.");
    }

    /**
     * Decideix si l'atac és crític segons probabilitat base i sort, amb límit màxim.
     */
    private boolean throwCriticism(Statistics stats, Random rng) {
        double probTotal = criticalProb + (stats.getLuck() * 0.002);
        probTotal = Math.clamp(probTotal, 0.0, 0.95);
        return rng.nextDouble() < probTotal;
    }

    /** Arrodoneix a 2 decimals. */
    private double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    public boolean lastWasCritic() {
        return lastWasCrit;
    }

    /** Retorna si el tipus d'arma es pot equipar amb aquestes estadístiques. */
    public boolean canEquip(Statistics stats) {
        return type.canEquip(stats);
    }

    public String getDescription() {
        return description;
    }

    public int getBaseDamage() {
        return damage;
    }

    public double getCriticalProb() {
        return criticalProb;
    }

    public double getCriticalDamage() {
        return criticalDamage;
    }

    public WeaponType getType() {
        return type;
    }

    public double getManaPrice() {
        return manaPrice;
    }
}