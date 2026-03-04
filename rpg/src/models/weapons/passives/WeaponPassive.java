package models.weapons.passives;

import java.util.Random;

import models.weapons.Weapon;
import models.weapons.passives.HitContext.Phase;

/**
 * Passiu d'arma flexible per fases.
 *
 * <p>
 * Cada mètode és opcional (default), i només s'executa quan el pipeline
 * del combat entra a la fase corresponent.
 * </p>
 *
 * <p>
 * Retorna un missatge opcional per mostrar al log; {@code null} o buit = no mostrar.
 * </p>
 */
public interface WeaponPassive {

    /**
     * Dispatcher central de fases.
     *
     * <p>
     * Permet que {@link Weapon} executi passius amb una única crida.
     * </p>
     */
    default String onPhase(Weapon weapon, HitContext ctx, Random rng, Phase phase) {
        return switch (phase) {
            case START_TURN -> startTurn(weapon, ctx, rng);
            case BEFORE_ATTACK -> beforeAttack(weapon, ctx, rng);
            case MODIFY_DAMAGE -> modifyDamage(weapon, ctx, rng);
            case BEFORE_DEFENSE -> beforeDefense(weapon, ctx, rng);
            case AFTER_DEFENSE -> afterDefense(weapon, ctx, rng);
            case AFTER_HIT -> afterHit(weapon, ctx, rng);
            case END_TURN -> endTurn(weapon, ctx, rng);
        };
    }

    /** Al inici del torn. */
    default String startTurn(Weapon weapon, HitContext ctx, Random rng) { return null; }

    /** Abans de calcular/modificar el dany (ideal per setMeta, checks, etc.). */
    default String beforeAttack(Weapon weapon, HitContext ctx, Random rng) { return null; }

    /** Per modificar el dany abans de defensar (ctx.addFlatDamage / ctx.multiplyDamage). */
    default String modifyDamage(Weapon weapon, HitContext ctx, Random rng) { return null; }

    /** Abans d'aplicar DEFEND/DODGE (p.ex. "si l'objectiu defensa, ..."). */
    default String beforeDefense(Weapon weapon, HitContext ctx, Random rng) { return null; }

    /** Després de DEFEND/DODGE, però abans del log final (ctx.defenderResult ja existeix). */
    default String afterDefense(Weapon weapon, HitContext ctx, Random rng) { return null; }

    /** Després d'un impacte real (ctx.damageDealt > 0). */
    default String afterHit(Weapon weapon, HitContext ctx, Random rng) { return null; }

    /** Final del torn. */
    default String endTurn(Weapon weapon, HitContext ctx, Random rng) { return null; }
}