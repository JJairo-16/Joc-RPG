package models.weapons;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import models.characters.Statistics;
import models.weapons.passives.HitContext;
import models.weapons.passives.HitContext.Phase;
import models.weapons.passives.WeaponPassive;

/**
 * Representa una arma amb dany base, probabilitat/multiplicador de crític,
 * un atac associat i una llista de passius que s'activen per fases.
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

    private final Arsenal id;

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
            List<WeaponPassive> passives,
        Arsenal id) {
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
        this.id = id;
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
        baseDamage *= damageVariance(rng);
        lastWasCrit = rollsCritical(stats, rng);

        if (!lastWasCrit) {
            baseDamage = round2(baseDamage);
            lastAttackDamage = baseDamage;
            return baseDamage;
        }

        // El crític augmenta amb el multiplicador base + una petita escala amb la sort.
        double multiplier = criticalDamage + stats.getLuck() * 0.01;
        lastAttackDamage = round2(baseDamage * multiplier);
        return lastAttackDamage;
    }

    private static final double DAMAGE_VARIANCE = 0.05;
    private static final double DOWN_VARIANCE = 1.0 - DAMAGE_VARIANCE;
    private static final double UP_VARIANCE = DAMAGE_VARIANCE * 2;

    private double damageVariance(Random rng) {
        return DOWN_VARIANCE + rng.nextDouble() * UP_VARIANCE;
    }

    /**
     * Com {@link #basicAttack(Statistics, Random)} però retornant també un
     * missatge.
     */
    public AttackResult basicAttackWithMessage(Statistics stats, Random rng) {
        double dmg = basicAttack(stats, rng);
        return lastWasCrit
                ? new AttackResult(dmg, "llença un cop crític.")
                : new AttackResult(dmg, "llença un atac.");
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
     * Activa els passius en una fase concreta.
     *
     * <p>
     * Aquesta és la nova API recomanada. Permet passius per fases i no força
     * el model "afterHit" com a únic punt d'entrada.
     * </p>
     */
    public List<String> triggerPhase(HitContext ctx, Random rng, Phase phase) {
        if (passives == null || passives.isEmpty())
            return List.of();

        List<String> messages = new ArrayList<>();
        triggerPhase(ctx, rng, phase, messages);

        return messages;
    }

    /**
     * Activa els passius en una fase concreta.
     *
     * <p>
     * Aquesta és la nova API recomanada. Permet passius per fases i no força
     * el model "afterHit" com a únic punt d'entrada.
     * </p>
     */
    public void triggerPhase(HitContext ctx, Random rng, Phase phase, List<String> out) {
        if (passives == null || passives.isEmpty())
            return;

        for (WeaponPassive p : passives) {
            String msg = p.onPhase(this, ctx, rng, phase);
            if (msg != null && !msg.isBlank()) {
                out.add(msg);
            }
        }
    }

    /**
     * Compatibilitat amb l'API antiga: executa la fase {@link Phase#AFTER_HIT}.
     */
    public void triggerAfterHit(HitContext ctx, Random rng, List<String> out) {
        triggerPhase(ctx, rng, Phase.AFTER_HIT, out);
    }

    /**
     * Compatibilitat amb l'API antiga: executa la fase {@link Phase#AFTER_HIT}.
     */
    public List<String> triggerAfterHit(HitContext ctx, Random rng) {
        return triggerPhase(ctx, rng, Phase.AFTER_HIT);
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

    public Arsenal getId() {
        return id;
    }
}