package models.weapons.passives;

import java.util.Random;

import models.weapons.Weapon;

/**
 * Efecte passiu que s'executa després d'un impacte.
 */
@FunctionalInterface
public interface WeaponPassive {

    /**
     * Callback executat després de resoldre un hit.
     *
     * @param weapon arma que aplica el passiu
     * @param ctx context de l'impacte (participants i resultats)
     * @param rng font d'aleatorietat per a procs/variacions
     */
    
    String afterHit(Weapon weapon, HitContext ctx, Random rng);
}