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
 *
 * <p>
 * Aquesta classe continua encapsulant el comportament bàsic de l'arma, però
 * ara exposa també la configuració del crític perquè el sistema de combat
 * la pugui portar al {@link HitContext} i permetre que passives i efectes la
 * modifiquin abans de resoldre el cop final.
 * </p>
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

    // Estat intern informatiu de l'últim atac generat per l'arma.
    private boolean lastWasCrit = false;
    private double lastAttackDamage = 0;
    private double lastNonCriticalDamage = 0;

    /**
     * Crea una nova arma.
     *
     * @param name           nom de l'arma
     * @param description    descripció de l'arma
     * @param damage         dany base intern
     * @param criticalProb   probabilitat base de crític
     * @param criticalDamage multiplicador base de crític
     * @param type           tipus d'arma
     * @param attack         comportament d'atac
     * @param price          cost de mana
     * @param passives       passives associades
     * @param id             identificador d'arsenal
     */
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
        this.passives = (passives == null) ? List.of() : passives;
        this.id = id;
    }

    // ── Getters ──────────────────────────────────────────────────

    /**
     * @return nom de l'arma
     */
    public String getName() {
        return name;
    }

    /**
     * @return descripció de l'arma
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return dany base intern de l'arma
     */
    public int getBaseDamage() {
        return damage;
    }

    /**
     * @return probabilitat base de crític
     */
    public double getCriticalProb() {
        return criticalProb;
    }

    /**
     * @return multiplicador base de crític
     */
    public double getCriticalDamage() {
        return criticalDamage;
    }

    /**
     * @return tipus de l'arma
     */
    public WeaponType getType() {
        return type;
    }

    /**
     * @return cost de mana de l'atac especial
     */
    public double getManaPrice() {
        return manaPrice;
    }

    /**
     * @return identificador de catàleg de l'arma
     */
    public Arsenal getId() {
        return id;
    }

    // ── Lògica d'atac ────────────────────────────────────────────

    /**
     * Executa l'atac especial de l'arma si hi ha mana suficient.
     *
     * <p>
     * Si no n'hi ha, fa un atac físic bàsic de fallback.
     * </p>
     *
     * @param stats estadístiques de l'usuari
     * @param rng   generador aleatori
     * @return resultat base de l'atac
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
     * Calcula el dany base de l'arma aplicant variància i, per compatibilitat amb
     * les {@code Skills} actuals, també resol el crític intern de l'arma.
     *
     * <p>
     * Tot i això, el sistema de combat pot reconstruir el dany previ al crític i
     * tornar a resoldre'l dins el {@link HitContext} per donar flexibilitat a les
     * passives i efectes.
     * </p>
     *
     * @param stats estadístiques del personatge
     * @param rng   generador aleatori
     * @return dany de l'atac calculat per l'arma
     */
    public double basicAttack(Statistics stats, Random rng) {
        double baseDamage = type.getBasicDamage(damage, stats);
        baseDamage *= damageVariance(rng);
        baseDamage = round2(baseDamage);

        lastNonCriticalDamage = baseDamage;
        lastWasCrit = rollsCritical(stats, rng);

        if (!lastWasCrit) {
            lastAttackDamage = baseDamage;
            return baseDamage;
        }

        double multiplier = resolveCriticalMultiplier(stats);
        lastAttackDamage = round2(baseDamage * multiplier);
        return lastAttackDamage;
    }

    /**
     * Igual que {@link #basicAttack(Statistics, Random)} però retornant també un
     * missatge.
     *
     * @param stats estadístiques del personatge
     * @param rng   generador aleatori
     * @return resultat d'atac amb missatge
     */
    public AttackResult basicAttackWithMessage(Statistics stats, Random rng) {
        double dmg = basicAttack(stats, rng);
        return lastWasCrit
                ? new AttackResult(dmg, "llença un cop crític.")
                : new AttackResult(dmg, "llença un atac.");
    }

    /**
     * Retorna la probabilitat efectiva de crític segons l'arma i les estadístiques.
     *
     * @param stats estadístiques del personatge
     * @return probabilitat de crític ja escalada
     */
    public double resolveCriticalChance(Statistics stats) {
        double probTotal = criticalProb + (stats.getLuck() * 0.002);
        return Math.clamp(probTotal, 0.0, 0.95);
    }

    /**
     * Retorna el multiplicador efectiu de crític segons l'arma i les estadístiques.
     *
     * @param stats estadístiques del personatge
     * @return multiplicador efectiu de crític
     */
    public double resolveCriticalMultiplier(Statistics stats) {
        return Math.max(1.0, criticalDamage + stats.getLuck() * 0.01);
    }

    /**
     * Retorna el dany no crític de l'últim atac calculat per l'arma.
     *
     * @return dany base no crític de l'últim atac
     */
    public double lastNonCriticalDamage() {
        return lastNonCriticalDamage;
    }

    /**
     * Retorna si l'últim atac calculat per l'arma va sortir crític.
     *
     * @return {@code true} si l'últim atac intern de l'arma va ser crític
     */
    public boolean lastWasCritic() {
        return lastWasCrit;
    }

    /**
     * Retorna el dany final de l'últim atac calculat per l'arma.
     *
     * @return dany final de l'últim atac
     */
    public double lastAttackDamage() {
        return lastAttackDamage;
    }

    /**
     * Permet que el sistema de combat actualitzi l'estat informatiu final de l'arma
     * després de resoldre el crític real del {@link HitContext}.
     *
     * @param wasCrit     resultat final de crític
     * @param finalDamage dany final resolt per aquest cop
     */
    public void registerResolvedAttack(boolean wasCrit, double finalDamage) {
        this.lastWasCrit = wasCrit;
        this.lastAttackDamage = round2(finalDamage);
    }

    /**
     * Retorna si el tipus d'arma es pot equipar amb aquestes estadístiques.
     *
     * @param stats estadístiques a comprovar
     * @return {@code true} si es pot equipar
     */
    public boolean canEquip(Statistics stats) {
        return type.canEquip(stats);
    }

    /**
     * Activa els passius en una fase concreta.
     *
     * @param ctx   context del cop
     * @param rng   generador aleatori
     * @param phase fase del pipeline
     * @return missatges produïts pels passius
     */
    public List<String> triggerPhase(HitContext ctx, Random rng, Phase phase) {
        if (passives == null || passives.isEmpty()) {
            return List.of();
        }

        List<String> messages = new ArrayList<>();
        triggerPhase(ctx, rng, phase, messages);
        return messages;
    }

    /**
     * Activa els passius en una fase concreta i escriu els missatges a la sortida
     * indicada.
     *
     * @param ctx   context del cop
     * @param rng   generador aleatori
     * @param phase fase del pipeline
     * @param out   col·lecció de missatges de sortida
     */
    public void triggerPhase(HitContext ctx, Random rng, Phase phase, List<String> out) {
        if (passives == null || passives.isEmpty()) {
            return;
        }

        for (WeaponPassive p : passives) {
            String msg = p.onPhase(this, ctx, rng, phase);
            if (msg != null && !msg.isBlank()) {
                out.add(msg);
            }
        }
    }

    /**
     * Compatibilitat amb l'API antiga: executa la fase {@link Phase#AFTER_HIT}.
     *
     * @param ctx context del cop
     * @param rng generador aleatori
     * @param out llista de sortida
     */
    public void triggerAfterHit(HitContext ctx, Random rng, List<String> out) {
        triggerPhase(ctx, rng, Phase.AFTER_HIT, out);
    }

    /**
     * Compatibilitat amb l'API antiga: executa la fase {@link Phase#AFTER_HIT}.
     *
     * @param ctx context del cop
     * @param rng generador aleatori
     * @return missatges produïts
     */
    public List<String> triggerAfterHit(HitContext ctx, Random rng) {
        return triggerPhase(ctx, rng, Phase.AFTER_HIT);
    }

    // ── Helpers interns ──────────────────────────────────────────

    private static final double DAMAGE_VARIANCE = 0.07;
    private static final double DOWN_VARIANCE = 1.0 - DAMAGE_VARIANCE;
    private static final double UP_VARIANCE = DAMAGE_VARIANCE * 2.0;

    /**
     * Calcula la variància del dany.
     *
     * @param rng generador aleatori
     * @return factor de variància
     */
    private double damageVariance(Random rng) {
        double roll = (rng.nextDouble() + rng.nextDouble()) / 2.0;
        return DOWN_VARIANCE + roll * UP_VARIANCE;
    }

    /**
     * Decideix si l'atac és crític segons probabilitat base i sort.
     *
     * @param stats estadístiques del personatge
     * @param rng   generador aleatori
     * @return {@code true} si el cop surt crític
     */
    private boolean rollsCritical(Statistics stats, Random rng) {
        return rng.nextDouble() < resolveCriticalChance(stats);
    }

    /**
     * Arrodoneix a 2 decimals.
     *
     * @param n valor
     * @return valor arrodonit
     */
    private double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}