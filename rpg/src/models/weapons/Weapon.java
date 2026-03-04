package models.weapons;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import models.characters.Statistics;
import models.weapons.passives.HitContext;
import models.weapons.passives.WeaponPassive;

/**
 * Representa una arma amb dany base, probabilitat/multiplicador de crític,
 * un atac associat i una llista de passius que s'activen després d'un hit.
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

    private final List<WeaponPassive> passives;

    // Estat intern per informar del resultat de l'últim atac bàsic.
    private boolean lastWasCrit = false;
    private double lastAttackDamage = 0;

    public Weapon(
            String name,
            String description,
            int damage,
            double criticalProb,
            double criticalDamage,
            WeaponType type,
            Attack attack,
            double price,
            List<WeaponPassive> passives) {
        this.name = name;
        this.description = description;

        this.damage = damage;
        this.criticalProb = criticalProb;
        this.criticalDamage = criticalDamage;

        this.type = type;
        this.attack = attack;
        this.manaPrice = price;

        // Si no hi ha passius, fem servir una llista immutable buida per evitar nulls.
        this.passives = (passives == null) ? List.of() : passives;
    }

    // --- Accés bàsic (getters) ---

    public String getName() {
        return name;
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

    // --- Lògica d'atac ---

    /**
     * Executa l'atac especial de l'arma si hi ha mana suficient.
     * Si no n'hi ha, fa un atac físic bàsic.
     */
    public AttackResult attack(Statistics stats, Random rng) {
        if (stats.getMana() < manaPrice) {
            return new AttackResult(
                    WeaponType.PHYSICAL.getBasicDamage(5, stats),
                    "no li quedava mana, aixi que li dona un cop.");
        }
        return attack.execute(this, stats, rng);
    }

    /**
     * Calcula el dany base aplicant (si toca) el crític i deixa registrat l'últim
     * resultat.
     */
    public double basicAttack(Statistics stats, Random rng) {
        double baseDamage = type.getBasicDamage(damage, stats);
        lastWasCrit = rollsCritical(stats, rng);

        if (!lastWasCrit) {
            lastAttackDamage = baseDamage;
            return baseDamage;
        }

        // El crític augmenta amb el multiplicador base + una petita escala amb la sort.
        double multiplier = criticalDamage + stats.getLuck() * 0.01;
        lastAttackDamage = round2(baseDamage * multiplier);
        return lastAttackDamage;
    }

    /**
     * Com {@link #basicAttack(Statistics, Random)} però retornant també un
     * missatge.
     */
    public AttackResult basicAttackWithMessage(Statistics stats, Random rng) {
        double dmg = basicAttack(stats, rng);
        return lastWasCrit
                ? new AttackResult(dmg, "Llença un cop crític.")
                : new AttackResult(dmg, "Llença un atac.");
    }

    public boolean lastWasCritic() {
        return lastWasCrit;
    }

    public double lastAttackDamage() {
        return lastAttackDamage;
    }

    /** Retorna si el tipus d'arma es pot equipar amb aquestes estadístiques. */
    public boolean canEquip(Statistics stats) {
        return type.canEquip(stats);
    }

    /**
     * Activa tots els passius després d'un hit, en l'ordre en què estan a la
     * llista.
     */
    public List<String> triggerAfterHit(HitContext ctx, Random rng) {

        if (passives == null || passives.isEmpty())
            return List.of();

        List<String> messages = new ArrayList<>();

        for (WeaponPassive p : passives) {
            String msg = p.afterHit(this, ctx, rng);
            if (msg != null && !msg.isBlank()) {
                messages.add(msg);
            }
        }

        return messages;
    }

    // --- Helpers interns ---

    /**
     * Decideix si l'atac és crític segons probabilitat base i sort, amb límit
     * màxim.
     */
    private boolean rollsCritical(Statistics stats, Random rng) {
        double probTotal = criticalProb + (stats.getLuck() * 0.002);
        probTotal = Math.clamp(probTotal, 0.0, 0.95);
        return rng.nextDouble() < probTotal;
    }

    /** Arrodoneix a 2 decimals. */
    private double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}