package models.weapons;

import static models.weapons.Target.*;

import java.util.Random;

import models.characters.Statistics;

/**
 * Habilitats especials relacionades amb armes.
 */
public class Skills {
    private Skills() {}

    /**
     * Dispar explosiu: pot impactar a l'enemic o, segons la sort, a un mateix.
     *
     * @param weapon arma usada per calcular el dany base i crítics
     * @param stats  estadístiques (s'utilitza la sort per a la probabilitat d'autodispar)
     * @param rng    generador aleatori
     * @return resultat de l'atac amb dany, missatge i objectiu (si s'escau)
     */
    public static AttackResult explosiveShot(Weapon weapon, Statistics stats, Random rng) {
        double base = weapon.basicAttack(stats, rng);

        // Multiplicadors
        double selfMultiplier = 0.50;  // reducció si et dispares a tu mateix
        double enemyMultiplier = 1.10; // bonus si encertes l'enemic

        int luck = stats.getLuck();

        // Probabilitat d'autodispar influenciada per la sort
        double selfShotProb = 0.22 - 0.0047 * luck;
        selfShotProb = Math.clamp(selfShotProb, 0.08, 0.22);

        boolean selfShot = rng.nextDouble() < selfShotProb;

        double finalDamage = base * (selfShot ? selfMultiplier : enemyMultiplier);

        boolean crit = weapon.lastWasCritic();
        if (selfShot) {
            if (crit) {
                return new AttackResult(
                        finalDamage,
                        "s'ha pegat un tir crític a sí mateix. (crec que alla no era)",
                        SELF
                );
            }
            return new AttackResult(finalDamage, "s'ha pegat un tir a sí mateix. (crec que alla no era)", SELF);
        }

        if (crit) {
            return new AttackResult(finalDamage, "ha pegat un tir crític.");
        }

        return new AttackResult(finalDamage, "ha pegat un tir.");
    }
}